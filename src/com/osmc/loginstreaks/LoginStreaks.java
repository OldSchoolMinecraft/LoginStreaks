package com.osmc.loginstreaks;

import com.oldschoolminecraft.OSMEss.OSMEss;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class LoginStreaks extends JavaPlugin {

    Logger logger = Bukkit.getLogger();
    private LoginStreakConfig config;
    private DatabaseConfig databaseConfig;
    private EssentialsHook essentialsHook;
    private StreakManager streakManager;
    private StreakWarningManager warningManager;
    private LoginStreaksEvents events;
    private DatabaseManager databaseManager;

    public OSMEss osmEss;

    @Override
    public void onEnable() {
        logger.info("LoginStreaks has been enabled!");

        // Initialize Essentials hook
        essentialsHook = new EssentialsHook(this);

        // Initialize OSM-Ess API (Soft Depend)
        if (getServer().getPluginManager().getPlugin("OSM-Ess") != null) {
            osmEss = (OSMEss) getServer().getPluginManager().getPlugin("OSM-Ess");
            Bukkit.getServer().getLogger().info("[LoginStreaks] OSM-Ess v" + osmEss.getDescription().getVersion() + " found!");
        }
        else {
            Bukkit.getServer().getLogger().severe("[LoginStreaks] OSM-Ess not found, thus its api functions are disabled!");
        }


        // Load main configuration
        config = new LoginStreakConfig(this);
        config.load();

        // Load database configuration
        databaseConfig = new DatabaseConfig(this);
        databaseConfig.load();

        // Initialize database manager if enabled
        databaseManager = new DatabaseManager(this);
        if (databaseConfig.isEnabled()) {
            databaseManager.initialize(
                databaseConfig.getHost(),
                databaseConfig.getPort(),
                databaseConfig.getName(),
                databaseConfig.getUsername(),
                databaseConfig.getPassword(),
                databaseConfig.useSSL()
            );
        }

        // Initialize streak manager
        streakManager = new StreakManager(this, config, essentialsHook, databaseManager);

        // Start cache refresh task
        streakManager.startCacheRefreshTask();

        // Initialize and start streak warning manager
        warningManager = new StreakWarningManager(this, streakManager, config);
        warningManager.start();

        // Initialize event handler
        events = new LoginStreaksEvents(this, config, streakManager, warningManager);

        getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, new OSASPoseidonListener(streakManager), Event.Priority.Normal, this);

        // Register commands
        CommandStreaks commandHandler = new CommandStreaks(this, streakManager, config);
        this.getCommand("loginstreak").setExecutor(commandHandler);

        CommandStreaksAdmin adminCommandHandler = new CommandStreaksAdmin(this, streakManager, config);
        this.getCommand("loginstreakadmin").setExecutor(adminCommandHandler);

        logger.info("LoginStreaks initialization complete!");
    }

    @Override
    public void onDisable() {
        logger.info("LoginStreaks has been disabled!");

        // Stop cache refresh task
        if (streakManager != null) {
            streakManager.stopCacheRefreshTask();
        }

        // Stop warning manager
        if (warningManager != null) {
            warningManager.stop();
        }

        // Disconnect from database if connected
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    // Public getters for other classes to access these components
    public LoginStreakConfig getStreakConfig() {
        return config;
    }

    public EssentialsHook getEssentialsHook() {
        return essentialsHook;
    }

    public boolean isOSMEssEnabled() {
        if (getServer().getPluginManager().getPlugin("OSM-Ess") != null && getServer().getPluginManager().isPluginEnabled("OSM-Ess")) return true;
        else return false;
    }

    public OSMEss getOSMEss() {
        if (isOSMEssEnabled()) {
            return osmEss;
        }
        else return null;
    }

    public StreakManager getStreakManager() {
        return streakManager;
    }

    public LoginStreaksEvents getEvents() {
        return events;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public StreakWarningManager getWarningManager() {
        return warningManager;
    }
}
