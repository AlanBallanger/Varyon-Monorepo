package com.varyon.playtime.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.config.Reward;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

    private HikariDataSource dataSource;
    private final File dataFolder;
    private boolean isMySQL;
    private final Logger logger = LoggerFactory.getLogger("Playtime");

    private static final long DB_ERROR_LOG_BACKOFF_MS = 60_000;
    private volatile long lastClaimCheckErrorLoggedAt = 0;
    private volatile long lastMilestoneCheckErrorLoggedAt = 0;
    private volatile long lastClaimLogErrorLoggedAt = 0;
    private volatile long lastClaimTimestampsErrorLoggedAt = 0;
    /** reward.id values already warned about for an unresolved period — logged once, not every cycle. */
    private final Set<String> unknownPeriodWarned = ConcurrentHashMap.newKeySet();

    public boolean isMySQL() {
        return isMySQL;
    }

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void init() {
        PlaytimeConfig.DatabaseSettings settings = Playtime.get().getConfigManager().getConfig().database;
        HikariConfig config = new HikariConfig();

        if (settings.type.equalsIgnoreCase("mysql")) {
            config.setJdbcUrl(
                    "jdbc:mysql://" + settings.host + ":" + settings.port + "/" + settings.databaseName + "?useSSL="
                            + settings.useSSL);
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            isMySQL = true;
            logger.info("Connexion à la base MySQL…");
        } else {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "playtime.db").getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            isMySQL = false;
            logger.info("Base SQLite locale.");
        }

        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        createTable();
    }

    private void createTable() {
        String sessionsSql;
        String rewardsSql;

        if (isMySQL) {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "uuid VARCHAR(36),"
                    + "username VARCHAR(16),"
                    + "start_time BIGINT,"
                    + "duration BIGINT,"
                    + "session_date DATE"
                    + ")";
            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "uuid VARCHAR(36),"
                    + "reward_id VARCHAR(64),"
                    + "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";
        } else {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "uuid VARCHAR(36),"
                    + "username VARCHAR(16),"
                    + "start_time BIGINT,"
                    + "duration BIGINT,"
                    + "session_date DATE DEFAULT CURRENT_DATE"
                    + ")";
            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "uuid VARCHAR(36),"
                    + "reward_id VARCHAR(64),"
                    + "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sessionsSql);
            stmt.execute(rewardsSql);
            logger.info("Tables de base vérifiées.");
        } catch (SQLException e) {
            logger.error("Création des tables impossible : " + e.getMessage(), e);
            throw new RuntimeException("Impossible de créer les tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public boolean hasClaimedReward(String uuid, Reward reward) {
        if (reward == null || uuid == null || uuid.isBlank()) {
            return false;
        }
        if (reward.id == null || reward.id.isBlank()) {
            return false;
        }
        if (reward.period == null || reward.period.isBlank()) {
            return false;
        }

        String internal = Playtime.get().getConfigManager().getConfig().resolvePeriodKey(reward.period);
        if (internal == null) {
            internal = reward.period.trim();
        }

        String timeClause = "";

        if (isMySQL) {
            if ("daily".equalsIgnoreCase(internal)) {
                timeClause = " AND DATE(claim_date) = CURDATE()";
            } else if ("weekly".equalsIgnoreCase(internal)) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            } else if ("monthly".equalsIgnoreCase(internal)) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            } else if ("all".equalsIgnoreCase(internal)) {
                timeClause = "";
            }
        } else {
            if ("daily".equalsIgnoreCase(internal)) {
                timeClause = " AND date(claim_date) = date('now')";
            } else if ("weekly".equalsIgnoreCase(internal)) {
                timeClause = " AND date(claim_date) >= date('now', '-7 days')";
            } else if ("monthly".equalsIgnoreCase(internal)) {
                timeClause = " AND date(claim_date) >= date('now', '-1 month')";
            } else if ("all".equalsIgnoreCase(internal)) {
                timeClause = "";
            }
        }

        if (timeClause.isEmpty() && !"all".equalsIgnoreCase(internal)) {
            timeClause = isMySQL ? " AND DATE(claim_date) = CURDATE()" : " AND date(claim_date) = date('now')";
            if (unknownPeriodWarned.add(reward.id)) {
                logger.warn("Période inconnue pour récompense {}, filtre journalier appliqué.", reward.id);
            }
        }

        String query = "SELECT id FROM playtime_rewards_log WHERE uuid = ? AND reward_id = ?" + timeClause;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, reward.id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            long now = System.currentTimeMillis();
            if (now - lastClaimCheckErrorLoggedAt >= DB_ERROR_LOG_BACKOFF_MS) {
                lastClaimCheckErrorLoggedAt = now;
                logger.error("Erreur lors de la vérification d'une récompense (autres occurrences dans la minute qui suit supprimées)", e);
            }
            return false;
        }
    }

    /**
     * Fetches the most recent claim_date per reward_id for a player in a single query, instead of
     * one query per reward/milestone. Used by the scheduled reward/milestone cycle to avoid
     * N (players) x M (rewards+milestones) individual DB round-trips every minute.
     *
     * @return map of reward_id/milestone_id -> most recent claim epoch millis. Missing entries mean never claimed.
     */
    public Map<String, Long> getLatestClaimTimestamps(String uuid) {
        Map<String, Long> result = new HashMap<>();
        if (uuid == null || uuid.isBlank()) {
            return result;
        }
        String query = "SELECT reward_id, MAX(claim_date) as last_claim FROM playtime_rewards_log WHERE uuid = ? GROUP BY reward_id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rewardId = rs.getString("reward_id");
                    Timestamp ts = rs.getTimestamp("last_claim");
                    if (rewardId != null && ts != null) {
                        result.put(rewardId, ts.getTime());
                    }
                }
            }
        } catch (SQLException e) {
            long now = System.currentTimeMillis();
            if (now - lastClaimTimestampsErrorLoggedAt >= DB_ERROR_LOG_BACKOFF_MS) {
                lastClaimTimestampsErrorLoggedAt = now;
                logger.error("Erreur lors de la lecture groupée des récompenses réclamées (autres occurrences dans la minute qui suit supprimées)", e);
            }
        }
        return result;
    }

    /**
     * Evaluates, from an already-fetched claim timestamp, whether a reward/milestone claimed at
     * {@code lastClaimMillis} is still considered "claimed" for the given period — mirrors the SQL
     * time-window logic in {@link #hasClaimedReward} but works in-memory against a batch-fetched map.
     */
    public boolean isClaimStillValidForPeriod(long lastClaimMillis, String period, String rewardIdForWarning) {
        String internal = Playtime.get().getConfigManager().getConfig().resolvePeriodKey(period);
        if (internal == null) {
            internal = period == null ? "" : period.trim();
        }
        if ("all".equalsIgnoreCase(internal)) {
            return true;
        }

        long now = System.currentTimeMillis();
        if ("weekly".equalsIgnoreCase(internal)) {
            return now - lastClaimMillis < 7L * 24 * 60 * 60 * 1000;
        }
        if ("monthly".equalsIgnoreCase(internal)) {
            return now - lastClaimMillis < 30L * 24 * 60 * 60 * 1000;
        }
        // "daily" or unrecognized period (falls back to daily, same as hasClaimedReward).
        if (!"daily".equalsIgnoreCase(internal) && rewardIdForWarning != null && unknownPeriodWarned.add(rewardIdForWarning)) {
            logger.warn("Période inconnue pour récompense {}, filtre journalier appliqué.", rewardIdForWarning);
        }
        java.time.LocalDate claimDate = java.time.Instant.ofEpochMilli(lastClaimMillis)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        return claimDate.equals(today);
    }

    /** @return true if the claim was actually persisted; false on failure (caller should not treat the reward as granted). */
    public boolean logRewardClaim(String uuid, String rewardId) {
        String sql = "INSERT INTO playtime_rewards_log (uuid, reward_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, rewardId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            long now = System.currentTimeMillis();
            if (now - lastClaimLogErrorLoggedAt >= DB_ERROR_LOG_BACKOFF_MS) {
                lastClaimLogErrorLoggedAt = now;
                logger.error("Erreur en enregistrant une récompense (autres occurrences dans la minute qui suit supprimées)", e);
            }
            return false;
        }
    }

    public void resetSessions(String uuid) {
        String sqlSessions = "DELETE FROM playtime_sessions WHERE uuid = ?";
        String sqlRewardsLog = "DELETE FROM playtime_rewards_log WHERE uuid = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlSessions)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlRewardsLog)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du reset des sessions", e);
        }
    }

    public String getUuidByUsername(String username) {
        String sql = "SELECT uuid FROM playtime_sessions WHERE username = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche UUID par username", e);
        }
        return null;
    }

    public boolean hasMilestoneTriggered(String uuid, String milestoneId) {
        String query = "SELECT id FROM playtime_rewards_log WHERE uuid = ? AND reward_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, milestoneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            long now = System.currentTimeMillis();
            if (now - lastMilestoneCheckErrorLoggedAt >= DB_ERROR_LOG_BACKOFF_MS) {
                lastMilestoneCheckErrorLoggedAt = now;
                logger.error("Erreur lors de la vérification d'un milestone (autres occurrences dans la minute qui suit supprimées)", e);
            }
            return false;
        }
    }

    /** @return true if the milestone claim was actually persisted. */
    public boolean logMilestone(String uuid, String milestoneId) {
        return logRewardClaim(uuid, milestoneId);
    }
}
