package client.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import client.command.commands.gm2.SetExpCommand;
import java.lang.reflect.Field;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class CommandsExecutorTest {

    @Test
    void registersExpAsGm2Command() throws Exception {
        Command command = registeredCommand("exp");

        assertInstanceOf(SetExpCommand.class, command);
        assertEquals(2, command.getRank());
        assertFalse(registeredCommands().containsKey("setexp"));
    }

    private static Command registeredCommand(String commandName) throws Exception {
        HashMap<String, Command> registeredCommands = registeredCommands();

        assertTrue(registeredCommands.containsKey(commandName));
        return registeredCommands.get(commandName);
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, Command> registeredCommands() throws Exception {
        Field field = CommandsExecutor.class.getDeclaredField("registeredCommands");
        field.setAccessible(true);
        return (HashMap<String, Command>) field.get(CommandsExecutor.getInstance());
    }
}
