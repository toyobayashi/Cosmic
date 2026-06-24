package scripting;

import javax.script.ScriptException;

public final class SynchronizedScriptHandle implements ScriptHandle {
    private final ScriptHandle delegate;

    private SynchronizedScriptHandle(ScriptHandle delegate) {
        this.delegate = delegate;
    }

    public static ScriptHandle of(ScriptHandle delegate) {
        return new SynchronizedScriptHandle(delegate);
    }

    @Override
    public synchronized Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
            throws ScriptException, NoSuchMethodException {
        return delegate.invoke(callback, context, arguments);
    }

    @Override
    public synchronized boolean hasCallback(String callback) {
        return delegate.hasCallback(callback);
    }

    @Override
    public synchronized void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
