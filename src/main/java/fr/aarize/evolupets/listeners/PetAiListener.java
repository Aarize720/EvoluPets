package fr.aarize.evolupets.listeners;

import fr.aarize.evolupets.EvoluPets;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

public class PetAiListener implements Listener {

    private final EvoluPets plugin;

    public PetAiListener(EvoluPets plugin) { this.plugin = plugin; }

    // Prevent mobs from targeting pets
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() == null) return;
        if (plugin.getPetManager().isPet(e.getTarget())) e.setCancelled(true);
    }

    // Prevent pets from being damaged
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (plugin.getPetManager().isPet(e.getEntity())) e.setCancelled(true);
    }

    // Prevent pets from damaging players (safety)
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity target = e.getEntity();
        if (plugin.getPetManager().isPet(damager) && target instanceof Player) e.setCancelled(true);
        if (plugin.getPetManager().isPet(target)) e.setCancelled(true); // no damage to pet
    }
}
