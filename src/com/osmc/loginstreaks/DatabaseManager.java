package com.osmc.loginstreaks;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private final LoginStreaks plugin;
    private final Logger logger;
    private Connection connection;

    // Database connection details
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public DatabaseManager(LoginStreaks plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger;
    }

    public void initialize(String host, int port, String database, String username, String password, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;

        connect();
        createTables();
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true",
                                     host, port, database, useSSL);

            connection = DriverManager.getConnection(url, username, password);
            logger.info("[LoginStreaks] Connected to MySQL database successfully!");

        } catch (ClassNotFoundException e) {
            logger.severe("[LoginStreaks] MySQL driver not found! Please add mysql-connector-java to your server.");
        } catch (SQLException e) {
            logger.severe("[LoginStreaks] Failed to connect to database: " + e.getMessage());
        }
    }

    private void createTables() {
        String createStreaksTable = "CREATE TABLE IF NOT EXISTS loginstreaks_players (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "player_name VARCHAR(16) NOT NULL UNIQUE, " +
            "current_streak INT DEFAULT 0, " +
            "best_streak INT DEFAULT 0, " +
            "total_logins INT DEFAULT 0, " +
            "last_login BIGINT DEFAULT 0, " +
            "first_login BIGINT DEFAULT 0, " +
            "total_rewards DOUBLE DEFAULT 0.0, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "INDEX idx_player_name (player_name)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createRewardsTable = "CREATE TABLE IF NOT EXISTS loginstreaks_rewards (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "player_name VARCHAR(16) NOT NULL, " +
            "streak_day INT NOT NULL, " +
            "reward_amount DOUBLE NOT NULL, " +
            "timestamp BIGINT NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "INDEX idx_player_name (player_name), " +
            "INDEX idx_timestamp (timestamp)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute(createStreaksTable);
            stmt.execute(createRewardsTable);
            logger.info("[LoginStreaks] Database tables created successfully!");
        } catch (SQLException e) {
            logger.severe("[LoginStreaks] Failed to create database tables: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    public void savePlayerStreak(String playerName, int currentStreak, int bestStreak,
                                long lastLogin, long firstLogin, int totalLogins, double totalRewards) {
        String sql = "INSERT INTO loginstreaks_players " +
            "(player_name, current_streak, best_streak, last_login, first_login, total_logins, total_rewards) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "current_streak = VALUES(current_streak), " +
            "best_streak = GREATEST(best_streak, VALUES(best_streak)), " +
            "last_login = VALUES(last_login), " +
            "total_logins = VALUES(total_logins), " +
            "total_rewards = VALUES(total_rewards), " +
            "updated_at = CURRENT_TIMESTAMP";

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerName);
            stmt.setInt(2, currentStreak);
            stmt.setInt(3, Math.max(bestStreak, currentStreak));
            stmt.setLong(4, lastLogin);
            stmt.setLong(5, firstLogin > 0 ? firstLogin : lastLogin);
            stmt.setInt(6, totalLogins);
            stmt.setDouble(7, totalRewards);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[LoginStreaks] Failed to save player streak data: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    public void logReward(String playerName, int streakDay, double rewardAmount, long timestamp) {
        String sql = "INSERT INTO loginstreaks_rewards " +
            "(player_name, streak_day, reward_amount, timestamp) " +
            "VALUES (?, ?, ?, ?)";

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerName);
            stmt.setInt(2, streakDay);
            stmt.setDouble(3, rewardAmount);
            stmt.setLong(4, timestamp);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[LoginStreaks] Failed to log reward: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    public PlayerDatabaseData getPlayerData(String playerName) {
        String sql = "SELECT current_streak, best_streak, last_login, first_login, total_logins, total_rewards " +
            "FROM loginstreaks_players " +
            "WHERE player_name = ?";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlayerDatabaseData(
                    rs.getInt("current_streak"),
                    rs.getInt("best_streak"),
                    rs.getLong("last_login"),
                    rs.getLong("first_login"),
                    rs.getInt("total_logins"),
                    rs.getDouble("total_rewards")
                );
            }
        } catch (SQLException e) {
            logger.warning("[LoginStreaks] Failed to get player data: " + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    public int getTopStreakPosition(String playerName) {
        String sql = "SELECT COUNT(*) + 1 as position " +
            "FROM loginstreaks_players " +
            "WHERE best_streak > (" +
            "SELECT COALESCE(best_streak, 0) " +
            "FROM loginstreaks_players " +
            "WHERE player_name = ?" +
            ")";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (SQLException e) {
            logger.warning("[LoginStreaks] Failed to get top streak position: " + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        return -1;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("[LoginStreaks] Disconnected from MySQL database.");
            }
        } catch (SQLException e) {
            logger.warning("[LoginStreaks] Error disconnecting from database: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    // Data class to hold player database information
    public static class PlayerDatabaseData {
        public final int currentStreak;
        public final int bestStreak;
        public final long lastLogin;
        public final long firstLogin;
        public final int totalLogins;
        public final double totalRewards;

        public PlayerDatabaseData(int currentStreak, int bestStreak,
                                long lastLogin, long firstLogin, int totalLogins, double totalRewards) {
            this.currentStreak = currentStreak;
            this.bestStreak = bestStreak;
            this.lastLogin = lastLogin;
            this.firstLogin = firstLogin;
            this.totalLogins = totalLogins;
            this.totalRewards = totalRewards;
        }
    }
}
