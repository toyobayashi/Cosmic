package database;

import java.sql.Connection;
import java.sql.SQLException;

public final class MySqlDialect implements DatabaseDialect {
    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String upsertQuickslotSql() {
        return "INSERT INTO quickslotkeymapped (accountid, keymap) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE keymap = ?";
    }

    @Override
    public String upsertMonsterBookSql() {
        return "INSERT INTO monsterbook (charid, cardid, level) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE level = ?";
    }

    @Override
    public String upsertSkillSql() {
        return "REPLACE INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    public String insertCouponSql() {
        return "INSERT IGNORE INTO `nxcode` (`code`, `expiration`) VALUES (?, ?)";
    }

    @Override
    public String insertCouponItemSql() {
        return "INSERT IGNORE INTO `nxcode_items` (`codeid`, `type`, `item`, `quantity`) VALUES (?, ?, ?, ?)";
    }

    @Override
    public String insertExtremeEntrySql() {
        return "INSERT IGNORE INTO monster_park_extreme_entries (characterid, week_start) VALUES (?, ?)";
    }

    @Override
    public String forUpdateClause() {
        return " FOR UPDATE";
    }

    @Override
    public void beginWriteTransaction(Connection connection) throws SQLException {
        // MySQL starts the transaction when the first statement is executed.
    }
}
