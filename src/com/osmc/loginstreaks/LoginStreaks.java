package com.osmc.loginstreaks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class LoginStreaks extends JavaPlugin {

    Logger logger = Bukkit.getLogger();
    private LoginStreakConfig config;
    private EssentialsHook essentialsHook;
    private StreakManager streakManager;
    private LoginStreaksEvents events;

    @Override
    public void onEnable() {
        logger.info("LoginStreaks has been enabled!");

        // Initialize Essentials hook
        essentialsHook = new EssentialsHook(this);

        // Load configuration
        config = new LoginStreakConfig(this);
        config.load(); // This actually creates the config file

        // Initialize streak manager
        streakManager = new StreakManager(this, config, essentialsHook);

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
        // Cleanup resources here if needed
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
}
