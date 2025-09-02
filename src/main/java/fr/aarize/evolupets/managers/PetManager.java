package fr.aarize.evolupets.managers;

import fr.aarize.evolupets.EvoluPets;
import fr.aarize.evolupets.models.PetType;
import fr.aarize.evolupets.models.PlayerPet;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère collection joueurs, spawn / dismiss des pets, xp et follow.
 */
public class PetManager implements Listener {

    private final EvoluPets plugin;
    private final DataManager dataManager;

    // in-memory
    private final Map<UUID, Map<PetType, PlayerPet>> collections = new ConcurrentHashMap<>();
    private final Map<UUID, PetType> active = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> spawned = new ConcurrentHashMap<>();

    public PetManager(EvoluPets plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        loadAll();

        // follow task
        long interval = plugin.getConfig().getLong("follow.tick-interval", 40L);
        new BukkitRunnable() {
            @Override
            public void run() {
                double tpDist = plugin.getConfig().getDouble("follow.teleport-distance", 12.0);
                double runDist = plugin.getConfig().getDouble("follow.run-distance", 4.0);
                double speed = plugin.getConfig().getDouble("follow.run-speed", 0.30);

                for (UUID u : new ArrayList<>(spawned.keySet())) {
                    LivingEntity pet = spawned.get(u);
                    if (pet == null || pet.isDead()) {
                        spawned.remove(u);
                        continue;
                    }
                    Player p = plugin.getServer().getPlayer(u);
                    if (p == null || !p.isOnline()) {
                        pet.remove();
                        spawned.remove(u);
                        continue;
                    }

                    if (!pet.getWorld().equals(p.getWorld())) {
                        pet.teleport(p.getLocation().add(0.5, 0, 0.5));
                        continue;
                    }
                    double dist = pet.getLocation().distance(p.getLocation());
                    if (dist > tpDist) {
                        pet.teleport(p.getLocation().add(randomOffset(), 0, randomOffset()));
                    } else if (dist > runDist) {
                        pet.setVelocity(
                                p.getLocation().toVector()
                                        .subtract(pet.getLocation().toVector())
                                        .normalize()
                                        .multiply(speed)
                        );
                    } else {
                        // face player
                        try {
                            pet.teleport(pet.getLocation().setDirection(
                                    p.getLocation().toVector().subtract(pet.getLocation().toVector())
                            ));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private double randomOffset() {
        return (Math.random() - 0.5) * 2.0;
    }

    // ---------- Data ----------
    public void loadAll() {
        UUID[] uuids = dataManager.getAllPlayersWithData();
        for (UUID u : uuids) {
            Map<PetType, PlayerPet> col = dataManager.loadCollection(u);
            collections.put(u, col);
            PetType a = dataManager.loadActive(u);
            if (a != null) active.put(u, a);
        }
    }

    public void saveAll() {
        for (UUID u : collections.keySet()) {
            dataManager.savePlayer(u, collections.get(u), active.get(u));
        }
    }

    public Map<PetType, PlayerPet> getCollection(UUID uuid) {
        return collections.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }

    public PetType getActive(UUID uuid) {
        return active.get(uuid);
    }

    public PlayerPet getPlayerPet(UUID uuid, PetType type) {
        return getCollection(uuid).get(type);
    }

    public boolean owns(UUID uuid, PetType type) {
        return getCollection(uuid).containsKey(type);
    }

    // ---------- Purchase (Vault fallback) ----------
    public boolean purchase(UUID uuid, PetType type, double price) {
        Map<PetType, PlayerPet> col = getCollection(uuid);
        if (col.containsKey(type)) return false;

        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (plugin.getEconomy() != null) {
            if (!plugin.getEconomy().has(off, price)) return false;
            EconomyResponse r = plugin.getEconomy().withdrawPlayer(off, price);
            if (!r.transactionSuccess()) return false;
        } else {
            return false; // pas de Vault
        }

        col.put(type, new PlayerPet(type, 1, 0));
        dataManager.savePlayer(uuid, col, active.get(uuid));
        return true;
    }

    public void give(UUID uuid, PetType type) {
        Map<PetType, PlayerPet> col = getCollection(uuid);
        col.putIfAbsent(type, new PlayerPet(type, 1, 0));
        dataManager.savePlayer(uuid, col, active.get(uuid));
    }

    // ---------- Active / spawn ----------
    public void setActive(UUID uuid, PetType type) {
        if (!owns(uuid, type)) return;
        active.put(uuid, type);
        dataManager.savePlayer(uuid, getCollection(uuid), type);
    }

    public void summon(UUID uuid) {
        PetType pt = getActive(uuid);
        if (pt == null) return;
        dismiss(uuid);

        Player p = plugin.getServer().getPlayer(uuid);
        if (p == null || !p.isOnline()) return;

        Location loc = p.getLocation().add(1, 0, 0);
        EntityType et = pt.getEntityType();

        LivingEntity ent;
        try {
            ent = (LivingEntity) p.getWorld().spawnEntity(loc, et);
        } catch (Exception ex) {
            plugin.getLogger().warning("Impossible de spawn " + et + " pour " + p.getName());
            return;
        }

        // Mark & protect
        ent.addScoreboardTag("EVOLUPET");
        ent.addScoreboardTag("OWNER_" + uuid);
        ent.setCustomName("§d" + p.getName() + " §7— §e" + pt.getDisplayName() +
                " §7[§eLvl " + getPlayerLevelSafe(uuid, pt) + "§7]");
        ent.setCustomNameVisible(true);
        ent.setRemoveWhenFarAway(false);

        try {
            ent.setCollidable(false);
        } catch (NoSuchMethodError ignored) {}
        ent.setInvulnerable(true);

        // hp scaling
        PlayerPet pp = getPlayerPet(uuid, pt);
        int lvl = (pp == null ? 1 : pp.getLevel());
        double hp = 10 + lvl * 4;
        if (ent.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            ent.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
        }
        try {
            ent.setHealth(Math.min(hp, ent.getMaxHealth()));
        } catch (Throwable ignored) {}

        spawned.put(uuid, ent);
    }

    public void dismiss(UUID uuid) {
        LivingEntity ent = spawned.remove(uuid);
        if (ent != null && !ent.isDead()) ent.remove();
    }

    public void removeAllSpawned() {
        for (LivingEntity e : spawned.values()) {
            if (e != null && !e.isDead()) e.remove();
        }
        spawned.clear();
    }

    // ---------- XP ----------
    public void addXpForKill(UUID uuid, int amount) {
        PetType pt = getActive(uuid);
        if (pt == null) return;
        PlayerPet pp = getPlayerPet(uuid, pt);
        if (pp == null) return;

        pp.addXp(amount);
        boolean leveled = false;
        while (pp.getXp() >= xpNeeded(pp.getLevel())) {
            pp.setXp(pp.getXp() - xpNeeded(pp.getLevel()));
            pp.setLevel(pp.getLevel() + 1);
            leveled = true;
        }
        if (leveled) {
            if (spawned.containsKey(uuid)) summon(uuid); // refresh visuals
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§6[EvoluPets] §aTon pet est passé niveau " + pp.getLevel() + " !");
            }
        }
        dataManager.savePlayer(uuid, getCollection(uuid), active.get(uuid));
    }

    private int xpNeeded(int level) {
        double base = plugin.getConfig().getDouble("xp.base-unit", 100.0);
        double exp = plugin.getConfig().getDouble("xp.exponent", 1.2);
        return Math.max(1, (int) Math.round(base * Math.pow(level, exp)));
    }

    public int getXpNeededForNextLevel(UUID uuid, PetType type) {
        PlayerPet pp = getPlayerPet(uuid, type);
        return (pp == null ? xpNeeded(1) : xpNeeded(pp.getLevel()));
    }

    public int getPlayerLevelSafe(UUID uuid, PetType pt) {
        PlayerPet pp = getPlayerPet(uuid, pt);
        return (pp == null ? 1 : pp.getLevel());
    }

    // ---------- helpers ----------
    public boolean isPet(Entity e) {
        return e != null && e.getScoreboardTags().contains("EVOLUPET");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        spawned.entrySet().removeIf(en -> en.getValue() == null || en.getValue().isDead());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity entity : e.getChunk().getEntities()) {
            spawned.entrySet().removeIf(en -> {
                LivingEntity pet = en.getValue();
                return pet == null || pet.getUniqueId().equals(entity.getUniqueId());
            });
        }
    }
}
