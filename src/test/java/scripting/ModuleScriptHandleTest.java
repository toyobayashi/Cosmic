package scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleScriptHandleTest {
    @Test
    void invokesNamedExportWithCtxBeforeBusinessArguments(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("npc.mjs"),
                "export function action(ctx, mode) { return ctx.cm.get('prefix') + mode; }");

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
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

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
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

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
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

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
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

        try (ScriptHandle handle = ModuleScriptHandle.load(tempDir.resolve("a.mjs"))) {
            assertEquals("ab", handle.invoke("value", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void resolvesBareNodeModulesPackageMain(@TempDir Path tempDir) throws Exception {
        Path packageDir = tempDir.resolve("node_modules").resolve("pkg");
        Files.createDirectories(packageDir);
        write(packageDir.resolve("package.json"), "{\"main\":\"main.js\"}");
        write(packageDir.resolve("main.js"), "export const value = 'pkg-main';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { value } from 'pkg';
                export function result(ctx) { return value; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("pkg-main", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void ignoresImportSyntaxInsideEsmComments(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                // import missing from 'does-not-exist';
                /* export { missing } from 'also-missing'; */
                export function result(ctx) { return 'ok'; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("ok", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void resolvesPackageExportsAndSubpaths(@TempDir Path tempDir) throws Exception {
        Path packageDir = tempDir.resolve("node_modules").resolve("pkg");
        Files.createDirectories(packageDir.resolve("lib"));
        write(packageDir.resolve("package.json"), """
                {
                  "exports": {
                    ".": "./lib/index.js",
                    "./feature": "./lib/feature.js"
                  },
                  "type": "module"
                }
                """);
        write(packageDir.resolve("lib").resolve("index.js"), "export const root = 'root';");
        write(packageDir.resolve("lib").resolve("feature.js"), "export const feature = 'feature';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { root } from 'pkg';
                import { feature } from 'pkg/feature';
                export function result(ctx) { return root + ':' + feature; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("root:feature", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void importsCommonJsFromEsmWithDefaultAndNamedExports(@TempDir Path tempDir) throws Exception {
        Path packageDir = tempDir.resolve("node_modules").resolve("pkg");
        Files.createDirectories(packageDir);
        write(packageDir.resolve("package.json"), "{\"main\":\"index.cjs\"}");
        write(packageDir.resolve("index.cjs"), """
                module.exports = { defaultValue: 'default' };
                module.exports.named = 'named';
                module.exports.extra = 'extra';
                """);
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import pkg, { named, extra } from 'pkg';
                export function result(ctx) { return pkg.defaultValue + ':' + named + ':' + extra; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("default:named:extra", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsCanRequireCommonJsAndSynchronousEsm(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("esm.mjs"), "export const value = 'esm';");
        write(tempDir.resolve("helper.cjs"), "exports.value = 'cjs';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const helper = require('./helper.cjs');
                const esm = require('./esm.mjs');
                exports.result = function(ctx) { return helper.value + ':' + esm.value; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("cjs:esm", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsProvidesFilenameAndDirname(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("nested").resolve("entry.cjs"), """
                exports.result = function(ctx) {
                    return __filename + '|' + __dirname;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals(entry.toRealPath() + "|" + entry.toRealPath().getParent(), handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsSeesNodeLikeProcessGlobal(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.cjs"), """
                exports.result = function(ctx) {
                    return typeof process.version + ':'
                        + process.versions.node + ':'
                        + process.arch + ':'
                        + process.platform + ':'
                        + typeof process.cwd + ':'
                        + process.cwd() + ':'
                        + Array.isArray(process.argv) + ':'
                        + process.argv[1] + ':'
                        + typeof process.env.PATH;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            String result = (String) handle.invoke("result", ScriptInvocationContext.empty());
            assertTrue(result.matches("string:.+:(arm|arm64|ia32|loong64|mips|mipsel|ppc64|riscv64|s390x|x64):(aix|android|darwin|freebsd|linux|openbsd|sunos|win32):function:.+:true:.+:string"));
            assertTrue(result.contains(":" + entry.toRealPath() + ":"));
        }
    }

    @Test
    void esmSeesNodeLikeProcessGlobal(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function result(ctx) {
                    return typeof process.version + ':'
                        + process.versions.node + ':'
                        + process.arch + ':'
                        + process.platform + ':'
                        + typeof process.cwd + ':'
                        + process.cwd() + ':'
                        + Array.isArray(process.argv) + ':'
                        + process.argv[1] + ':'
                        + typeof process.env.PATH;
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            String result = (String) handle.invoke("result", ScriptInvocationContext.empty());
            assertTrue(result.matches("string:.+:(arm|arm64|ia32|loong64|mips|mipsel|ppc64|riscv64|s390x|x64):(aix|android|darwin|freebsd|linux|openbsd|sunos|win32):function:.+:true:.+:string"));
            assertTrue(result.contains(":" + entry.toRealPath() + ":"));
        }
    }

    @Test
    void commonJsCanRequireFsAndNodeFsSyncApis(@TempDir Path tempDir) throws Exception {
        Path data = tempDir.resolve("data.txt");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const fs = require('fs');
                const nodeFs = require('node:fs');
                exports.result = function(ctx) {
                    const file = __dirname + '/data.txt';
                    fs.writeFileSync(file, 'hello', 'utf8');
                    nodeFs.appendFileSync(file, ' world', 'utf8');
                    return fs.existsSync(file) + ':' + fs.readFileSync(file, 'utf8') + ':' + fs.statSync(file).isFile();
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("true:hello world:true", handle.invoke("result", ScriptInvocationContext.empty()));
            assertEquals("hello world", Files.readString(data));
        }
    }

    @Test
    void commonJsCanUseFsFdSyncApis(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("fd.txt");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const fs = require('fs');
                exports.result = function(ctx) {
                    const fd = fs.openSync(%s, 'w+');
                    fs.writeSync(fd, 'abcdef', 0, 'utf8');
                    fs.ftruncateSync(fd, 3);
                    const stat = fs.fstatSync(fd);
                    const buffer = new Uint8Array(3);
                    fs.readSync(fd, buffer, 0, 3, 0);
                    fs.closeSync(fd);
                    return stat.isFile() + ':' + stat.size() + ':' + String.fromCharCode(...buffer);
                };
                """.formatted(NodeModuleGraph.jsString(file.toString())));

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("true:3:abc", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmCanImportFsAndNodeFsSyncApis(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import fs from 'fs';
                import * as nodeFs from 'node:fs';
                export function result(ctx) {
                    const file = import.meta.dirname + '/esm-data.txt';
                    fs.writeFileSync(file, 'esm', 'utf8');
                    return nodeFs.readFileSync(file, 'utf8');
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("esm", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsCanRequirePathAndNodePathFromBundledStdPath(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const path = require('path');
                const nodePath = require('node:path');
                exports.result = function(ctx) {
                    return path.join('a', 'b', 'c.txt') + ':' + nodePath.basename('/tmp/example.txt');
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("a/b/c.txt:example.txt", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmCanImportPathAndNodePathFromBundledStdPath(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import path from 'path';
                import * as nodePath from 'node:path';
                export function result(ctx) {
                    return path.normalize('a/../b/file.txt') + ':' + nodePath.extname('demo.json');
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("b/file.txt:.json", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void processGetBuiltinModuleMatchesRequireAndImportBuiltins(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import fs from 'fs';
                import path from 'node:path';
                export function result(ctx) {
                    const processFs = process.getBuiltinModule('node:fs');
                    const processPath = process.getBuiltinModule('path');
                    return (fs === processFs) + ':' + (path === processPath) + ':'
                        + processFs.existsSync(import.meta.filename) + ':'
                        + processPath.basename(import.meta.filename) + ':'
                        + process.getBuiltinModule('missing');
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("true:true:true:entry.mjs:undefined", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmProvidesImportMetaFilePropertiesAndResolve(@TempDir Path tempDir) throws Exception {
        Path helper = write(tempDir.resolve("helper.mjs"), "export const value = 'helper';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function result(ctx) {
                    return import.meta.filename + '|'
                        + import.meta.dirname + '|'
                        + import.meta.url + '|'
                        + import.meta.resolve('./helper.mjs');
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals(
                    entry.toRealPath() + "|"
                            + entry.toRealPath().getParent() + "|"
                            + entry.toRealPath().toUri() + "|"
                            + helper.toRealPath().toUri(),
                    handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void sameOriginalModuleUrlCanBeLoadedByMultipleHandles(@TempDir Path tempDir) throws Exception {
        Path helper = write(tempDir.resolve("helper.mjs"), "export const value = 'helper';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function result(ctx) {
                    return import.meta.url + '|' + import.meta.resolve('./helper.mjs');
                }
                """);
        ModuleScriptHandle first = ModuleScriptHandle.load(entry);
        ModuleScriptHandle second = ModuleScriptHandle.load(entry);

        try {
            second.close();

            assertEquals(
                    entry.toRealPath().toUri() + "|" + helper.toRealPath().toUri(),
                    first.invoke("result", ScriptInvocationContext.empty()));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void esmCanCreateRequireFromNodeModuleBuiltin(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("data.json"), "{ \"value\": \"created-require\" }");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { createRequire } from 'node:module';
                const require = createRequire(import.meta.url);
                const data = require('./data.json');
                export function result(ctx) { return data.value; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("created-require", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmCanCreateRequireFromModuleBuiltinAlias(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("helper.cjs"), "exports.value = 'module-alias';");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import { createRequire } from 'module';
                const require = createRequire(import.meta.url);
                const helper = require('./helper.cjs');
                export function result(ctx) { return helper.value + ':' + require.resolve('./helper.cjs'); }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("module-alias:" + tempDir.resolve("helper.cjs").toRealPath(), handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void moduleBuiltinSourceDoesNotExposeHostRuntimeInternals() {
        String moduleSource = ScriptRuntimeSupport.readResource("/scripting/builtins/module.cjs");

        assertFalse(moduleSource.contains("ModuleScriptHandle"));
        assertFalse(moduleSource.contains("return createRequire("));
    }

    @Test
    void builtinSourcesUseInternalBindingForHostCapabilities() {
        String processSource = ScriptRuntimeSupport.readResource("/scripting/builtins/process.cjs");
        String fsSource = ScriptRuntimeSupport.readResource("/scripting/builtins/fs.cjs");

        assertFalse(processSource.contains(".install"));
        assertFalse(fsSource.contains("Java.type"));
        assertTrue(processSource.contains("internalBinding(\"process\")"));
        assertTrue(fsSource.contains("internalBinding(\"fs\")"));
    }

    @Test
    void builtinBootstrapDoesNotInlineBuiltinModuleSources() {
        String bootstrap = ScriptRuntimeSupport.builtinBootstrapSource();

        assertFalse(bootstrap.contains("module.exports = Object.freeze"));
        assertFalse(bootstrap.contains("Copyright 2018-2026 the Deno authors"));
        assertFalse(bootstrap.contains("/scripting/builtins/"));
        assertFalse(bootstrap.contains("ModuleScriptHandle"));
        assertTrue(bootstrap.contains("internalBinding('fs').readFileSync(filename"));
        assertTrue(bootstrap.contains("__cosmicBuiltinCache"));
    }

    @Test
    void generatedBuiltinBridgesDoNotInlineBuiltinModuleSources(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import fs from 'fs';
                export function result(ctx) { return typeof fs.readFileSync; }
                """);
        NodeModuleGraph.Result result = NodeModuleGraph.buildWithSourceMap(entry.toRealPath());

        try {
            for (Path generatedPath : result.sourceMap().keySet()) {
                String source = Files.readString(generatedPath, StandardCharsets.UTF_8);
                assertFalse(source.contains("/scripting/builtins/"));
                assertFalse(source.contains("module.exports = Object.freeze"));
                assertFalse(source.contains("Copyright 2018-2026 the Deno authors"));
            }
        } finally {
            try (var paths = Files.walk(result.generatedRoot())) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void userModulesCannotSeeInternalBinding(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("helper.cjs"), """
                exports.value = function() {
                    return typeof internalBinding + ':' + typeof globalThis.internalBinding;
                };
                """);
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import helper from './helper.cjs';
                export function result(ctx) {
                    return typeof internalBinding + ':' + typeof globalThis.internalBinding + ':' + helper.value();
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("undefined:undefined:undefined:undefined", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmDoesNotExposeGlobalRequireOrModule(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function result(ctx) {
                    return typeof require + ':' + typeof module + ':'
                        + typeof globalThis.require + ':' + typeof globalThis.module;
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("undefined:undefined:undefined:undefined", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsCanRequireJsonFile(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("data.json"), """
                { "name": "json", "nested": { "value": 7 } }
                """);
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const data = require('./data.json');
                exports.result = function(ctx) { return data.name + ':' + data.nested.value; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("json:7", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsCanRequireJsonWithoutExtension(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("config.json"), """
                { "enabled": true, "label": "json-extensionless" }
                """);
        Path entry = write(tempDir.resolve("entry.cjs"), """
                exports.result = function(ctx) { return require('./config').label; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("json-extensionless", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmCanImportJsonWithTypeAttribute(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("data.json"), """
                { "name": "esm-json", "items": [1, 2, 3] }
                """);
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import data from './data.json' with { type: 'json' };
                export function result(ctx) { return data.name + ':' + data.items.length; }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("esm-json:3", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmJsonImportSharesCommonJsRequireCache(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("data.json"), "{ \"count\": 1 }");
        write(tempDir.resolve("reader.cjs"), """
                exports.read = function() {
                    return require('./data.json').count;
                };
                """);
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import data from './data.json' with { type: 'json' };
                import reader from './reader.cjs';
                data.count = 9;
                export function result(ctx) { return reader.read(); }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals(9, handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void esmRejectsJsonImportWithoutTypeAttribute(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("data.json"), "{ \"name\": \"missing-attribute\" }");
        Path entry = write(tempDir.resolve("entry.mjs"), """
                import data from './data.json';
                export function result(ctx) { return data.name; }
                """);

        ScriptLoadException exception = assertThrows(ScriptLoadException.class, () -> ModuleScriptHandle.load(entry));

        assertTrue(exception.getMessage().contains("with { type: 'json' }"));
    }

    @Test
    void commonJsSupportsComputedRequireOfSynchronousEsm(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("esm.mjs"), "export const value = 'dynamic-esm';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const file = './esm.mjs';
                exports.result = function(ctx) {
                    return require(file).value;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("dynamic-esm", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsDoesNotResolveUncalledLiteralRequireAtLoadTime(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.cjs"), """
                function unused() {
                    return require('missing-package');
                }
                exports.result = function(ctx) { return 'loaded'; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("loaded", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsRuntimeDoesNotExposeCosmicGlobal(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.cjs"), """
                exports.result = function(ctx) {
                    return typeof globalThis.__cosmicCjs + ':' + typeof __cosmicCjs + ':'
                        + typeof globalThis.require + ':' + typeof globalThis.module + ':'
                        + typeof require + ':' + typeof module;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("undefined:undefined:undefined:undefined:function:object", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsRequireResolveReturnsResolvedFilename(@TempDir Path tempDir) throws Exception {
        Path helper = write(tempDir.resolve("helper.cjs"), "exports.value = 'resolved';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                exports.result = function(ctx) {
                    return require.resolve('./helper');
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals(helper.toRealPath().toString(), handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsRequireCacheExposesLoadedModules(@TempDir Path tempDir) throws Exception {
        Path helper = write(tempDir.resolve("helper.cjs"), """
                exports.count = (exports.count ?? 0) + 1;
                """);
        Path entry = write(tempDir.resolve("entry.cjs"), """
                exports.result = function(ctx) {
                    const helper = require('./helper.cjs');
                    const cached = require.cache[require.resolve('./helper.cjs')];
                    return helper.count + ':' + cached.exports.count;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("1:1", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsSupportsComputedRelativeRequire(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("helpers").resolve("reward.cjs"), "exports.value = 'dynamic-relative';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const name = 'reward';
                exports.result = function(ctx) {
                    return require('./helpers/' + name + '.cjs').value;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("dynamic-relative", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsResolvesComputedRequireRelativeToRequestingModule(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("lib").resolve("nested.cjs"), """
                const file = './value.cjs';
                exports.value = require(file).value;
                """);
        write(tempDir.resolve("lib").resolve("value.cjs"), "exports.value = 'nested-relative';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const nested = require('./lib/nested.cjs');
                exports.result = function(ctx) { return nested.value; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("nested-relative", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsSupportsComputedBarePackageRequire(@TempDir Path tempDir) throws Exception {
        Path packageDir = tempDir.resolve("node_modules").resolve("pkg");
        Files.createDirectories(packageDir);
        write(packageDir.resolve("package.json"), "{\"main\":\"index.cjs\"}");
        write(packageDir.resolve("index.cjs"), "exports.value = 'dynamic-package';");
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const packageName = 'pkg';
                exports.result = function(ctx) { return require(packageName).value; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("dynamic-package", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void commonJsComputedRequireUsesModuleCache(@TempDir Path tempDir) throws Exception {
        write(tempDir.resolve("counter.cjs"), """
                globalThis.dynamicRequireCount = (globalThis.dynamicRequireCount ?? 0) + 1;
                exports.value = globalThis.dynamicRequireCount;
                """);
        Path entry = write(tempDir.resolve("entry.cjs"), """
                const specifier = './counter.cjs';
                exports.result = function(ctx) {
                    const first = require(specifier).value;
                    const second = require(specifier).value;
                    return first + ':' + second + ':' + globalThis.dynamicRequireCount;
                };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("1:1:1", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void ignoresRequireSyntaxInsideCommonJsComments(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.cjs"), """
                // const missing = require('does-not-exist');
                const text = "require('also-missing')";
                exports.result = function(ctx) { return 'ok'; };
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertEquals("ok", handle.invoke("result", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void missingExportRetainsNoSuchMethodException(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), "export function start(ctx) { return true; }");

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            assertThrows(NoSuchMethodException.class, () -> handle.invoke("missing", ScriptInvocationContext.empty()));
        }
    }

    @Test
    void invocationErrorsExposeGuestSourceLocation(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function start(ctx) {
                    const value = 1;
                    throw new Error('boom');
                }
                """);

        try (ScriptHandle handle = ModuleScriptHandle.load(entry)) {
            ScriptException exception = assertThrows(
                    ScriptException.class,
                    () -> handle.invoke("start", ScriptInvocationContext.empty()));

            assertEquals(entry.toRealPath().toString(), exception.getFileName());
            assertEquals(3, exception.getLineNumber());
            assertTrue(exception.getMessage().contains("entry.mjs:3"));
        }
    }

    @Test
    void consoleLogWritesToConfiguredOutput(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function start(ctx) {
                    console.log('hello from js');
                }
                """);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ScriptHandle handle = ModuleScriptHandle.loadForTesting(entry, out, new ByteArrayOutputStream())) {
            handle.invoke("start", ScriptInvocationContext.empty());
        }

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("hello from js"));
    }

    @Test
    void closeDeletesGeneratedModuleFiles(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), "export function start(ctx) { return true; }");
        ModuleScriptHandle handle = ModuleScriptHandle.load(entry);

        assertTrue(handle.generatedRootExistsForTesting());

        handle.close();

        assertTrue(!handle.generatedRootExistsForTesting());
    }

    @Test
    void closeDuringInvocationIsDeferredUntilResultIsRead(@TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), """
                export function action(ctx) {
                    ctx.cm.dispose();
                    return 'disposed';
                }
                """);
        AtomicReference<ModuleScriptHandle> currentHandle = new AtomicReference<>();
        ClosingHost host = new ClosingHost(currentHandle);

        ModuleScriptHandle handle = ModuleScriptHandle.load(entry);
        currentHandle.set(handle);

        assertEquals("disposed", handle.invoke("action", ScriptInvocationContext.of("cm", host)));
        assertTrue(handle.isClosedForTesting());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "export const load = import('./later.mjs');",
            "await Promise.resolve(); export function start(ctx) {}"
    })
    void rejectsUnsupportedModuleFeatures(String source, @TempDir Path tempDir) throws Exception {
        Path entry = write(tempDir.resolve("entry.mjs"), source);

        ScriptLoadException exception = assertThrows(ScriptLoadException.class, () -> ModuleScriptHandle.load(entry));

        assertTrue(exception.getMessage().contains(entry.toString()));
    }

    private static Path write(Path path, String source) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
        return path;
    }

    public static final class ClosingHost {
        private final AtomicReference<ModuleScriptHandle> handle;

        private ClosingHost(AtomicReference<ModuleScriptHandle> handle) {
            this.handle = handle;
        }

        public void dispose() {
            handle.get().close();
        }
    }
}
