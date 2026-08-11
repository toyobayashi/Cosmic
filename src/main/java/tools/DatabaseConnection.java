package tools;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.YamlConfig;
import database.DatabaseDialect;
import database.DatabaseType;
import database.MySqlDialect;
import database.note.NoteRowMapper;
import database.SqliteDialect;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.TimeZone;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Frz (Big Daddy)
 * @author The Real Spookster - some modifications to this beautiful code
 * @author Ronan - some connection pool to this beautiful code
 */
public class DatabaseConnection {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;
    private static Jdbi jdbi;
    private static DatabaseType databaseType;
    private static DatabaseDialect databaseDialect;
    private static String databaseUrl;

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Unable to get connection - connection pool is uninitialized");
        }

        Connection connection = dataSource.getConnection();
        if (databaseType != null && databaseType.isSqlite()) {
            try {
                configureSqliteConnection(connection);
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
        }
        return connection;
    }

    public static Handle getHandle() {
        if (jdbi == null) {
            throw new IllegalStateException("Unable to get handle - connection pool is uninitialized");
        }

        return jdbi.open();
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        jdbi = null;
        databaseType = null;
        databaseDialect = null;
        databaseUrl = null;
    }

    public static DatabaseType getDatabaseType() {
        if (databaseType != null) {
            return databaseType;
        }
        return resolveDatabaseType();
    }

    public static DatabaseDialect getDialect() {
        if (databaseDialect != null) {
            return databaseDialect;
        }
        return getDatabaseType().isSqlite() ? new SqliteDialect() : new MySqlDialect();
    }

    public static boolean isSqlite() {
        return getDatabaseType().isSqlite();
    }

    public static void setTransactionIsolation(Connection connection, int level) throws SQLException {
        if (!isSqlite()) {
            connection.setTransactionIsolation(level);
        }
    }

    /**
     * Bind temporal values in UTC for SQLite. Xerial formats TEXT dates using
     * the Calendar supplied to setTimestamp/setDate; without this explicit
     * calendar it uses the JVM default timezone while CURRENT_TIMESTAMP is UTC.
     */
    public static void setTimestamp(PreparedStatement statement, int parameterIndex, Timestamp value) throws SQLException {
        if (isSqlite()) {
            statement.setTimestamp(parameterIndex, value, utcCalendar());
        } else {
            statement.setTimestamp(parameterIndex, value);
        }
    }

    public static void setDate(PreparedStatement statement, int parameterIndex, Date value) throws SQLException {
        if (isSqlite()) {
            statement.setString(parameterIndex, value + " 00:00:00");
        } else {
            statement.setDate(parameterIndex, value);
        }
    }

    public static void setLocalDateTime(PreparedStatement statement, int parameterIndex, LocalDateTime value) throws SQLException {
        if (isSqlite()) {
            Timestamp timestamp = Timestamp.from(value.atZone(ZoneId.systemDefault()).toInstant());
            setTimestamp(statement, parameterIndex, timestamp);
        } else {
            statement.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        }
    }

    public static Timestamp getTimestamp(ResultSet resultSet, int columnIndex) throws SQLException {
        return isSqlite()
                ? resultSet.getTimestamp(columnIndex, utcCalendar())
                : resultSet.getTimestamp(columnIndex);
    }

    public static Timestamp getTimestamp(ResultSet resultSet, String columnLabel) throws SQLException {
        return isSqlite()
                ? resultSet.getTimestamp(columnLabel, utcCalendar())
                : resultSet.getTimestamp(columnLabel);
    }

    public static Date getDate(ResultSet resultSet, int columnIndex) throws SQLException {
        if (!isSqlite()) {
            return resultSet.getDate(columnIndex);
        }
        return parseSqliteDate(resultSet.getString(columnIndex));
    }

    public static Date getDate(ResultSet resultSet, String columnLabel) throws SQLException {
        if (!isSqlite()) {
            return resultSet.getDate(columnLabel);
        }
        return parseSqliteDate(resultSet.getString(columnLabel));
    }

    private static Date parseSqliteDate(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        String dateValue = value.length() > 10 ? value.substring(0, 10) : value;
        try {
            return Date.valueOf(dateValue);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Error parsing SQLite date", e);
        }
    }

    private static Calendar utcCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    private static DatabaseType resolveDatabaseType() {
        String configuredType = environmentOrConfig("DB_TYPE", YamlConfig.config.server.DB_TYPE);
        return DatabaseType.fromConfig(configuredType);
    }

    private static String environmentOrConfig(String name, String configuredValue) {
        String environmentValue = System.getenv(name);
        return environmentValue == null || environmentValue.isBlank() ? configuredValue : environmentValue;
    }

    private static String getDbUrl() {
        // Environment variables override what's defined in the config file
        // This feature is used for the Docker support
        DatabaseType type = getDatabaseType();
        String explicitUrl = environmentOrConfig("DB_URL", YamlConfig.config.server.DB_URL);
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            databaseUrl = explicitUrl;
            return explicitUrl;
        }

        if (type.isSqlite()) {
            String configuredPath = environmentOrConfig("SQLITE_PATH", YamlConfig.config.server.SQLITE_PATH);
            if (configuredPath == null || configuredPath.isBlank()) {
                configuredPath = "database/cosmic.db";
            }
            if (configuredPath.startsWith("jdbc:sqlite:")) {
                databaseUrl = configuredPath;
                return configuredPath;
            }

            try {
                Path path = Path.of(configuredPath).toAbsolutePath().normalize();
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                databaseUrl = "jdbc:sqlite:" + path;
                return databaseUrl;
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to prepare SQLite database path '" + configuredPath + "'", e);
            }
        }

        String hostOverride = System.getenv("DB_HOST");
        String host = hostOverride != null ? hostOverride : YamlConfig.config.server.DB_HOST;
        databaseUrl = String.format(YamlConfig.config.server.DB_URL_FORMAT, host);
        return databaseUrl;
    }

    private static HikariConfig getConfig() {
        HikariConfig config = new HikariConfig();

        DatabaseType type = getDatabaseType();
        config.setJdbcUrl(getDbUrl());

        if (!type.isSqlite()) {
            config.setUsername(YamlConfig.config.server.DB_USER);
            config.setPassword(YamlConfig.config.server.DB_PASS);
        }

        final int initFailTimeoutSeconds = YamlConfig.config.server.INIT_CONNECTION_POOL_TIMEOUT;
        config.setInitializationFailTimeout(SECONDS.toMillis(initFailTimeoutSeconds));
        config.setConnectionTimeout(SECONDS.toMillis(30)); // Hikari default
        config.setMaximumPoolSize(type.isSqlite()
                ? Math.max(1, YamlConfig.config.server.SQLITE_POOL_SIZE)
                : 10);

        if (type.isSqlite()) {
            int busyTimeout = Math.max(0, YamlConfig.config.server.SQLITE_BUSY_TIMEOUT_MS);
            config.setConnectionInitSql(SqliteDialect.connectionInitSql(busyTimeout));
            // These are also passed as driver properties so connections opened
            // directly by Jdbi receive the same settings as JDBC callers.
            config.addDataSourceProperty("foreign_keys", true);
            config.addDataSourceProperty("busy_timeout", busyTimeout);
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "FULL");
            config.addDataSourceProperty("transaction_mode", "IMMEDIATE");
            config.addDataSourceProperty("date_class", "TEXT");
            config.addDataSourceProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");
        } else {
            config.addDataSourceProperty("cachePrepStmts", true);
            config.addDataSourceProperty("prepStmtCacheSize", 25);
            config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            config.addDataSourceProperty("useUnicode", true);
            config.addDataSourceProperty("characterEncoding", "UTF-8");
            config.addDataSourceProperty("connectionCollation", "utf8mb4_unicode_ci");
        }

        return config;
    }

    /**
     * Initiate connection to the database
     *
     * @return true if connection to the database initiated successfully, false if not successful
     */
    public static boolean initializeConnectionPool() {
        if (dataSource != null) {
            return true;
        }

        final HikariConfig config = getConfig();
        databaseType = resolveDatabaseType();
        databaseDialect = databaseType.isSqlite() ? new SqliteDialect() : new MySqlDialect();
        log.info("Initializing {} database connection pool. Connecting to '{}' with pool size {}{}",
                databaseType.configValue(), config.getJdbcUrl(), config.getMaximumPoolSize(),
                databaseType.isSqlite() ? " (SQLite user/password are ignored)" : "");
        Instant initStart = Instant.now();
        try {
            dataSource = new HikariDataSource(config);
            if (databaseType.isSqlite()) {
                initializeSqliteDatabase(dataSource);
            }
            initializeJdbi(dataSource);
            long initDuration = Duration.between(initStart, Instant.now()).toMillis();
            log.info("Connection pool initialized in {} ms", initDuration);
            return true;
        } catch (Exception e) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
            long timeout = Duration.between(initStart, Instant.now()).getSeconds();
            log.error("Failed to initialize {} database connection pool. Gave up after {} seconds.",
                    databaseType.configValue(), timeout, e);
        }

        // Timed out - failed to initialize
        return false;
    }

    private static void initializeJdbi(DataSource dataSource) {
        jdbi = Jdbi.create(dataSource)
                .registerRowMapper(new NoteRowMapper());
    }

    private static void initializeSqliteDatabase(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            configureSqliteConnection(connection);
            try (var resultSet = statement.executeQuery("PRAGMA journal_mode = WAL")) {
                if (!resultSet.next() || !"wal".equalsIgnoreCase(resultSet.getString(1))) {
                    throw new SQLException("SQLite did not enable WAL journal mode");
                }
            }
            log.info("SQLite ready: path='{}', journal_mode=WAL, synchronous=FULL, foreign_keys=ON, busy_timeout={} ms",
                    databaseUrl.substring("jdbc:sqlite:".length()),
                    Math.max(0, YamlConfig.config.server.SQLITE_BUSY_TIMEOUT_MS));
        }
    }

    private static void configureSqliteConnection(Connection connection) throws SQLException {
        int busyTimeout = Math.max(0, YamlConfig.config.server.SQLITE_BUSY_TIMEOUT_MS);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = " + busyTimeout);
            statement.execute("PRAGMA synchronous = FULL");
        }
    }
}
