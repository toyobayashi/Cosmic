package scripting;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleScriptHandle implements ScriptHandle {
    private static final String MODULE_MIME_TYPE = "application/javascript+module";
    private static final Logger scriptLog = LoggerFactory.getLogger("scripting.js");
    private static final Map<Path, CommonJsRuntime> runtimeByGeneratedModulePath = new ConcurrentHashMap<>();

    private final Path entryPath;
    private final Context context;
    private final Value namespace;
    private final Map<Path, Path> sourceMap;
    private final Map<Path, Integer> lineOffsets;
    private final Path generatedRoot;
    private final CommonJsRuntime commonJsRuntime;
    private boolean closed;
    private boolean closeRequested;
    private int activeInvocations;

    private ModuleScriptHandle(Path entryPath, Context context, Value namespace, Map<Path, Path> sourceMap, Map<Path, Integer> lineOffsets, Path generatedRoot, CommonJsRuntime commonJsRuntime) {
        this.entryPath = entryPath;
        this.context = context;
        this.namespace = namespace;
        this.sourceMap = sourceMap;
        this.lineOffsets = lineOffsets;
        this.generatedRoot = generatedRoot;
        this.commonJsRuntime = commonJsRuntime;
    }

    public static ModuleScriptHandle load(Path entryPath) {
        return load(entryPath, new LoggingOutputStream(scriptLog, false), new LoggingOutputStream(scriptLog, true));
    }

    static ModuleScriptHandle loadForTesting(Path entryPath, OutputStream out, OutputStream err) {
        return load(entryPath, out, err);
    }

    private static ModuleScriptHandle load(Path entryPath, OutputStream out, OutputStream err) {
        Objects.requireNonNull(entryPath);
        Path realEntryPath = realPath(entryPath, entryPath);
        NodeModuleGraph.Result moduleGraph;
        try {
            moduleGraph = NodeModuleGraph.buildWithSourceMap(realEntryPath);
        } catch (ScriptLoadException e) {
            // toRealPath() can expand Windows 8.3 paths. Preserve the path
            // supplied by the caller in the public load error as well.
            throw new ScriptLoadException(
                    "Failed to load module script " + entryPath + ": " + e.getMessage(), e);
        }
        Path generatedEntryPath = moduleGraph.entryPath();

        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .allowIO(IOAccess.ALL)
                .out(out)
                .err(err)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true")
                .build();

        CommonJsRuntime commonJsRuntime = null;
        try {
            commonJsRuntime = new CommonJsRuntime(context, moduleGraph.graph(), moduleGraph.sourceMap(), moduleGraph.lineOffsets(), moduleGraph.builtinNames());
            commonJsRuntime.registerGeneratedModules(moduleGraph.sourceMap().keySet());
            ScriptRuntimeSupport.installGlobals(context, commonJsRuntime, realEntryPath);
            Source source = Source.newBuilder("js", generatedEntryPath.toFile())
                    .mimeType(MODULE_MIME_TYPE)
                    .build();
            Value namespace = context.eval(source);
            return new ModuleScriptHandle(realEntryPath, context, namespace, moduleGraph.sourceMap(), moduleGraph.lineOffsets(), moduleGraph.generatedRoot(), commonJsRuntime);
        } catch (IOException | PolyglotException e) {
            removeGeneratedRuntime(commonJsRuntime);
            context.close();
            deleteGeneratedRoot(moduleGraph.generatedRoot());
            throw new ScriptLoadException("Failed to load module script " + realEntryPath + ": " + e.getMessage(), e);
        }
    }

    public static Value requireCommonJsFromGenerated(String generatedUrl) {
        CommonJsRuntime runtime = runtimeFromGenerated(generatedUrl);
        return runtime.requireByPath(runtime.originalPathForGenerated(generatedUrl).toString());
    }

    public static Value importJsonFromGenerated(String generatedUrl) {
        CommonJsRuntime runtime = runtimeFromGenerated(generatedUrl);
        return runtime.requireJson(runtime.originalPathForGenerated(generatedUrl));
    }

    public static String importMetaFilename(String generatedUrl) {
        return runtimeFromGenerated(generatedUrl).originalPathForGenerated(generatedUrl).toString();
    }

    public static String importMetaDirname(String generatedUrl) {
        return runtimeFromGenerated(generatedUrl).originalPathForGenerated(generatedUrl).getParent().toString();
    }

    public static String importMetaSourceUrl(String generatedUrl) {
        return runtimeFromGenerated(generatedUrl).originalPathForGenerated(generatedUrl).toUri().toString();
    }

    public static String resolveImportMetaFromGenerated(String generatedUrl, String specifier) {
        CommonJsRuntime runtime = runtimeFromGenerated(generatedUrl);
        return runtime.resolveImportMeta(runtime.originalPathForGenerated(generatedUrl).toString(), specifier);
    }

    public static String builtinNameFromGenerated(String generatedUrl) {
        return runtimeFromGenerated(generatedUrl).builtinNameForGenerated(generatedUrl);
    }

    private static CommonJsRuntime runtimeFromGenerated(String generatedUrl) {
        CommonJsRuntime runtime = runtimeByGeneratedModulePath.get(realGeneratedPath(generatedUrl));
        if (runtime == null) {
            throw new ScriptLoadException("Module runtime is closed for generated module: " + generatedUrl);
        }
        return runtime;
    }

    private static Path realGeneratedPath(String generatedUrl) {
        try {
            Path path = generatedUrl.startsWith("file:") ? Path.of(URI.create(generatedUrl)) : Path.of(generatedUrl);
            return path.toRealPath();
        } catch (IOException e) {
            throw new ScriptLoadException("Missing generated module: " + generatedUrl, e);
        }
    }

    @Override
    public Object invoke(String callback, ScriptInvocationContext invocationContext, Object... arguments)
            throws ScriptException, NoSuchMethodException {
        boolean entered = false;
        try {
            enterInvocation();
            entered = true;
            Value exportedCallback = namespace.getMember(callback);
            if (exportedCallback == null || !exportedCallback.canExecute()) {
                throw new NoSuchMethodException("Module script " + entryPath + " does not export function " + callback);
            }

            Object[] moduleArguments = new Object[arguments.length + 1];
            moduleArguments[0] = context.asValue(invocationContext.toProxyObject());
            System.arraycopy(arguments, 0, moduleArguments, 1, arguments.length);

            Value result = exportedCallback.execute(moduleArguments);
            return result.isNull() ? null : result.as(Object.class);
        } catch (PolyglotException e) {
            ScriptException scriptException = toScriptException(
                    "Failed to invoke module callback " + callback + " in " + entryPath,
                    e,
                    entryPath,
                    sourceMap,
                    lineOffsets);
            scriptException.initCause(e);
            throw scriptException;
        } finally {
            if (entered) {
                exitInvocation();
            }
        }
    }

    @Override
    public boolean hasCallback(String callback) {
        ensureOpen();
        Value member = namespace.getMember(callback);
        return member != null && member.canExecute();
    }

    @Override
    public void close() {
        boolean shouldClose = false;
        synchronized (this) {
            if (closed || closeRequested) {
                return;
            }
            if (activeInvocations > 0) {
                closeRequested = true;
            } else {
                closed = true;
                shouldClose = true;
            }
        }

        if (shouldClose) {
            closeContext();
        }
    }

    boolean isClosedForTesting() {
        return closed;
    }

    boolean generatedRootExistsForTesting() {
        return Files.exists(generatedRoot);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Module script handle is closed: " + entryPath);
        }
    }

    private synchronized void enterInvocation() {
        ensureOpen();
        activeInvocations++;
    }

    private void exitInvocation() {
        boolean shouldClose = false;
        synchronized (this) {
            activeInvocations--;
            if (activeInvocations == 0 && closeRequested && !closed) {
                closed = true;
                shouldClose = true;
            }
        }

        if (shouldClose) {
            closeContext();
        }
    }

    private void closeContext() {
        removeGeneratedRuntime(commonJsRuntime);
        context.close();
        deleteGeneratedRoot(generatedRoot);
    }

    private static void removeGeneratedRuntime(CommonJsRuntime commonJsRuntime) {
        if (commonJsRuntime != null) {
            runtimeByGeneratedModulePath.entrySet().removeIf(entry -> entry.getValue() == commonJsRuntime);
        }
    }

    private static void deleteGeneratedRoot(Path generatedRoot) {
        if (generatedRoot == null || !Files.exists(generatedRoot)) {
            return;
        }
        try (var paths = Files.walk(generatedRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static Path realPath(Path path, Path importer) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new ScriptLoadException("Missing module imported from " + importer + ": " + path, e);
        }
    }

    private static ScriptException toScriptException(
            String prefix,
            PolyglotException exception,
            Path fallbackPath,
            Map<Path, Path> sourceMap,
            Map<Path, Integer> lineOffsets) {
        SourceSection sourceLocation = sourceLocation(exception);
        String location = locationString(sourceLocation, sourceMap, lineOffsets);
        String reason = prefix + (location.isEmpty() ? "" : " at " + location) + ": " + exception.getMessage();
        if (sourceLocation == null || !sourceLocation.isAvailable()) {
            return new ScriptException(reason, fallbackPath.toString(), -1);
        }

        String fileName = sourceName(sourceLocation, fallbackPath, sourceMap);
        int line = Math.max(1, sourceLocation.getStartLine() - sourceLineOffset(sourceLocation, lineOffsets));
        return new ScriptException(reason, fileName, line, sourceLocation.getStartColumn());
    }

    private static SourceSection sourceLocation(PolyglotException exception) {
        SourceSection sourceLocation = exception.getSourceLocation();
        if (sourceLocation != null && sourceLocation.isAvailable()) {
            return sourceLocation;
        }

        for (PolyglotException.StackFrame frame : exception.getPolyglotStackTrace()) {
            if (!frame.isGuestFrame()) {
                continue;
            }
            SourceSection frameLocation = frame.getSourceLocation();
            if (frameLocation != null && frameLocation.isAvailable()) {
                return frameLocation;
            }
        }

        return sourceLocation;
    }

    private static String locationString(SourceSection sourceLocation, Map<Path, Path> sourceMap, Map<Path, Integer> lineOffsets) {
        if (sourceLocation == null || !sourceLocation.isAvailable()) {
            return "";
        }

        int line = Math.max(1, sourceLocation.getStartLine() - sourceLineOffset(sourceLocation, lineOffsets));
        return sourceName(sourceLocation, null, sourceMap) + ":" + line + ":" + sourceLocation.getStartColumn();
    }

    private static int sourceLineOffset(SourceSection sourceLocation, Map<Path, Integer> lineOffsets) {
        Source source = sourceLocation.getSource();
        if (source == null || source.getPath() == null) {
            return 0;
        }
        return lineOffsets.getOrDefault(Path.of(source.getPath()), 0);
    }

    private static String sourceName(SourceSection sourceLocation, Path fallbackPath, Map<Path, Path> sourceMap) {
        Source source = sourceLocation.getSource();
        if (source != null) {
            String path = source.getPath();
            if (path != null) {
                return sourceMap.getOrDefault(Path.of(path), Path.of(path)).toString();
            }
            String name = source.getName();
            if (name != null) {
                return name;
            }
        }
        return fallbackPath == null ? "<unknown>" : fallbackPath.toString();
    }

    private static final class LoggingOutputStream extends OutputStream {
        private final Logger logger;
        private final boolean error;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private LoggingOutputStream(Logger logger, boolean error) {
            this.logger = logger;
            this.error = error;
        }

        @Override
        public synchronized void write(int value) {
            if (value == '\n') {
                logBufferedLine();
            } else if (value != '\r') {
                buffer.write(value);
            }
        }

        @Override
        public synchronized void flush() {
            logBufferedLine();
        }

        private void logBufferedLine() {
            if (buffer.size() == 0) {
                return;
            }

            String line = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            if (error) {
                logger.error(line);
            } else {
                logger.info(line);
            }
        }
    }

    public static final class CommonJsRuntime {
        private final Context context;
        private final NodeModuleGraph moduleGraph;
        private final Map<Path, Path> sourceMap;
        private final Map<Path, Integer> lineOffsets;
        private final Map<Path, String> builtinNames;
        private final Value requireCache;
        private final Map<Path, Value> commonJsModules = new HashMap<>();
        private final Map<Path, Value> esmModules = new HashMap<>();
        private final Map<String, Value> builtinModules = new HashMap<>();

        private CommonJsRuntime(Context context, NodeModuleGraph moduleGraph, Map<Path, Path> sourceMap, Map<Path, Integer> lineOffsets, Map<Path, String> builtinNames) {
            this.context = context;
            this.moduleGraph = moduleGraph;
            this.sourceMap = sourceMap;
            this.lineOffsets = lineOffsets;
            this.builtinNames = new HashMap<>(builtinNames);
            this.requireCache = context.eval("js", "Object.create(null)");
        }

        private void registerGeneratedModules(Iterable<Path> generatedPaths) {
            for (Path generatedPath : generatedPaths) {
                try {
                    runtimeByGeneratedModulePath.put(generatedPath.toRealPath(), this);
                } catch (IOException e) {
                    throw new ScriptLoadException("Failed to register generated module " + generatedPath + ": " + e.getMessage(), e);
                }
            }
        }

        private Path originalPathForGenerated(String generatedUrl) {
            Path generatedPath = realGeneratedPath(generatedUrl);
            Path originalPath = sourceMap.get(generatedPath);
            if (originalPath == null) {
                throw new ScriptLoadException("Unknown generated module: " + generatedUrl);
            }
            return originalPath;
        }

        private String builtinNameForGenerated(String generatedUrl) {
            String builtinName = builtinNames.get(realGeneratedPath(generatedUrl));
            if (builtinName == null) {
                throw new ScriptLoadException("Generated module is not a builtin: " + generatedUrl);
            }
            return builtinName;
        }

        public Value requireByPath(String modulePath) {
            Path path = realPath(Path.of(modulePath), Path.of(modulePath));
            if (NodeModuleGraph.isJsonPath(path)) {
                return requireJson(path);
            }
            ScriptMode mode = moduleGraph.moduleMode(path, NodeModuleGraph.LinkKind.REQUIRE);
            if (mode == ScriptMode.ESM) {
                return requireEsm(path, path);
            }
            return requireCommonJs(path);
        }

        public Value require(String importerPath, String specifier) {
            if (NodeModuleGraph.isBuiltin(specifier)) {
                return requireBuiltin(specifier);
            }
            Path importer = realPath(Path.of(importerPath), Path.of(importerPath));
            Path resolved = moduleGraph.resolve(importer, specifier, NodeModuleGraph.LinkKind.REQUIRE);
            if (NodeModuleGraph.isJsonPath(resolved)) {
                return requireJson(resolved);
            }
            ScriptMode mode = moduleGraph.moduleMode(resolved, NodeModuleGraph.LinkKind.REQUIRE);
            if (mode == ScriptMode.ESM) {
                return requireEsm(resolved, importer);
            }
            return requireCommonJs(resolved);
        }

        public String resolve(String importerPath, String specifier) {
            if (NodeModuleGraph.isBuiltin(specifier)) {
                return specifier;
            }
            Path importer = realPath(Path.of(importerPath), Path.of(importerPath));
            return moduleGraph.resolve(importer, specifier, NodeModuleGraph.LinkKind.REQUIRE).toString();
        }

        public Value createRequire(String filename) {
            Path importer = fileOrPathToPath(filename);
            String source = """
                    (function(__runtime, __filename, __requireCache) {
                      function require(specifier) {
                        return __runtime.require(__filename, String(specifier));
                      }
                      require.cache = __requireCache;
                      require.resolve = function(specifier) {
                        return __runtime.resolve(__filename, String(specifier));
                      };
                      return require;
                    })
                    """;
            Value factory = context.eval("js", source);
            return factory.execute(this, importer.toString(), requireCache);
        }

        public String resolveImportMeta(String importerPath, String specifier) {
            if (NodeModuleGraph.isBuiltin(specifier)) {
                return specifier.startsWith("node:") ? specifier : "node:" + NodeModuleGraph.builtinName(specifier);
            }
            Path importer = realPath(Path.of(importerPath), Path.of(importerPath));
            return moduleGraph.resolve(importer, specifier, NodeModuleGraph.LinkKind.IMPORT).toUri().toString();
        }

        private Value requireBuiltin(String specifier) {
            String builtinName = NodeModuleGraph.builtinName(specifier);
            Value existing = builtinModules.get(builtinName);
            if (existing != null) {
                return existing;
            }

            Value builtinGetter = context.eval("js", "(function(name) { return process.getBuiltinModule(name); })");
            Value exports = builtinGetter.execute(builtinName);
            builtinModules.put(builtinName, exports);
            return exports;
        }

        private Value requireJson(Path modulePath) {
            Path realModulePath = realPath(modulePath, modulePath);
            Value module = commonJsModules.get(realModulePath);
            if (module != null) {
                return module.getMember("exports");
            }

            module = context.eval("js", "({ exports: {}, loaded: false })");
            module.putMember("id", realModulePath.toString());
            module.putMember("filename", realModulePath.toString());
            commonJsModules.put(realModulePath, module);
            requireCache.putMember(realModulePath.toString(), module);

            try {
                Value parsed = context.eval("js", "JSON.parse").execute(NodeModuleGraph.read(realModulePath));
                module.putMember("exports", parsed);
                module.putMember("loaded", true);
                return parsed;
            } catch (PolyglotException e) {
                commonJsModules.remove(realModulePath);
                requireCache.removeMember(realModulePath.toString());
                throw e;
            }
        }

        private Value requireCommonJs(Path modulePath) {
            Path realModulePath = realPath(modulePath, modulePath);
            Value module = commonJsModules.get(realModulePath);
            if (module != null) {
                return module.getMember("exports");
            }

            module = context.eval("js", "({ exports: {} })");
            module.putMember("id", realModulePath.toString());
            module.putMember("filename", realModulePath.toString());
            module.putMember("loaded", false);
            commonJsModules.put(realModulePath, module);
            requireCache.putMember(realModulePath.toString(), module);

            String source = NodeModuleGraph.read(realModulePath);
            String wrapper = commonJsWrapper(source);
            try {
                Value moduleFunction = context.eval(Source.newBuilder("js", wrapper, realModulePath.toString()).build());
                Value exports = moduleFunction.execute(module, this, requireCache, realModulePath.toString(), realModulePath.getParent().toString());
                module.putMember("loaded", true);
                return exports;
            } catch (IOException e) {
                commonJsModules.remove(realModulePath);
                requireCache.removeMember(realModulePath.toString());
                throw new ScriptLoadException("Failed to build CommonJS wrapper for " + realModulePath + ": " + e.getMessage(), e);
            } catch (PolyglotException e) {
                commonJsModules.remove(realModulePath);
                requireCache.removeMember(realModulePath.toString());
                throw e;
            }
        }

        private Value requireEsm(Path modulePath, Path importer) {
            Path realModulePath = realPath(modulePath, importer);
            Value namespace = esmModules.get(realModulePath);
            if (namespace != null) {
                return namespace;
            }

            Path generatedPath = moduleGraph.materializeModule(realModulePath, importer, NodeModuleGraph.LinkKind.REQUIRE);
            try {
                Path realGeneratedPath = generatedPath.toRealPath();
                sourceMap.put(realGeneratedPath, realModulePath);
                lineOffsets.put(realGeneratedPath, moduleGraph.lineOffset(realModulePath));
                registerGeneratedModules(List.of(realGeneratedPath));
                Source source = Source.newBuilder("js", generatedPath.toFile())
                        .mimeType(MODULE_MIME_TYPE)
                        .build();
                namespace = context.eval(source);
                esmModules.put(realModulePath, namespace);
                return namespace;
            } catch (IOException e) {
                throw new ScriptLoadException("Failed to load ESM module required from " + importer + ": " + realModulePath, e);
            }
        }

        private static String commonJsWrapper(String source) {
            return """
                    (function(module, __runtime, __requireCache, __filename, __dirname) {
                    let exports = module.exports;
                    function require(specifier) {
                      return __runtime.require(__filename, String(specifier));
                    }
                    require.cache = __requireCache;
                    require.resolve = function(specifier) {
                      return __runtime.resolve(__filename, String(specifier));
                    };
                    (function(exports, module, require, __filename, __dirname) {
                    %s
                    }).call(module.exports, exports, module, require, __filename, __dirname);
                    return module.exports;
                    })
                    """.formatted(source);
        }

        private static Path fileOrPathToPath(String filename) {
            if (filename.startsWith("file:")) {
                return realPath(Path.of(URI.create(filename)), Path.of(URI.create(filename)));
            }
            return realPath(Path.of(filename), Path.of(filename));
        }

    }

}
