package database;

import client.DefaultDates;
import config.YamlConfig;
import liquibase.Liquibase;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.DatabaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apply changes to the database so that the server and database work in harmony.
 *
 * @author Ponk
 */
public class DatabaseMigrations {

    public static void runDatabaseMigrations() {
        suppressLiquibaseLogs();
        runLiquibaseUpdate();
    }

    private static void suppressLiquibaseLogs() {
        Logger liquibaseLogger = Logger.getLogger("liquibase");
        liquibaseLogger.setLevel(Level.WARNING);
    }

    private static void runLiquibaseUpdate() {
        Path changelogDir = extractChangelogs();
        try (Connection connection = DatabaseConnection.getConnection()) {
            liquibase.database.DatabaseConnection databaseConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(databaseConnection);

            String changelogPath = DatabaseConnection.getDatabaseType().isSqlite()
                    ? "db/sqlite/changelog-root.xml"
                    : "db/changelog-root.xml";
            Liquibase liquibase = new Liquibase(
                    changelogPath,
                    new DirectoryResourceAccessor(changelogDir),
                    database);

            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
            liquibase.update();
            normalizeSqliteDefaultTempban(connection);
        } catch (SQLException | LiquibaseException | IOException e) {
            throw new RuntimeException("Failed to run database migrations", e);
        } finally {
            cleanupChangelogs(changelogDir);
        }
    }

    private static Path extractChangelogs() {
        try {
            Path tempDir = Files.createTempDirectory("cosmic-changelog");
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:db/**/*");
            for (Resource resource : resources) {
                String relativePath = getRelativeResourcePath(resource);
                if (relativePath == null || !relativePath.contains(".")) {
                    continue;
                }
                Path target = tempDir.resolve(relativePath);
                Files.createDirectories(target.getParent());
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (DatabaseConnection.getDatabaseType().isSqlite() && relativePath.endsWith(".sql")) {
                        content = normalizeSqliteComments(content);
                    }
                    Files.writeString(target, content, java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract liquibase changelogs", e);
        }
    }

    private static void normalizeSqliteDefaultTempban(Connection connection) throws SQLException {
        if (!DatabaseConnection.getDatabaseType().isSqlite()) {
            return;
        }

        ZoneId configuredZone = getConfiguredZone();
        String utcDefault = DefaultDates.getTempban()
                .atZone(configuredZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // The SQLite schema and old seed data used the symbolic local value.
        // Convert it once so getTimestamp(..., UTC) round-trips it back to the
        // configured local date-time used by the account checks.
        boolean autoCommit = connection.getAutoCommit();
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET tempban = ? WHERE tempban = '2005-05-11 00:00:00'")) {
            statement.setString(1, utcDefault);
            statement.executeUpdate();
            if (!autoCommit) {
                connection.commit();
            }
        }
    }

    private static ZoneId getConfiguredZone() {
        String timezone = YamlConfig.config.server.TIMEZONE;
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        return TimeZone.getTimeZone(timezone).toZoneId();
    }

    private static String normalizeSqliteComments(String content) {
        return Arrays.stream(content.split("\\R", -1))
                .map(line -> {
                    String trimmed = line.stripLeading();
                    return trimmed.startsWith("#")
                            ? line.substring(0, line.length() - trimmed.length()) + "--" + trimmed.substring(1)
                            : line;
                })
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    private static String getRelativeResourcePath(Resource resource) throws IOException {
        String url = resource.getURL().toString();
        int idx = url.lastIndexOf("/db/");
        if (idx >= 0) {
            return url.substring(idx + 1);
        }
        return null;
    }

    private static void cleanupChangelogs(Path changelogDir) {
        if (changelogDir != null) {
            try (var walk = Files.walk(changelogDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }
}
