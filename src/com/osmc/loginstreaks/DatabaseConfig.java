package com.osmc.loginstreaks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

public class DatabaseConfig {

    private final LoginStreaks plugin;
    private final File file;
    private final Properties props = new Properties();

    public DatabaseConfig(LoginStreaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "database.properties");
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
            plugin.logger.warning("[LoginStreaks] Failed to load database config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            FileOutputStream out = new FileOutputStream(file);

            // Write organized database config with proper sections and comments
            StringBuilder sb = new StringBuilder();
            sb.append("#LoginStreaks Database Configuration\n\n");

            sb.append("# === DATABASE SETTINGS ===\n");
            sb.append("# Set enabled=true to use MySQL database for streak tracking\n");
            sb.append("enabled=").append(props.getProperty("enabled", "false")).append("\n");
            sb.append("host=").append(props.getProperty("host", "localhost")).append("\n");
            sb.append("port=").append(props.getProperty("port", "3306")).append("\n");
            sb.append("name=").append(props.getProperty("name", "minecraft")).append("\n");
            sb.append("username=").append(props.getProperty("username", "root")).append("\n");
            sb.append("password=").append(props.getProperty("password", "")).append("\n");
            sb.append("useSSL=").append(props.getProperty("useSSL", "false")).append("\n\n");

            sb.append("# === CONNECTION POOL SETTINGS ===\n");
            sb.append("maxPoolSize=").append(props.getProperty("maxPoolSize", "10")).append("\n");
            sb.append("connectionTimeout=").append(props.getProperty("connectionTimeout", "30000")).append("\n");
            sb.append("idleTimeout=").append(props.getProperty("idleTimeout", "600000")).append("\n");
            sb.append("maxLifetime=").append(props.getProperty("maxLifetime", "1800000")).append("\n");

            out.write(sb.toString().getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            plugin.logger.warning("[LoginStreaks] Failed to save database config: " + e.getMessage());
        }
    }

    private void setDefaults() {
        // === DATABASE SETTINGS ===
        props.setProperty("enabled", "false");
        props.setProperty("host", "localhost");
        props.setProperty("port", "3306");
        props.setProperty("name", "minecraft");
        props.setProperty("username", "root");
        props.setProperty("password", "");
        props.setProperty("useSSL", "false");

        // === CONNECTION POOL SETTINGS ===
        props.setProperty("maxPoolSize", "10");
        props.setProperty("connectionTimeout", "30000");
        props.setProperty("idleTimeout", "600000");
        props.setProperty("maxLifetime", "1800000");
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
        p.setProperty("enabled", "false");
        p.setProperty("host", "localhost");
        p.setProperty("port", "3306");
        p.setProperty("name", "minecraft");
        p.setProperty("username", "root");
        p.setProperty("password", "");
        p.setProperty("useSSL", "false");
        p.setProperty("maxPoolSize", "10");
        p.setProperty("connectionTimeout", "30000");
        p.setProperty("idleTimeout", "600000");
        p.setProperty("maxLifetime", "1800000");
    }

    // Database configuration getters
    public boolean isEnabled() {
        return getBoolean("enabled", false);
    }

    public String getHost() {
        return props.getProperty("host", "localhost");
    }

    public int getPort() {
        return getInt("port", 3306);
    }

    public String getName() {
        return props.getProperty("name", "minecraft");
    }

    public String getUsername() {
        return props.getProperty("username", "root");
    }

    public String getPassword() {
        return props.getProperty("password", "");
    }

    public boolean useSSL() {
        return getBoolean("useSSL", false);
    }

    // Connection pool getters
    public int getMaxPoolSize() {
        return getInt("maxPoolSize", 10);
    }

    public int getConnectionTimeout() {
        return getInt("connectionTimeout", 30000);
    }

    public int getIdleTimeout() {
        return getInt("idleTimeout", 600000);
    }

    public int getMaxLifetime() {
        return getInt("maxLifetime", 1800000);
    }

    // Helper methods
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
}
