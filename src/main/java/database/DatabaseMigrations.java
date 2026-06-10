package database;

import liquibase.Liquibase;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import tools.DatabaseConnection;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apply changes to the database so that the server and database work in harmony.
 *
 * @author Ponk
 */
public class DatabaseMigrations {

    private static final List<String> CHANGELOG_FILES = List.of(
            "changelog-root.xml",
            "changelog-tables.xml",
            "changelog-data.xml"
    );

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
                    "changelog-root.xml",
                    new DirectoryResourceAccessor(changelogDir),
                    database);

            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
            liquibase.update();
        } catch (SQLException | LiquibaseException | FileNotFoundException e) {
            throw new RuntimeException("Failed to run database migrations", e);
        } finally {
            cleanupChangelogs(changelogDir);
        }
    }

    private static Path extractChangelogs() {
        try {
            Path tempDir = Files.createTempDirectory("cosmic-changelog");
            ClassLoader classLoader = DatabaseMigrations.class.getClassLoader();
            for (String file : CHANGELOG_FILES) {
                String resourcePath = "db/" + file;
                try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        throw new RuntimeException("Changelog not found on classpath: " + resourcePath);
                    }
                    Files.copy(is, tempDir.resolve(file));
                }
            }
            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract liquibase changelogs", e);
        }
    }

    private static void cleanupChangelogs(Path changelogDir) {
        if (changelogDir != null) {
            try {
                for (String file : CHANGELOG_FILES) {
                    Files.deleteIfExists(changelogDir.resolve(file));
                }
                Files.deleteIfExists(changelogDir);
            } catch (IOException ignored) {
            }
        }
    }
}
