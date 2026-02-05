package com.osmc.loginstreaks;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.Event;

public class LoginStreaksEvents extends PlayerListener {

    private final LoginStreaks plugin;
    private final LoginStreakConfig config;
    private final StreakManager streakManager;
    private final StreakWarningManager warningManager;

    public LoginStreaksEvents(LoginStreaks plugin, LoginStreakConfig config, StreakManager streakManager, StreakWarningManager warningManager) {
        this.plugin = plugin;
        this.config = config;
        this.streakManager = streakManager;
        this.warningManager = warningManager;


        // Register player join event for b1.7.3
        plugin.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, this, Event.Priority.Normal, plugin);
        plugin.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, this, Event.Priority.Normal, plugin);
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Reset warning state for this player
        if (warningManager != null) {
            warningManager.onPlayerJoin(player.getName());
        }

        // Delegate to streak manager (commented out as in original)
        // streakManager.handlePlayerLogin(player);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up warning state for this player
        if (warningManager != null) {
            warningManager.onPlayerQuit(player.getName());
        }
    }
}
