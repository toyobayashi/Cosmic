package client.command.commands.gm6;

import client.Client;
import client.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.ScriptHandle;
import scripting.ScriptInvocationContext;
import javax.script.ScriptException;

public class DevtestCommand extends Command {
    {
        setDescription("Runs devtest.js. Developer utility - test stuff without restarting the server.");
    }

    private static final Logger log = LoggerFactory.getLogger(DevtestCommand.class);

    private static class DevtestScriptManager extends AbstractScriptManager {

        public ScriptHandle load(String path) {
            return super.loadScript(path);
        }

    }

    @Override
    public void execute(Client client, String[] params) {
        DevtestScriptManager scriptManager = new DevtestScriptManager();
        ScriptHandle scriptHandle = scriptManager.load("devtest.js");
        if (scriptHandle == null) {
            log.info("devtest.js was not found or failed to load");
            return;
        }
        try {
            scriptHandle.invoke("run", ScriptInvocationContext.empty(), client.getPlayer());
        } catch (ScriptException | NoSuchMethodException e) {
            log.info("devtest.js run() threw an exception", e);
        } finally {
            scriptHandle.close();
        }
    }
}
