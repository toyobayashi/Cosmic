package scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsmScriptHandleTest {
    @Test
    void invokesNamedExportWithCtxBeforeBusinessArguments(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("npc.mjs"),
                "export function action(ctx, mode) { return ctx.cm.get('prefix') + mode; }");

        try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
            Object result = handle.invoke(
                    "action",
                    ScriptInvocationContext.of("cm", Map.of("prefix", "npc:")),
                    3);

            assertEquals("npc:3", result);
        }
    }

    @Test
    void importedJsWithoutExportsRunsForSideEffects(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("setup.js"), "globalThis.counter = (globalThis.counter ?? 0) + 1;");
        Path entry = write(tempDir.resolve("entry.mjs"),
                "import './setup.js'; export function count(ctx) { return globalThis.counter; }");

        try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
            assertEquals(1, handle.invoke("count", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void permitsRelativeAndAbsoluteStaticImports(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("relative.js"), "export const relative = 'relative:';");
        Path absolute = write(tempDir.resolve("absolute.mjs"), "export const absolute = 'absolute';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { relative } from './relative.js';
                import { absolute } from '%s';
                export function result(ctx) { return relative + absolute; }
                """.formatted(absolute.toRealPath()));

        try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
            assertEquals("relative:absolute", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void evaluatesOneModuleOnlyOncePerHandle(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("counter.js"), """
                globalThis.count = (globalThis.count ?? 0) + 1;
                export const count = globalThis.count;
                """);
        Files.createDirectory(tempDir.resolve("nested"));
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { count as first } from './counter.js';
                import { count as second } from './nested/../counter.js';
                export function result(ctx) { return `${first}:${second}`; }
                """);

        try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
            assertEquals("1:1", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void supportsCyclicStaticImports(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("a.mjs"), """
                import { b } from './b.mjs';
                export const a = 'a';
                export function value(ctx) { return a + b; }
                """);
        write(tempDir.resolve("b.mjs"), """
                import { a } from './a.mjs';
                export const b = 'b';
                export function peer(ctx) { return a + b; }
                """);

        try (ScriptHandle handle = EsmScriptHandle.load(tempDir.resolve("a.mjs"))) {
            assertEquals("ab", handle.invoke("value", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void missingExportRetainsNoSuchMethodException(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), "export function start(ctx) { return true; }");

        try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
            assertThrows(NoSuchMethodException.class, () -> handle.invoke("missing", ScriptInvocationContext.empty()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "export const load = import('./later.mjs');",
            "await Promise.resolve(); export function start(ctx) {}",
            "import value from 'lodash'; export { value };",
            "import value from 'node:fs'; export { value };"
    })
    void rejectsUnsupportedModuleFeatures(String source, @TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), source);

        ScriptLoadException exception = assertThrows(ScriptLoadException.class, () -> EsmScriptHandle.load(entry));

        assertTrue(exception.getMessage().contains(entry.toString()));
    }

    private static Path write(Path path, String source) throws IOException {
        Files.writeString(path, source, StandardCharsets.UTF_8);
        return path;
    }
}
