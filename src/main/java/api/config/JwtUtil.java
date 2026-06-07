package api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    private final SecretKey key;
    private final long duration;

    public JwtUtil(String secret, long duration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.duration = duration;
    }

    public String generateToken(int accountId, String username, int webadmin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + duration);
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("username", username)
                .claim("webadmin", webadmin)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getAccountIdFromToken(String token) {
        return Integer.parseInt(parseToken(token).getSubject());
    }
}
