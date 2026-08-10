package com.varyon.points;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PointsDatabase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Connection connection;
    private final File dbFile;
    private final File legacyDbFile;

    public PointsDatabase(File pluginFolder) {
        this.dbFile = new File(pluginFolder, "points.db");
        this.legacyDbFile = new File(pluginFolder, "essence.db");
    }

    public void initialize() {
        migrateLegacyDbFile();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            LOGGER.at(Level.INFO).log("Points database initialized at: " + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.SEVERE).log("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize points database: " + e.getMessage());
        }
    }

    private void migrateLegacyDbFile() {
        if (legacyDbFile.exists() && !dbFile.exists()) {
            try {
                Files.move(legacyDbFile.toPath(), dbFile.toPath());
                LOGGER.at(Level.INFO).log("Migrated legacy essence.db to points.db at: " + dbFile.getAbsolutePath());
            } catch (java.io.IOException e) {
                LOGGER.at(Level.SEVERE).log("Failed to migrate essence.db to points.db: " + e.getMessage());
            }
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            migrateLegacyTable(stmt);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_points (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    points REAL NOT NULL DEFAULT 0,
                    last_updated INTEGER NOT NULL
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_points ON player_points(points DESC)");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS global_balance (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    balance INTEGER NOT NULL DEFAULT 0
                )
            """);
            stmt.execute("INSERT OR IGNORE INTO global_balance (id, balance) VALUES (1, 0)");

            migrateIntToReal(stmt);
        }
    }

    private void migrateLegacyTable(Statement stmt) {
        try {
            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='player_essence'");
            if (rs.next()) {
                stmt.execute("DROP INDEX IF EXISTS idx_essence");
                stmt.execute("ALTER TABLE player_essence RENAME TO player_points");
                stmt.execute("ALTER TABLE player_points RENAME COLUMN essence TO points");
                LOGGER.at(Level.INFO).log("Migrated player_essence table to player_points");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.FINE).log("Legacy table migration check: " + e.getMessage());
        }
    }

    private void migrateIntToReal(Statement stmt) {
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_points)");
            while (rs.next()) {
                if ("points".equals(rs.getString("name")) && "INTEGER".equalsIgnoreCase(rs.getString("type"))) {
                    stmt.execute("ALTER TABLE player_points RENAME COLUMN points TO points_old");
                    stmt.execute("ALTER TABLE player_points ADD COLUMN points REAL NOT NULL DEFAULT 0");
                    stmt.execute("UPDATE player_points SET points = CAST(points_old AS REAL)");
                    LOGGER.at(Level.INFO).log("Migrated points column from INTEGER to REAL");
                    break;
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.FINE).log("Points column migration check: " + e.getMessage());
        }
    }

    public double getPoints(UUID playerUuid) {
        String query = "SELECT points FROM player_points WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("points");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get points for " + playerUuid + ": " + e.getMessage());
        }
        return 0.0;
    }

    public void setPoints(UUID playerUuid, String playerName, double points) {
        String upsert = """
            INSERT INTO player_points (player_uuid, player_name, points, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                points = excluded.points,
                last_updated = excluded.last_updated
        """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setDouble(3, points);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set points for " + playerUuid + ": " + e.getMessage());
        }
    }

    public void setPointsUncapped(UUID playerUuid, String playerName, double points) {
        String upsert = """
            INSERT INTO player_points (player_uuid, player_name, points, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                points = excluded.points,
                last_updated = excluded.last_updated
        """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setDouble(3, points);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set points (uncapped) for " + playerUuid + ": " + e.getMessage());
        }
    }

    public List<PlayerPointsData> getTopPlayers(int limit) {
        List<PlayerPointsData> topPlayers = new ArrayList<>();
        String query = "SELECT player_uuid, player_name, points FROM player_points ORDER BY points DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                topPlayers.add(new PlayerPointsData(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getDouble("points")
                ));
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get top players: " + e.getMessage());
        }

        return topPlayers;
    }

    public int getPlayerRank(UUID playerUuid) {
        String query = """
            SELECT COUNT(*) + 1 as rank FROM player_points
            WHERE points > (SELECT points FROM player_points WHERE player_uuid = ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get player rank: " + e.getMessage());
        }
        return -1;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.at(Level.WARNING).log("Failed to close database: " + e.getMessage());
            }
        }
    }

    public int getGlobalBalance() {
        String query = "SELECT balance FROM global_balance WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("balance");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get global balance: " + e.getMessage());
        }
        return 0;
    }

    public void setGlobalBalance(int balance) {
        String update = "UPDATE global_balance SET balance = ? WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setInt(1, balance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set global balance: " + e.getMessage());
        }
    }

    public void addToGlobalBalance(int amount) {
        int current = getGlobalBalance();
        setGlobalBalance(current + amount);
    }
}
