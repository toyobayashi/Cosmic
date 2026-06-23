package scripting.map;

import client.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapScriptManagerTest {
    @Test
    void passesMapScriptMethodsAsLegacyStartArgument() throws IOException {
        Path script = Path.of("scripts", "map", "codex_legacy_map_argument_test.js");
        Files.writeString(script, """
                function start(ms) {
                    if (ms === undefined) {
                        throw new Error('missing legacy map argument');
                    }
                    if (ms !== msm) {
                        throw new Error('legacy argument should match msm global');
                    }
                }
                """, StandardCharsets.UTF_8);

        MapScriptManager manager = MapScriptManager.getInstance();
        try {
            assertTrue(manager.runMapScript(Client.createMock(), "codex_legacy_map_argument_test", false));
            assertTrue(manager.runMapScript(Client.createMock(), "codex_legacy_map_argument_test", false));
        } finally {
            manager.reloadScripts();
            Files.deleteIfExists(script);
        }
    }
}
