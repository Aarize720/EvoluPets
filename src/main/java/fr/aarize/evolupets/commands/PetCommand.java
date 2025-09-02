package fr.aarize.evolupets.commands;

import fr.aarize.evolupets.EvoluPets;
import fr.aarize.evolupets.models.PetType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PetCommand implements CommandExecutor {

    private final EvoluPets plugin;

    public PetCommand(EvoluPets plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("In-game only."); return true; }
        Player p = (Player) sender;

        if (args.length == 0) { plugin.getGuiManager().openMain(p); return true; }

        switch (args[0].toLowerCase()) {
            case "gui": plugin.getGuiManager().openMain(p); return true;
            case "summon": plugin.getPetManager().summon(p.getUniqueId()); p.sendMessage("Pet summoned."); return true;
            case "dismiss": plugin.getPetManager().dismiss(p.getUniqueId()); p.sendMessage("Pet dismissed."); return true;
            case "info":
                var pt = plugin.getPetManager().getActive(p.getUniqueId());
                if (pt == null) { p.sendMessage("No active pet."); return true; }
                var pp = plugin.getPetManager().getPlayerPet(p.getUniqueId(), pt);
                p.sendMessage("Active: " + pt.getDisplayName() + " lvl " + (pp == null ? 1 : pp.getLevel()));
                return true;
            case "balance":
                if (plugin.getEconomy() == null) { p.sendMessage("No economy provider."); return true; }
                double bal = plugin.getEconomy().getBalance(p);
                p.sendMessage("Balance: " + bal);
                return true;
            case "give":
                if (!p.hasPermission("evolupets.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length < 3) { p.sendMessage("Usage: /pet give <player> <petKey>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                PetType t = PetType.fromKey(args[2]);
                if (t == null) { p.sendMessage("Invalid pet key."); return true; }
                plugin.getPetManager().give(target.getUniqueId(), t);
                p.sendMessage("Given " + t.getDisplayName() + " to " + args[1]);
                return true;
            case "reload":
                if (!p.hasPermission("evolupets.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
                plugin.reloadConfig();
                p.sendMessage("Config reloaded.");
                return true;
            default:
                plugin.getGuiManager().openMain(p);
                return true;
        }
    }
}
