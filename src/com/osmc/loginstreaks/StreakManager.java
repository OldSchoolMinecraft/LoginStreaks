package com.osmc.loginstreaks;

import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class StreakManager {

    private final LoginStreaks plugin;
    private final LoginStreakConfig config;
    private final EssentialsHook essentials;
    private final DatabaseManager database;
    private final Map<String, PlayerStreakData> streakCache;

    // Cached top streaks leaderboard
    private java.util.List<java.util.Map.Entry<String, Integer>> cachedTopStreaks;
    private int cacheTaskId = -1;
    private long lastCacheRefreshTime = 0;

    public StreakManager(LoginStreaks plugin, LoginStreakConfig config, EssentialsHook essentials, DatabaseManager database) {
        this.plugin = plugin;
        this.config = config;
        this.essentials = essentials;
        this.database = database;
        this.streakCache = new HashMap<String, PlayerStreakData>();
        this.cachedTopStreaks = new java.util.ArrayList<java.util.Map.Entry<String, Integer>>();
    }

    /**
     * Start the periodic cache refresh task
     */
    public void startCacheRefreshTask() {
        // Initial load
        refreshTopStreaksCache();

        // Schedule periodic refresh
        int refreshMinutes = config.getCacheRefreshMinutes();
        long refreshTicks = refreshMinutes * 60 * 20L; // Convert minutes to ticks (20 ticks = 1 second)

        cacheTaskId = plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                refreshTopStreaksCache();
            }
        }, refreshTicks, refreshTicks);

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] Cache refresh task started (every " + refreshMinutes + " minutes)");
        }
    }

    /**
     * Stop the cache refresh task
     */
    public void stopCacheRefreshTask() {
        if (cacheTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(cacheTaskId);
            cacheTaskId = -1;
            if (config.debug()) {
                plugin.getServer().getLogger().info("[LoginStreaks] Cache refresh task stopped");
            }
        }
    }

    /**
     * Refresh the top streaks cache by reading all player files
     */
    private void refreshTopStreaksCache() {
        java.util.List<java.util.Map.Entry<String, Integer>> topStreaks = new java.util.ArrayList<java.util.Map.Entry<String, Integer>>();

        // Get all player data files
        java.io.File playerDataDir = new java.io.File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            cachedTopStreaks = topStreaks;
            lastCacheRefreshTime = System.currentTimeMillis();
            return;
        }

        java.io.File[] playerFiles = playerDataDir.listFiles();
        if (playerFiles == null) {
            cachedTopStreaks = topStreaks;
            lastCacheRefreshTime = System.currentTimeMillis();
            return;
        }

        // Read each player's longest streak
        for (java.io.File playerFile : playerFiles) {
            if (playerFile.getName().endsWith(".yml")) {
                String playerName = playerFile.getName().substring(0, playerFile.getName().length() - 4);
                int longestStreak = config.getPlayerLongestStreak(playerName);
                if (longestStreak > 0) {
                    topStreaks.add(new java.util.AbstractMap.SimpleEntry<String, Integer>(playerName, longestStreak));
                }
            }
        }

        // Sort by longest streak descending
        java.util.Collections.sort(topStreaks, new java.util.Comparator<java.util.Map.Entry<String, Integer>>() {
            public int compare(java.util.Map.Entry<String, Integer> a, java.util.Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        // Update cache
        cachedTopStreaks = topStreaks;
        lastCacheRefreshTime = System.currentTimeMillis();

        if (config.debug()) {
            plugin.getServer().getLogger().info("[LoginStreaks] Top streaks cache refreshed (" + topStreaks.size() + " players)");
        }
    }

    public void handlePlayerLogin(Player player) {
        String playerName = player.getName();

        long currentTime = System.currentTimeMillis();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        long lastLogin = data.getLastLogin();
        int currentStreak = data.getStreak();

        boolean streakContinues = false;
        boolean newStreak = false;
        boolean shouldGiveReward = false;

        if (lastLogin == 0) {
            // First time login - no reward on day 1
            newStreak = true;
            currentStreak = 1;
            shouldGiveReward = false; // First day doesn't get a reward
        } else {
            long timeDiff = currentTime - lastLogin;
            long windowMs = 24 * 60 * 60 * 1000L; // 24 hours exactly - no grace period

            if (timeDiff <= windowMs) {
                // Within streak window
                if (isNewDay(lastLogin, currentTime)) {
                    currentStreak++;
                    streakContinues = true;
                    shouldGiveReward = true;
                } else {
                    // Same day login - no streak increment, no reward
                    streakContinues = true;
                    shouldGiveReward = false;
                }
            } else {
                // Streak broken - reset personal streak to day 1, no reward
                currentStreak = 1;
                newStreak = true;
                shouldGiveReward = false;
            }
        }

        // Update data
        data.setLastLogin(currentTime);
        data.setStreak(currentStreak);
        streakCache.put(playerName, data);

        // Save to file using config class
        config.savePlayerData(playerName, currentTime, currentStreak);

        // Handle rewards and messages
        if (newStreak && currentStreak == 1) {
            // Don't show reset message for first-time players
            if (lastLogin != 0) {
                player.sendMessage(config.msgReset());
            }
        }

        if (shouldGiveReward) {
            // Use personal streak for reward calculation
            double reward = config.rewardFor(currentStreak);

            if (reward > 0 && essentials.isHooked()) {
                essentials.giveMoney(player, reward);
                player.sendMessage(config.msgReward(currentStreak, reward, player.getName()));
            } else {
                player.sendMessage(config.msgContinue(currentStreak, player.getName()));
            }
        } else {
            // Player already logged in today or it's same-day login or reset-without-reward
            player.sendMessage(config.msgContinue(currentStreak, player.getName()));
        }
    }

    private boolean isNewDay(long lastLogin, long currentTime) {
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(lastLogin);

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(currentTime);

        // Check if it's a different day (hardcoded reset at midnight)
        int resetHour = 0; // Hardcoded to midnight

        // Adjust for reset hour
        if (lastCal.get(Calendar.HOUR_OF_DAY) < resetHour) {
            lastCal.add(Calendar.DAY_OF_YEAR, -1);
        }
        if (currentCal.get(Calendar.HOUR_OF_DAY) < resetHour) {
            currentCal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return lastCal.get(Calendar.DAY_OF_YEAR) != currentCal.get(Calendar.DAY_OF_YEAR) ||
               lastCal.get(Calendar.YEAR) != currentCal.get(Calendar.YEAR);
    }

    private PlayerStreakData getOrCreatePlayerData(String playerName) {
        PlayerStreakData data = streakCache.get(playerName);
        if (data == null) {
            // Load from config class
            long lastLogin = config.getPlayerLastLogin(playerName);
            int streak = config.getPlayerStreak(playerName);
            int longestStreak = config.getPlayerLongestStreak(playerName);
            data = new PlayerStreakData(lastLogin, streak, longestStreak);
            streakCache.put(playerName, data);
        }
        return data;
    }

    // Public API methods
    public int getPlayerStreak(Player player) {
        String playerName = player.getName();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getStreak();
    }

    public int getPlayerStreakByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getStreak();
    }

    public int getPlayerLongestStreak(Player player) {
        String playerName = player.getName();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLongestStreak();
    }

    public int getPlayerLongestStreakByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLongestStreak();
    }

    public long getPlayerLastLogin(Player player) {
        String playerName = player.getName();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLastLogin();
    }

    public long getPlayerLastLoginByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLastLogin();
    }

    /**
     * Get top longest streaks from cache (no file I/O)
     */
    public java.util.List<java.util.Map.Entry<String, Integer>> getTopLongestStreaks(int limit) {
        // Return from cache instead of reading files
        if (cachedTopStreaks.size() > limit) {
            return cachedTopStreaks.subList(0, limit);
        }
        return cachedTopStreaks;
    }

    /**
     * Force refresh the cache immediately (useful after player streak updates)
     */
    public void forceRefreshCache() {
        refreshTopStreaksCache();
    }

    /**
     * Get time in milliseconds until next cache refresh
     */
    public long getTimeUntilNextCacheRefresh() {
        if (lastCacheRefreshTime == 0) {
            return 0; // Not yet initialized
        }
        int refreshMinutes = config.getCacheRefreshMinutes();
        long refreshIntervalMs = refreshMinutes * 60 * 1000L;
        long nextRefreshTime = lastCacheRefreshTime + refreshIntervalMs;
        long timeRemaining = nextRefreshTime - System.currentTimeMillis();
        return timeRemaining > 0 ? timeRemaining : 0;
    }

    public void resetPlayerStreak(Player player) {
        String playerName = player.getName();
        PlayerStreakData data = new PlayerStreakData();
        streakCache.put(playerName, data);
        config.savePlayerData(playerName, 0, 0);
    }

    public void setPlayerStreak(Player player, int streak) {
        String playerName = player.getName();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        data.setStreak(streak);
        streakCache.put(playerName, data);
        config.savePlayerData(playerName, data.getLastLogin(), streak);
    }

    // Inner class to hold player streak data
    private static class PlayerStreakData {
        private long lastLogin;
        private int streak;
        private int longestStreak;

        public PlayerStreakData() {
            this.lastLogin = 0;
            this.streak = 0;
            this.longestStreak = 0;
        }

        public PlayerStreakData(long lastLogin, int streak) {
            this.lastLogin = lastLogin;
            this.streak = streak;
        }

        public PlayerStreakData(long lastLogin, int streak, int longestStreak) {
            this.lastLogin = lastLogin;
            this.streak = streak;
            this.longestStreak = longestStreak;
        }

        public long getLastLogin() { return lastLogin; }
        public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

        public int getStreak() { return streak; }
        public void setStreak(int streak) {
            this.streak = streak;
            // Update longest streak if current streak is higher
            if (streak > longestStreak) {
                longestStreak = streak;
            }
        }

        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    }
}
