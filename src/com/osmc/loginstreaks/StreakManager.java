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
        String playerName = player.getName().toLowerCase();
        long currentTime = System.currentTimeMillis();

        PlayerStreakData data = getOrCreatePlayerData(playerName);
        long lastLogin = data.getLastLogin();

        LoginResult result = classifyLogin(lastLogin, currentTime);
        int currentStreak = nextStreakValue(data.getStreak(), result);

        data.setLastLogin(currentTime);
        data.setStreak(currentStreak);
        streakCache.put(playerName, data);
        config.savePlayerData(playerName, currentTime, currentStreak);

        sendLoginMessages(player, result, currentStreak);
    }

    private LoginResult classifyLogin(long lastLogin, long currentTime) {
        if (lastLogin == 0) return LoginResult.FIRST_LOGIN;
        if (isSameDay(lastLogin, currentTime)) return LoginResult.SAME_DAY;
        if (isNextDay(lastLogin, currentTime)) return LoginResult.CONSECUTIVE_DAY;
        return LoginResult.STREAK_BROKEN;
    }

    private int nextStreakValue(int currentStreak, LoginResult result) {
        switch (result) {
            case FIRST_LOGIN:
            case STREAK_BROKEN:
                return 1;
            case CONSECUTIVE_DAY:
                return currentStreak + 1;
            case SAME_DAY:
            default:
                return currentStreak;
        }
    }

    private void sendLoginMessages(Player player, LoginResult result, int currentStreak) {
        if (result == LoginResult.STREAK_BROKEN) {
            player.sendMessage(config.msgReset());
            // NOTE: original code also falls through to msgContinue below for this
            // case - preserved as-is rather than assumed to be a bug.
        }

        if (result == LoginResult.CONSECUTIVE_DAY) {
            double reward = config.rewardFor(currentStreak);
            if (reward > 0 && essentials.isHooked()) {
                essentials.giveMoney(player, reward);
                player.sendMessage(config.msgReward(currentStreak, reward, player.getName()));
                return;
            }
        }

        player.sendMessage(config.msgContinue(currentStreak, player.getName()));
    }

    private boolean isSameDay(long a, long b) {
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();
        ca.setTimeInMillis(a);
        cb.setTimeInMillis(b);

        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isNextDay(long last, long now) {
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();
        ca.setTimeInMillis(last);
        cb.setTimeInMillis(now);

        ca.add(Calendar.DAY_OF_YEAR, 1);

        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private PlayerStreakData getOrCreatePlayerData(String playerName) {
        PlayerStreakData data = streakCache.get(playerName.toLowerCase());
        if (data == null) {
            // Load from config class
            long lastLogin = config.getPlayerLastLogin(playerName.toLowerCase());
            int streak = config.getPlayerStreak(playerName.toLowerCase());
            int longestStreak = config.getPlayerLongestStreak(playerName.toLowerCase());
            data = new PlayerStreakData(lastLogin, streak, longestStreak);
            streakCache.put(playerName.toLowerCase(), data);
        }
        return data;
    }

    // Public API methods
    public int getPlayerStreak(Player player) {
        String playerName = player.getName().toLowerCase();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getStreak();
    }

    public int getPlayerStreakByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName.toLowerCase());
        return data.getStreak();
    }

    public int getPlayerLongestStreak(Player player) {
        String playerName = player.getName().toLowerCase();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLongestStreak();
    }

    public int getPlayerLongestStreakByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName.toLowerCase());
        return data.getLongestStreak();
    }

    public long getPlayerLastLogin(Player player) {
        String playerName = player.getName().toLowerCase();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLastLogin();
    }

    public long getPlayerLastLoginByName(String playerName) {
        PlayerStreakData data = getOrCreatePlayerData(playerName.toLowerCase());
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
        String playerName = player.getName().toLowerCase();
        PlayerStreakData data = new PlayerStreakData();
        streakCache.put(playerName, data);
        config.savePlayerData(playerName, 0, 0);
    }

    public void setPlayerStreak(Player player, int streak) {
        String playerName = player.getName().toLowerCase();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        data.setStreak(streak);
        streakCache.put(playerName, data);
        config.savePlayerData(playerName, data.getLastLogin(), streak);
    }

    /**
     * Reload a player's data from file and update the cache.
     * Used after admin commands modify player data externally.
     */
    public void reloadPlayerData(String playerName) {
        long lastLogin = config.getPlayerLastLogin(playerName.toLowerCase());
        int streak = config.getPlayerStreak(playerName.toLowerCase());
        int longestStreak = config.getPlayerLongestStreak(playerName.toLowerCase());
        PlayerStreakData data = new PlayerStreakData(lastLogin, streak, longestStreak);
        streakCache.put(playerName.toLowerCase(), data);
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
