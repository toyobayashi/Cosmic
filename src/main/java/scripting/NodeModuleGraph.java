package scripting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NodeModuleGraph {
    private static final Pattern PACKAGE_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern STATIC_IMPORT_EXPORT_PATTERN = Pattern.compile(
            "\\b(import\\s+(?:[^'\";]*?\\s+from\\s*)?|export\\s+[^'\";]*?\\s+from\\s*)(['\"])([^'\"]+)(\\2)(\\s+with\\s*\\{([^}]*)})?");
    private static final Pattern JSON_IMPORT_ATTRIBUTE_PATTERN = Pattern.compile(
            "\\s*\\btype\\s*:\\s*(['\"])json\\1\\s*");
    private static final int IMPORT_META_PRELUDE_LINES = 7;
    private static final Pattern CJS_NAMED_EXPORT_PATTERN = Pattern.compile(
            "(?:exports|module\\.exports)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*=");
    private static final Pattern CJS_OBJECT_EXPORT_PATTERN = Pattern.compile(
            "module\\.exports\\s*=\\s*(?:Object\\.freeze\\s*\\()?\\s*\\{");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private final Path generatedRoot;
    private final Map<Path, ModuleRecord> modules = new HashMap<>();
    private final Map<String, Path> builtinModulePaths = new HashMap<>();
    private final Map<Path, String> builtinNamesByPath = new HashMap<>();

    private NodeModuleGraph(Path generatedRoot) {
        this.generatedRoot = generatedRoot;
    }

    static Result buildWithSourceMap(Path entryPath) {
        try {
            NodeModuleGraph graph = new NodeModuleGraph(Files.createTempDirectory("cosmic-script-modules-"));
            ModuleRecord entry = graph.materialize(entryPath.toRealPath(), null, LinkKind.IMPORT);
            Map<Path, Path> sourceMap = new HashMap<>();
            Map<Path, Integer> lineOffsets = new HashMap<>();
            Map<Path, String> builtinNames = new HashMap<>();
            for (ModuleRecord module : graph.modules.values()) {
                Path generatedPath = module.generatedPath().toRealPath();
                sourceMap.put(generatedPath, module.originalPath());
                lineOffsets.put(generatedPath, module.lineOffset());
                String builtinName = graph.builtinNamesByPath.get(module.originalPath());
                if (builtinName != null) {
                    builtinNames.put(generatedPath, builtinName);
                }
            }
            return new Result(entry.generatedPath(), sourceMap, lineOffsets, builtinNames, graph.generatedRoot, graph);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to create script module graph for " + entryPath + ": " + e.getMessage(), e);
        }
    }

    private ModuleRecord materialize(Path path, Path importer, LinkKind linkKind) {
        Path realPath = realPath(path, importer == null ? path : importer);
        ModuleRecord existing = modules.get(realPath);
        if (existing != null) {
            return existing;
        }

        Path generatedPath = generatedRoot.resolve("m" + modules.size() + ".mjs");
        String source = read(realPath);
        ScriptMode mode = isJsonPath(realPath) ? ScriptMode.ESM : classifyModule(realPath, source, linkKind);
        if (mode == ScriptMode.LEGACY) {
            mode = ScriptMode.COMMONJS;
        }
        int lineOffset = !isJsonPath(realPath) && mode == ScriptMode.ESM ? IMPORT_META_PRELUDE_LINES : 0;
        ModuleRecord record = new ModuleRecord(realPath, generatedPath, mode, lineOffset);
        modules.put(realPath, record);

        String generatedSource = isJsonPath(realPath) ? bridgeJson() : switch (mode) {
            case ESM -> rewriteEsm(realPath, source);
            case COMMONJS -> bridgeCommonJs(realPath, source);
            case LEGACY -> throw new ScriptLoadException("Legacy script cannot be imported as a module: " + realPath);
        };
        try {
            Files.writeString(generatedPath, generatedSource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to write generated module " + generatedPath + ": " + e.getMessage(), e);
        }
        return record;
    }

    Path materializeModule(Path path, Path importer, LinkKind linkKind) {
        return materialize(path, importer, linkKind).generatedPath();
    }

    int lineOffset(Path path) {
        Path realPath = realPath(path, path);
        ModuleRecord existing = modules.get(realPath);
        return existing == null ? 0 : existing.lineOffset();
    }

    ScriptMode moduleMode(Path path, LinkKind linkKind) {
        Path realPath = realPath(path, path);
        ModuleRecord existing = modules.get(realPath);
        if (existing != null) {
            return existing.mode();
        }
        if (isJsonPath(realPath)) {
            return ScriptMode.COMMONJS;
        }
        String source = read(realPath);
        ScriptMode mode = classifyModule(realPath, source, linkKind);
        return mode == ScriptMode.LEGACY ? ScriptMode.COMMONJS : mode;
    }

    private ScriptMode classifyModule(Path path, String source, LinkKind linkKind) {
        ScriptMode mode = ScriptClassifier.classify(path, source);
        if (mode == ScriptMode.LEGACY && linkKind == LinkKind.IMPORT) {
            return ScriptMode.COMMONJS;
        }
        return mode;
    }

    private String rewriteEsm(Path modulePath, String source) {
        ModuleScanner scanner = new ModuleScanner(modulePath, source);
        scanner.validateUnsupportedSyntax();

        boolean[] codeMask = codeMask(source);
        Matcher matcher = STATIC_IMPORT_EXPORT_PATTERN.matcher(source);
        StringBuilder rewritten = new StringBuilder();
        while (matcher.find()) {
            if (!codeMask[matcher.start()]) {
                continue;
            }
            String specifier = matcher.group(3);
            String importAttributes = matcher.group(6);
            Path dependencyPath;
            if (isBuiltin(specifier)) {
                dependencyPath = builtinModulePath(specifier);
            } else {
                dependencyPath = resolve(modulePath, specifier, LinkKind.IMPORT);
            }
            Path resolved = dependencyPath;
            if (isJsonPath(resolved) && !isJsonImportAttribute(importAttributes)) {
                throw new ScriptLoadException("JSON module import in " + modulePath + " must use with { type: 'json' }: " + specifier);
            }
            if (!isJsonPath(resolved) && importAttributes != null) {
                throw new ScriptLoadException("Unsupported import attributes in " + modulePath + ": " + specifier);
            }
            ModuleRecord dependency = materialize(resolved, modulePath, LinkKind.IMPORT);
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(
                    matcher.group(1) + matcher.group(2) + generatedImportSpecifier(dependency.generatedPath()) + matcher.group(2)));
        }
        matcher.appendTail(rewritten);
        return importMetaPrelude(modulePath) + rewritten;
    }

    private String bridgeJson() {
        return "const __json = Java.type('scripting.ModuleScriptHandle').importJsonFromGenerated(import.meta.url);\n"
                + "export default __json;\n";
    }

    Path builtinModulePath(String specifier) {
        String builtinName = builtinName(specifier);
        Path existing = builtinModulePaths.get(builtinName);
        if (existing != null) {
            return existing;
        }
        Path builtinPath = generatedRoot.resolve("node_" + builtinName + "_builtin.cjs");
        try {
            Files.writeString(builtinPath, "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to write generated module " + builtinPath + ": " + e.getMessage(), e);
        }
        builtinModulePaths.put(builtinName, builtinPath);
        builtinNamesByPath.put(realPath(builtinPath, builtinPath), builtinName);
        return builtinPath;
    }

    private String importMetaPrelude(Path modulePath) {
        return "const __cosmicGeneratedUrl = import.meta.url;\n"
                + "Object.defineProperties(import.meta, {\n"
                + "  filename: { get: function() { return Java.type('scripting.ModuleScriptHandle').importMetaFilename(__cosmicGeneratedUrl); }, configurable: true },\n"
                + "  dirname: { get: function() { return Java.type('scripting.ModuleScriptHandle').importMetaDirname(__cosmicGeneratedUrl); }, configurable: true },\n"
                + "  url: { get: function() { return Java.type('scripting.ModuleScriptHandle').importMetaSourceUrl(__cosmicGeneratedUrl); }, configurable: true },\n"
                + "  resolve: { value: function(specifier) { return Java.type('scripting.ModuleScriptHandle').resolveImportMetaFromGenerated(__cosmicGeneratedUrl, String(specifier)); }, configurable: true }\n"
                + "});\n";
    }

    private String bridgeCommonJs(Path modulePath, String source) {
        String builtinName = builtinNamesByPath.get(modulePath);
        Set<String> namedExports = builtinName == null ? commonJsNamedExports(source) : builtinNamedExports(builtinName);
        StringBuilder wrapper = new StringBuilder();
        if (builtinName == null) {
            wrapper.append("const __moduleExports = Java.type('scripting.ModuleScriptHandle').requireCommonJsFromGenerated(import.meta.url);\n");
        } else {
            wrapper.append("const __moduleExports = process.getBuiltinModule(Java.type('scripting.ModuleScriptHandle').builtinNameFromGenerated(import.meta.url));\n");
        }
        wrapper.append("export default __moduleExports;\n");
        wrapper.append("export const __esModule = true;\n");
        wrapper.append("export const module_exports = __moduleExports;\n");
        for (String name : namedExports) {
            wrapper.append("export const ").append(name).append(" = __moduleExports[")
                    .append(jsString(name)).append("];\n");
        }
        return wrapper.toString();
    }

    private static String generatedImportSpecifier(Path generatedPath) {
        return "./" + generatedPath.getFileName();
    }

    private static Set<String> builtinNamedExports(String builtinName) {
        return switch (builtinName) {
            case "module" -> Set.of("createRequire");
            case "fs" -> Set.of(
                    "readFileSync", "writeFileSync", "appendFileSync", "existsSync", "accessSync",
                    "openSync", "closeSync", "fstatSync", "fsyncSync", "fdatasyncSync", "ftruncateSync",
                    "readSync", "writeSync", "statSync", "lstatSync", "readdirSync", "mkdirSync",
                    "rmSync", "rmdirSync", "unlinkSync", "renameSync", "copyFileSync", "cpSync",
                    "realpathSync", "readlinkSync", "symlinkSync", "truncateSync", "utimesSync", "constants");
            case "path" -> Set.of(
                    "DELIMITER", "SEPARATOR", "SEPARATOR_PATTERN", "basename", "common", "delimiter",
                    "dirname", "extname", "format", "fromFileUrl", "globToRegExp", "isAbsolute",
                    "isGlob", "join", "joinGlobs", "normalize", "normalizeGlob", "parse", "relative",
                    "resolve", "sep", "toFileUrl", "toNamespacedPath");
            default -> Set.of();
        };
    }

    Path resolve(Path importer, String specifier, LinkKind linkKind) {
        if (specifier.startsWith("node:") || specifier.contains("://")) {
            throw new ScriptLoadException("Unsupported Node module specifier in " + importer + ": " + specifier);
        }
        if (specifier.startsWith("./") || specifier.startsWith("../")) {
            return resolvePath(importer.getParent().resolve(specifier).normalize(), linkKind);
        }
        if (Path.of(specifier).isAbsolute()) {
            return resolvePath(Path.of(specifier).normalize(), linkKind);
        }
        return resolveBare(importer, specifier, linkKind);
    }

    private Path resolveBare(Path importer, String specifier, LinkKind linkKind) {
        PackageSpecifier packageSpecifier = PackageSpecifier.parse(specifier);
        Path current = importer.getParent();
        while (current != null) {
            Path packageRoot = current.resolve("node_modules").resolve(packageSpecifier.name());
            if (Files.isDirectory(packageRoot)) {
                return resolvePackage(packageRoot, packageSpecifier.subpath(), linkKind);
            }
            current = current.getParent();
        }
        throw new ScriptLoadException("Cannot find module " + specifier + " imported from " + importer);
    }

    private Path resolvePackage(Path packageRoot, String subpath, LinkKind linkKind) {
        Path packageJson = packageRoot.resolve("package.json");
        String packageSource = Files.isRegularFile(packageJson) ? read(packageJson) : "";
        if (subpath.isEmpty()) {
            String exports = packageExport(packageSource, ".", linkKind);
            if (exports != null) {
                return resolvePath(packageRoot.resolve(exports).normalize(), linkKind);
            }
            String main = packageField(packageSource, "main");
            if (main != null) {
                return resolvePath(packageRoot.resolve(main).normalize(), linkKind);
            }
            return resolvePath(packageRoot.resolve("index").normalize(), linkKind);
        }

        String exports = packageExport(packageSource, "./" + subpath, linkKind);
        if (exports != null) {
            return resolvePath(packageRoot.resolve(exports).normalize(), linkKind);
        }
        return resolvePath(packageRoot.resolve(subpath).normalize(), linkKind);
    }

    private Path resolvePath(Path path, LinkKind linkKind) {
        if (Files.isRegularFile(path)) {
            return realPath(path, path);
        }
        for (String extension : List.of(".js", ".json", ".mjs", ".cjs")) {
            Path candidate = Path.of(path + extension);
            if (Files.isRegularFile(candidate)) {
                return realPath(candidate, path);
            }
        }
        if (Files.isDirectory(path)) {
            String packageSource = Files.isRegularFile(path.resolve("package.json")) ? read(path.resolve("package.json")) : "";
            String exports = packageExport(packageSource, ".", linkKind);
            if (exports != null) {
                return resolvePath(path.resolve(exports).normalize(), linkKind);
            }
            String main = packageField(packageSource, "main");
            if (main != null) {
                return resolvePath(path.resolve(main).normalize(), linkKind);
            }
            for (String extension : List.of(".js", ".json", ".mjs", ".cjs")) {
                Path candidate = path.resolve("index" + extension);
                if (Files.isRegularFile(candidate)) {
                    return realPath(candidate, path);
                }
            }
        }
        throw new ScriptLoadException("Cannot resolve module path: " + path);
    }

    private static String packageExport(String source, String subpath, LinkKind linkKind) {
        if (source.isBlank()) {
            return null;
        }
        Matcher direct = Pattern.compile("\"exports\"\\s*:\\s*\"([^\"]+)\"").matcher(source);
        if (".".equals(subpath) && direct.find()) {
            return direct.group(1);
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(subpath) + "\"\\s*:\\s*(\"[^\"]+\"|\\{[^}]*})").matcher(source);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value.startsWith("\"")) {
                return stripQuotes(value);
            }
            String condition = linkKind == LinkKind.REQUIRE ? "require" : "import";
            String conditional = fieldFromObject(value, condition);
            return conditional != null ? conditional : fieldFromObject(value, "default");
        }
        return null;
    }

    private static boolean isJsonImportAttribute(String attributes) {
        return attributes != null && JSON_IMPORT_ATTRIBUTE_PATTERN.matcher(attributes).matches();
    }

    static boolean isJsonPath(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(".json");
    }

    static boolean isBuiltin(String specifier) {
        return "module".equals(specifier)
                || "node:module".equals(specifier)
                || "fs".equals(specifier)
                || "node:fs".equals(specifier)
                || "path".equals(specifier)
                || "node:path".equals(specifier);
    }

    static boolean isModuleBuiltin(String specifier) {
        return "module".equals(specifier) || "node:module".equals(specifier);
    }

    static String builtinName(String specifier) {
        return specifier.startsWith("node:") ? specifier.substring("node:".length()) : specifier;
    }

    private static String packageField(String source, String field) {
        if (source.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile(PACKAGE_FIELD_PATTERN.pattern().formatted(field)).matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String fieldFromObject(String objectSource, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"").matcher(objectSource);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Set<String> commonJsNamedExports(String source) {
        boolean[] codeMask = codeMask(source);
        Matcher matcher = CJS_NAMED_EXPORT_PATTERN.matcher(source);
        Set<String> names = new LinkedHashSet<>();
        while (matcher.find()) {
            if (!codeMask[matcher.start()]) {
                continue;
            }
            names.add(matcher.group(1));
        }
        Matcher objectExportMatcher = CJS_OBJECT_EXPORT_PATTERN.matcher(source);
        while (objectExportMatcher.find()) {
            if (!codeMask[objectExportMatcher.start()]) {
                continue;
            }
            collectObjectExportNames(source, objectExportMatcher.end(), names);
        }
        return names;
    }

    private static void collectObjectExportNames(String source, int objectStart, Set<String> names) {
        int depth = 1;
        int index = objectStart;
        while (index < source.length() && depth > 0) {
            char current = source.charAt(index);
            if (current == '\'' || current == '"' || current == '`') {
                index = skipStringLike(source, index + 1, current);
                continue;
            }
            if (current == '{') {
                depth++;
                index++;
                continue;
            }
            if (current == '}') {
                depth--;
                index++;
                continue;
            }
            if (depth == 1) {
                Matcher identifier = IDENTIFIER_PATTERN.matcher(source);
                identifier.region(index, source.length());
                if (identifier.lookingAt()) {
                    names.add(identifier.group());
                    index = identifier.end();
                    continue;
                }
            }
            index++;
        }
    }

    private static boolean[] codeMask(String source) {
        boolean[] code = new boolean[source.length()];
        java.util.Arrays.fill(code, true);
        for (int index = 0; index < source.length(); ) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (current == '/' && next == '/') {
                int end = index + 2;
                while (end < source.length() && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
                    end++;
                }
                markNonCode(code, index, end);
                index = end;
            } else if (current == '/' && next == '*') {
                int end = index + 2;
                while (end + 1 < source.length() && !(source.charAt(end) == '*' && source.charAt(end + 1) == '/')) {
                    end++;
                }
                end = Math.min(source.length(), end + 2);
                markNonCode(code, index, end);
                index = end;
            } else if (current == '\'' || current == '"' || current == '`') {
                int end = skipStringLike(source, index + 1, current);
                markNonCode(code, index + 1, Math.max(index + 1, end - 1));
                index = end;
            } else {
                index++;
            }
        }
        return code;
    }

    private static int skipStringLike(String source, int index, char quote) {
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

    private static void markNonCode(boolean[] code, int start, int end) {
        for (int index = start; index < end && index < code.length; index++) {
            code[index] = false;
        }
    }

    static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to read module " + path + ": " + e.getMessage(), e);
        }
    }

    private static Path realPath(Path path, Path importer) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new ScriptLoadException("Missing module imported from " + importer + ": " + path, e);
        }
    }

    private static String stripQuotes(String value) {
        Matcher matcher = STRING_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1) : value;
    }

    static String jsString(String value) {
        return "'" + escapeForSingleQuoted(value) + "'";
    }

    private static String escapeForSingleQuoted(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private record ModuleRecord(Path originalPath, Path generatedPath, ScriptMode mode, int lineOffset) {
    }

    record Result(Path entryPath, Map<Path, Path> sourceMap, Map<Path, Integer> lineOffsets, Map<Path, String> builtinNames, Path generatedRoot, NodeModuleGraph graph) {
    }

    private record PackageSpecifier(String name, String subpath) {
        private static PackageSpecifier parse(String specifier) {
            if (specifier.startsWith("@")) {
                String[] parts = specifier.split("/", 3);
                if (parts.length < 2) {
                    throw new ScriptLoadException("Invalid scoped package specifier: " + specifier);
                }
                return new PackageSpecifier(parts[0] + "/" + parts[1], parts.length == 3 ? parts[2] : "");
            }
            int slash = specifier.indexOf('/');
            return slash < 0
                    ? new PackageSpecifier(specifier, "")
                    : new PackageSpecifier(specifier.substring(0, slash), specifier.substring(slash + 1));
        }
    }

    enum LinkKind {
        IMPORT,
        REQUIRE
    }

    private static final class ModuleScanner {
        private final Path path;
        private final String source;

        private ModuleScanner(Path path, String source) {
            this.path = path;
            this.source = source;
        }

        private void validateUnsupportedSyntax() {
            int braceDepth = 0;
            int bracketDepth = 0;
            int parenDepth = 0;
            for (int index = 0; index < source.length(); ) {
                char current = source.charAt(index);
                char next = charAt(source, index + 1);
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
                    String token = source.substring(index, end);
                    if ("import".equals(token) && nextSignificantChar(end) == '(') {
                        throw unsupported("dynamic import()", index);
                    }
                    if ("await".equals(token) && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        throw unsupported("top-level await", index);
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
        }

        private char nextSignificantChar(int index) {
            int current = index;
            while (current < source.length() && Character.isWhitespace(source.charAt(current))) {
                current++;
            }
            return current < source.length() ? source.charAt(current) : '\0';
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

        private static char charAt(String source, int index) {
            return index < source.length() ? source.charAt(index) : '\0';
        }
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || character == '$' || Character.isLetter(character);
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || Character.isDigit(character);
    }
}
