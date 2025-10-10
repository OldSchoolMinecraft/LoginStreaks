package com.osmc.loginstreaks;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.Event;

public class LoginStreaksEvents extends PlayerListener {

    private final LoginStreaks plugin;
    private final LoginStreakConfig config;
    private final StreakManager streakManager;

    public LoginStreaksEvents(LoginStreaks plugin, LoginStreakConfig config, StreakManager streakManager) {
        this.plugin = plugin;
        this.config = config;
        this.streakManager = streakManager;


        // Register player join event for b1.7.3
        plugin.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, this, Event.Priority.Normal, plugin);

    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delegate to streak manager
        streakManager.handlePlayerLogin(player);
    }
}
