package com.osmc.loginstreaks;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages streak expiration warnings for online players.
 * Warns players at 10min, 5min, 1min, 30sec, then countdown from 20, 10, 9...1
 */
public class StreakWarningManager {

    private final LoginStreaks plugin;
    private final StreakManager streakManager;
    private final LoginStreakConfig config;

    // Task ID for the warning checker
    private int warningTaskId = -1;

    // Track which warnings have been sent to each player (by warning stage)
    private final Map<String, Integer> lastWarningSent = new HashMap<String, Integer>();

    // Warning thresholds in seconds before expiration
    // Major warnings, then countdown: 10min, 5min, 1min, 30sec, 20sec, then 10-1
    private static final int[] WARNING_THRESHOLDS = {
        600, 300, 60, 30, 20, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };

    // Streak window is 24 hours
    private static final long STREAK_WINDOW_MS = 24 * 60 * 60 * 1000L;

    public StreakWarningManager(LoginStreaks plugin, StreakManager streakManager, LoginStreakConfig config) {
        this.plugin = plugin;
        this.streakManager = streakManager;
        this.config = config;
    }

    /**
     * Start the warning task that checks all online players
     */
    public void start() {
        // Run every second (20 ticks)
        warningTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                checkAllPlayers();
            }
        }, 20L, 20L); // Check every second

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] Streak warning manager started");
        }
    }

    /**
     * Stop the warning task
     */
    public void stop() {
        if (warningTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(warningTaskId);
            warningTaskId = -1;
            if (config.debug()) {
                plugin.getServer().getLogger().info("[LoginStreaks] Streak warning manager stopped");
            }
        }
        lastWarningSent.clear();
    }

    /**
     * Check all online players and send warnings as needed
     */
    private void checkAllPlayers() {
        long currentTime = System.currentTimeMillis();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkPlayer(player, currentTime);
        }
    }

    /**
     * Check a single player and send warning if needed
     */
    private void checkPlayer(Player player, long currentTime) {
        String playerName = player.getName();

        // Get player's last login time
        long lastLogin = streakManager.getPlayerLastLoginByName(playerName);

        // Skip if player has no previous login (first time player)
        if (lastLogin == 0) {
            return;
        }

        // Skip if player has no streak to lose
        int currentStreak = streakManager.getPlayerStreakByName(playerName);
        if (currentStreak <= 0) {
            return;
        }

        // Calculate time until streak expires
        long expirationTime = lastLogin + STREAK_WINDOW_MS;
        long timeRemainingMs = expirationTime - currentTime;
        int timeRemainingSec = (int) (timeRemainingMs / 1000);

        // Debug logging every 10 seconds
        if (config.debug() && timeRemainingSec > 0 && timeRemainingSec <= 600 && timeRemainingSec % 10 == 0) {
            plugin.getServer().getLogger().info("[LoginStreaks] " + playerName + " streak expires in " + timeRemainingSec + "s");
        }

        // If already expired, send the "streak gone" message once
        if (timeRemainingSec <= 0) {
            Integer lastStage = lastWarningSent.get(playerName);
            if (lastStage == null || lastStage != -1) {
                player.sendMessage(config.msgWarningExpired());
                lastWarningSent.put(playerName, -1);
            }
            return;
        }

        // Determine which warning stage we're in and send appropriate message
        int warningStage = getWarningStage(timeRemainingSec);

        if (warningStage > 0) {
            Integer lastStage = lastWarningSent.get(playerName);
            if (lastStage == null || lastStage != warningStage) {
                sendWarning(player, warningStage, timeRemainingSec);
                lastWarningSent.put(playerName, warningStage);
            }
        }
    }

    /**
     * Get the warning stage based on time remaining
     * Returns 0 if no warning should be sent at this time
     * Returns the current warning threshold bucket
     */
    private int getWarningStage(int timeRemainingSec) {
        // Not in warning zone yet (more than 10 minutes remaining)
        if (timeRemainingSec > WARNING_THRESHOLDS[0]) {
            return 0;
        }

        // Find the current warning stage
        // We want the smallest threshold that is >= timeRemaining
        for (int i = WARNING_THRESHOLDS.length - 1; i >= 0; i--) {
            if (WARNING_THRESHOLDS[i] >= timeRemainingSec) {
                return WARNING_THRESHOLDS[i];
            }
        }
        return 0;
    }

    /**
     * Send the appropriate warning message based on the stage
     */
    private void sendWarning(Player player, int stage, int timeRemainingSec) {
        String message;

        if (stage >= 60) {
            // Minutes warning (60, 300, 600 seconds = 1, 5, 10 minutes)
            message = config.msgWarningMinutes(stage / 60);
        } else if (stage >= 20) {
            // Seconds warning (20, 30 seconds)
            message = config.msgWarningSeconds(stage);
        } else {
            // Countdown (1-10 seconds)
            message = config.msgWarningCountdown(stage);
        }

        player.sendMessage(message);

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] Sent warning stage " + stage + " to " + player.getName());
        }
    }

    /**
     * Called when a player joins - reset their warning state
     */
    public void onPlayerJoin(String playerName) {
        lastWarningSent.remove(playerName);
    }

    /**
     * Called when a player leaves - clean up their warning state
     */
    public void onPlayerQuit(String playerName) {
        lastWarningSent.remove(playerName);
    }

    /**
     * Get time remaining until a player's streak expires (for display purposes)
     */
    public long getTimeUntilExpiration(String playerName) {
        long lastLogin = streakManager.getPlayerLastLoginByName(playerName);
        if (lastLogin == 0) return -1;

        long expirationTime = lastLogin + STREAK_WINDOW_MS;
        long timeRemaining = expirationTime - System.currentTimeMillis();
        return timeRemaining > 0 ? timeRemaining : 0;
    }

    /**
     * Format time remaining as a human-readable string
     */
    public String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) return "Expired";

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
