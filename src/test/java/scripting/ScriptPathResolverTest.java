package scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptPathResolverTest {
    @Test
    void extensionlessEntryDefaultsToJs() {
        ScriptPathResolver resolver = new ScriptPathResolver(Path.of("scripts"));

        Path resolvedPath = resolver.resolveEntry("npc", "9000000");

        assertEquals(Path.of("scripts", "npc", "9000000.js"), resolvedPath);
    }

    @Test
    void extensionlessEntryResolvesExistingMjsWhenJsIsMissing(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("npc"));
        Files.createFile(tempDir.resolve(Path.of("npc", "9000000.mjs")));
        ScriptPathResolver resolver = new ScriptPathResolver(tempDir);

        Path resolvedPath = resolver.resolveEntry("npc", "9000000");

        assertEquals(tempDir.resolve(Path.of("npc", "9000000.mjs")), resolvedPath);
    }

    @Test
    void extensionlessEntryResolvesExistingCjsWhenJsAndMjsAreMissing(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("npc"));
        Files.createFile(tempDir.resolve(Path.of("npc", "9000000.cjs")));
        ScriptPathResolver resolver = new ScriptPathResolver(tempDir);

        Path resolvedPath = resolver.resolveEntry("npc", "9000000");

        assertEquals(tempDir.resolve(Path.of("npc", "9000000.cjs")), resolvedPath);
    }

    @Test
    void explicitJsIsNotAppended() {
        ScriptPathResolver resolver = new ScriptPathResolver(Path.of("scripts"));

        Path resolvedPath = resolver.resolveEntry("npc", "9000000.js");

        assertEquals(Path.of("scripts", "npc", "9000000.js"), resolvedPath);
    }

    @Test
    void explicitMjsIsNotAppended() {
        ScriptPathResolver resolver = new ScriptPathResolver(Path.of("scripts"));

        Path resolvedPath = resolver.resolveEntry("npc", "9000000.mjs");

        assertEquals(Path.of("scripts", "npc", "9000000.mjs"), resolvedPath);
    }

    @Test
    void explicitCjsIsNotAppended() {
        ScriptPathResolver resolver = new ScriptPathResolver(Path.of("scripts"));

        Path resolvedPath = resolver.resolveEntry("npc", "9000000.cjs");

        assertEquals(Path.of("scripts", "npc", "9000000.cjs"), resolvedPath);
    }

    @Test
    void nestedExplicitMjsIdentifierRemainsExplicit() {
        ScriptPathResolver resolver = new ScriptPathResolver(Path.of("scripts"));

        Path resolvedPath = resolver.resolveEntry("npc", "sub/entry.mjs");

        assertEquals(Path.of("scripts", "npc", "sub", "entry.mjs"), resolvedPath);
    }
}
