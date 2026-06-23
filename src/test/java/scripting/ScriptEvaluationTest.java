package scripting;

import config.YamlConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        return List.of("npc/mtsCustomEntry.js");
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

    private void assertScriptLoads(String scriptPath) {
        ScriptHandle scriptHandle = scriptManager.loadScript(scriptPath);

        assertNotNull(scriptHandle);
        scriptHandle.close();
    }
}
