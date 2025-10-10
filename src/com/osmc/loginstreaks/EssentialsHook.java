package com.osmc.loginstreaks;

import com.earth2me.essentials.Essentials;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

public class EssentialsHook extends ServerListener {

    private LoginStreaks plugin;
    private Essentials essentials;
    private boolean PluginLoaded;

    public EssentialsHook (LoginStreaks plugin){
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.PLUGIN_ENABLE, this, org.bukkit.event.Event.Priority.Normal, plugin);
        plugin.getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.PLUGIN_DISABLE, this, org.bukkit.event.Event.Priority.Normal, plugin);
        tryHook();
    }

    @Override
    public void onPluginEnable(PluginEnableEvent e) {
        if (essentials != null) return; // already hooked
        Plugin p = e.getPlugin();
        if (p instanceof com.earth2me.essentials.Essentials && p.isEnabled()) {
            essentials = (com.earth2me.essentials.Essentials) p;
            plugin.getServer().getLogger().info("[LoginStreaks] Essentials hooked.");
        }
    }

    @Override
    public void onPluginDisable(PluginDisableEvent e) {
        Plugin p = e.getPlugin();
        if (p == essentials) {
            essentials = null;
            plugin.getServer().getLogger().info("[LoginStreaks] Essentials unhooked.");
        }
    }

    public boolean isHooked() {
        return essentials != null && essentials.isEnabled();
    }

    public boolean canAfford(Player ply, double required) {
        return essentials.getUser(ply).canAfford(required);
    }

    public void takeMoney(Player ply, double amount) {
        essentials.getUser(ply).takeMoney(amount);
    }

    public double getBalance(Player ply) {
        return essentials.getUser(ply).getMoney();
    }

    public void giveMoney(Player ply, double amount) {
        essentials.getUser(ply).giveMoney(amount);
    }

    private void tryHook() {
        Plugin p = plugin.getServer().getPluginManager().getPlugin("Essentials");

        if (p instanceof Essentials && p.isEnabled()) {
            essentials = (Essentials) p;
            plugin.logger.info("Essentials detected and hooked (startup).");
        }

    }

}
