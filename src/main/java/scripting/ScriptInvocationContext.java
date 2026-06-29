package scripting;

import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ScriptInvocationContext {
    private static final ScriptInvocationContext EMPTY = new ScriptInvocationContext(Map.of());

    private final Map<String, Object> values;

    private ScriptInvocationContext(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static ScriptInvocationContext empty() {
        return EMPTY;
    }

    public static ScriptInvocationContext of(String key, Object value) {
        Objects.requireNonNull(key);
        return new ScriptInvocationContext(Map.of(key, value));
    }

    public Map<String, Object> values() {
        return values;
    }

    public ProxyObject toProxyObject() {
        return ProxyObject.fromMap(new HashMap<>(values));
    }
}
