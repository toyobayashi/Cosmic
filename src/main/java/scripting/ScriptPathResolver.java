package scripting;

import java.nio.file.Path;
import java.util.Objects;

public class ScriptPathResolver {
    private final Path scriptsRoot;

    public ScriptPathResolver(Path scriptsRoot) {
        this.scriptsRoot = Objects.requireNonNull(scriptsRoot);
    }

    public Path resolveEntry(String directory, String identifier) {
        Objects.requireNonNull(directory);
        Objects.requireNonNull(identifier);

        String filename = hasExplicitScriptExtension(identifier) ? identifier : identifier + ".js";
        return scriptsRoot.resolve(directory).resolve(filename).normalize();
    }

    private static boolean hasExplicitScriptExtension(String identifier) {
        return identifier.endsWith(".js") || identifier.endsWith(".mjs");
    }
}
