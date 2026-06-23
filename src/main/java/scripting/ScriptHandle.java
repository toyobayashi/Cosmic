package scripting;

import javax.script.ScriptException;

public interface ScriptHandle extends AutoCloseable {
    Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
            throws ScriptException, NoSuchMethodException;

    boolean hasCallback(String callback);

    @Override
    void close();
}
