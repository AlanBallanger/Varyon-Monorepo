package com.varyon.playtime;

import com.varyon.playtime.database.DatabaseManager;
import com.varyon.playtime.listeners.SessionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaytimeService {

    private final DatabaseManager db;
    private final boolean isMySQL;
    private final Logger logger = LoggerFactory.getLogger("Playtime");

    private static final long DB_ERROR_LOG_BACKOFF_MS = 60_000;
    private volatile long lastErrorLoggedAt = 0;

    public PlaytimeService(DatabaseManager db) {
        this.db = db;
        this.isMySQL = db.isMySQL();
    }

    private void logDbError(String context, SQLException e) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLoggedAt >= DB_ERROR_LOG_BACKOFF_MS) {
            lastErrorLoggedAt = now;
            logger.error(context + " (autres occurrences dans la minute qui suit supprimées)", e);
        }
    }

    public void saveSession(String uuid, String name, long start, long duration) {
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO playtime_sessions (uuid, username, start_time, duration, session_date) VALUES (?, ?, ?, ?, "
                            + (isMySQL ? "CURDATE()" : "date('now')") + ")");
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setLong(3, start);
            ps.setLong(4, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            logDbError("Erreur lors de l'enregistrement d'une session", e);
        }
    }

    public long getTotalPlaytime(String uuid) {
        return getPlaytime(uuid, "all");
    }

    public long getPlaytime(String uuid, String type) {
        String dateFilter = getDateFilter(canonicalPeriod(type));
        String query = "SELECT SUM(duration) FROM playtime_sessions WHERE uuid = ? " + dateFilter;

        long dbTime = 0;
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                dbTime = rs.getLong(1);
            }
        } catch (SQLException e) {
            logDbError("Erreur lors de la lecture du temps de jeu", e);
        }

        try {
            dbTime += SessionListener.getCurrentSession(UUID.fromString(uuid));
        } catch (Exception ignored) {
        }

        return dbTime;
    }

    /**
     * Computes the player's rank via a SQL COUNT of players with a strictly higher total, instead
     * of loading up to 1000 players into memory just to count how many rank above this one.
     */
    public int getRank(String uuid, String type) {
        long myTime = getPlaytime(uuid, type);
        java.util.Set<UUID> onlineUuids = SessionListener.getOnlineUuids();

        String dateFilter = getDateFilter(canonicalPeriod(type));
        String where = dateFilter.isEmpty() ? "" : "WHERE " + dateFilter.substring(4) + " ";
        // Exclude online players from the SQL count: their DB-only total could double count against
        // the live-session comparison done below, since their in-progress time isn't stored yet.
        String excludeOnline = "";
        if (!onlineUuids.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(onlineUuids.size(), "?"));
            excludeOnline = (where.isEmpty() ? "WHERE " : "AND ") + "uuid NOT IN (" + placeholders + ") ";
        }
        String query = "SELECT COUNT(*) FROM (SELECT uuid, SUM(duration) as total FROM playtime_sessions "
                + where + excludeOnline + "GROUP BY uuid HAVING total > ?) ranked";

        int higherCount = 0;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            int idx = 1;
            for (UUID onlineUuid : onlineUuids) {
                ps.setString(idx++, onlineUuid.toString());
            }
            ps.setLong(idx, myTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    higherCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logDbError("Erreur lors du calcul du rang", e);
        }

        // Online players aren't reflected in the DB sum yet (in-progress session) — compare their
        // live total (DB + current session) directly instead.
        for (UUID onlineUuid : onlineUuids) {
            if (onlineUuid.toString().equalsIgnoreCase(uuid)) {
                continue;
            }
            long otherTime = getPlaytime(onlineUuid.toString(), type);
            if (otherTime > myTime) {
                higherCount++;
            }
        }

        return higherCount + 1;
    }

    public Map<String, Long> getTopPlayers(String type) {
        return getTopPlayers(type, 10);
    }

    /**
     * Loads the top {@code limit} players by playtime, plus any currently-online players (whose
     * in-progress session time isn't in the DB yet and could otherwise push them into the top
     * results without being fetched), instead of aggregating every lifetime player's full session
     * history — which used to load and sort the entire playtime_sessions table on every call.
     */
    public Map<String, Long> getTopPlayers(String type, int limit) {
        String dateFilter = getDateFilter(canonicalPeriod(type));
        String where = dateFilter.isEmpty() ? "" : "WHERE " + dateFilter.substring(4) + " ";

        Map<String, RankedPlayer> byUuid = new HashMap<>();

        String topQuery = "SELECT uuid, username, SUM(duration) as total FROM playtime_sessions " + where
                + "GROUP BY uuid ORDER BY total DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(topQuery)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    collectRow(byUuid, rs);
                }
            }
        } catch (SQLException e) {
            logDbError("Erreur lors de la lecture du classement", e);
        }

        java.util.Set<UUID> onlineUuids = SessionListener.getOnlineUuids();
        java.util.List<String> missingOnline = new java.util.ArrayList<>();
        for (UUID uuid : onlineUuids) {
            if (!byUuid.containsKey(uuid.toString())) {
                missingOnline.add(uuid.toString());
            }
        }
        if (!missingOnline.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(missingOnline.size(), "?"));
            String onlineQuery = "SELECT uuid, username, SUM(duration) as total FROM playtime_sessions "
                    + where + (where.isEmpty() ? "WHERE " : "AND ") + "uuid IN (" + placeholders + ") GROUP BY uuid";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(onlineQuery)) {
                for (int i = 0; i < missingOnline.size(); i++) {
                    ps.setString(i + 1, missingOnline.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        collectRow(byUuid, rs);
                    }
                }
            } catch (SQLException e) {
                logDbError("Erreur lors de la lecture du classement (joueurs en ligne)", e);
            }
            // Online players with no session_date row matching the period filter yet (e.g. brand
            // new player, first session still in progress) still need to be considered by name.
            for (String uuidStr : missingOnline) {
                byUuid.putIfAbsent(uuidStr, new RankedPlayer(uuidStr, SessionListener.usernameOf(UUID.fromString(uuidStr)), 0L));
            }
        }

        Map<String, Long> tempMap = new HashMap<>();
        for (RankedPlayer p : byUuid.values()) {
            long total = p.total;
            try {
                total += SessionListener.getCurrentSession(UUID.fromString(p.uuid));
            } catch (Exception ignored) {
            }
            if (p.username != null) {
                tempMap.put(p.username, total);
            }
        }

        return tempMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static void collectRow(Map<String, RankedPlayer> byUuid, ResultSet rs) throws SQLException {
        String rowUuid = rs.getString("uuid");
        String name = rs.getString("username");
        long total = rs.getLong("total");
        byUuid.put(rowUuid, new RankedPlayer(rowUuid, name, total));
    }

    private record RankedPlayer(String uuid, String username, long total) {}

    private String getDateFilter(String type) {
        if (isMySQL) {
            if (type.equalsIgnoreCase("daily")) {
                return "AND session_date = CURDATE() ";
            }
            if (type.equalsIgnoreCase("weekly")) {
                return "AND session_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) ";
            }
            if (type.equalsIgnoreCase("monthly")) {
                return "AND session_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) ";
            }
        } else {
            if (type.equalsIgnoreCase("daily")) {
                return "AND session_date = date('now') ";
            }
            if (type.equalsIgnoreCase("weekly")) {
                return "AND session_date >= date('now', '-7 days') ";
            }
            if (type.equalsIgnoreCase("monthly")) {
                return "AND session_date >= date('now', '-1 month') ";
            }
        }
        return "";
    }

    public long getFirstLogin(String uuid) {
        long dbMin = 0L;
        String query = "SELECT MIN(start_time) FROM playtime_sessions WHERE uuid = ?";
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long v = rs.getLong(1);
                if (!rs.wasNull()) {
                    dbMin = v;
                }
            }
        } catch (SQLException e) {
            logDbError("Erreur lors de la lecture de la première connexion", e);
        }
        long onlineStart = 0L;
        try {
            onlineStart = SessionListener.getSessionJoinTime(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
        }
        if (onlineStart > 0L) {
            if (dbMin <= 0L) {
                return onlineStart;
            }
            return Math.min(dbMin, onlineStart);
        }
        return dbMin;
    }

    public long getLastLogin(String uuid) {
        long dbMax = 0L;
        String query = "SELECT MAX(start_time) FROM playtime_sessions WHERE uuid = ?";
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long v = rs.getLong(1);
                if (!rs.wasNull()) {
                    dbMax = v;
                }
            }
        } catch (SQLException e) {
            logDbError("Erreur lors de la lecture de la dernière connexion", e);
        }
        long onlineStart = 0L;
        try {
            onlineStart = SessionListener.getSessionJoinTime(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
        }
        if (onlineStart > 0L) {
            if (dbMax <= 0L) {
                return onlineStart;
            }
            return Math.max(dbMax, onlineStart);
        }
        return dbMax;
    }

    public void resetPlaytime(String uuid) {
        db.resetSessions(uuid);
    }

    public void setPlaytime(String uuid, String username, long millis) {
        db.resetSessions(uuid);
        if (millis > 0) {
            long now = System.currentTimeMillis();
            saveSession(uuid, username, now - millis, millis);
        }
    }

    private String canonicalPeriod(String type) {
        try {
            String k = Playtime.get().getConfigManager().getConfig().resolvePeriodKey(type);
            return k != null ? k : "all";
        } catch (Exception e) {
            return "all";
        }
    }
}
