package scripting;

import org.junit.jupiter.api.Test;

import javax.script.Invocable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyScriptHandleTest {
    @Test
    void passesOnlyLegacyArgumentsAndInjectsContextValues() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        engine.eval("function action(mode) { return cm.get('prefix') + mode; }");
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        Object result = handle.invoke(
                "action",
                ScriptInvocationContext.of("cm", Map.of("prefix", "npc:")),
                (byte) 2);

        assertEquals("npc:2", result);
    }

    @Test
    void missingCallbackRetainsNoSuchMethodException() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        engine.eval("function start() { return true; }");
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertThrows(NoSuchMethodException.class, () -> handle.invoke("missing", ScriptInvocationContext.empty()));
    }

    @Test
    void detectsExistingAndMissingCallbacks() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        engine.eval("function start() { return true; } var value = 1;");
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertTrue(handle.hasCallback("start"));
        assertFalse(handle.hasCallback("value"));
        assertFalse(handle.hasCallback("missing"));
    }

    @Test
    void legacyScriptSeesNodeLikeProcessGlobal() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    return typeof process.version + ':'
                        + process.versions.node + ':'
                        + process.arch + ':'
                        + process.platform + ':'
                        + typeof process.cwd + ':'
                        + process.cwd() + ':'
                        + Array.isArray(process.argv) + ':'
                        + typeof process.env.PATH;
                }
                """);
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        String result = (String) handle.invoke("start", ScriptInvocationContext.empty());

        assertTrue(result.matches("string:.+:(arm|arm64|ia32|loong64|mips|mipsel|ppc64|riscv64|s390x|x64):(aix|android|darwin|freebsd|linux|openbsd|sunos|win32):function:.+:true:string"));
    }

    @Test
    void legacyScriptCanUseBuiltinModulesFromProcess(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path file = tempDir.resolve("legacy.txt");
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    var fs = process.getBuiltinModule('node:fs');
                    var path = process.getBuiltinModule('path');
                    fs.writeFileSync(%s, 'legacy', 'utf8');
                    return fs.readFileSync(%s, 'utf8') + ':'
                        + path.basename(%s) + ':'
                        + process.getBuiltinModule('missing');
                }
                """.formatted(
                NodeModuleGraph.jsString(file.toString()),
                NodeModuleGraph.jsString(file.toString()),
                NodeModuleGraph.jsString(file.toString())));
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertEquals("legacy:legacy.txt:undefined", handle.invoke("start", ScriptInvocationContext.empty()));
    }

    @Test
    void legacyScriptProvidesReplLikeRequireAndModuleGlobals() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    var fs = require('fs');
                    var path = require('node:path');
                    return typeof require + ':'
                        + typeof module + ':'
                        + typeof module.exports + ':'
                        + (module.require === require) + ':'
                        + (module.require('fs') === fs) + ':'
                        + (fs === process.getBuiltinModule('node:fs')) + ':'
                        + path.basename('/tmp/example.txt') + ':'
                        + require.resolve('node:fs');
                }
                """);
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertEquals("function:object:object:false:true:true:example.txt:node:fs", handle.invoke("start", ScriptInvocationContext.empty()));
    }

    @Test
    void legacyScriptCanCreateRequireFromModuleBuiltin() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    var createRequire = process.getBuiltinModule('module').createRequire;
                    var require = createRequire('file://' + process.cwd() + '/');
                    return require('fs').existsSync(process.cwd()) + ':' + require('node:path').basename(process.cwd());
                }
                """);
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertEquals("true:Cosmic", handle.invoke("start", ScriptInvocationContext.empty()));
    }

    @Test
    void legacyScriptCannotSeeInternalBinding() throws Exception {
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    return typeof internalBinding + ':' + typeof globalThis.internalBinding;
                }
                """);
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        assertEquals("undefined:undefined", handle.invoke("start", ScriptInvocationContext.empty()));
    }

    @Test
    void processPropertiesReflectJavaRuntimeChanges() throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        ScriptEngine engine = newHostEnabledEngine();
        ScriptRuntimeSupport.installGlobals(engine);
        engine.eval("""
                function start() {
                    return process.cwd();
                }
                """);
        ScriptHandle handle = new LegacyScriptHandle(engine, (Invocable) engine);

        try {
            System.setProperty("user.dir", originalUserDir + "/changed");
            assertEquals(originalUserDir + "/changed", handle.invoke("start", ScriptInvocationContext.empty()));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void nodeArchMatchesNodeSupportedValuesAndRejectsUnknown() {
        assertEquals("x64", ScriptRuntimeSupport.nodeArch("amd64"));
        assertEquals("x64", ScriptRuntimeSupport.nodeArch("x86_64"));
        assertEquals("ia32", ScriptRuntimeSupport.nodeArch("i386"));
        assertEquals("arm", ScriptRuntimeSupport.nodeArch("armv7l"));
        assertEquals("arm64", ScriptRuntimeSupport.nodeArch("aarch64"));
        assertEquals("loong64", ScriptRuntimeSupport.nodeArch("loongarch64"));
        assertEquals("mips", ScriptRuntimeSupport.nodeArch("mips"));
        assertEquals("mipsel", ScriptRuntimeSupport.nodeArch("mipsel"));
        assertEquals("ppc64", ScriptRuntimeSupport.nodeArch("ppc64le"));
        assertEquals("riscv64", ScriptRuntimeSupport.nodeArch("riscv64"));
        assertEquals("s390x", ScriptRuntimeSupport.nodeArch("s390x"));

        assertThrows(ScriptLoadException.class, () -> ScriptRuntimeSupport.nodeArch("mystery64"));
    }

    @Test
    void nodePlatformMatchesNodeSupportedValuesAndRejectsUnknown() {
        assertEquals("win32", ScriptRuntimeSupport.nodePlatform("Windows 11", ""));
        assertEquals("darwin", ScriptRuntimeSupport.nodePlatform("Mac OS X", ""));
        assertEquals("linux", ScriptRuntimeSupport.nodePlatform("Linux", ""));
        assertEquals("android", ScriptRuntimeSupport.nodePlatform("Linux", "Android Runtime"));
        assertEquals("aix", ScriptRuntimeSupport.nodePlatform("AIX", ""));
        assertEquals("freebsd", ScriptRuntimeSupport.nodePlatform("FreeBSD", ""));
        assertEquals("openbsd", ScriptRuntimeSupport.nodePlatform("OpenBSD", ""));
        assertEquals("sunos", ScriptRuntimeSupport.nodePlatform("SunOS", ""));
        assertEquals("sunos", ScriptRuntimeSupport.nodePlatform("Solaris", ""));

        assertThrows(ScriptLoadException.class, () -> ScriptRuntimeSupport.nodePlatform("Plan 9", ""));
    }

    @Test
    void synchronizedWrapperKeepsLegacyNullDelegateBehavior() {
        ScriptHandle handle = SynchronizedScriptHandle.of(null);

        assertThrows(NullPointerException.class, () -> handle.invoke("init", ScriptInvocationContext.empty()));
        assertThrows(NullPointerException.class, () -> handle.hasCallback("init"));
        handle.close();
    }

    private static ScriptEngine newHostEnabledEngine() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", true);
        return engine;
    }
}
