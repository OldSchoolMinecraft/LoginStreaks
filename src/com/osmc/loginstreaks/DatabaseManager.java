package com.osmc.loginstreaks;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;

public class DatabaseManager {
    private JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize(String host, int port, String database, String username, String password, boolean useSSL) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            if (!useSSL) {
                url += "?useSSL=false";
            }
            connection = DriverManager.getConnection(url, username, password);

            // Create tables automatically
            createTables();

            System.out.println("[LoginStreaks] Connected to database and created tables!");
            return true;
        } catch (Exception e) {
            System.err.println("[LoginStreaks] Failed to connect to database: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Create login streaks table
        String createTable = "CREATE TABLE IF NOT EXISTS loginstreaks (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "player_name VARCHAR(16) NOT NULL UNIQUE, " +
            "current_streak INT DEFAULT 0, " +
            "longest_streak INT DEFAULT 0, " +
            "last_login DATE, " +
            "total_logins INT DEFAULT 0" +
            ")";

        stmt.executeUpdate(createTable);
        stmt.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[LoginStreaks] Disconnected from database.");
            }
        } catch (SQLException e) {
            System.err.println("[LoginStreaks] Error disconnecting: " + e.getMessage());
        }
    }
    
}
