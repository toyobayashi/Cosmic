package scripting;

import config.YamlConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScriptEvaluationTest {
    private AbstractScriptManager scriptManager = new AbstractScriptManager() {};

    @BeforeAll
    static void setUp() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        YamlConfig.load();
    }

    private static List<String> eventScriptFilePaths() throws IOException {
        return getScriptFilePaths("event");
    }

    private static List<String> itemScriptFilePaths() throws IOException {
        return getScriptFilePaths("item");
    }

    private static List<String> npcScriptFilePaths() throws IOException {
        return getScriptFilePaths("npc");
    }

    private static List<String> portalScriptFilePaths() throws IOException {
        return getScriptFilePaths("portal");
    }

    private static List<String> questScriptFilePaths() throws IOException {
        return getScriptFilePaths("quest");
    }

    private static List<String> reactorScriptFilePaths() throws IOException {
        return getScriptFilePaths("reactor");
    }

    private static List<String> getScriptFilePaths(final String scriptsSubdirectory) throws IOException {
        Path scriptDirectory = Path.of("scripts", scriptsSubdirectory);
        try (Stream<Path> pathStream = Files.walk(scriptDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(path -> "%s/%s".formatted(scriptsSubdirectory, path.getFileName().toString()))
                    .toList();
        }
    }

    @ParameterizedTest
    @MethodSource("eventScriptFilePaths")
    void eventScriptShouldEvaluate(String eventScriptPath) {
        assertScriptLoads(eventScriptPath);
    }

    @ParameterizedTest
    @MethodSource("itemScriptFilePaths")
    void itemScriptShouldEvaluate(String itemScriptPath) {
        assertScriptLoads(itemScriptPath);
    }

    @ParameterizedTest
    @MethodSource("npcScriptFilePaths")
    void npcScriptShouldEvaluate(String npcScriptPath) {
        assertScriptLoads(npcScriptPath);
    }

    @ParameterizedTest
    @MethodSource("mtsEntryScriptPath")
    void mtsCustomEntryScriptShouldEvaluate(String npcScriptPath) {
        assertScriptLoads(npcScriptPath);
    }

    private static List<String> mtsEntryScriptPath() {
        return List.of("npc/mtsCustomEntry.mjs");
    }

    @Test
    void sharedI18nTranslatesMessageEntriesByClientCodePage(@TempDir Path tempDir) throws Exception {
        String i18nModule = Path.of("scripts", "lib", "i18n", "index.mjs").toAbsolutePath().normalize().toString();
        Path entry = tempDir.resolve("entry.mjs");
        Files.writeString(entry, """
                import { defineMessages, localeForCodePage, translateForCodePage } from '%s';

                const messages = defineMessages({
                    main: {
                        question: {
                            en: 'Hello {name}',
                            zhCN: '你好，{name}'
                        }
                    },
                    fallback: {
                        onlyEnglish: {
                            en: 'Fallback {name}'
                        }
                    }
                });

                export function result(ctx) {
                    return localeForCodePage(936) + '|'
                        + translateForCodePage(messages.main.question, 936, { name: 'Codex' }) + '|'
                        + translateForCodePage(messages.main.question, 1252, { name: 'Codex' }) + '|'
                        + translateForCodePage(messages.fallback.onlyEnglish, 936, { name: 'Codex' });
                }
                """.formatted(i18nModule.replace("\\", "\\\\")), StandardCharsets.UTF_8);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("zhCN|你好，Codex|Hello Codex|Fallback Codex",
                    handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @ParameterizedTest
    @MethodSource("portalScriptFilePaths")
    void portalScriptShouldEvaluate(String portalScriptPath) {
        assertScriptLoads(portalScriptPath);
    }

    @ParameterizedTest
    @MethodSource("questScriptFilePaths")
    void questScriptShouldEvaluate(String questScriptPath) {
        assertScriptLoads(questScriptPath);
    }

    @ParameterizedTest
    @MethodSource("reactorScriptFilePaths")
    void reactorScriptShouldEvaluate(String reactorScriptPath) {
        assertScriptLoads(reactorScriptPath);
    }

    @Test
    void moduleScriptWithContextAndStaticImportEvaluates(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("helpers.js"), "export const message = 'ok';", StandardCharsets.UTF_8);
        Path entry = tempDir.resolve("entry.mjs");
        Files.writeString(entry, """
                import { message } from './helpers.js';
                export function start(ctx) { return message; }
                """, StandardCharsets.UTF_8);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertNotNull(handle);
            assertEquals("ok", handle.invoke("start", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmNpcScriptExportsActionCallback() {
        try (ScriptHandle handle = scriptManager.loadScript("npc/9000020.js")) {
            assertNotNull(handle);
            assertTrue(handle.hasCallback("start"));
            assertTrue(handle.hasCallback("action"));
        }
    }

    private void assertScriptLoads(String scriptPath) {
        ScriptHandle scriptHandle = scriptManager.loadScript(scriptPath);

        assertNotNull(scriptHandle);
        scriptHandle.close();
    }
}
