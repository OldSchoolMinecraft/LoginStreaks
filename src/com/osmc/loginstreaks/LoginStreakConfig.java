package com.osmc.loginstreaks;

import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public final class LoginStreakConfig {

    private final LoginStreaks plugin;
    private final File file;
    private final Properties props = new Properties();

    public LoginStreakConfig(LoginStreaks plugin) {
        this(plugin, "config.properties");
    }

    public LoginStreakConfig(LoginStreaks plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    public void load() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            if (!file.exists()) {
                setDefaults();
                save(); // write defaults
            }
            FileInputStream in = new FileInputStream(file);
            props.load(in);
            in.close();
            // ensure any newly added defaults exist
            boolean changed = ensureMissingDefaults();
            if (changed) save();
        } catch (Exception e) {
            plugin.logger.warning("[LoginStreak] Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            FileOutputStream out = new FileOutputStream(file);

            // Write organized config with proper sections and comments
            StringBuilder sb = new StringBuilder();
            sb.append("#LoginStreak configuration\n\n");

            sb.append("# === TIMEZONE SETTINGS ===\n");
            sb.append("# Use UTC offset numbers: 0=UTC, 1=GMT+1, -5=EST, 8=Asia, etc.\n");
            sb.append("timezone=").append(props.getProperty("timezone", "0")).append("\n\n");

            sb.append("# === REWARD SETTINGS ===\n");
            sb.append("# Base reward amount that increases each day\n");
            sb.append("reward.increase=").append(props.getProperty("reward.increase", "15.0")).append("\n");
            sb.append("# Maximum reward cap (0 = no limit)\n");
            sb.append("reward.max=").append(props.getProperty("reward.max", "500.0")).append("\n\n");

            sb.append("# === CACHE SETTINGS ===\n");
            sb.append("# How often to refresh the top streaks leaderboard cache (in minutes)\n");
            sb.append("cache.refresh_minutes=").append(props.getProperty("cache.refresh_minutes", "5")).append("\n\n");

            sb.append("# === PLAYER MESSAGES ===\n");
            sb.append("message.reward=").append(props.getProperty("message.reward", "&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!")).append("\n");
            sb.append("message.continue=").append(props.getProperty("message.continue", "&e{player}&a's login streak: &e{streak}d&a.")).append("\n");
            sb.append("message.reset=").append(props.getProperty("message.reset", "&cYour streak has reset.")).append("\n\n");

            sb.append("# === DEBUG SETTINGS ===\n");
            sb.append("debug=").append(props.getProperty("debug", "false")).append("\n");

            out.write(sb.toString().getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            plugin.logger.warning("[LoginStreak] Failed to save config: " + e.getMessage());
        }
    }

    /* ---------------- Defaults ---------------- */

    private void setDefaults() {
        // === TIMEZONE SETTINGS ===
        // Use UTC offset numbers: 0=UTC, 1=GMT+1, -5=EST, 8=Asia, etc.
        props.setProperty("timezone", "0");

        // === REWARD SETTINGS ===
        // Base reward amount that increases each day
        props.setProperty("reward.increase", "15.0");
        // Maximum reward cap (0 = no limit)
        props.setProperty("reward.max", "500.0");

        // === CACHE SETTINGS ===
        // How often to refresh the top streaks leaderboard cache (in minutes)
        props.setProperty("cache.refresh_minutes", "5");

        // === PLAYER MESSAGES ===
        props.setProperty("message.reward", "&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!");
        props.setProperty("message.continue", "&e{player}&a's login streak: &e{streak}d&a.");
        props.setProperty("message.reset", "&cYour streak has reset.");

        // === DEBUG SETTINGS ===
        props.setProperty("debug", "false");
    }

    private boolean ensureMissingDefaults() {
        Properties def = new Properties();
        setDefaults(def);
        boolean changed = false;
        for (Map.Entry<Object, Object> e : def.entrySet()) {
            if (!props.containsKey(e.getKey())) {
                props.setProperty((String) e.getKey(), (String) e.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private void setDefaults(Properties p) {
        p.setProperty("timezone", "0");
        p.setProperty("reward.increase", "15.0");
        p.setProperty("reward.max", "500.0");
        p.setProperty("cache.refresh_minutes", "5");
        p.setProperty("message.reward", "&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!");
        p.setProperty("message.continue", "&e{player}&a's login streak: &e{streak}d&a.");
        p.setProperty("message.reset", "&cYour streak has reset.");
        p.setProperty("debug", "false");
    }

    /* ---------------- Typed getters ---------------- */

    public boolean debug() {
        return getBoolean("debug", false);
    }

    public String msgReward(int streak, double amount, String player) {
        String s = props.getProperty("message.reward",
                "&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!");
        return color(s.replace("{player}", player)
                .replace("{streak}", String.valueOf(streak))
                .replace("{amount}", String.valueOf(amount)));
    }

    public String msgContinue(int streak, String player) {
        String s = props.getProperty("message.continue",
                "&e{player}&a's login streak: &e{streak}d&a.");
        return color(s.replace("{player}", player)
                .replace("{streak}", String.valueOf(streak)));
    }

    public String msgReset() {
        String s = props.getProperty("message.reset",
                "&cYour streak has reset.");
        return color(s);
    }

    /** Calculate reward based on streak day multiplied by increase amount, capped by max */
    public double rewardFor(int streak) {
        double increaseAmount = parseDouble(props.getProperty("reward.increase", "15.0"));
        double maxReward = parseDouble(props.getProperty("reward.max", "500.0"));

        double calculatedReward = streak * increaseAmount;

        // Apply max cap if set (0 means no limit)
        if (maxReward > 0 && calculatedReward > maxReward) {
            return maxReward;
        }

        return calculatedReward;
    }

    public int getCacheRefreshMinutes() {
        return getInt("cache.refresh_minutes", 5);
    }

    /* ---------------- Helpers ---------------- */

    private int getInt(String key, int def) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private boolean getBoolean(String key, boolean def) {
        String v = props.getProperty(key, String.valueOf(def));
        return Boolean.parseBoolean(v);
    }

    private double parseDouble(String s) {
        try { return Double.valueOf(s.trim()).doubleValue(); } catch (Exception e) { return 0.0D; }
    }

    private int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String color(String s) {
        if (s == null) return "";
        // support & codes typical for configs on that era
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /* ---------------- Time Utilities ---------------- */

    public String getTimezone() {
        return props.getProperty("timezone", "0");
    }

    public java.util.TimeZone getConfiguredTimeZone() {
        try {
            // Convert the simple UTC offset to a TimeZone ID
            int offsetHours = Integer.parseInt(getTimezone());
            return java.util.TimeZone.getTimeZone("GMT" + (offsetHours >= 0 ? "+" : "") + offsetHours);
        } catch (Exception e) {
            return java.util.TimeZone.getTimeZone("UTC");
        }
    }

    /* ---------------- Simplified Player Data Management ---------------- */

    public long getPlayerLastLogin(String playerName) {
        try {
            File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
            File playerFile = new File(playerDataDir, playerName + ".yml");

            if (!playerFile.exists()) {
                return 0;
            }

            Properties props = new Properties();
            FileInputStream in = new FileInputStream(playerFile);
            props.load(in);
            in.close();

            return Long.parseLong(props.getProperty("lastLogin", "0"));
        } catch (Exception e) {
            if (debug()) {
                plugin.getServer().getLogger().warning("[LoginStreaks] Failed to load lastLogin for " + playerName + ": " + e.getMessage());
            }
            return 0;
        }
    }

    public int getPlayerStreak(String playerName) {
        try {
            File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
            File playerFile = new File(playerDataDir, playerName + ".yml");

            if (!playerFile.exists()) {
                return 0;
            }

            Properties props = new Properties();
            FileInputStream in = new FileInputStream(playerFile);
            props.load(in);
            in.close();

            return Integer.parseInt(props.getProperty("streak", "0"));
        } catch (Exception e) {
            if (debug()) {
                plugin.getServer().getLogger().warning("[LoginStreaks] Failed to load streak for " + playerName + ": " + e.getMessage());
            }
            return 0;
        }
    }

    public int getPlayerLongestStreak(String playerName) {
        try {
            File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
            File playerFile = new File(playerDataDir, playerName + ".yml");

            if (!playerFile.exists()) {
                return 0;
            }

            Properties props = new Properties();
            FileInputStream in = new FileInputStream(playerFile);
            props.load(in);
            in.close();

            return Integer.parseInt(props.getProperty("longestStreak", "0"));
        } catch (Exception e) {
            if (debug()) {
                plugin.getServer().getLogger().warning("[LoginStreaks] Failed to load longestStreak for " + playerName + ": " + e.getMessage());
            }
            return 0;
        }
    }

    public void savePlayerData(String playerName, long lastLogin, int streak) {
        try {
            File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
            if (!playerDataDir.exists()) {
                playerDataDir.mkdirs();
            }

            File playerFile = new File(playerDataDir, playerName + ".yml");

            // Load existing longest streak or calculate it
            int longestStreak = getPlayerLongestStreak(playerName);
            if (streak > longestStreak) {
                longestStreak = streak;
            }

            // Write clean player data without any comments
            StringBuilder sb = new StringBuilder();
            sb.append("streak=").append(streak).append("\n");
            sb.append("lastLogin=").append(lastLogin).append("\n");
            sb.append("longestStreak=").append(longestStreak).append("\n");

            FileOutputStream out = new FileOutputStream(playerFile);
            out.write(sb.toString().getBytes());
            out.flush();
            out.close();

        } catch (Exception e) {
            plugin.getServer().getLogger().warning("[LoginStreaks] Failed to save data for " + playerName + ": " + e.getMessage());
        }
    }

    // Keep the simplified method for convenience
    public void savePlayerLastLogin(String playerName, long lastLogin) {
        int currentStreak = getPlayerStreak(playerName);
        savePlayerData(playerName, lastLogin, currentStreak);
    }
}
