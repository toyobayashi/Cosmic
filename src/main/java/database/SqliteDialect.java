package database;

import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

public final class SqliteDialect implements DatabaseDialect {
    public static String connectionInitSql(int busyTimeoutMs) {
        return "PRAGMA foreign_keys = ON;"
                + " PRAGMA busy_timeout = " + busyTimeoutMs + ";"
                + " PRAGMA synchronous = FULL;";
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLITE;
    }

    @Override
    public String upsertQuickslotSql() {
        return "INSERT INTO quickslotkeymapped (accountid, keymap) VALUES (?, ?) "
                + "ON CONFLICT(accountid) DO UPDATE SET keymap = ?";
    }

    @Override
    public String upsertMonsterBookSql() {
        return "INSERT INTO monsterbook (charid, cardid, level) VALUES (?, ?, ?) "
                + "ON CONFLICT(charid, cardid) DO UPDATE SET level = ?";
    }

    @Override
    public String upsertSkillSql() {
        return "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(skillid, characterid) DO UPDATE SET skilllevel = excluded.skilllevel, "
                + "masterlevel = excluded.masterlevel, expiration = excluded.expiration";
    }

    @Override
    public String insertCouponSql() {
        return "INSERT INTO `nxcode` (`code`, `expiration`) VALUES (?, ?) ON CONFLICT(`code`) DO NOTHING";
    }

    @Override
    public String insertCouponItemSql() {
        return "INSERT INTO `nxcode_items` (`codeid`, `type`, `item`, `quantity`) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT DO NOTHING";
    }

    @Override
    public String insertExtremeEntrySql() {
        return "INSERT INTO monster_park_extreme_entries (characterid, week_start) VALUES (?, ?) "
                + "ON CONFLICT(characterid, week_start) DO NOTHING";
    }

    @Override
    public String forUpdateClause() {
        return "";
    }

    @Override
    public void beginWriteTransaction(Connection connection) throws SQLException {
        // Xerial's JDBC driver starts the transaction when auto-commit is disabled.
        // Configure that implicit BEGIN as IMMEDIATE so a later read-then-write
        // sequence cannot fail while upgrading a deferred read snapshot.
        if (connection instanceof SQLiteConnection sqliteConnection) {
            sqliteConnection.setCurrentTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        }
    }
}
