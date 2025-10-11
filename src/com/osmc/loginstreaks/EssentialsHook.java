package com.osmc.loginstreaks;

import com.earth2me.essentials.Essentials;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

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
        org.bukkit.plugin.Plugin p = e.getPlugin();
        if (p instanceof Essentials && p.isEnabled()) {
            essentials = (Essentials) p;
            plugin.getServer().getLogger().info("[LoginStreaks] Essentials hooked.");
        }
    }

    @Override
    public void onPluginDisable(PluginDisableEvent e) {
        org.bukkit.plugin.Plugin p = e.getPlugin();
        if (p == essentials) {
            essentials = null;
            plugin.getServer().getLogger().info("[LoginStreaks] Essentials unhooked.");
        }
    }

    public boolean isHooked() {
        return essentials != null && essentials.isEnabled();
    }

    public boolean canAfford(Player ply, double required) {
        if (!isHooked()) return false;
        return essentials.getUser(ply).getMoney() >= required;
    }

    public void takeMoney(Player ply, double amount) {
        if (!isHooked()) return;
        essentials.getUser(ply).takeMoney(amount);
    }

    public double getBalance(Player ply) {
        if (!isHooked()) return 0.0;
        return essentials.getUser(ply).getMoney();
    }

    public void giveMoney(Player ply, double amount) {
        if (!isHooked()) return;
        essentials.getUser(ply).giveMoney(amount);
    }

    private void tryHook() {
        org.bukkit.plugin.Plugin p = plugin.getServer().getPluginManager().getPlugin("Essentials");

        if (p instanceof Essentials && p.isEnabled()) {
            essentials = (Essentials) p;
            plugin.getServer().getLogger().info("[LoginStreaks] Essentials detected and hooked (startup).");
        }
    }
}
