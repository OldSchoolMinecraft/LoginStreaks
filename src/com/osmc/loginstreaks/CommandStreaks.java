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

        // Handle top command
        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            showTopStreaks(player);
            return true;
        }

        // Handle other player lookup
        if (args.length > 0) {
            String targetPlayerName = args[0];
            showOtherPlayerInfo(player, targetPlayerName);
            return true;
        }

        // Handle main command - show own streak info
        showStreakInfo(player);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§e=== LoginStreaks Help ===");
        player.sendMessage("§a/loginstreak §7- Show your current streak status");
        player.sendMessage("§a/loginstreak <player> §7- Show another player's streak");
        player.sendMessage("§a/loginstreak top §7- Show top login streaks");
        player.sendMessage("§a/loginstreak help §7- Show this help message");
        player.sendMessage("");
        player.sendMessage("§7Login daily to maintain your streak and earn rewards!");
    }

    private void showStreakInfo(Player player) {
        int currentStreak = streakManager.getPlayerStreak(player);
        int longestStreak = streakManager.getPlayerLongestStreak(player);
        long lastLogin = streakManager.getPlayerLastLogin(player);

        if (lastLogin == 0) {
            player.sendMessage("§eYou haven't started a login streak yet!");
            player.sendMessage("§7Login tomorrow to begin your streak.");
            return;
        }

        // Calculate next login deadline (24 hours from last login)
        long nextLoginDeadline = lastLogin + (24 * 60 * 60 * 1000L);
        long currentTime = System.currentTimeMillis();
        long timeRemaining = nextLoginDeadline - currentTime;

        player.sendMessage("§e=== Login Streak ===");
        player.sendMessage("§aCurrent streak: §e" + currentStreak + "d");
        player.sendMessage("§aLongest streak: §e" + longestStreak + "d");

        // Format last login time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        dateFormat.setTimeZone(config.getConfiguredTimeZone());
        String lastLoginFormatted = dateFormat.format(new Date(lastLogin));
        player.sendMessage("§aLast login: §7" + lastLoginFormatted);

        // Show time remaining or if streak expired
        if (timeRemaining > 0) {
            // Calculate time until next midnight (when streak can be incremented)
            java.util.Calendar nextMidnight = java.util.Calendar.getInstance(config.getConfiguredTimeZone());
            nextMidnight.setTimeInMillis(currentTime);
            nextMidnight.add(java.util.Calendar.DAY_OF_YEAR, 1);
            nextMidnight.set(java.util.Calendar.HOUR_OF_DAY, 0);
            nextMidnight.set(java.util.Calendar.MINUTE, 0);
            nextMidnight.set(java.util.Calendar.SECOND, 0);
            nextMidnight.set(java.util.Calendar.MILLISECOND, 0);

            long timeUntilNextStreak = nextMidnight.getTimeInMillis() - currentTime;
            String timeRemainingFormatted = formatTimeRemaining(timeUntilNextStreak);
            player.sendMessage("§aNext login available in: §e" + timeRemainingFormatted);

            // Show streak expiration deadline
            String expirationFormatted = formatTimeRemaining(timeRemaining);
            player.sendMessage("§cStreak expires in: §e" + expirationFormatted);

            // Show next reward
            double nextReward = config.rewardFor(currentStreak + 1);
            if (nextReward > 0) {
                player.sendMessage("§aNext reward (day " + (currentStreak + 1) + "): §6$" + nextReward);
            }
        } else {
            player.sendMessage("§cYour streak has expired! Login now to start a new streak.");
        }
    }

    private void showTopStreaks(Player player) {
        player.sendMessage("§e=== Top Login Streaks ===");

        java.util.List<java.util.Map.Entry<String, Integer>> topStreaks = streakManager.getTopLongestStreaks(10);

        if (topStreaks.isEmpty()) {
            player.sendMessage("§7No streak data available yet.");
            return;
        }

        int rank = 1;
        for (java.util.Map.Entry<String, Integer> entry : topStreaks) {
            String playerName = entry.getKey();
            int longestStreak = entry.getValue();

            String rankColor = "§7";
            if (rank == 1) rankColor = "§6"; // Gold for 1st
            else if (rank == 2) rankColor = "§e"; // Yellow for 2nd
            else if (rank == 3) rankColor = "§c"; // Red for 3rd

            player.sendMessage(rankColor + rank + ". §a" + playerName + " §7- §e" + longestStreak + "d");
            rank++;
        }

        // Display next cache update time
        long timeUntilUpdate = streakManager.getTimeUntilNextCacheRefresh();
        if (timeUntilUpdate > 0) {
            String timeFormatted = formatTimeRemaining(timeUntilUpdate);
            player.sendMessage("§7Next update in: §e" + timeFormatted);
        } else {
            player.sendMessage("§7Next update: §eRefreshing soon...");
        }
    }

    private void showOtherPlayerInfo(Player player, String targetPlayerName) {
        // Check if player data exists
        long lastLogin = streakManager.getPlayerLastLoginByName(targetPlayerName);
        if (lastLogin == 0) {
            player.sendMessage("§cPlayer '" + targetPlayerName + "' has no streak data or doesn't exist.");
            return;
        }

        int currentStreak = streakManager.getPlayerStreakByName(targetPlayerName);
        int longestStreak = streakManager.getPlayerLongestStreakByName(targetPlayerName);

        player.sendMessage("§e=== " + targetPlayerName + "'s Login Streak ===");
        player.sendMessage("§aCurrent streak: §e" + currentStreak + "d");
        player.sendMessage("§aLongest streak: §e" + longestStreak + "d");

        // Format last login time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        dateFormat.setTimeZone(config.getConfiguredTimeZone());
        String lastLoginFormatted = dateFormat.format(new Date(lastLogin));
        player.sendMessage("§aLast login: §7" + lastLoginFormatted);

        // Calculate if streak is active or expired
        long nextLoginDeadline = lastLogin + (24 * 60 * 60 * 1000L);
        long currentTime = System.currentTimeMillis();
        long timeRemaining = nextLoginDeadline - currentTime;

        if (timeRemaining > 0) {
            player.sendMessage("§aStreak status: §eActive");
        } else {
            player.sendMessage("§aStreak status: §cExpired");
        }
    }

    private String formatTimeRemaining(long timeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
