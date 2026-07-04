package api.service;

import client.Client;
import config.YamlConfig;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteAssetSessionService {
    public static final String HEADER_NAME = "X-Resource-Session";
    private static final long DEFAULT_TTL_SECONDS = 900;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final RemoteAssetSessionService INSTANCE = new RemoteAssetSessionService(defaultAssetBaseUrl(), DEFAULT_TTL_SECONDS);

    private final String assetBaseUrl;
    private final long ttlMillis;
    private final Clock clock;
    private final Map<String, RemoteAssetSession> sessions = new ConcurrentHashMap<>();

    public RemoteAssetSessionService(String assetBaseUrl, long ttlSeconds) {
        this(assetBaseUrl, ttlSeconds, Clock.systemUTC());
    }

    RemoteAssetSessionService(String assetBaseUrl, long ttlSeconds, Clock clock) {
        this.assetBaseUrl = normalizeAssetBaseUrl(assetBaseUrl);
        this.ttlMillis = Math.max(0, ttlSeconds) * 1000L;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static RemoteAssetSessionService getInstance() {
        return INSTANCE;
    }

    public RemoteAssetSession issueSession(Client client, int characterId) {
        long expiresAtMillis = clock.millis() + ttlMillis;
        RemoteAssetSession session = new RemoteAssetSession(
                assetBaseUrl,
                randomToken(),
                expiresAtMillis,
                client.getAccID(),
                client.getSessionId(),
                normalizeRemoteAddress(client.getRemoteAddress()),
                characterId
        );
        sessions.put(session.session(), session);
        return session;
    }

    public boolean isValid(String session, String remoteAddress, int accountId, long gameSessionId) {
        return validate(session)
                .filter(value -> value.accountId() == accountId)
                .filter(value -> value.gameSessionId() == gameSessionId)
                .filter(value -> value.remoteAddress().equals(normalizeRemoteAddress(remoteAddress)))
                .isPresent();
    }

    public Optional<RemoteAssetSession> validateHttpSession(String session, String remoteAddress) {
        return validate(session)
                .filter(value -> value.remoteAddress().equals(normalizeRemoteAddress(remoteAddress)));
    }

    private Optional<RemoteAssetSession> validate(String session) {
        if (session == null || session.isBlank()) {
            return Optional.empty();
        }

        RemoteAssetSession value = sessions.get(session);
        if (value == null) {
            return Optional.empty();
        }
        if (value.expiresAtMillis() <= clock.millis()) {
            sessions.remove(session, value);
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeRemoteAddress(String remoteAddress) {
        return remoteAddress == null ? "" : remoteAddress.trim();
    }

    private static String normalizeAssetBaseUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String defaultAssetBaseUrl() {
        String override = System.getProperty("remoteAssets.baseUrl");
        if (override != null && !override.isBlank()) {
            return override;
        }
        override = System.getenv("REMOTE_ASSETS_BASE_URL");
        if (override != null && !override.isBlank()) {
            return override;
        }

        String host = "127.0.0.1";
        int port = 8686;
        try {
            if (YamlConfig.config != null && YamlConfig.config.server != null) {
                if (YamlConfig.config.server.HOST != null && !YamlConfig.config.server.HOST.isBlank()) {
                    host = YamlConfig.config.server.HOST;
                }
                if (YamlConfig.config.server.API_PORT > 0) {
                    port = YamlConfig.config.server.API_PORT;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return "http://" + host + ":" + port + "/remote-assets";
    }
}
