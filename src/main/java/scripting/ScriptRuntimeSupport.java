package scripting;

import org.graalvm.polyglot.Context;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScriptRuntimeSupport {
    private static final String BUILTINS_RESOURCE_ROOT = "/scripting/builtins/";
    private static final String BOOTSTRAP_BINDING = "__cosmicBootstrap";
    private static final String BUILTIN_BOOTSTRAP_SOURCE = """
            (function() {
            const __cosmicBootstrap = globalThis.__cosmicBootstrap;
            delete globalThis.__cosmicBootstrap;
            const __cosmicBuiltinIds = Object.freeze({ fs: true, path: true, module: true, process: true });
            const __cosmicBuiltinCache = Object.create(null);
            function __cosmicBuiltinName(specifier) {
              const name = String(specifier);
              return name.startsWith('node:') ? name.slice(5) : name;
            }
            function internalBinding(name) {
              if (name === 'fs') return Object.freeze({
                Support: Java.type('scripting.ScriptRuntimeSupport'),
                readFileSync: function(path) { return __cosmicBootstrap.readResource(String(path)); }
              });
              if (name === 'process') return Object.freeze({ Process: __cosmicBootstrap });
              if (name === 'module') return Object.freeze({ createRequire: function(filename) {
                return __cosmicBootstrap.hasModuleRuntime() ? __cosmicBootstrap.createRequire(String(filename)) : createLegacyRequire();
              } });
              throw new Error('Unsupported internal binding: ' + name);
            }
            function require(specifier) {
              const name = __cosmicBuiltinName(specifier);
              if (!Object.prototype.hasOwnProperty.call(__cosmicBuiltinIds, name)) throw new Error('Unsupported builtin module: ' + specifier);
              if (__cosmicBuiltinCache[name]) return __cosmicBuiltinCache[name].exports;
              const module = { exports: {} };
              __cosmicBuiltinCache[name] = module;
              const filename = __cosmicBootstrap.builtinResourceRoot() + name + '.cjs';
              const source = internalBinding('fs').readFileSync(filename, 'utf8');
              const factory = eval('(function(module, exports, require, internalBinding) {\\n' + source + '\\n})');
              factory(module, module.exports, require, internalBinding);
              return module.exports;
            }
            function createLegacyRequire() {
              const legacyRequire = function(specifier) { return require(String(specifier)); };
              legacyRequire.cache = Object.create(null);
              legacyRequire.resolve = function(specifier) {
                const name = __cosmicBuiltinName(specifier);
                if (!Object.prototype.hasOwnProperty.call(__cosmicBuiltinIds, name)) throw new Error('Cannot find module: ' + specifier);
                return String(specifier);
              };
              return legacyRequire;
            }
            globalThis.process = require('process');
            if (__cosmicBootstrap.exposeLegacyCommonJsGlobals()) {
              globalThis.require = createLegacyRequire();
              globalThis.module = { exports: {}, id: '<repl>', filename: null, loaded: false, require: createLegacyRequire() };
            }
            })();
            """;
    private static final AtomicInteger nextFd = new AtomicInteger(100);
    private static final Map<Integer, OpenFile> openFiles = new ConcurrentHashMap<>();

    private ScriptRuntimeSupport() {
    }

    static void installGlobals(Context context, ModuleScriptHandle.CommonJsRuntime moduleRuntime) {
        installGlobals(context, moduleRuntime, null);
    }

    static void installGlobals(Context context, ModuleScriptHandle.CommonJsRuntime moduleRuntime, Path entryPath) {
        var bindings = context.getBindings("js");
        bindings.putMember(BOOTSTRAP_BINDING, new BuiltinBootstrap(moduleRuntime, entryPath, false));
        try {
            context.eval("js", builtinBootstrapSource());
        } finally {
            bindings.removeMember(BOOTSTRAP_BINDING);
        }
    }

    static void installGlobals(ScriptEngine engine) throws ScriptException {
        installGlobals(engine, null);
    }

    static void installGlobals(ScriptEngine engine, Path entryPath) throws ScriptException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(BOOTSTRAP_BINDING, new BuiltinBootstrap(null, entryPath, true));
        try {
            engine.eval(builtinBootstrapSource());
        } finally {
            bindings.remove(BOOTSTRAP_BINDING);
        }
    }

    public static String readResource(String resourcePath) {
        try (InputStream stream = ScriptRuntimeSupport.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new ScriptLoadException("Missing script runtime resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScriptLoadException("Failed to read script runtime resource " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    public static byte[] readFileBytes(String path) {
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String readFileString(String path, String encoding) {
        try {
            return Files.readString(Path.of(path), charset(encoding));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeFileString(String path, String data, String encoding, boolean append) {
        try {
            if (append) {
                Files.writeString(Path.of(path), data, charset(encoding), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(Path.of(path), data, charset(encoding), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int open(String path, String flags) {
        try {
            int fd = nextFd.getAndIncrement();
            openFiles.put(fd, new OpenFile(Path.of(path), FileChannel.open(Path.of(path), openOptions(flags))));
            return fd;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void close(int fd) {
        OpenFile openFile = openFiles.remove(fd);
        if (openFile == null) {
            throw new ScriptLoadException("EBADF: bad file descriptor, close '" + fd + "'");
        }
        try {
            openFile.channel().close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FileStat fstat(int fd) {
        OpenFile openFile = openFile(fd);
        try {
            return new FileStat(Files.readAttributes(openFile.path(), BasicFileAttributes.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void fsync(int fd, boolean metadata) {
        try {
            openFile(fd).channel().force(metadata);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void ftruncate(int fd, long length) {
        try {
            openFile(fd).channel().truncate(length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int readFd(int fd, byte[] buffer, int offset, int length, long position) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
            int count = position < 0 ? openFile(fd).channel().read(byteBuffer) : openFile(fd).channel().read(byteBuffer, position);
            return Math.max(count, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int writeFdBytes(int fd, byte[] buffer, int offset, int length, long position) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
            return position < 0 ? openFile(fd).channel().write(byteBuffer) : openFile(fd).channel().write(byteBuffer, position);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int writeFdString(int fd, String data, String encoding, long position) {
        byte[] bytes = data.getBytes(charset(encoding));
        return writeFdBytes(fd, bytes, 0, bytes.length, position);
    }

    public static byte[] byteArray(int length) {
        return new byte[length];
    }

    public static boolean exists(String path) {
        return Files.exists(Path.of(path));
    }

    public static void access(String path) {
        if (!Files.exists(Path.of(path))) {
            throw new ScriptLoadException("ENOENT: no such file or directory, access '" + path + "'");
        }
    }

    public static FileStat stat(String path, boolean followLinks) {
        try {
            LinkOption[] linkOptions = followLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
            return new FileStat(Files.readAttributes(Path.of(path), BasicFileAttributes.class, linkOptions));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String[] readdir(String path) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(path))) {
            return java.util.stream.StreamSupport.stream(stream.spliterator(), false)
                    .map(entry -> entry.getFileName().toString())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void mkdir(String path, boolean recursive) {
        try {
            if (recursive) {
                Files.createDirectories(Path.of(path));
            } else {
                Files.createDirectory(Path.of(path));
            }
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void remove(String path, boolean recursive) {
        Path target = Path.of(path);
        try {
            if (!recursive || !Files.isDirectory(target)) {
                Files.deleteIfExists(target);
                return;
            }
            Files.walkFileTree(target, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void rename(String oldPath, String newPath) {
        try {
            Files.move(Path.of(oldPath), Path.of(newPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyFile(String source, String target) {
        try {
            Files.copy(Path.of(source), Path.of(target), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void cp(String source, String target, boolean recursive) {
        Path sourcePath = Path.of(source);
        Path targetPath = Path.of(target);
        try {
            if (!Files.isDirectory(sourcePath) || !recursive) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String realpath(String path) {
        try {
            return Path.of(path).toRealPath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String readlink(String path) {
        try {
            return Files.readSymbolicLink(Path.of(path)).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void symlink(String target, String path) {
        try {
            Files.createSymbolicLink(Path.of(path), Path.of(target));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void truncate(String path, long length) {
        try (var channel = java.nio.channels.FileChannel.open(Path.of(path), StandardOpenOption.WRITE)) {
            channel.truncate(length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void utimes(String path, double atimeSeconds, double mtimeSeconds) {
        try {
            Files.setLastModifiedTime(Path.of(path), FileTime.fromMillis((long) (mtimeSeconds * 1000)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Charset charset(String encoding) {
        if (encoding == null || encoding.isBlank() || "utf8".equalsIgnoreCase(encoding) || "utf-8".equalsIgnoreCase(encoding)) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }

    private static Set<OpenOption> openOptions(String flags) {
        String value = flags == null || flags.isBlank() ? "r" : flags;
        boolean exclusive = value.contains("x");
        value = value.replace("x", "");
        Set<OpenOption> options = new HashSet<>();
        switch (value) {
            case "r" -> options.add(StandardOpenOption.READ);
            case "r+" -> {
                options.add(StandardOpenOption.READ);
                options.add(StandardOpenOption.WRITE);
            }
            case "w" -> {
                options.add(StandardOpenOption.WRITE);
                options.add(exclusive ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE);
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            }
            case "w+" -> {
                options.add(StandardOpenOption.READ);
                options.add(StandardOpenOption.WRITE);
                options.add(exclusive ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE);
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            }
            case "a" -> {
                options.add(StandardOpenOption.WRITE);
                options.add(exclusive ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE);
                options.add(StandardOpenOption.APPEND);
            }
            case "a+" -> {
                options.add(StandardOpenOption.READ);
                options.add(StandardOpenOption.WRITE);
                options.add(exclusive ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE);
                options.add(StandardOpenOption.APPEND);
            }
            default -> throw new ScriptLoadException("Unsupported fs.openSync flags: " + flags);
        }
        return options;
    }

    private static OpenFile openFile(int fd) {
        OpenFile openFile = openFiles.get(fd);
        if (openFile == null) {
            throw new ScriptLoadException("EBADF: bad file descriptor '" + fd + "'");
        }
        return openFile;
    }

    static String builtinBootstrapSource() {
        return BUILTIN_BOOTSTRAP_SOURCE;
    }

    private static String nodeArch() {
        return nodeArch(System.getProperty("os.arch", ""));
    }

    static String nodeArch(String osArch) {
        String arch = osArch.toLowerCase(Locale.ROOT);
        if (arch.equals("x86_64") || arch.equals("amd64")) return "x64";
        if (arch.equals("x86") || arch.equals("i386") || arch.equals("i486") || arch.equals("i586") || arch.equals("i686")) return "ia32";
        if (arch.equals("aarch64") || arch.equals("arm64")) return "arm64";
        if (arch.equals("arm") || arch.startsWith("armv")) return "arm";
        if (arch.equals("loongarch64") || arch.equals("loong64")) return "loong64";
        if (arch.equals("mips")) return "mips";
        if (arch.equals("mipsel")) return "mipsel";
        if (arch.equals("ppc64") || arch.equals("ppc64le") || arch.equals("powerpc64") || arch.equals("powerpc64le")) return "ppc64";
        if (arch.equals("riscv64")) return "riscv64";
        if (arch.equals("s390x")) return "s390x";
        throw new ScriptLoadException("Unsupported Node.js process.arch mapping for os.arch: " + osArch);
    }

    private static String nodePlatform() {
        return nodePlatform(System.getProperty("os.name", ""), System.getProperty("java.runtime.name", ""));
    }

    static String nodePlatform(String osName, String runtimeName) {
        String os = osName.toLowerCase(Locale.ROOT);
        String runtime = runtimeName.toLowerCase(Locale.ROOT);
        if (os.contains("android") || runtime.contains("android")) return "android";
        if (os.contains("win")) return "win32";
        if (os.contains("mac") || os.contains("darwin")) return "darwin";
        if (os.contains("aix")) return "aix";
        if (os.contains("freebsd")) return "freebsd";
        if (os.contains("openbsd")) return "openbsd";
        if (os.contains("sunos") || os.contains("solaris")) return "sunos";
        if (os.contains("linux")) return "linux";
        throw new ScriptLoadException("Unsupported Node.js process.platform mapping for os.name: " + osName);
    }

    static String jsString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t") + "'";
    }

    public record FileStat(BasicFileAttributes attributes) {
        public boolean isFile() {
            return attributes.isRegularFile();
        }

        public boolean isDirectory() {
            return attributes.isDirectory();
        }

        public boolean isSymbolicLink() {
            return attributes.isSymbolicLink();
        }

        public long size() {
            return attributes.size();
        }

        public long mtimeMs() {
            return attributes.lastModifiedTime().toMillis();
        }

        public long ctimeMs() {
            return attributes.creationTime().toMillis();
        }

        public long atimeMs() {
            return attributes.lastAccessTime().toMillis();
        }
    }

    private record OpenFile(Path path, FileChannel channel) {
    }

    public static final class BuiltinBootstrap {
        private final ModuleScriptHandle.CommonJsRuntime moduleRuntime;
        private final Path entryPath;
        private final boolean exposeLegacyCommonJsGlobals;

        private BuiltinBootstrap(ModuleScriptHandle.CommonJsRuntime moduleRuntime, Path entryPath, boolean exposeLegacyCommonJsGlobals) {
            this.moduleRuntime = moduleRuntime;
            this.entryPath = entryPath == null ? null : entryPath.toAbsolutePath().normalize();
            this.exposeLegacyCommonJsGlobals = exposeLegacyCommonJsGlobals;
        }

        public boolean hasModuleRuntime() {
            return moduleRuntime != null;
        }

        public boolean exposeLegacyCommonJsGlobals() {
            return exposeLegacyCommonJsGlobals;
        }

        public Object createRequire(String filename) {
            if (moduleRuntime == null) {
                throw new ScriptLoadException("No CommonJS runtime is available for createRequire");
            }
            return moduleRuntime.createRequire(filename);
        }

        public String builtinResourceRoot() {
            return BUILTINS_RESOURCE_ROOT;
        }

        public String readResource(String resourcePath) {
            return ScriptRuntimeSupport.readResource(resourcePath);
        }

        public String version() {
            return "v" + System.getProperty("java.version");
        }

        public String nodeVersion() {
            return System.getProperty("java.version");
        }

        public String arch() {
            return nodeArch();
        }

        public String platform() {
            return nodePlatform();
        }

        public String cwd() {
            return System.getProperty("user.dir");
        }

        public String env(String key) {
            return System.getenv(key);
        }

        public String[] envKeys() {
            return System.getenv().keySet().toArray(String[]::new);
        }

        public String[] argv() {
            return new String[] { javaExecutablePath(), scriptArgvPath() };
        }

        private String scriptArgvPath() {
            return entryPath == null ? System.getProperty("user.dir") : entryPath.toString();
        }

        private String javaExecutablePath() {
            String executable = "win32".equals(nodePlatform()) ? "java.exe" : "java";
            return Path.of(System.getProperty("java.home"), "bin", executable).toString();
        }
    }
}
