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
package scripting;

import client.Client;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);
    private final Path scriptsRoot;
    private final ScriptPathResolver scriptPathResolver;
    private final ScriptEngineFactory sef;

    protected AbstractScriptManager() {
        this(Path.of("scripts"));
    }

    protected AbstractScriptManager(Path scriptsRoot) {
        this.scriptsRoot = scriptsRoot;
        this.scriptPathResolver = new ScriptPathResolver(scriptsRoot);
        sef = new ScriptEngineManager().getEngineByName("graal.js").getFactory();
    }

    protected ScriptHandle loadScript(String directory, String identifier) {
        return loadScript(scriptPathResolver.resolveEntry(directory, identifier));
    }

    protected ScriptHandle loadScript(String directory, String identifier, Client c) {
        Path scriptFile = scriptPathResolver.resolveEntry(directory, identifier);
        String cacheKey = cacheKey(scriptFile);
        ScriptHandle handle = c.getScriptHandle(cacheKey);
        if (handle == null) {
            handle = loadScript(scriptFile);
            if (handle != null) {
                c.setScriptHandle(cacheKey, handle);
            }
        }
        return handle;
    }

    protected ScriptHandle loadScript(String path) {
        return loadScript(scriptsRoot.resolve(path).normalize());
    }

    private ScriptHandle loadScript(Path scriptFile) {
        if (!Files.exists(scriptFile)) {
            return null;
        }

        String source;
        try {
            source = Files.readString(scriptFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Exception during script read for file: {}", scriptFile, e);
            return null;
        }

        ScriptMode mode;
        try {
            mode = ScriptClassifier.classify(scriptFile, source);
        } catch (IllegalArgumentException e) {
            log.warn("Exception during script classification for file: {}", scriptFile, e);
            return null;
        }

        return switch (mode) {
            case LEGACY -> loadLegacyScript(scriptFile);
            case ESM -> loadEsmScript(scriptFile);
        };
    }

    private LegacyScriptHandle loadLegacyScript(Path scriptFile) {
        ScriptEngine engine = sef.getScriptEngine();
        if (!(engine instanceof GraalJSScriptEngine graalScriptEngine)) {
            throw new IllegalStateException("ScriptEngineFactory did not provide a GraalJSScriptEngine");
        }

        enableScriptHostAccess(graalScriptEngine);

        try (BufferedReader br = Files.newBufferedReader(scriptFile, StandardCharsets.UTF_8)) {
            engine.eval(br);
        } catch (final ScriptException | IOException t) {
            log.warn("Exception during script eval for file: {}", scriptFile, t);
            return null;
        }

        return new LegacyScriptHandle(engine, (Invocable) graalScriptEngine);
    }

    private EsmScriptHandle loadEsmScript(Path scriptFile) {
        try {
            return EsmScriptHandle.load(scriptFile);
        } catch (ScriptLoadException e) {
            log.warn("Exception during ESM script eval for file: {}", scriptFile, e);
            return null;
        }
    }

    protected ScriptEngine getInvocableScriptEngine(String path) {
        Path scriptFile = Path.of("scripts", path);
        if (!Files.exists(scriptFile)) {
            return null;
        }

        ScriptEngine engine = sef.getScriptEngine();
        if (!(engine instanceof GraalJSScriptEngine graalScriptEngine)) {
            throw new IllegalStateException("ScriptEngineFactory did not provide a GraalJSScriptEngine");
        }

        enableScriptHostAccess(graalScriptEngine);

        try (BufferedReader br = Files.newBufferedReader(scriptFile, StandardCharsets.UTF_8)) {
            engine.eval(br);
        } catch (final ScriptException | IOException t) {
            log.warn("Exception during script eval for file: {}", path, t);
            return null;
        }

        return graalScriptEngine;
    }

    protected ScriptEngine getInvocableScriptEngine(String path, Client c) {
        ScriptEngine engine = c.getScriptEngine("scripts/" + path);
        if (engine == null) {
            engine = getInvocableScriptEngine(path);
            c.setScriptEngine(path, engine);
        }

        return engine;
    }

    /**
     * Allow usage of "Java.type()" in script to look up host class
     */
    private void enableScriptHostAccess(GraalJSScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", true);
    }

    protected void resetContext(String path, Client c) {
        c.removeScriptEngine("scripts/" + path);
    }

    protected void resetContext(String directory, String identifier, Client c) {
        c.removeScriptHandle(cacheKey(scriptPathResolver.resolveEntry(directory, identifier)));
    }

    private static String cacheKey(Path scriptFile) {
        return scriptFile.normalize().toString();
    }
}
