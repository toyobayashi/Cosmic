package api.controller;

import api.service.RemoteAssetImageValidator;
import api.service.RemoteAssetPathResolver;
import api.service.RemoteAssetSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class RemoteAssetController {
    private static final String PREFIX = "/remote-assets/";
    private static final int MAX_IMAGE_WIDTH = 250;
    private static final int MAX_IMAGE_HEIGHT = 250;
    private static final int MAX_IMAGE_BYTES = 128 * 1024;

    private final RemoteAssetPathResolver pathResolver;
    private final RemoteAssetSessionService sessionService;

    @Autowired
    public RemoteAssetController(@Value("${remoteAssets.root:${REMOTE_ASSETS_ROOT:private-assets}}") String root) {
        this(RemoteAssetSessionService.getInstance(), Path.of(root));
    }

    RemoteAssetController(RemoteAssetSessionService sessionService, Path root) {
        this.sessionService = sessionService;
        this.pathResolver = new RemoteAssetPathResolver(root);
    }

    @GetMapping("/remote-assets/**")
    public void getRemoteAsset(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String session = request.getHeader(RemoteAssetSessionService.HEADER_NAME);
        if (sessionService.validateHttpSession(session, request.getRemoteAddr()).isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Path path;
        try {
            path = pathResolver.resolve(extractRelativePath(request));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!Files.isRegularFile(path) || !path.getFileName().toString().toLowerCase().endsWith(".png")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (Files.size(path) > MAX_IMAGE_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        byte[] data = Files.readAllBytes(path);
        try {
            RemoteAssetImageValidator.validatePng(data, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT, MAX_IMAGE_BYTES);
        } catch (IllegalArgumentException e) {
            response.sendError(422);
            return;
        }

        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getOutputStream().write(data);
    }

    private static String extractRelativePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        int index = path.indexOf(PREFIX);
        if (index < 0) {
            throw new IllegalArgumentException("missing remote asset prefix");
        }
        String relativePath = path.substring(index + PREFIX.length());
        return URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
    }
}
