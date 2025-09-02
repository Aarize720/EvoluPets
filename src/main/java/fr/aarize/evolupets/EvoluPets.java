package fr.aarize.evolupets;

import fr.aarize.evolupets.commands.PetCommand;
import fr.aarize.evolupets.listeners.PlayerListener;
import fr.aarize.evolupets.listeners.PetAiListener;
import fr.aarize.evolupets.managers.DataManager;
import fr.aarize.evolupets.managers.GUIManager;
import fr.aarize.evolupets.managers.PetManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EvoluPets extends JavaPlugin {

    private static EvoluPets instance;
    private DataManager dataManager;
    private PetManager petManager;
    private GUIManager guiManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Vault
        if (!setupEconomy()) {
            getLogger().warning("Vault or economy provider not found. Plugin will still run but purchases will fail.");
        }

        // managers
        this.dataManager = new DataManager(this);
        this.petManager = new PetManager(this);
        this.guiManager = new GUIManager(this);

        // commands & listeners
        getCommand("pet").setExecutor(new PetCommand(this));
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PetAiListener(this), this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);
        Bukkit.getPluginManager().registerEvents(petManager, this); // pet manager listens for follow/cleanup

        getLogger().info("[EvoluPets] enabled");
    }

    @Override
    public void onDisable() {
        if (petManager != null) petManager.saveAll();
        getLogger().info("[EvoluPets] disabled");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static EvoluPets getInstance() { return instance; }
    public DataManager getDataManager() { return dataManager; }
    public PetManager getPetManager() { return petManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public Economy getEconomy() { return economy; }
}
