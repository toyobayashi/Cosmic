package provider.wz;

import io.github.toyobayashi.libwz.WzDirectory;
import io.github.toyobayashi.libwz.WzEnums;
import io.github.toyobayashi.libwz.WzEnums.MapleVersion;
import io.github.toyobayashi.libwz.WzFile;
import io.github.toyobayashi.libwz.WzImage;
import io.github.toyobayashi.libwz.WzImageProperty;
import io.github.toyobayashi.libwz.WzObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DataProvider} implementation that reads MapleStory data directly
 * from binary {@code .wz} files using libwz.  Replaces the XML-based
 * {@link XMLWZFile} when the {@code wz-mode} system property is set to
 * {@code "binary"}.
 */
public class BinaryWZFile implements DataProvider {

    private static final Logger log = LoggerFactory.getLogger(BinaryWZFile.class);
    private static final MapleVersion MAPLE_VERSION = MapleVersion.GMS;

    /** Reuse already-parsed WzFile instances across providers. */
    private static final Map<String, WzFile> wzFileCache = new HashMap<>();

    private final WzFile wzFile;
    private final WZDirectoryEntry rootForNavigation;

    /**
     * Opens and parses the {@code .wz} file at {@code wzFilePath}.
     * Uses a static cache so the same physical file is parsed at most once.
     */
    public BinaryWZFile(String wzFilePath) {
        Path wzPath = Path.of(wzFilePath);
        if (!Files.isRegularFile(wzPath)) {
            throw new IllegalArgumentException(
                    "WZ file does not exist or is not a regular file: " + wzPath
            );
        }

        synchronized (wzFileCache) {
            wzFile = wzFileCache.computeIfAbsent(wzFilePath, path -> {
                log.info("Parsing WZ file: {}", path);
                WzFile f = new WzFile(path, (short) -1, MAPLE_VERSION);
                WzEnums.ParseStatus status = f.parseWzFile();
                if (status != WzEnums.ParseStatus.SUCCESS) {
                    log.error("Failed to parse WZ file: {} (status: {})", path, status);
                    return null;
                }
                log.info("Parsed WZ file: {} (v{})", path, f.getVersion());
                return f;
            });
        }

        rootForNavigation = new WZDirectoryEntry();
        if (wzFile != null) {
            WzDirectory rootDir = wzFile.getWzDirectory();
            if (rootDir != null) {
                fillNavigationTree(rootDir, rootForNavigation);
            }
        }
    }

    /** Recursively build the navigation tree so {@link #getRoot()} works. */
    private void fillNavigationTree(WzDirectory wzDir, WZDirectoryEntry dirEntry) {
        for (WzDirectory subDir : wzDir.wzDirectories()) {
            if (subDir == null) continue;
            WZDirectoryEntry subEntry = new WZDirectoryEntry(
                    subDir.getName(), subDir.getBlockSize(),
                    subDir.getChecksum(), dirEntry);
            dirEntry.addDirectory(subEntry);
            fillNavigationTree(subDir, subEntry);
        }

        for (WzImage img : wzDir.wzImages()) {
            if (img == null) continue;
            dirEntry.addFile(new WZFileEntry(
                    img.getName(), img.getBlockSize(),
                    img.getChecksum(), dirEntry));
        }
    }

    // ---- DataProvider --------------------------------------------------

    @Override
    public synchronized Data getData(String path) {
        if (wzFile == null) return null;

        WzObject obj = wzFile.getObjectFromPath(path, false);
        if (obj == null) return null;

        if (obj instanceof WzImage img) {
            img.parseImage();
            return new BinaryWZMapleData(img);
        }

        if (obj instanceof WzImageProperty prop) {
            return new BinaryWZMapleData(prop);
        }

        return null;
    }

    @Override
    public DataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
