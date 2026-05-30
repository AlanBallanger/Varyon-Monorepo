package fr.varyon.vrpg.classes;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class SqliteClassStorage {

    private static final String DB_FILE = "varyon-classes.db";
    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG-Classes");

    private final Path dataDirectory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VaryonRPG-Classes-IO");
        t.setDaemon(false);
        return t;
    });

    private Connection connection;

    public SqliteClassStorage(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDirectory);
                String dbPath = dataDirectory.resolve(DB_FILE).toAbsolutePath().toString();
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("SQLite driver missing", e);
                }
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA journal_mode=WAL");
                    st.execute("PRAGMA synchronous=NORMAL");
                    st.execute("PRAGMA foreign_keys=ON");
                }
                createTables();
                LOGGER.at(Level.INFO).log("Classes SQLite database ready at %s", dbPath);
            } catch (IOException | SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to initialize Classes SQLite: %s", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_account (
                  uuid         TEXT PRIMARY KEY,
                  player_name  TEXT,
                  active_class TEXT,
                  updated_at   INTEGER NOT NULL
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_progress (
                  uuid        TEXT NOT NULL,
                  class_id    TEXT NOT NULL,
                  level       INTEGER NOT NULL DEFAULT 1,
                  xp_in_level INTEGER NOT NULL DEFAULT 0,
                  active_spec TEXT,
                  updated_at  INTEGER NOT NULL,
                  PRIMARY KEY (uuid, class_id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_talent (
                  uuid     TEXT NOT NULL,
                  class_id TEXT NOT NULL,
                  node_id  TEXT NOT NULL,
                  rank     INTEGER NOT NULL DEFAULT 0,
                  PRIMARY KEY (uuid, class_id, node_id)
                )
            """);
        }
    }

    public CompletableFuture<ClassAccount> loadPlayer(@Nonnull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadPlayerSync(uuid), executor);
    }

    private ClassAccount loadPlayerSync(@Nonnull UUID uuid) {
        try {
            ClassAccount account = null;
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_name, active_class FROM player_class_account WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        account = new ClassAccount(uuid, rs.getString("player_name"));
                        account.setActiveClass(PlayerClass.fromId(rs.getString("active_class")));
                    }
                }
            }
            if (account == null) {
                account = new ClassAccount(uuid, null);
                writeInitialAccount(account);
                return account;
            }
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT class_id, level, xp_in_level, active_spec FROM player_class_progress WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                        if (c == null) continue;
                        account.getProgress(c).setLevel(rs.getInt("level"), rs.getLong("xp_in_level"));
                        account.getProgress(c).setActiveSpec(PlayerSpecialization.fromId(rs.getString("active_spec")));
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT class_id, node_id, rank FROM player_class_talent WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                        if (c == null) continue;
                        account.setTalentRank(c, rs.getString("node_id"), rs.getInt("rank"));
                    }
                }
            }
            return account;
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("loadPlayer(%s) failed: %s", uuid, e.getMessage());
            return new ClassAccount(uuid, null);
        }
    }

    private void writeInitialAccount(@Nonnull ClassAccount account) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_account (uuid, player_name, active_class, updated_at) VALUES (?,?,?,?)")) {
            ps.setString(1, account.getUuid().toString());
            ps.setString(2, account.getPlayerName());
            ps.setString(3, null);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_progress (uuid, class_id, level, xp_in_level, active_spec, updated_at) VALUES (?,?,?,?,?,?)")) {
            for (PlayerClass c : PlayerClass.values()) {
                ClassProgress prog = account.getProgress(c);
                ps.setString(1, account.getUuid().toString());
                ps.setString(2, c.getId());
                ps.setInt(3, prog.getLevel());
                ps.setLong(4, prog.getXpInLevel());
                ps.setString(5, null);
                ps.setLong(6, now);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public CompletableFuture<Void> savePlayer(@Nonnull UUID uuid, @Nonnull ClassAccount account) {
        return CompletableFuture.runAsync(() -> savePlayerSync(uuid, account), executor);
    }

    private void savePlayerSync(@Nonnull UUID uuid, @Nonnull ClassAccount account) {
        long now = System.currentTimeMillis();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_class_account (uuid, player_name, active_class, updated_at)
                VALUES (?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    active_class = excluded.active_class,
                    updated_at = excluded.updated_at
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, account.getPlayerName());
                ps.setString(3, account.getActiveClass() == null ? null : account.getActiveClass().getId());
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_class_progress (uuid, class_id, level, xp_in_level, active_spec, updated_at)
                VALUES (?,?,?,?,?,?)
                ON CONFLICT(uuid, class_id) DO UPDATE SET
                    level = excluded.level,
                    xp_in_level = excluded.xp_in_level,
                    active_spec = excluded.active_spec,
                    updated_at = excluded.updated_at
            """)) {
                for (PlayerClass c : PlayerClass.values()) {
                    ClassProgress prog = account.getProgress(c);
                    PlayerSpecialization spec = prog.getActiveSpec();
                    ps.setString(1, uuid.toString());
                    ps.setString(2, c.getId());
                    ps.setInt(3, prog.getLevel());
                    ps.setLong(4, prog.getXpInLevel());
                    ps.setString(5, spec == null ? null : spec.getId());
                    ps.setLong(6, now);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM player_class_talent WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement(
                "INSERT INTO player_class_talent (uuid, class_id, node_id, rank) VALUES (?,?,?,?)")) {
                for (PlayerClass c : PlayerClass.values()) {
                    for (Map.Entry<String, Integer> e : account.getTalents(c).entrySet()) {
                        if (e.getValue() == null || e.getValue() <= 0) continue;
                        ins.setString(1, uuid.toString());
                        ins.setString(2, c.getId());
                        ins.setString(3, e.getKey());
                        ins.setInt(4, e.getValue());
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("savePlayer(%s) failed: %s", uuid, e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) {}
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public void shutdown() {
        executor.submit(() -> {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException e) {
                LOGGER.at(Level.WARNING).log("Error closing Classes DB: %s", e.getMessage());
            }
        });
        executor.shutdown();
    }
}
