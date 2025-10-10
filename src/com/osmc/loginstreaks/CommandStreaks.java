package com.osmc.loginstreaks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommandStreaks implements CommandExecutor {

    private final LoginStreaks plugin;
    private final StreakManager streakManager;
    private final LoginStreakConfig config;

    public CommandStreaks(LoginStreaks plugin, StreakManager streakManager, LoginStreakConfig config) {
        this.plugin = plugin;
        this.streakManager = streakManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Handle help command
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        // Handle main command - show streak info
        showStreakInfo(player);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§e=== LoginStreaks Help ===");
        player.sendMessage("§a/loginstreak §7- Show your current streak status");
        player.sendMessage("§a/loginstreak help §7- Show this help message");
        player.sendMessage("");
        player.sendMessage("§7Login daily to maintain your streak and earn rewards!");
    }

    private void showStreakInfo(Player player) {
        int currentStreak = streakManager.getPlayerStreak(player);
        long lastLogin = streakManager.getPlayerLastLogin(player);

        if (lastLogin == 0) {
            player.sendMessage("§eYou haven't started a login streak yet!");
            player.sendMessage("§7Login tomorrow to begin your streak.");
            return;
        }

        // Calculate next login deadline
        long nextLoginDeadline = lastLogin + (24 * 60 * 60 * 1000L); // 24 hours from last login
        long currentTime = System.currentTimeMillis();
        long timeRemaining = nextLoginDeadline - currentTime;

        player.sendMessage("§e=== Login Streak ===");
        player.sendMessage("§aCurrent streak: §e" + currentStreak + "d");

        // Format last login time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        dateFormat.setTimeZone(config.getConfiguredTimeZone());
        String lastLoginFormatted = dateFormat.format(new Date(lastLogin));
        player.sendMessage("§aLast login: §7" + lastLoginFormatted);

        // Show time remaining or if streak expired
        if (timeRemaining > 0) {
            String timeRemainingFormatted = formatTimeRemaining(timeRemaining);
            player.sendMessage("§aNext login needed in: §e" + timeRemainingFormatted);

            // Show next reward
            double nextReward = config.rewardFor(currentStreak + 1);
            if (nextReward > 0) {
                player.sendMessage("§aNext reward (day " + (currentStreak + 1) + "): §6$" + nextReward);
            }
        } else {
            player.sendMessage("§cYour streak has expired! Login now to start a new streak.");
        }
    }

    private String formatTimeRemaining(long timeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
