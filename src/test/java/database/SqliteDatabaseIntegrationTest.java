package database;

import api.model.dto.AddAccountDTO;
import api.service.AccountService;
import client.DefaultDates;
import config.ServerConfig;
import config.YamlConfig;
import api.service.JdbiSponsorRepository;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.server.Server;
import server.MonsterParkEntryService;
import server.expeditions.ExpeditionBossLog;
import tools.DatabaseConnection;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void createsAndReopensCompleteSqliteDatabase() throws Exception {
        YamlConfig previousConfig = YamlConfig.config;
        TimeZone previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        YamlConfig.config = new YamlConfig();
        YamlConfig.config.server = new ServerConfig();
        YamlConfig.config.server.DB_TYPE = "sqlite";
        YamlConfig.config.server.DB_URL = "jdbc:sqlite:" + tempDir.resolve("cosmic.db");
        YamlConfig.config.server.SQLITE_BUSY_TIMEOUT_MS = 5000;
        YamlConfig.config.server.SQLITE_POOL_SIZE = 2;
        YamlConfig.config.server.INIT_CONNECTION_POOL_TIMEOUT = 30;
        YamlConfig.config.server.USE_CLEAR_OUTDATED_COUPONS = true;
        YamlConfig.config.server.TIMEZONE = "Asia/Shanghai";

        try {
            assertTrue(DatabaseConnection.initializeConnectionPool());
            DatabaseMigrations.runDatabaseMigrations();

            try (var connection = DatabaseConnection.getConnection();
                 var tables = connection.createStatement().executeQuery(
                         "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'DATABASECHANGELOG%' AND name <> 'sqlite_sequence'") ) {
                Set<String> tableNames = new HashSet<>();
                while (tables.next()) {
                    tableNames.add(tables.getString(1));
                }
                assertEquals(76, tableNames.size());
                assertTrue(tableNames.contains("accounts"));
                assertTrue(tableNames.contains("monster_park_entries"));
                assertTrue(tableNames.contains("sponsor_orders"));
            }

            try (var connection = DatabaseConnection.getConnection();
                 var statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM accounts WHERE name = 'admin'")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
                try (ResultSet foreignKeys = statement.executeQuery("PRAGMA foreign_key_check")) {
                    assertFalse(foreignKeys.next());
                }
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT birthday FROM accounts WHERE name = 'admin'")) {
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertTrue(DatabaseConnection.getDate(result, 1) != null);
                }
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT tempban FROM accounts WHERE name = 'admin'")) {
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("2005-05-10 16:00:00", result.getString(1));
                    assertEquals(DefaultDates.getTempban(),
                            DatabaseConnection.getTimestamp(result, 1).toLocalDateTime());
                }
            }
            try (var connection = DatabaseConnection.getConnection();
                 var update = connection.prepareStatement("UPDATE accounts SET tempban = ? WHERE name = 'admin'")) {
                DatabaseConnection.setLocalDateTime(update, 1, DefaultDates.getTempban());
                update.executeUpdate();
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT tempban FROM accounts WHERE name = 'admin'")) {
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(DefaultDates.getTempban(),
                            DatabaseConnection.getTimestamp(result, 1).toLocalDateTime());
                }
            }
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT birthday FROM accounts WHERE name = 'admin'")) {
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(LocalDate.of(2005, 5, 11),
                            DatabaseConnection.getDate(result, 1).toLocalDate());
                }
            } finally {
                TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            }

            AddAccountDTO dto = new AddAccountDTO();
            dto.setName("sqlite_api_account");
            dto.setPassword("sqlite-api-password");
            dto.setEmail("sqlite-api@example.com");
            new AccountService().addAccount(dto);
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT tempban FROM accounts WHERE name = ?")) {
                query.setString(1, dto.getName());
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("2005-05-10 16:00:00", result.getString(1));
                    assertEquals(DefaultDates.getTempban(),
                            DatabaseConnection.getTimestamp(result, 1).toLocalDateTime());
                }
            }

            int accountId;
            int characterId;
            try (var connection = DatabaseConnection.getConnection();
                 var insert = connection.prepareStatement(
                         "INSERT INTO accounts (name, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, "sqlite_test_account");
                insert.setString(2, "test");
                insert.executeUpdate();
                try (var keys = insert.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    accountId = keys.getInt(1);
                }
            }

            try (var connection = DatabaseConnection.getConnection();
                 var insert = connection.prepareStatement(
                         "INSERT INTO characters (accountid, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, accountId);
                insert.setString(2, "sqlite_test_character");
                insert.executeUpdate();
                try (var keys = insert.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    characterId = keys.getInt(1);
                    try (var upsert = connection.prepareStatement(DatabaseConnection.getDialect().upsertMonsterBookSql())) {
                        upsert.setInt(1, characterId);
                        upsert.setInt(2, 123456);
                        upsert.setInt(3, 1);
                        upsert.setInt(4, 2);
                        upsert.executeUpdate();
                    }
                    try (var upsert = connection.prepareStatement(DatabaseConnection.getDialect().upsertMonsterBookSql())) {
                        upsert.setInt(1, characterId);
                        upsert.setInt(2, 123456);
                        upsert.setInt(3, 1);
                        upsert.setInt(4, 3);
                        upsert.executeUpdate();
                    }
                    try (var query = connection.prepareStatement("SELECT level FROM monsterbook WHERE charid = ? AND cardid = ?")) {
                        query.setInt(1, characterId);
                        query.setInt(2, 123456);
                        try (var result = query.executeQuery()) {
                            assertTrue(result.next());
                            assertEquals(3, result.getInt(1));
                        }
                    }
                }
            }

            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement(
                         "SELECT b.buddyid, b.pending, b.`group`, c.name AS buddyname "
                                 + "FROM buddies AS b, characters AS c "
                                 + "WHERE c.id = b.buddyid AND b.characterid = ?")) {
                query.setInt(1, characterId);
                try (var result = query.executeQuery()) {
                    assertFalse(result.next());
                }
            }

            Timestamp now = Timestamp.from(Instant.now());
            try (var connection = DatabaseConnection.getConnection();
                 var update = connection.prepareStatement("UPDATE accounts SET lastlogin = ? WHERE id = ?")) {
                DatabaseConnection.setTimestamp(update, 1, now);
                update.setInt(2, accountId);
                update.executeUpdate();
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement(
                         "SELECT typeof(lastlogin), lastlogin, strftime('%s', lastlogin), "
                                 + "strftime('%s', CURRENT_TIMESTAMP) FROM accounts WHERE id = ?")) {
                query.setInt(1, accountId);
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("text", result.getString(1));
                    assertEquals(now.getTime(), DatabaseConnection.getTimestamp(result, 2).getTime(), 1000);
                    assertEquals(result.getLong(3), result.getLong(4), 2);
                }
            }

            Instant futureExpiry = Instant.now().plusSeconds(24 * 60 * 60);
            Instant expiredExpiry = Instant.now().minusSeconds(24 * 60 * 60);
            try (var connection = DatabaseConnection.getConnection()) {
                SessionDAO.registerAccountAccess(connection, accountId, new Hwid("future-hwid"), futureExpiry);
                SessionDAO.registerAccountAccess(connection, accountId, new Hwid("expired-hwid"), expiredExpiry);
            }
            SessionDAO.deleteExpiredHwidAccounts();
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT hwid, typeof(expiresat) FROM hwidaccounts WHERE accountid = ? ORDER BY hwid")) {
                query.setInt(1, accountId);
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("future-hwid", result.getString("hwid"));
                    assertEquals("text", result.getString(2));
                    assertFalse(result.next());
                }
            }

            Timestamp oldFame = Timestamp.from(Instant.now().minusSeconds(31L * 24 * 60 * 60));
            Timestamp recentFame = Timestamp.from(Instant.now().minusSeconds(24 * 60 * 60));
            try (var connection = DatabaseConnection.getConnection();
                var insert = connection.prepareStatement(
                         "INSERT INTO famelog (characterid, characterid_to, `when`) VALUES (?, ?, ?)")) {
                insert.setInt(1, characterId);
                insert.setInt(2, 2001);
                DatabaseConnection.setTimestamp(insert, 3, oldFame);
                insert.executeUpdate();
                insert.setInt(2, 2002);
                DatabaseConnection.setTimestamp(insert, 3, recentFame);
                insert.executeUpdate();
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement(
                         "SELECT characterid_to FROM famelog WHERE characterid = ? AND `when` >= ?")) {
                query.setInt(1, characterId);
                DatabaseConnection.setTimestamp(query, 2,
                        Timestamp.from(Instant.now().minusSeconds(30L * 24 * 60 * 60)));
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(2002, result.getInt(1));
                    assertFalse(result.next());
                }
            }

            Timestamp oldBossAttempt = Timestamp.from(Instant.now().minusSeconds(2L * 24 * 60 * 60));
            try (var connection = DatabaseConnection.getConnection();
                 var insertDaily = connection.prepareStatement(
                         "INSERT INTO bosslog_daily (characterid, bosstype, attempttime) VALUES (?, ?, ?)");
                 var insertWeekly = connection.prepareStatement(
                         "INSERT INTO bosslog_weekly (characterid, bosstype, attempttime) VALUES (?, ?, ?)")) {
                insertDaily.setInt(1, characterId);
                insertDaily.setString(2, "ZAKUM");
                DatabaseConnection.setTimestamp(insertDaily, 3, oldBossAttempt);
                insertDaily.executeUpdate();
                insertWeekly.setInt(1, characterId);
                insertWeekly.setString(2, "ZAKUM");
                DatabaseConnection.setTimestamp(insertWeekly, 3, oldBossAttempt);
                insertWeekly.executeUpdate();
            }
            ExpeditionBossLog.resetBossLogTable();
            try (var connection = DatabaseConnection.getConnection();
                 var deleteWeekly = connection.prepareStatement(
                         "DELETE FROM bosslog_weekly WHERE attempttime <= ? AND bosstype LIKE ?");
                 var dailyCount = connection.prepareStatement(
                         "SELECT COUNT(*) FROM bosslog_daily WHERE characterid = ?");
                 var weeklyCount = connection.prepareStatement(
                         "SELECT COUNT(*) FROM bosslog_weekly WHERE characterid = ?")) {
                DatabaseConnection.setTimestamp(deleteWeekly, 1, Timestamp.from(Instant.now()));
                deleteWeekly.setString(2, "ZAKUM");
                assertEquals(1, deleteWeekly.executeUpdate());
                dailyCount.setInt(1, characterId);
                try (var result = dailyCount.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
                weeklyCount.setInt(1, characterId);
                try (var result = weeklyCount.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
            }

            JdbiSponsorRepository sponsorRepository = new JdbiSponsorRepository();
            var sponsorOrder = sponsorRepository.createOrder(accountId, 100, 500, "SQLite test");
            assertEquals("PENDING", sponsorOrder.getStatus());
            var awardedOrder = sponsorRepository.awardPendingOrder(sponsorOrder.getOrderId(), 500, 1);
            assertEquals("AWARDED", awardedOrder.getStatus());
            assertThrows(IllegalArgumentException.class,
                    () -> sponsorRepository.awardPendingOrder(sponsorOrder.getOrderId(), 500, 1));
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT nxCredit FROM accounts WHERE id = ?")) {
                query.setInt(1, accountId);
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(500, result.getInt(1));
                }
            }

            long expiredAt = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000;
            int expiredCouponId;
            try (var connection = DatabaseConnection.getConnection();
                 var insertCoupon = connection.prepareStatement(
                         "INSERT INTO nxcode (code, expiration) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insertCoupon.setString(1, "sqlite-expired-coupon");
                insertCoupon.setLong(2, expiredAt);
                insertCoupon.executeUpdate();
                try (var keys = insertCoupon.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    expiredCouponId = keys.getInt(1);
                }
                try (var insertItem = connection.prepareStatement("INSERT INTO nxcode_items (codeid) VALUES (?)")) {
                    insertItem.setInt(1, expiredCouponId);
                    insertItem.executeUpdate();
                }
            }
            try (var connection = DatabaseConnection.getConnection()) {
                Server.cleanNxcodeCoupons(connection);
            }
            try (var connection = DatabaseConnection.getConnection();
                 var query = connection.prepareStatement("SELECT COUNT(*) FROM nxcode_items WHERE codeid = ?")) {
                query.setInt(1, expiredCouponId);
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
            }

            assertEquals(MonsterParkEntryService.EntryResult.FREE,
                    MonsterParkEntryService.tryRegisterEntry(characterId, false));
            assertEquals(MonsterParkEntryService.EntryResult.TICKET_REQUIRED,
                    MonsterParkEntryService.tryRegisterEntry(characterId, false));
            assertEquals(0, MonsterParkEntryService.tryRegisterExtremeEntry(characterId));
            assertEquals(1, MonsterParkEntryService.tryRegisterExtremeEntry(characterId));

            int changesetsBefore;
            try (var connection = DatabaseConnection.getConnection();
                 var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG")) {
                result.next();
                changesetsBefore = result.getInt(1);
            }

            DatabaseMigrations.runDatabaseMigrations();

            try (var connection = DatabaseConnection.getConnection();
                 var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG")) {
                result.next();
                assertEquals(changesetsBefore, result.getInt(1));
            }
        } finally {
            DatabaseConnection.shutdown();
            YamlConfig.config = previousConfig;
            TimeZone.setDefault(previousTimeZone);
        }
    }
}
