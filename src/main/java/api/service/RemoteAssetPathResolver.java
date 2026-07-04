package api.service;

import java.nio.file.Path;

public class RemoteAssetPathResolver {
    private final Path root;

    public RemoteAssetPathResolver(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("remote asset path is empty");
        }
        if (relativePath.startsWith("/") || relativePath.startsWith("\\") || relativePath.contains("\\") || relativePath.contains(":")) {
            throw new IllegalArgumentException("remote asset path must be a relative URL path");
        }

        String[] parts = relativePath.split("/");
        for (String part : parts) {
            if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("remote asset path contains an invalid segment");
            }
        }

        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("remote asset path escapes root");
        }
        return resolved;
    }
}
