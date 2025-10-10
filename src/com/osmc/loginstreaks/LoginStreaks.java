package com.osmc.loginstreaks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class LoginStreaks extends JavaPlugin {

    Logger logger = Bukkit.getLogger();
    private LoginStreakConfig config;
    private DatabaseConfig databaseConfig;
    private EssentialsHook essentialsHook;
    private StreakManager streakManager;
    private LoginStreaksEvents events;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        logger.info("LoginStreaks has been enabled!");

        // Initialize Essentials hook
        essentialsHook = new EssentialsHook(this);

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

        // Initialize event handler
        events = new LoginStreaksEvents(this, config, streakManager);

        // Register commands
        CommandStreaks commandHandler = new CommandStreaks(this, streakManager, config);
        this.getCommand("loginstreak").setExecutor(commandHandler);

        logger.info("LoginStreaks initialization complete!");
    }

    @Override
    public void onDisable() {
        logger.info("LoginStreaks has been disabled!");

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
}
