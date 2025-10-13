package com.osmc.loginstreaks;

import com.oldschoolminecraft.osas.impl.event.PlayerAuthenticationEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class OSASPoseidonListener extends CustomEventListener
{
    private StreakManager streakManager;

    public OSASPoseidonListener(StreakManager manager)
    {
        this.streakManager = manager;
    }

    public void onCustomEvent(Event event)
    {
        if (!(event instanceof PlayerAuthenticationEvent)) return;
        PlayerAuthenticationEvent authEvent = (PlayerAuthenticationEvent) event;
        UUID playerUUID = authEvent.getPlayer();

        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.getUniqueId().equals(playerUUID))
            {
                System.out.println("[LoginStreaks] Handling login for player: " + p.getName());
                streakManager.handlePlayerLogin(p);
                return;
            }
        }

        System.out.println("[LoginStreaks] Could not find player with UUID: " + playerUUID);
    }
}
