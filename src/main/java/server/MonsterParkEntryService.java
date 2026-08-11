package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public final class MonsterParkEntryService {
    private static final Logger log = LoggerFactory.getLogger(MonsterParkEntryService.class);
    private static final int MAX_ADDITIONAL_ENTRIES = 6;
    private MonsterParkEntryService() {
    }

    public record EntryState(int freeEntries, int additionalEntries) {
    }

    public enum EntryResult {
        FREE(0),
        ADDITIONAL(1),
        TICKET_REQUIRED(2),
        DAILY_LIMIT_REACHED(3),
        ERROR(4);

        private final int code;

        EntryResult(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static EntryState getEntryState(int characterId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("""
                     SELECT free_entries, additional_entries
                     FROM monster_park_entries
                     WHERE characterid = ? AND entrydate = ?
                     """)) {
            ps.setInt(1, characterId);
            ps.setString(2, currentDate().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EntryState(rs.getInt("free_entries"), rs.getInt("additional_entries"));
                }
            }
            return new EntryState(0, 0);
        } catch (SQLException e) {
            log.error("Failed to read Monster Park entry state for character {}", characterId, e);
            return null;
        }
    }

    public static synchronized EntryResult tryRegisterEntry(int characterId, boolean hasAdditionalEntryTicket) {
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseConnection.getDialect().beginWriteTransaction(con);
            con.setAutoCommit(false);
            try {
                EntryState state = selectEntryStateForUpdate(con, characterId);
                EntryResult result;

                if (state == null) {
                    insertFirstFreeEntry(con, characterId);
                    result = EntryResult.FREE;
                } else if (state.freeEntries() == 0) {
                    updateEntryCounts(con, characterId, 1, state.additionalEntries());
                    result = EntryResult.FREE;
                } else if (state.additionalEntries() >= MAX_ADDITIONAL_ENTRIES) {
                    result = EntryResult.DAILY_LIMIT_REACHED;
                } else if (!hasAdditionalEntryTicket) {
                    result = EntryResult.TICKET_REQUIRED;
                } else {
                    updateEntryCounts(con, characterId, state.freeEntries(), state.additionalEntries() + 1);
                    result = EntryResult.ADDITIONAL;
                }

                con.commit();
                return result;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Failed to register Monster Park entry for character {}", characterId, e);
            return EntryResult.ERROR;
        }
    }

    public static int getExtremeEntryStatus(int characterId) {
        String sql = """
                SELECT 1
                FROM monster_park_extreme_entries
                WHERE characterid = ? AND week_start = ?
                """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            ps.setString(2, currentThursday().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? 1 : 0;
            }
        } catch (SQLException e) {
            log.error("Failed to read Extreme Monster Park entry state for character {}", characterId, e);
            return -1;
        }
    }

    public static synchronized int tryRegisterExtremeEntry(int characterId) {
        String sql = """
                %s
                """.formatted(DatabaseConnection.getDialect().insertExtremeEntrySql());
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            ps.setString(2, currentThursday().toString());
            return ps.executeUpdate() == 1 ? 0 : 1;
        } catch (SQLException e) {
            log.error("Failed to register Extreme Monster Park entry for character {}", characterId, e);
            return -1;
        }
    }

    private static EntryState selectEntryStateForUpdate(Connection con, int characterId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT free_entries, additional_entries
                FROM monster_park_entries
                WHERE characterid = ? AND entrydate = ?
                """ + DatabaseConnection.getDialect().forUpdateClause())) {
            ps.setInt(1, characterId);
            ps.setString(2, currentDate().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EntryState(rs.getInt("free_entries"), rs.getInt("additional_entries"));
                }
                return null;
            }
        }
    }

    private static void insertFirstFreeEntry(Connection con, int characterId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO monster_park_entries
                    (characterid, entrydate, free_entries, additional_entries)
                VALUES (?, ?, 1, 0)
                """)) {
            ps.setInt(1, characterId);
            ps.setString(2, currentDate().toString());
            ps.executeUpdate();
        }
    }

    private static void updateEntryCounts(
            Connection con,
            int characterId,
            int freeEntries,
            int additionalEntries
    ) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                UPDATE monster_park_entries
                SET free_entries = ?, additional_entries = ?, updated_at = CURRENT_TIMESTAMP
                WHERE characterid = ? AND entrydate = ?
                """)) {
            ps.setInt(1, freeEntries);
            ps.setInt(2, additionalEntries);
            ps.setInt(3, characterId);
            ps.setString(4, currentDate().toString());
            ps.executeUpdate();
        }
    }

    private static LocalDate currentDate() {
        return LocalDate.now();
    }

    private static LocalDate currentThursday() {
        LocalDate today = currentDate();
        return today.minusDays((today.getDayOfWeek().getValue() + 3L) % 7L);
    }
}
