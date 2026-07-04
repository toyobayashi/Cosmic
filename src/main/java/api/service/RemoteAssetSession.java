package api.service;

public record RemoteAssetSession(
        String assetBaseUrl,
        String session,
        long expiresAtMillis,
        int accountId,
        long gameSessionId,
        String remoteAddress,
        int characterId
) {
}
