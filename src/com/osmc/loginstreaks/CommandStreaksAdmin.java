package com.osmc.loginstreaks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStreaksAdmin implements CommandExecutor {

    private final LoginStreaks plugin;
    private final StreakManager streakManager;
    private final LoginStreakConfig config;

    public CommandStreaksAdmin(LoginStreaks plugin, StreakManager streakManager, LoginStreakConfig config) {
        this.plugin = plugin;
        this.streakManager = streakManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("loginstreaks.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Show help if no args
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                handleSetCommand(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "refresh":
                handleRefreshCommand(sender);
                break;
            case "help":
                showAdminHelp(sender);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use §e/lsa help §cfor a list of commands.");
                break;
        }

        return true;
    }

    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage("§e=== LoginStreaks Admin Commands ===");
        sender.sendMessage("§a/lsa set <player> <streak> §7- Set a player's current streak");
        sender.sendMessage("§a/lsa reset <player> §7- Reset a player's streak to 0");
        sender.sendMessage("§a/lsa reload §7- Reload the configuration");
        sender.sendMessage("§a/lsa refresh §7- Force refresh the leaderboard cache");
        sender.sendMessage("§a/lsa help §7- Show this help message");
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lsa set <player> <streak>");
            return;
        }

        String targetPlayerName = args[1];
        int newStreak;

        try {
            newStreak = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid streak value. Must be a number.");
            return;
        }

        if (newStreak < 0) {
            sender.sendMessage("§cStreak value must be 0 or greater.");
            return;
        }

        // Check if player exists in the data
        long lastLogin = streakManager.getPlayerLastLoginByName(targetPlayerName);

        // If player has no data, create initial data
        if (lastLogin == 0) {
            lastLogin = System.currentTimeMillis();
        }

        // Save the new streak
        config.savePlayerData(targetPlayerName, lastLogin, newStreak);

        // Update cache if player is online
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer != null) {
            streakManager.setPlayerStreak(targetPlayer, newStreak);
        }

        sender.sendMessage("§aSet §e" + targetPlayerName + "§a's streak to §e" + newStreak + "§a days.");

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] " + sender.getName() + " set " + targetPlayerName + "'s streak to " + newStreak);
        }
    }

    private void handleResetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lsa reset <player>");
            return;
        }

        String targetPlayerName = args[1];

        // Check if player exists in the data
        long lastLogin = streakManager.getPlayerLastLoginByName(targetPlayerName);
        if (lastLogin == 0) {
            sender.sendMessage("§cPlayer §e" + targetPlayerName + " §chas no streak data.");
            return;
        }

        // Reset the player's streak
        config.savePlayerData(targetPlayerName, 0, 0);

        // Update cache if player is online
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer != null) {
            streakManager.resetPlayerStreak(targetPlayer);
            targetPlayer.sendMessage("§cYour login streak has been reset.");
        }

        sender.sendMessage("§aReset §e" + targetPlayerName + "§a's streak.");

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] " + sender.getName() + " reset " + targetPlayerName + "'s streak");
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        try {
            // Reload configuration
            config.load();

            sender.sendMessage("§aLoginStreaks configuration reloaded successfully.");

            if (config.debug()) {
                plugin.getServer().getLogger().info("[LoginStreaks] " + sender.getName() + " reloaded the configuration");
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload configuration: " + e.getMessage());
            plugin.getServer().getLogger().warning("[LoginStreaks] Failed to reload config: " + e.getMessage());
        }
    }

    private void handleRefreshCommand(CommandSender sender) {
        try {
            // Force refresh the leaderboard cache
            streakManager.forceRefreshCache();

            sender.sendMessage("§aLeaderboard cache refreshed successfully.");

            if (config.debug()) {
                plugin.getServer().getLogger().info("[LoginStreaks] " + sender.getName() + " forced a cache refresh");
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to refresh cache: " + e.getMessage());
            plugin.getServer().getLogger().warning("[LoginStreaks] Failed to refresh cache: " + e.getMessage());
        }
    }
}
