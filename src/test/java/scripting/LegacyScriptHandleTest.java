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
