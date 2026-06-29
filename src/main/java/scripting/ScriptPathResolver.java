package scripting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ScriptPathResolver {
    private static final String[] SCRIPT_EXTENSIONS = {".mjs", ".cjs", ".js"};

    private final Path scriptsRoot;

    public ScriptPathResolver(Path scriptsRoot) {
        this.scriptsRoot = Objects.requireNonNull(scriptsRoot);
    }

    public Path resolveEntry(String directory, String identifier) {
        Objects.requireNonNull(directory);
        Objects.requireNonNull(identifier);

        Path scriptDirectory = scriptsRoot.resolve(directory);
        if (hasExplicitScriptExtension(identifier)) {
            return scriptDirectory.resolve(identifier).normalize();
        }

        for (String extension : SCRIPT_EXTENSIONS) {
            Path candidate = scriptDirectory.resolve(identifier + extension).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return scriptDirectory.resolve(identifier + ".js").normalize();
    }

    private static boolean hasExplicitScriptExtension(String identifier) {
        return identifier.endsWith(".js") || identifier.endsWith(".mjs") || identifier.endsWith(".cjs");
    }
}
