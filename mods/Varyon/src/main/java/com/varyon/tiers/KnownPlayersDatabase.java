package com.varyon.tiers;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class KnownPlayersDatabase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Connection connection;
    private final File dbFile;

    public KnownPlayersDatabase(File pluginFolder) {
        this.dbFile = new File(pluginFolder, "known_players.db");
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            LOGGER.at(Level.INFO).log("Known players database initialized at: " + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.SEVERE).log("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize known players database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS known_players (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    first_seen INTEGER NOT NULL
                )
            """);
        }
    }

    public void recordPlayer(UUID playerUuid, String playerName) {
        String upsert = """
            INSERT INTO known_players (player_uuid, player_name, first_seen)
            VALUES (?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name
        """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to record known player " + playerUuid + ": " + e.getMessage());
        }
    }

    public List<KnownPlayer> getAllPlayers() {
        List<KnownPlayer> players = new ArrayList<>();
        String query = "SELECT player_uuid, player_name FROM known_players";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                players.add(new KnownPlayer(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name")
                ));
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get known players: " + e.getMessage());
        }

        return players;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.at(Level.WARNING).log("Failed to close known players database: " + e.getMessage());
            }
        }
    }

    public record KnownPlayer(UUID uuid, String name) {}
}
