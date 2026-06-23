package scripting;

import org.graalvm.polyglot.Value;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Objects;

public final class LegacyScriptHandle implements ScriptHandle {
    private final ScriptEngine engine;
    private final Invocable invocable;

    public LegacyScriptHandle(ScriptEngine engine, Invocable invocable) {
        this.engine = Objects.requireNonNull(engine);
        this.invocable = Objects.requireNonNull(invocable);
    }

    @Override
    public Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
            throws ScriptException, NoSuchMethodException {
        bindContext(context);
        return invocable.invokeFunction(callback, arguments);
    }

    @Override
    public boolean hasCallback(String callback) {
        Object value = engine.get(callback);
        if (value instanceof Value polyglotValue) {
            return polyglotValue.canExecute();
        }
        try {
            Object result = engine.eval("typeof globalThis[" + quote(callback) + "] === 'function'");
            return Boolean.TRUE.equals(result);
        } catch (ScriptException e) {
            return false;
        }
    }

    @Override
    public void close() {
    }

    private void bindContext(ScriptInvocationContext context) {
        for (var entry : context.values().entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }
    }

    private static String quote(String value) {
        StringBuilder quoted = new StringBuilder("'");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\' || character == '\'') {
                quoted.append('\\');
            }
            quoted.append(character);
        }
        return quoted.append("'").toString();
    }
}
