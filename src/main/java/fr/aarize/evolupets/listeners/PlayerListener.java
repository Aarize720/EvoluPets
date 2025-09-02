package fr.aarize.evolupets.listeners;

import fr.aarize.evolupets.EvoluPets;
import fr.aarize.evolupets.managers.PetManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final EvoluPets plugin;

    public PlayerListener(EvoluPets plugin) { this.plugin = plugin; }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        Player killer = e.getEntity().getKiller();
        if (e.getEntityType() == EntityType.PLAYER) return; // no xp on players
        PetManager pm = plugin.getPetManager();
        UUID uuid = killer.getUniqueId();
        if (pm.getActive(uuid) == null) return;
        pm.addXpForKill(uuid, plugin.getConfig().getInt("xp.per-kill", 5));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getPetManager().dismiss(e.getPlayer().getUniqueId());
    }
}
