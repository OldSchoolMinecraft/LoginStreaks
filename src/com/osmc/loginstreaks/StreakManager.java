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

    public StreakManager(LoginStreaks plugin, LoginStreakConfig config, EssentialsHook essentials, DatabaseManager database) {
        this.plugin = plugin;
        this.config = config;
        this.essentials = essentials;
        this.database = database;
        this.streakCache = new HashMap<String, PlayerStreakData>();
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
            // First time login
            newStreak = true;
            currentStreak = 1;
            shouldGiveReward = true; // First time players always get reward
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
                // Streak broken - reset personal streak
                currentStreak = 1;
                newStreak = true;
                shouldGiveReward = true;
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
            // Player already logged in today or it's same-day login
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
            data = new PlayerStreakData(lastLogin, streak);
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

    public long getPlayerLastLogin(Player player) {
        String playerName = player.getName();
        PlayerStreakData data = getOrCreatePlayerData(playerName);
        return data.getLastLogin();
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

        public PlayerStreakData() {
            this.lastLogin = 0;
            this.streak = 0;
        }

        public PlayerStreakData(long lastLogin, int streak) {
            this.lastLogin = lastLogin;
            this.streak = streak;
        }

        public long getLastLogin() { return lastLogin; }
        public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

        public int getStreak() { return streak; }
        public void setStreak(int streak) { this.streak = streak; }
    }
}
