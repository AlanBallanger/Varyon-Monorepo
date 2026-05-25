package com.varyon.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceDatabase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Connection connection;
    private final File dbFile;

    public EssenceDatabase(File pluginFolder) {
        this.dbFile = new File(pluginFolder, "essence.db");
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            LOGGER.at(Level.INFO).log("Essence database initialized at: " + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.SEVERE).log("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize essence database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_essence (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    essence REAL NOT NULL DEFAULT 0,
                    last_updated INTEGER NOT NULL
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_essence ON player_essence(essence DESC)");
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

    private void migrateIntToReal(Statement stmt) {
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_essence)");
            while (rs.next()) {
                if ("essence".equals(rs.getString("name")) && "INTEGER".equalsIgnoreCase(rs.getString("type"))) {
                    stmt.execute("ALTER TABLE player_essence RENAME COLUMN essence TO essence_old");
                    stmt.execute("ALTER TABLE player_essence ADD COLUMN essence REAL NOT NULL DEFAULT 0");
                    stmt.execute("UPDATE player_essence SET essence = CAST(essence_old AS REAL)");
                    LOGGER.at(Level.INFO).log("Migrated essence column from INTEGER to REAL");
                    break;
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.FINE).log("Essence column migration check: " + e.getMessage());
        }
    }

    public double getEssence(UUID playerUuid) {
        String query = "SELECT essence FROM player_essence WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("essence");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get essence for " + playerUuid + ": " + e.getMessage());
        }
        return 0.0;
    }

    public void setEssence(UUID playerUuid, String playerName, double essence) {
        String upsert = """
            INSERT INTO player_essence (player_uuid, player_name, essence, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                essence = excluded.essence,
                last_updated = excluded.last_updated
        """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setDouble(3, essence);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set essence for " + playerUuid + ": " + e.getMessage());
        }
    }
    
    public void setEssenceUncapped(UUID playerUuid, String playerName, double essence) {
        String upsert = """
            INSERT INTO player_essence (player_uuid, player_name, essence, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                essence = excluded.essence,
                last_updated = excluded.last_updated
        """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setDouble(3, essence);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set essence (uncapped) for " + playerUuid + ": " + e.getMessage());
        }
    }

    public List<PlayerEssenceData> getTopPlayers(int limit) {
        List<PlayerEssenceData> topPlayers = new ArrayList<>();
        String query = "SELECT player_uuid, player_name, essence FROM player_essence ORDER BY essence DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                topPlayers.add(new PlayerEssenceData(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getDouble("essence")
                ));
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get top players: " + e.getMessage());
        }

        return topPlayers;
    }

    public int getPlayerRank(UUID playerUuid) {
        String query = """
            SELECT COUNT(*) + 1 as rank FROM player_essence
            WHERE essence > (SELECT essence FROM player_essence WHERE player_uuid = ?)
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
