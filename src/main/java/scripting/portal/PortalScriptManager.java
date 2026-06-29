/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.portal;

import client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.ScriptHandle;
import scripting.ScriptInvocationContext;
import server.maps.Portal;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class PortalScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(PortalScriptManager.class);
    private static final PortalScriptManager instance = new PortalScriptManager();

    private final Map<String, ScriptHandle> scripts = new HashMap<>();

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private ScriptHandle getPortalScript(String scriptName) throws ScriptException {
        ScriptHandle handle = scripts.get(scriptName);
        if (handle != null) {
            return handle;
        }

        handle = loadScript("portal", scriptName);
        if (handle == null) {
            return null;
        }

        if (!handle.hasCallback("enter")) {
            throw new ScriptException(String.format("Portal script \"%s\" fails to implement the PortalScript interface", scriptName));
        }

        scripts.put(scriptName, handle);
        return handle;
    }

    public boolean executePortalScript(Portal portal, Client c) {
        try {
            ScriptHandle handle = getPortalScript(portal.getScriptName());
            if (handle != null) {
                PortalPlayerInteraction interaction = new PortalPlayerInteraction(c, portal);
                Object result = handle.invoke("enter", ScriptInvocationContext.of("pi", interaction), interaction);
                return result instanceof Boolean entered && entered;
            }
        } catch (Exception e) {
            log.warn("Portal script error in: {}", portal.getScriptName(), e);
        }
        return false;
    }

    public void reloadPortalScripts() {
        for (ScriptHandle handle : scripts.values()) {
            handle.close();
        }
        scripts.clear();
    }
}
