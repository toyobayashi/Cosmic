package scripting;

import client.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractScriptManagerTest {
    @Test
    void loadsExtensionlessLegacyScriptThroughHandle(@TempDir Path tempDir) throws Exception {
        Path entry = tempDir.resolve("npc").resolve("entry.js");
        write(entry, "function start() { return 'legacy:' + process.argv[1]; }");
        TestScriptManager manager = new TestScriptManager(tempDir);

        ScriptHandle handle = manager.load("npc", "entry");

        assertNotNull(handle);
        assertEquals("legacy:" + entry.toAbsolutePath().normalize(), handle.invoke("start", ScriptInvocationContext.empty()));
    }

    @Test
    void loadsExplicitMjsScriptThroughHandle(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("npc").resolve("entry.mjs"), "export function start(ctx) { return 'esm'; }");
        TestScriptManager manager = new TestScriptManager(tempDir);

        ScriptHandle handle = manager.load("npc", "entry.mjs");

        assertNotNull(handle);
        assertEquals("esm", handle.invoke("start", ScriptInvocationContext.empty()));
        handle.close();
    }

    @Test
    void loadsExplicitCjsScriptThroughHandle(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("npc").resolve("entry.cjs"), "exports.start = function(ctx) { return 'cjs'; };");
        TestScriptManager manager = new TestScriptManager(tempDir);

        ScriptHandle handle = manager.load("npc", "entry.cjs");

        assertNotNull(handle);
        assertEquals("cjs", handle.invoke("start", ScriptInvocationContext.empty()));
        handle.close();
    }

    @Test
    void packageJsonTypeModuleMakesExtensionlessJsLoadAsEsm(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("package.json"), "{\"type\":\"module\"}");
        write(tempDir.resolve("npc").resolve("entry.js"), "export function start(ctx) { return 'typed-esm'; }");
        TestScriptManager manager = new TestScriptManager(tempDir);

        ScriptHandle handle = manager.load("npc", "entry");

        assertNotNull(handle);
        assertEquals("typed-esm", handle.invoke("start", ScriptInvocationContext.empty()));
        handle.close();
    }

    @Test
    void clientCacheReusesOneHandleAndClosesItOnReset(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("npc").resolve("entry.mjs"), "export function start(ctx) { return 'esm'; }");
        TestScriptManager manager = new TestScriptManager(tempDir);
        Client client = Client.createMock();

        ScriptHandle first = manager.load("npc", "entry.mjs", client);
        ScriptHandle second = manager.load("npc", "entry.mjs", client);

        assertSame(first, second);
        manager.reset("npc", "entry.mjs", client);
        assertTrue(((ModuleScriptHandle) first).isClosedForTesting());
    }

    @Test
    void fallbackScriptRemainsCachedWhenDifferentIdentifierIsReset(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("quest").resolve("medalQuest.mjs"), "export function start(ctx) { return 'fallback'; }");
        TestScriptManager manager = new TestScriptManager(tempDir);
        Client client = Client.createMock();

        ScriptHandle fallback = manager.load("quest", "medalQuest.mjs", client);

        manager.reset("quest", "29900", client);

        assertSame(fallback, manager.load("quest", "medalQuest.mjs", client));
        manager.reset("quest", "medalQuest.mjs", client);
        assertTrue(((ModuleScriptHandle) fallback).isClosedForTesting());
    }

    private static void write(Path path, String source) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
    }

    private static final class TestScriptManager extends AbstractScriptManager {
        private TestScriptManager(Path scriptsRoot) {
            super(scriptsRoot);
        }

        private ScriptHandle load(String directory, String identifier) {
            return loadScript(directory, identifier);
        }

        private ScriptHandle load(String directory, String identifier, Client client) {
            return loadScript(directory, identifier, client);
        }

        private void reset(String directory, String identifier, Client client) {
            resetContext(directory, identifier, client);
        }
    }
}
