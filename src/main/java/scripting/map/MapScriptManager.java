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
package scripting.map;

import client.Character;
import client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.ScriptHandle;
import scripting.ScriptInvocationContext;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class MapScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(MapScriptManager.class);
    private static final MapScriptManager instance = new MapScriptManager();

    private final Map<String, ScriptHandle> scripts = new HashMap<>();

    public static MapScriptManager getInstance() {
        return instance;
    }

    public void reloadScripts() {
        for (ScriptHandle handle : scripts.values()) {
            handle.close();
        }
        scripts.clear();
    }

    public boolean runMapScript(Client c, String mapScriptPath, boolean firstUser) {
        if (firstUser) {
            Character chr = c.getPlayer();
            int mapid = chr.getMapId();
            if (chr.hasEntered(mapScriptPath, mapid)) {
                return false;
            } else {
                chr.enteredScript(mapScriptPath, mapid);
            }
        }

        ScriptHandle handle = scripts.get(mapScriptPath);
        if (handle != null) {
            try {
                MapScriptMethods methods = new MapScriptMethods(c);
                handle.invoke("start", ScriptInvocationContext.of("msm", methods), methods);
                return true;
            } catch (final ScriptException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        try {
            handle = loadScript("map", mapScriptPath);
            if (handle == null) {
                return false;
            }

            scripts.put(mapScriptPath, handle);
            MapScriptMethods methods = new MapScriptMethods(c);
            handle.invoke("start", ScriptInvocationContext.of("msm", methods), methods);
            return true;
        } catch (final Exception e) {
            log.error("Error running map script {}", mapScriptPath, e);
        }

        return false;
    }
}
