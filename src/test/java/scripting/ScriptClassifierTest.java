package scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptClassifierTest {
    private record ClassificationCase(String name, String filename, String source, ScriptMode expectedMode) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static Stream<ClassificationCase> classifiableEntries() {
        return Stream.of(
                new ClassificationCase(
                        "mjs entry with no import or export is ESM",
                        "entry.mjs",
                        "function start(ctx) { ctx.sendOk('hello'); }",
                        ScriptMode.ESM),
                new ClassificationCase(
                        "js entry with export declaration is ESM",
                        "entry.js",
                        "export function start(ctx) { ctx.sendOk('hello'); }",
                        ScriptMode.ESM),
                new ClassificationCase(
                        "js entry with static import declaration is ESM",
                        "entry.js",
                        "import helper from './helper.mjs';\nfunction start(ctx) { helper(ctx); }",
                        ScriptMode.ESM),
                new ClassificationCase(
                        "string containing import is LEGACY",
                        "entry.js",
                        "var text = \"import helper from './helper.mjs'\";",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "line comment containing export is LEGACY",
                        "entry.js",
                        "// export function start(ctx) {}\nfunction start(ctx) {}",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "block comment containing import and export is LEGACY",
                        "entry.js",
                        "/* import helper from './helper.mjs'; export const value = 1; */\nfunction start(ctx) {}",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "template literal containing export is LEGACY",
                        "entry.js",
                        "var text = `export function start(ctx) {}`;",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "dynamic import is LEGACY",
                        "entry.js",
                        "function start(ctx) { return import('./later.js'); }",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "nested import and export text inside function body is LEGACY",
                        "entry.js",
                        "function start(ctx) { var importValue = 1; exportValue(); }",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "nested import and export property names inside object method are LEGACY",
                        "entry.js",
                        "var obj = { import: 1, export() { return 2; } };",
                        ScriptMode.LEGACY),
                new ClassificationCase(
                        "class body export method name is LEGACY",
                        "entry.js",
                        "class Handler { export() { return 'not a declaration'; } }",
                        ScriptMode.LEGACY)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("classifiableEntries")
    void classifyEntry(ClassificationCase classificationCase) {
        ScriptMode mode = ScriptClassifier.classify(Path.of(classificationCase.filename()), classificationCase.source());

        assertEquals(classificationCase.expectedMode(), mode);
    }

    @Test
    void invalidExtensionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScriptClassifier.classify(Path.of("entry.txt"), "export function start(ctx) {}"));
    }
}
