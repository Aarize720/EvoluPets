package fr.aarize.evolupets.managers;

import fr.aarize.evolupets.EvoluPets;
import fr.aarize.evolupets.models.PetType;
import fr.aarize.evolupets.models.PlayerPet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {

    private final EvoluPets plugin;
    private final String TITLE;

    public GUIManager(EvoluPets plugin) {
        this.plugin = plugin;
        this.TITLE = plugin.getConfig().getString("gui.title", "§eEvoluPets — Collection");
    }

    public void openMain(Player p) {
        int size = plugin.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, TITLE);
        int i = 0;
        for (PetType pt : PetType.values()) {
            inv.setItem(i++, buildPetItem(p.getUniqueId(), pt));
            if (i >= size) break;
        }
        for (int j = i; j < size; j++) if (inv.getItem(j) == null) inv.setItem(j, filler());
        p.openInventory(inv);
    }

    private ItemStack filler() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(" ");
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildPetItem(UUID uuid, PetType pt) {
        ItemStack it = new ItemStack(pt.getIcon());
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + pt.getDisplayName());

        List<String> lore = new ArrayList<>();
        Map<PetType, PlayerPet> col = plugin.getPetManager().getCollection(uuid);
        if (col.containsKey(pt)) {
            PlayerPet pp = col.get(pt);
            lore.add(ChatColor.GRAY + "Niveau: " + pp.getLevel());
            lore.add(ChatColor.GRAY + "XP: " + pp.getXp() + " / " + plugin.getPetManager().getXpNeededForNextLevel(uuid, pt));
            lore.add("");
            lore.add(ChatColor.GREEN + "Clic gauche: Activer");
            lore.add(ChatColor.BLUE + "Clic droit: Détails");
        } else {
            lore.add(ChatColor.GRAY + "Prix: " + (int)pt.getPrice() + " coins");
            lore.add("");
            lore.add(ChatColor.GOLD + "Clic gauche: Acheter");
        }
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getItemMeta() == null) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        PetType chosen = null;
        for (PetType pt : PetType.values()) if (pt.getDisplayName().equalsIgnoreCase(name)) { chosen = pt; break; }
        if (chosen == null) return;

        UUID uuid = p.getUniqueId();
        if (plugin.getPetManager().owns(uuid, chosen)) {
            if (e.isLeftClick()) {
                plugin.getPetManager().setActive(uuid, chosen);
                plugin.getPetManager().summon(uuid);
                p.sendMessage("§6[EvoluPets] §a" + chosen.getDisplayName() + " activé.");
                p.closeInventory();
            } else if (e.isRightClick()) {
                final PetType cf = chosen;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.closeInventory();
                    PlayerPet pp = plugin.getPetManager().getPlayerPet(uuid, cf);
                    p.sendMessage("§e--- " + cf.getDisplayName() + " ---");
                    p.sendMessage("§7Niveau: §f" + (pp == null ? 1 : pp.getLevel()));
                    p.sendMessage("§7XP: §f" + (pp == null ? 0 : pp.getXp()) + " / " + plugin.getPetManager().getXpNeededForNextLevel(uuid, cf));
                });
            }
        } else {
            if (e.isLeftClick()) {
                boolean ok = plugin.getPetManager().purchase(uuid, chosen, chosen.getPrice());
                if (ok) {
                    plugin.getPetManager().setActive(uuid, chosen);
                    plugin.getPetManager().summon(uuid);
                    p.sendMessage("§6[EvoluPets] §aAchat effectué : " + chosen.getDisplayName());
                    p.closeInventory();
                } else {
                    p.sendMessage("§c[EvoluPets] Fonds insuffisants ou Vault absent.");
                }
            }
        }
    }
}
