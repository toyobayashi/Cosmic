package scripting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScriptClassifier {
    private static final Pattern PACKAGE_TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");

    private ScriptClassifier() {
    }

    public static ScriptMode classify(Path entryPath) {
        Objects.requireNonNull(entryPath);
        try {
            return classify(entryPath, Files.readString(entryPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to read script " + entryPath + ": " + e.getMessage(), e);
        }
    }

    public static ScriptMode classify(Path entryPath, String source) {
        Objects.requireNonNull(entryPath);
        Objects.requireNonNull(source);

        String filename = entryPath.getFileName().toString();
        if (filename.endsWith(".mjs")) {
            return ScriptMode.ESM;
        }
        if (filename.endsWith(".cjs")) {
            return ScriptMode.COMMONJS;
        }
        if (!filename.endsWith(".js")) {
            throw new IllegalArgumentException("Unsupported script extension: " + entryPath);
        }

        String packageType = nearestPackageType(entryPath);
        if ("module".equals(packageType)) {
            return ScriptMode.ESM;
        }
        if ("commonjs".equals(packageType)) {
            return ScriptMode.COMMONJS;
        }

        return containsTopLevelModuleDeclaration(source) ? ScriptMode.ESM : ScriptMode.LEGACY;
    }

    static String nearestPackageType(Path entryPath) {
        Path directory = Files.isDirectory(entryPath) ? entryPath : entryPath.getParent();
        while (directory != null) {
            Path packageJson = directory.resolve("package.json");
            if (Files.isRegularFile(packageJson)) {
                try {
                    Matcher matcher = PACKAGE_TYPE_PATTERN.matcher(Files.readString(packageJson, StandardCharsets.UTF_8));
                    return matcher.find() ? matcher.group(1) : "";
                } catch (IOException e) {
                    throw new ScriptLoadException("Failed to read package.json " + packageJson + ": " + e.getMessage(), e);
                }
            }
            directory = directory.getParent();
        }
        return null;
    }

    private static boolean containsTopLevelModuleDeclaration(String source) {
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;

        for (int index = 0; index < source.length(); ) {
            char current = source.charAt(index);
            char next = charAt(source, index + 1);

            if (current == '/' && next == '/') {
                index = skipLineComment(source, index + 2);
            } else if (current == '/' && next == '*') {
                index = skipBlockComment(source, index + 2);
            } else if (current == '\'' || current == '"') {
                index = skipQuotedString(source, index + 1, current);
            } else if (current == '`') {
                index = skipTemplateLiteral(source, index + 1);
            } else if (isIdentifierStart(current)) {
                int end = skipIdentifier(source, index + 1);
                if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                    String token = source.substring(index, end);
                    if ("export".equals(token)) {
                        return true;
                    }
                    if ("import".equals(token) && isStaticImportDeclaration(source, end)) {
                        return true;
                    }
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

        return false;
    }

    private static boolean isStaticImportDeclaration(String source, int index) {
        int nextIndex = skipWhitespaceAndComments(source, index);
        return nextIndex < source.length() && source.charAt(nextIndex) != '(';
    }

    private static int skipWhitespaceAndComments(String source, int index) {
        int current = index;
        while (current < source.length()) {
            char character = source.charAt(current);
            char next = charAt(source, current + 1);
            if (Character.isWhitespace(character)) {
                current++;
            } else if (character == '/' && next == '/') {
                current = skipLineComment(source, current + 2);
            } else if (character == '/' && next == '*') {
                current = skipBlockComment(source, current + 2);
            } else {
                return current;
            }
        }
        return current;
    }

    private static int skipLineComment(String source, int index) {
        int current = index;
        while (current < source.length() && source.charAt(current) != '\n' && source.charAt(current) != '\r') {
            current++;
        }
        return current;
    }

    private static int skipBlockComment(String source, int index) {
        int current = index;
        while (current + 1 < source.length()) {
            if (source.charAt(current) == '*' && source.charAt(current + 1) == '/') {
                return current + 2;
            }
            current++;
        }
        return source.length();
    }

    private static int skipQuotedString(String source, int index, char quote) {
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

    private static int skipTemplateLiteral(String source, int index) {
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

    private static int skipIdentifier(String source, int index) {
        int current = index;
        while (current < source.length() && isIdentifierPart(source.charAt(current))) {
            current++;
        }
        return current;
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || character == '$' || Character.isLetter(character);
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || Character.isDigit(character);
    }

    private static char charAt(String source, int index) {
        return index < source.length() ? source.charAt(index) : '\0';
    }
}
