package fr.varyon.vrpg.ui.prefs;

import com.hypixel.hytale.logger.HytaleLogger;
import fr.varyon.vrpg.ui.ClassXpHud;
import fr.varyon.vrpg.ui.ProfessionXpHud;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class PlayerUiPreferencesManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG-UiPrefs");
    private static final String DB_FILE = "varyon-rpg.db";
    private static final int OFFSET_X_MIN = -200;
    private static final int OFFSET_X_MAX = 200;
    private static final int OFFSET_Y_MIN = -300;
    private static final int OFFSET_Y_MAX = 300;

    private final Path dataDirectory;
    private final ConcurrentHashMap<UUID, PlayerUiPreferences> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VaryonRPG-UiPrefs-IO");
        t.setDaemon(true);
        return t;
    });

    private Connection connection;

    public PlayerUiPreferencesManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataDirectory.resolve(DB_FILE).toAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
            }
            createTable();
            LOGGER.at(Level.INFO).log("PlayerUiPreferencesManager ready");
        } catch (Exception e) {
            throw new RuntimeException("Failed to init UI preferences storage", e);
        }
    }

    private void createTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_ui_prefs (
                  uuid                      TEXT PRIMARY KEY,
                  class_hud_visible         INTEGER NOT NULL DEFAULT 1,
                  class_hud_corner          TEXT NOT NULL DEFAULT 'top_left',
                  class_hud_offset_x        INTEGER NOT NULL DEFAULT 0,
                  class_hud_offset_y        INTEGER NOT NULL DEFAULT 0,
                  profession_hud_visible    INTEGER NOT NULL DEFAULT 1,
                  profession_hud_corner     TEXT NOT NULL DEFAULT 'top_left',
                  profession_hud_offset_x   INTEGER NOT NULL DEFAULT 0,
                  profession_hud_offset_y   INTEGER NOT NULL DEFAULT 0,
                  xp_notif_enabled          INTEGER NOT NULL DEFAULT 1,
                  skill_notif_enabled       INTEGER NOT NULL DEFAULT 1,
                  profession_sounds_enabled INTEGER NOT NULL DEFAULT 1
                )
            """);
        }
    }

    public void ensureLoaded(@Nonnull UUID uuid) {
        cache.computeIfAbsent(uuid, this::loadFromDb);
    }

    @Nonnull
    public PlayerUiPreferences get(@Nonnull UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDb);
    }

    public void update(@Nonnull UUID uuid, @Nonnull PlayerUiPreferences prefs) {
        cache.put(uuid, prefs.copy());
        CompletableFuture.runAsync(() -> saveToDb(uuid, prefs), executor);
    }

    public void applyHudLayout(@Nonnull UUID uuid) {
        PlayerUiPreferences prefs = get(uuid);
        ClassXpHud classHud = ClassXpHud.get(uuid);
        if (classHud != null) classHud.applyPreferences(prefs);
        ProfessionXpHud profHud = ProfessionXpHud.get(uuid);
        if (profHud != null) profHud.applyPreferences(prefs);
    }

    public void cleanup(@Nonnull UUID uuid) {
        PlayerUiPreferences prefs = cache.remove(uuid);
        if (prefs != null) {
            CompletableFuture.runAsync(() -> saveToDb(uuid, prefs), executor);
        }
    }

    public static int clampOffsetX(int value) {
        return Math.max(OFFSET_X_MIN, Math.min(OFFSET_X_MAX, value));
    }

    public static int clampOffsetY(int value) {
        return Math.max(OFFSET_Y_MIN, Math.min(OFFSET_Y_MAX, value));
    }

    public static int offsetXMin() { return OFFSET_X_MIN; }

    public static int offsetXMax() { return OFFSET_X_MAX; }

    public static int offsetYMin() { return OFFSET_Y_MIN; }

    public static int offsetYMax() { return OFFSET_Y_MAX; }

    @Nonnull
    private PlayerUiPreferences loadFromDb(@Nonnull UUID uuid) {
        PlayerUiPreferences prefs = new PlayerUiPreferences();
        if (connection == null) return prefs;
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT * FROM player_ui_prefs WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return prefs;
                prefs.classHudVisible = rs.getInt("class_hud_visible") != 0;
                prefs.classHudCorner = HudCorner.fromId(rs.getString("class_hud_corner"));
                prefs.classHudOffsetX = rs.getInt("class_hud_offset_x");
                prefs.classHudOffsetY = rs.getInt("class_hud_offset_y");
                prefs.professionHudVisible = rs.getInt("profession_hud_visible") != 0;
                prefs.professionHudCorner = HudCorner.fromId(rs.getString("profession_hud_corner"));
                prefs.professionHudOffsetX = rs.getInt("profession_hud_offset_x");
                prefs.professionHudOffsetY = rs.getInt("profession_hud_offset_y");
                prefs.xpNotificationsEnabled = rs.getInt("xp_notif_enabled") != 0;
                prefs.skillNotificationsEnabled = rs.getInt("skill_notif_enabled") != 0;
                prefs.professionSoundsEnabled = rs.getInt("profession_sounds_enabled") != 0;
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Load UI prefs failed uuid=%s: %s", uuid, e.getMessage());
        }
        return prefs;
    }

    private void saveToDb(@Nonnull UUID uuid, @Nonnull PlayerUiPreferences prefs) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO player_ui_prefs (
              uuid, class_hud_visible, class_hud_corner, class_hud_offset_x, class_hud_offset_y,
              profession_hud_visible, profession_hud_corner, profession_hud_offset_x, profession_hud_offset_y,
              xp_notif_enabled, skill_notif_enabled, profession_sounds_enabled
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
              class_hud_visible = excluded.class_hud_visible,
              class_hud_corner = excluded.class_hud_corner,
              class_hud_offset_x = excluded.class_hud_offset_x,
              class_hud_offset_y = excluded.class_hud_offset_y,
              profession_hud_visible = excluded.profession_hud_visible,
              profession_hud_corner = excluded.profession_hud_corner,
              profession_hud_offset_x = excluded.profession_hud_offset_x,
              profession_hud_offset_y = excluded.profession_hud_offset_y,
              xp_notif_enabled = excluded.xp_notif_enabled,
              skill_notif_enabled = excluded.skill_notif_enabled,
              profession_sounds_enabled = excluded.profession_sounds_enabled
            """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, prefs.classHudVisible ? 1 : 0);
            ps.setString(3, prefs.classHudCorner.id());
            ps.setInt(4, prefs.classHudOffsetX);
            ps.setInt(5, prefs.classHudOffsetY);
            ps.setInt(6, prefs.professionHudVisible ? 1 : 0);
            ps.setString(7, prefs.professionHudCorner.id());
            ps.setInt(8, prefs.professionHudOffsetX);
            ps.setInt(9, prefs.professionHudOffsetY);
            ps.setInt(10, prefs.xpNotificationsEnabled ? 1 : 0);
            ps.setInt(11, prefs.skillNotificationsEnabled ? 1 : 0);
            ps.setInt(12, prefs.professionSoundsEnabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Save UI prefs failed uuid=%s: %s", uuid, e.getMessage());
        }
    }
}
