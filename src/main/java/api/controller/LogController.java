package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/log")
public class LogController {

    private static final Path LOG_DIR = Path.of("logs");

    @Tag(name = "/log/" + ApiConstant.LATEST)
    @Operation(summary = "List log files")
    @GetMapping("/" + ApiConstant.LATEST + "/files")
    public ResultBody<List<Map<String, Object>>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        if (!Files.isDirectory(LOG_DIR)) {
            return ResultBody.success(files);
        }
        try (var stream = Files.newDirectoryStream(LOG_DIR, "*.log")) {
            for (Path p : stream) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", p.getFileName().toString());
                info.put("size", Files.size(p));
                info.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                files.add(info);
            }
        } catch (IOException e) {
            return ResultBody.error(500, "Failed to list log files: " + e.getMessage());
        }
        files.sort(Comparator.comparing(m -> (String) m.get("name")));
        return ResultBody.success(files);
    }

    @Tag(name = "/log/" + ApiConstant.LATEST)
    @Operation(summary = "Read log content")
    @GetMapping("/" + ApiConstant.LATEST + "/content")
    public ResultBody<Map<String, Object>> getContent(
            @RequestParam String file,
            @RequestParam(defaultValue = "500") int tail) {

        Path baseDir = LOG_DIR.toAbsolutePath().normalize();
        Path logFile = baseDir.resolve(file).normalize();
        if (!logFile.startsWith(baseDir)) {
            return ResultBody.error(400, "Invalid file path");
        }
        if (!Files.isRegularFile(logFile)) {
            return ResultBody.error(404, "Log file not found: " + file);
        }

        try {
            List<String> lines = readLastLines(logFile, Math.min(tail, 10000));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file", file);
            result.put("size", Files.size(logFile));
            result.put("lastModified", Files.getLastModifiedTime(logFile).toMillis());
            result.put("lines", lines);
            return ResultBody.success(result);
        } catch (IOException e) {
            return ResultBody.error(500, "Failed to read log file: " + e.getMessage());
        }
    }

    private List<String> readLastLines(Path file, int lineCount) throws IOException {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long pos = raf.length();
            if (pos == 0) {
                return lines;
            }
            int linesFound = 0;
            StringBuilder sb = new StringBuilder();

            while (--pos >= 0 && linesFound < lineCount) {
                raf.seek(pos);
                char c = (char) raf.readByte();
                if (c == '\n') {
                    if (pos + 1 < raf.length()) {
                        lines.add(sb.reverse().toString());
                        sb.setLength(0);
                        linesFound++;
                    }
                } else if (c != '\r') {
                    sb.append(c);
                }
            }
            if (sb.length() > 0 && linesFound < lineCount) {
                lines.add(sb.reverse().toString());
            }
        }
        Collections.reverse(lines);
        return lines;
    }
}
