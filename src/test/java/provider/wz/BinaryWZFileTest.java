package provider.wz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryWZFileTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsDirectoryPathBeforeCallingLibwz() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BinaryWZFile(tempDir.toString())
        );

        assertTrue(exception.getMessage().contains(tempDir.toString()));
    }

    @Test
    void rejectsMissingFilePathBeforeCallingLibwz() {
        Path missingFile = tempDir.resolve("Missing.wz");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BinaryWZFile(missingFile.toString())
        );

        assertTrue(exception.getMessage().contains(missingFile.toString()));
    }
}
