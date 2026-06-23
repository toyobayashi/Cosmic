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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class EsmScriptHandle implements ScriptHandle {
    private static final String MODULE_MIME_TYPE = "application/javascript+module";
    private static final Logger scriptLog = LoggerFactory.getLogger("scripting.js");

    private final Path entryPath;
    private final Context context;
    private final Value namespace;
    private boolean closed;
    private boolean closeRequested;
    private int activeInvocations;

    private EsmScriptHandle(Path entryPath, Context context, Value namespace) {
        this.entryPath = entryPath;
        this.context = context;
        this.namespace = namespace;
    }

    public static EsmScriptHandle load(Path entryPath) {
        return load(entryPath, new LoggingOutputStream(scriptLog, false), new LoggingOutputStream(scriptLog, true));
    }

    static EsmScriptHandle loadForTesting(Path entryPath, OutputStream out, OutputStream err) {
        return load(entryPath, out, err);
    }

    private static EsmScriptHandle load(Path entryPath, OutputStream out, OutputStream err) {
        Objects.requireNonNull(entryPath);
        Path realEntryPath = realPath(entryPath, entryPath);
        validateModuleGraph(realEntryPath, new HashSet<>());

        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .allowIO(IOAccess.ALL)
                .out(out)
                .err(err)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true")
                .build();

        try {
            Source source = Source.newBuilder("js", realEntryPath.toFile())
                    .mimeType(MODULE_MIME_TYPE)
                    .build();
            Value namespace = context.eval(source);
            return new EsmScriptHandle(realEntryPath, context, namespace);
        } catch (IOException | PolyglotException e) {
            context.close();
            throw new ScriptLoadException("Failed to load ESM script " + realEntryPath + ": " + e.getMessage(), e);
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
                throw new NoSuchMethodException("ESM script " + entryPath + " does not export function " + callback);
            }

            Object[] esmArguments = new Object[arguments.length + 1];
            esmArguments[0] = context.asValue(invocationContext.toProxyObject());
            System.arraycopy(arguments, 0, esmArguments, 1, arguments.length);

            Value result = exportedCallback.execute(esmArguments);
            return result.isNull() ? null : result.as(Object.class);
        } catch (PolyglotException e) {
            ScriptException scriptException = toScriptException(
                    "Failed to invoke ESM callback " + callback + " in " + entryPath,
                    e,
                    entryPath);
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
            context.close();
        }
    }

    boolean isClosedForTesting() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ESM script handle is closed: " + entryPath);
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
            context.close();
        }
    }

    private static void validateModuleGraph(Path modulePath, Set<Path> visited) {
        Path realModulePath = realPath(modulePath, modulePath);
        if (!visited.add(realModulePath)) {
            return;
        }
        if (!isJavaScriptPath(realModulePath)) {
            throw new ScriptLoadException("Unsupported ESM module file kind: " + realModulePath);
        }

        String source;
        try {
            source = Files.readString(realModulePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to read ESM module " + realModulePath + ": " + e.getMessage(), e);
        }

        ModuleScanner scanner = new ModuleScanner(realModulePath, source);
        scanner.validateUnsupportedSyntax();
        for (String specifier : scanner.staticSpecifiers()) {
            Path importedPath = resolveSpecifier(realModulePath, specifier);
            validateModuleGraph(importedPath, visited);
        }
    }

    private static Path resolveSpecifier(Path importer, String specifier) {
        if (specifier.startsWith("node:")) {
            throw new ScriptLoadException("Unsupported node: ESM specifier in " + importer + ": " + specifier);
        }
        if (specifier.contains("://")) {
            throw new ScriptLoadException("Unsupported URL ESM specifier in " + importer + ": " + specifier);
        }
        Path importedPath;
        if (specifier.startsWith("./") || specifier.startsWith("../")) {
            importedPath = importer.getParent().resolve(specifier).normalize();
        } else if (Path.of(specifier).isAbsolute()) {
            importedPath = Path.of(specifier).normalize();
        } else {
            throw new ScriptLoadException("Unsupported bare ESM specifier in " + importer + ": " + specifier);
        }
        if (!isJavaScriptPath(importedPath)) {
            throw new ScriptLoadException("Unsupported ESM module file kind in " + importer + ": " + specifier);
        }
        return realPath(importedPath, importer);
    }

    private static Path realPath(Path path, Path importer) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new ScriptLoadException("Missing ESM module imported from " + importer + ": " + path, e);
        }
    }

    private static boolean isJavaScriptPath(Path path) {
        String filename = path.getFileName().toString();
        return filename.endsWith(".js") || filename.endsWith(".mjs");
    }

    private static ScriptException toScriptException(String prefix, PolyglotException exception, Path fallbackPath) {
        SourceSection sourceLocation = sourceLocation(exception);
        String location = locationString(sourceLocation);
        String reason = prefix + (location.isEmpty() ? "" : " at " + location) + ": " + exception.getMessage();
        if (sourceLocation == null || !sourceLocation.isAvailable()) {
            return new ScriptException(reason, fallbackPath.toString(), -1);
        }

        String fileName = sourceName(sourceLocation, fallbackPath);
        return new ScriptException(reason, fileName, sourceLocation.getStartLine(), sourceLocation.getStartColumn());
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

    private static String locationString(SourceSection sourceLocation) {
        if (sourceLocation == null || !sourceLocation.isAvailable()) {
            return "";
        }

        return sourceName(sourceLocation, null) + ":" + sourceLocation.getStartLine() + ":" + sourceLocation.getStartColumn();
    }

    private static String sourceName(SourceSection sourceLocation, Path fallbackPath) {
        Source source = sourceLocation.getSource();
        if (source != null) {
            String path = source.getPath();
            if (path != null) {
                return path;
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

    private static final class ModuleScanner {
        private final Path path;
        private final String source;

        private ModuleScanner(Path path, String source) {
            this.path = path;
            this.source = source;
        }

        private void validateUnsupportedSyntax() {
            scan((token, tokenStart, tokenEnd, braceDepth, bracketDepth, parenDepth) -> {
                if ("import".equals(token) && nextSignificantChar(tokenEnd) == '(') {
                    throw unsupported("dynamic import()", tokenStart);
                }
                if ("await".equals(token) && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                    throw unsupported("top-level await", tokenStart);
                }
            });
        }

        private Set<String> staticSpecifiers() {
            Set<String> specifiers = new HashSet<>();
            scan((token, tokenStart, tokenEnd, braceDepth, bracketDepth, parenDepth) -> {
                if (braceDepth != 0 || bracketDepth != 0 || parenDepth != 0) {
                    return;
                }
                if ("import".equals(token) && nextSignificantChar(tokenEnd) != '(') {
                    String specifier = importSpecifierAfter(tokenEnd);
                    if (specifier != null) {
                        specifiers.add(specifier);
                    }
                } else if ("export".equals(token)) {
                    String specifier = exportSpecifierAfter(tokenEnd);
                    if (specifier != null) {
                        specifiers.add(specifier);
                    }
                }
            });
            return specifiers;
        }

        private void scan(TokenConsumer consumer) {
            int braceDepth = 0;
            int bracketDepth = 0;
            int parenDepth = 0;

            for (int index = 0; index < source.length(); ) {
                char current = source.charAt(index);
                char next = charAt(index + 1);
                if (current == '/' && next == '/') {
                    index = skipLineComment(index + 2);
                } else if (current == '/' && next == '*') {
                    index = skipBlockComment(index + 2);
                } else if (current == '\'' || current == '"') {
                    index = skipQuotedString(index + 1, current);
                } else if (current == '`') {
                    index = skipTemplateLiteral(index + 1);
                } else if (isIdentifierStart(current)) {
                    int end = skipIdentifier(index + 1);
                    consumer.accept(source.substring(index, end), index, end, braceDepth, bracketDepth, parenDepth);
                    index = end;
                } else {
                    switch (current) {
                        case '{' -> braceDepth++;
                        case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                        case '[' -> bracketDepth++;
                        case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                        case '(' -> parenDepth++;
                        case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                        default -> {
                        }
                    }
                    index++;
                }
            }
        }

        private String importSpecifierAfter(int tokenEnd) {
            int index = skipWhitespaceAndComments(tokenEnd);
            if (index >= source.length()) {
                return null;
            }
            char first = source.charAt(index);
            if (first == '\'' || first == '"') {
                return readStringLiteral(index);
            }
            int from = findTopLevelWordBeforeSemicolon(index, "from");
            if (from < 0) {
                return null;
            }
            int specifierStart = skipWhitespaceAndComments(from + "from".length());
            if (specifierStart < source.length()
                    && (source.charAt(specifierStart) == '\'' || source.charAt(specifierStart) == '"')) {
                return readStringLiteral(specifierStart);
            }
            return null;
        }

        private String exportSpecifierAfter(int tokenEnd) {
            int from = findTopLevelWordBeforeSemicolon(tokenEnd, "from");
            if (from < 0) {
                return null;
            }
            int specifierStart = skipWhitespaceAndComments(from + "from".length());
            if (specifierStart < source.length()
                    && (source.charAt(specifierStart) == '\'' || source.charAt(specifierStart) == '"')) {
                return readStringLiteral(specifierStart);
            }
            return null;
        }

        private int findTopLevelWordBeforeSemicolon(int start, String word) {
            int braceDepth = 0;
            int bracketDepth = 0;
            int parenDepth = 0;
            for (int index = start; index < source.length(); ) {
                char current = source.charAt(index);
                char next = charAt(index + 1);
                if (current == ';' && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                    return -1;
                }
                if (current == '/' && next == '/') {
                    index = skipLineComment(index + 2);
                } else if (current == '/' && next == '*') {
                    index = skipBlockComment(index + 2);
                } else if (current == '\'' || current == '"') {
                    index = skipQuotedString(index + 1, current);
                } else if (current == '`') {
                    index = skipTemplateLiteral(index + 1);
                } else if (isIdentifierStart(current)) {
                    int end = skipIdentifier(index + 1);
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0
                            && word.equals(source.substring(index, end))) {
                        return index;
                    }
                    index = end;
                } else {
                    switch (current) {
                        case '{' -> braceDepth++;
                        case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                        case '[' -> bracketDepth++;
                        case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                        case '(' -> parenDepth++;
                        case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                        default -> {
                        }
                    }
                    index++;
                }
            }
            return -1;
        }

        private char nextSignificantChar(int index) {
            int next = skipWhitespaceAndComments(index);
            return next < source.length() ? source.charAt(next) : '\0';
        }

        private int skipWhitespaceAndComments(int index) {
            int current = index;
            while (current < source.length()) {
                char character = source.charAt(current);
                char next = charAt(current + 1);
                if (Character.isWhitespace(character)) {
                    current++;
                } else if (character == '/' && next == '/') {
                    current = skipLineComment(current + 2);
                } else if (character == '/' && next == '*') {
                    current = skipBlockComment(current + 2);
                } else {
                    return current;
                }
            }
            return current;
        }

        private int skipLineComment(int index) {
            int current = index;
            while (current < source.length() && source.charAt(current) != '\n' && source.charAt(current) != '\r') {
                current++;
            }
            return current;
        }

        private int skipBlockComment(int index) {
            int current = index;
            while (current + 1 < source.length()) {
                if (source.charAt(current) == '*' && source.charAt(current + 1) == '/') {
                    return current + 2;
                }
                current++;
            }
            return source.length();
        }

        private int skipQuotedString(int index, char quote) {
            int current = index;
            while (current < source.length()) {
                char character = source.charAt(current);
                if (character == '\\') {
                    current += 2;
                } else if (character == quote) {
                    return current + 1;
                } else {
                    current++;
                }
            }
            return current;
        }

        private String readStringLiteral(int index) {
            char quote = source.charAt(index);
            StringBuilder value = new StringBuilder();
            int current = index + 1;
            while (current < source.length()) {
                char character = source.charAt(current);
                if (character == '\\' && current + 1 < source.length()) {
                    value.append(source.charAt(current + 1));
                    current += 2;
                } else if (character == quote) {
                    return value.toString();
                } else {
                    value.append(character);
                    current++;
                }
            }
            throw unsupported("unterminated string literal", index);
        }

        private int skipTemplateLiteral(int index) {
            int current = index;
            while (current < source.length()) {
                char character = source.charAt(current);
                if (character == '\\') {
                    current += 2;
                } else if (character == '`') {
                    return current + 1;
                } else {
                    current++;
                }
            }
            return current;
        }

        private int skipIdentifier(int index) {
            int current = index;
            while (current < source.length() && isIdentifierPart(source.charAt(current))) {
                current++;
            }
            return current;
        }

        private ScriptLoadException unsupported(String feature, int index) {
            return new ScriptLoadException("Unsupported ESM feature in " + path + " at " + lineColumn(index) + ": " + feature);
        }

        private String lineColumn(int index) {
            int line = 1;
            int column = 1;
            for (int current = 0; current < index && current < source.length(); current++) {
                if (source.charAt(current) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return line + ":" + column;
        }

        private char charAt(int index) {
            return index < source.length() ? source.charAt(index) : '\0';
        }
    }

    @FunctionalInterface
    private interface TokenConsumer {
        void accept(String token, int tokenStart, int tokenEnd, int braceDepth, int bracketDepth, int parenDepth);
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || character == '$' || Character.isLetter(character);
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || Character.isDigit(character);
    }
}
