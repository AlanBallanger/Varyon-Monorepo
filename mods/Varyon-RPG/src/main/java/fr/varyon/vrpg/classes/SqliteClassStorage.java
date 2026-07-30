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
                  uuid                TEXT PRIMARY KEY,
                  player_name         TEXT,
                  active_class        TEXT,
                  active_profile_idx  INTEGER NOT NULL DEFAULT 0,
                  updated_at          INTEGER NOT NULL
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
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_profiles (
                  uuid         TEXT NOT NULL,
                  profile_idx  INTEGER NOT NULL,
                  name         TEXT NOT NULL DEFAULT 'Profil',
                  active_class TEXT,
                  PRIMARY KEY (uuid, profile_idx)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_profile_talent (
                  uuid        TEXT NOT NULL,
                  profile_idx INTEGER NOT NULL,
                  class_id    TEXT NOT NULL,
                  node_id     TEXT NOT NULL,
                  rank        INTEGER NOT NULL DEFAULT 0,
                  spec_id     TEXT,
                  PRIMARY KEY (uuid, profile_idx, class_id, node_id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_skill_slot (
                  uuid     TEXT NOT NULL,
                  class_id TEXT NOT NULL,
                  slot_id  TEXT NOT NULL,
                  item_id  TEXT NOT NULL,
                  PRIMARY KEY (uuid, class_id, slot_id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_profile_skill_slot (
                  uuid        TEXT NOT NULL,
                  profile_idx INTEGER NOT NULL,
                  class_id    TEXT NOT NULL,
                  slot_id     TEXT NOT NULL,
                  item_id     TEXT NOT NULL,
                  PRIMARY KEY (uuid, profile_idx, class_id, slot_id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_class_profile_progress (
                  uuid        TEXT NOT NULL,
                  profile_idx INTEGER NOT NULL,
                  class_id    TEXT NOT NULL,
                  level       INTEGER NOT NULL DEFAULT 1,
                  xp_in_level INTEGER NOT NULL DEFAULT 0,
                  PRIMARY KEY (uuid, profile_idx, class_id)
                )
            """);
            migrateAddColumn(st, "player_class_account", "active_profile_idx", "INTEGER NOT NULL DEFAULT 0");
            migrateFixDoubleSpecPrefix(st);
        }
    }

    private static void migrateFixDoubleSpecPrefix(@Nonnull Statement st) {
        String validSlots = "('E','R','A','CrouchA','CrouchE','CrouchR')";
        try {
            st.execute("DELETE FROM player_class_skill_slot WHERE slot_id NOT IN " + validSlots
                + " AND INSTR(SUBSTR(slot_id, INSTR(slot_id, ':') + 1), ':') > 0");
            st.execute("DELETE FROM player_class_profile_skill_slot WHERE slot_id NOT IN " + validSlots
                + " AND INSTR(SUBSTR(slot_id, INSTR(slot_id, ':') + 1), ':') > 0");
        } catch (SQLException ignored) {}
    }

    private static void migrateAddColumn(@Nonnull Statement st, @Nonnull String table,
                                         @Nonnull String column, @Nonnull String def) {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + def);
        } catch (SQLException ignored) {}
    }

    public CompletableFuture<ClassAccount> loadPlayer(@Nonnull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadPlayerSync(uuid), executor);
    }

    private ClassAccount loadPlayerSync(@Nonnull UUID uuid) {
        try {
            ClassAccount account = null;
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_name, active_class, active_profile_idx FROM player_class_account WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        account = new ClassAccount(uuid, rs.getString("player_name"));
                        account.setActiveClass(PlayerClass.fromId(rs.getString("active_class")));
                        account.setActiveProfileIndex(rs.getInt("active_profile_idx"));
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
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT class_id, slot_id, item_id FROM player_class_skill_slot WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                        if (c == null) continue;
                        String slotId = rs.getString("slot_id");
                        String itemId = rs.getString("item_id");
                        if (fr.varyon.vrpg.config.VrpgConfig.isDebugTalents()) {
                            LOGGER.at(Level.INFO).log("[SkillSlot] LOAD uuid=%s class=%s slot=%s item=%s", uuid, c, slotId, itemId);
                        }
                        account.getSkillSlots(c).put(slotId, itemId);
                    }
                }
            }
            loadProfilesSync(uuid, account);
            if (!hasProfileProgress(uuid)) {
                migrateProfileProgressFromAccount(account);
            }
            account.getProfiles()[account.getActiveProfileIndex()].applyTo(account);
            return account;
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("loadPlayer(%s) failed: %s", uuid, e.getMessage());
            return new ClassAccount(uuid, null);
        }
    }

    private void loadProfilesSync(@Nonnull UUID uuid, @Nonnull ClassAccount account) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT profile_idx, name, active_class FROM player_class_profiles WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("profile_idx");
                    if (idx < 0 || idx >= ClassProfile.COUNT) continue;
                    ClassProfile p = account.getProfiles()[idx];
                    p.setName(rs.getString("name"));
                    p.setActiveClass(PlayerClass.fromId(rs.getString("active_class")));
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT profile_idx, class_id, node_id, rank, spec_id FROM player_class_profile_talent WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("profile_idx");
                    if (idx < 0 || idx >= ClassProfile.COUNT) continue;
                    PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                    if (c == null) continue;
                    ClassProfile p = account.getProfiles()[idx];
                    String nodeId = rs.getString("node_id");
                    int rank = rs.getInt("rank");
                    if ("__spec__".equals(nodeId)) {
                        p.setSpec(c, PlayerSpecialization.fromId(rs.getString("spec_id")));
                    } else {
                        p.setTalentRank(c, nodeId, rank);
                    }
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT profile_idx, class_id, slot_id, item_id FROM player_class_profile_skill_slot WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("profile_idx");
                    if (idx < 0 || idx >= ClassProfile.COUNT) continue;
                    PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                    if (c == null) continue;
                    account.getProfiles()[idx].setSkillSlot(c, rs.getString("slot_id"), rs.getString("item_id"));
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT profile_idx, class_id, level, xp_in_level FROM player_class_profile_progress WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("profile_idx");
                    if (idx < 0 || idx >= ClassProfile.COUNT) continue;
                    PlayerClass c = PlayerClass.fromId(rs.getString("class_id"));
                    if (c == null) continue;
                    account.getProfiles()[idx].getProgress(c).setLevel(
                        rs.getInt("level"), rs.getLong("xp_in_level"));
                }
            }
        }
    }

    private boolean hasProfileProgress(@Nonnull UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT 1 FROM player_class_profile_progress WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void migrateProfileProgressFromAccount(@Nonnull ClassAccount account) {
        ClassProfile active = account.getProfiles()[account.getActiveProfileIndex()];
        for (PlayerClass c : PlayerClass.values()) {
            ClassProgress accProg = account.getProgress(c);
            active.getProgress(c).setLevel(accProg.getLevel(), accProg.getXpInLevel());
            if (active.getSpec(c) == null && accProg.getActiveSpec() != null) {
                active.setSpec(c, accProg.getActiveSpec());
            }
        }
    }

    private void writeInitialAccount(@Nonnull ClassAccount account) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_account (uuid, player_name, active_class, active_profile_idx, updated_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, account.getUuid().toString());
            ps.setString(2, account.getPlayerName());
            ps.setString(3, null);
            ps.setInt(4, account.getActiveProfileIndex());
            ps.setLong(5, now);
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
                INSERT INTO player_class_account (uuid, player_name, active_class, active_profile_idx, updated_at)
                VALUES (?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    active_class = excluded.active_class,
                    active_profile_idx = excluded.active_profile_idx,
                    updated_at = excluded.updated_at
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, account.getPlayerName());
                ps.setString(3, account.getActiveClass() == null ? null : account.getActiveClass().getId());
                ps.setInt(4, account.getActiveProfileIndex());
                ps.setLong(5, now);
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
            try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM player_class_skill_slot WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement(
                "INSERT INTO player_class_skill_slot (uuid, class_id, slot_id, item_id) VALUES (?,?,?,?)")) {
                for (PlayerClass c : PlayerClass.values()) {
                    for (Map.Entry<String, String> e : account.getSkillSlots(c).entrySet()) {
                        if (e.getValue() == null || e.getValue().isBlank()) continue;
                        if (fr.varyon.vrpg.config.VrpgConfig.isDebugTalents()) {
                            LOGGER.at(Level.INFO).log("[SkillSlot] SAVE uuid=%s class=%s slot=%s item=%s", uuid, c, e.getKey(), e.getValue());
                        }
                        ins.setString(1, uuid.toString());
                        ins.setString(2, c.getId());
                        ins.setString(3, e.getKey());
                        ins.setString(4, e.getValue());
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
            account.getProfiles()[account.getActiveProfileIndex()].snapshotFrom(account);
            saveProfilesSync(uuid, account);
            connection.commit();
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("savePlayer(%s) failed: %s", uuid, e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) {}
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private void saveProfilesSync(@Nonnull UUID uuid, @Nonnull ClassAccount account) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
            "DELETE FROM player_class_profiles WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        try (PreparedStatement del = connection.prepareStatement(
            "DELETE FROM player_class_profile_talent WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        try (PreparedStatement del = connection.prepareStatement(
            "DELETE FROM player_class_profile_skill_slot WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        try (PreparedStatement del = connection.prepareStatement(
            "DELETE FROM player_class_profile_progress WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        String uuidStr = uuid.toString();
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_profiles (uuid, profile_idx, name, active_class) VALUES (?,?,?,?)")) {
            for (int i = 0; i < ClassProfile.COUNT; i++) {
                ClassProfile p = account.getProfiles()[i];
                ps.setString(1, uuidStr);
                ps.setInt(2, i);
                ps.setString(3, p.getName());
                ps.setString(4, p.getActiveClass() == null ? null : p.getActiveClass().getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_profile_talent (uuid, profile_idx, class_id, node_id, rank, spec_id) VALUES (?,?,?,?,?,?)")) {
            for (int i = 0; i < ClassProfile.COUNT; i++) {
                ClassProfile p = account.getProfiles()[i];
                for (PlayerClass c : PlayerClass.values()) {
                    PlayerSpecialization spec = p.getSpec(c);
                    if (spec != null) {
                        ps.setString(1, uuidStr);
                        ps.setInt(2, i);
                        ps.setString(3, c.getId());
                        ps.setString(4, "__spec__");
                        ps.setInt(5, 0);
                        ps.setString(6, spec.getId());
                        ps.addBatch();
                    }
                    for (Map.Entry<String, Integer> e : p.getTalents(c).entrySet()) {
                        if (e.getValue() == null || e.getValue() <= 0) continue;
                        ps.setString(1, uuidStr);
                        ps.setInt(2, i);
                        ps.setString(3, c.getId());
                        ps.setString(4, e.getKey());
                        ps.setInt(5, e.getValue());
                        ps.setString(6, null);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_profile_skill_slot (uuid, profile_idx, class_id, slot_id, item_id) VALUES (?,?,?,?,?)")) {
            for (int i = 0; i < ClassProfile.COUNT; i++) {
                ClassProfile p = account.getProfiles()[i];
                for (PlayerClass c : PlayerClass.values()) {
                    for (Map.Entry<String, String> e : p.getSkillSlots(c).entrySet()) {
                        if (e.getValue() == null || e.getValue().isBlank()) continue;
                        ps.setString(1, uuidStr);
                        ps.setInt(2, i);
                        ps.setString(3, c.getId());
                        ps.setString(4, e.getKey());
                        ps.setString(5, e.getValue());
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO player_class_profile_progress (uuid, profile_idx, class_id, level, xp_in_level) VALUES (?,?,?,?,?)")) {
            for (int i = 0; i < ClassProfile.COUNT; i++) {
                ClassProfile p = account.getProfiles()[i];
                for (PlayerClass c : PlayerClass.values()) {
                    ClassProgress prog = p.getProgress(c);
                    ps.setString(1, uuidStr);
                    ps.setInt(2, i);
                    ps.setString(3, c.getId());
                    ps.setInt(4, prog.getLevel());
                    ps.setLong(5, prog.getXpInLevel());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
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
