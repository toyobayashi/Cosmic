package database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseDialect {
    DatabaseType type();

    String upsertQuickslotSql();

    String upsertMonsterBookSql();

    String upsertSkillSql();

    String insertCouponSql();

    String insertCouponItemSql();

    String insertExtremeEntrySql();

    String forUpdateClause();

    void beginWriteTransaction(Connection connection) throws SQLException;
}
