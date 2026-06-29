package scripting.quest;

import client.Character;
import client.Client;
import org.junit.jupiter.api.Test;
import scripting.ScriptHandle;
import scripting.ScriptInvocationContext;

import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestScriptManagerTest {
    @Test
    void disposeResetsActualLoadedFallbackScriptIdentifier() throws Exception {
        QuestScriptManager manager = QuestScriptManager.getInstance();
        Client client = Client.createMock();
        Character player = mock(Character.class);
        when(player.getClient()).thenReturn(client);
        client.setPlayer(player);
        QuestActionManager qm = new QuestActionManager(client, 29900, 0, true);
        RecordingScriptHandle questHandle = new RecordingScriptHandle();
        RecordingScriptHandle fallbackHandle = new RecordingScriptHandle();

        try {
            put(manager, "scriptIdentifiers", client, "medalQuest");
            client.setScriptHandle("scripts/quest/29900.js", questHandle);
            client.setScriptHandle("scripts/quest/medalQuest.js", fallbackHandle);

            manager.dispose(qm, client);

            assertFalse(questHandle.closed);
            assertTrue(fallbackHandle.closed);
            assertNull(get(manager, "scriptIdentifiers", client));
        } finally {
            manager.reloadQuestScripts();
        }
    }

    @SuppressWarnings("unchecked")
    private static void put(QuestScriptManager manager, String fieldName, Client client, String value) throws Exception {
        Field field = QuestScriptManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<Client, String>) field.get(manager)).put(client, value);
    }

    @SuppressWarnings("unchecked")
    private static String get(QuestScriptManager manager, String fieldName, Client client) throws Exception {
        Field field = QuestScriptManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Map<Client, String>) field.get(manager)).get(client);
    }

    private static final class RecordingScriptHandle implements ScriptHandle {
        private boolean closed;

        @Override
        public Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
                throws ScriptException, NoSuchMethodException {
            return null;
        }

        @Override
        public boolean hasCallback(String callback) {
            return false;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
