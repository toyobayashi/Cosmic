package database;

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
import java.sql.Connection;
import java.sql.SQLException;
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

            Liquibase liquibase = new Liquibase(
                    "db/changelog-root.xml",
                    new DirectoryResourceAccessor(changelogDir),
                    database);

            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
            liquibase.update();
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
                    Files.copy(is, target);
                }
            }
            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract liquibase changelogs", e);
        }
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
