package api.service;

import api.config.JwtUtil;
import org.jdbi.v3.core.Handle;
import tools.BCrypt;
import tools.DatabaseConnection;

import java.util.Map;

public class AuthService {
    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String login(String username, String password) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Map<String, Object> row = handle.createQuery(
                    "SELECT id, name, password, banned, webadmin FROM accounts WHERE name = ?")
                    .bind(0, username)
                    .mapToMap()
                    .findOne()
                    .orElse(null);

            if (row == null) {
                throw new IllegalArgumentException("Account or password is incorrect");
            }

            boolean banned = ((Number) row.get("banned")).intValue() != 0;
            if (banned) {
                throw new IllegalArgumentException("Account is banned");
            }

            String storedHash = (String) row.get("password");
            if (!BCrypt.checkpw(password, storedHash)) {
                throw new IllegalArgumentException("Account or password is incorrect");
            }

            int accountId = ((Number) row.get("id")).intValue();
            int webadmin = ((Number) row.get("webadmin")).intValue();

            if (webadmin <= 0) {
                throw new IllegalArgumentException("Insufficient permissions");
            }

            return jwtUtil.generateToken(accountId, username, webadmin);
        }
    }
}
