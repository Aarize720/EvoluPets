package fr.aarize.evolupets.managers;

import fr.aarize.evolupets.EvoluPets;
import fr.aarize.evolupets.models.PetType;
import fr.aarize.evolupets.models.PlayerPet;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Sauvegarde par joueur dans plugins/EvoluPets/players/<uuid>.yml
 */
public class DataManager {

    private final EvoluPets plugin;
    private final File playersDir;

    public DataManager(EvoluPets plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) playersDir.mkdirs();
    }

    public FileConfiguration load(UUID uuid) {
        File f = new File(playersDir, uuid.toString() + ".yml");
        if (!f.exists()) {
            try { f.createNewFile(); } catch (IOException ignored) {}
        }
        return YamlConfiguration.loadConfiguration(f);
    }

    public void save(UUID uuid, FileConfiguration cfg) {
        File f = new File(playersDir, uuid.toString() + ".yml");
        try { cfg.save(f); } catch (IOException e) { plugin.getLogger().severe("Save failed: " + e.getMessage()); }
    }

    public Map<PetType, PlayerPet> loadCollection(UUID uuid) {
        FileConfiguration cfg = load(uuid);
        Map<PetType, PlayerPet> map = new HashMap<>();
        if (!cfg.isConfigurationSection("pets")) return map;
        for (String key : cfg.getConfigurationSection("pets").getKeys(false)) {
            PetType pt = PetType.fromKey(key);
            if (pt == null) continue;
            int lvl = cfg.getInt("pets." + key + ".level", 1);
            int xp  = cfg.getInt("pets." + key + ".xp", 0);
            map.put(pt, new PlayerPet(pt, lvl, xp));
        }
        return map;
    }

    public PetType loadActive(UUID uuid) {
        FileConfiguration cfg = load(uuid);
        String s = cfg.getString("active", null);
        return PetType.fromKey(s);
    }

    public void savePlayer(UUID uuid, Map<PetType, PlayerPet> collection, PetType active) {
        FileConfiguration cfg = load(uuid);
        cfg.set("pets", null);
        for (Map.Entry<PetType, PlayerPet> e : collection.entrySet()) {
            String key = e.getKey().getKey();
            cfg.set("pets." + key + ".level", e.getValue().getLevel());
            cfg.set("pets." + key + ".xp", e.getValue().getXp());
        }
        cfg.set("active", active == null ? null : active.getKey());
        save(uuid, cfg);
    }

    public UUID[] getAllPlayersWithData() {
        File[] files = playersDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return new UUID[0];
        List<UUID> list = new ArrayList<>();
        for (File f : files) {
            try { list.add(UUID.fromString(f.getName().replace(".yml", ""))); } catch (Exception ignored) {}
        }
        return list.toArray(new UUID[0]);
    }
}
