package database;

import java.util.Locale;

public enum DatabaseType {
    MYSQL("mysql"),
    SQLITE("sqlite");

    private final String configValue;

    DatabaseType(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isSqlite() {
        return this == SQLITE;
    }

    public static DatabaseType fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return MYSQL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DatabaseType type : values()) {
            if (type.configValue.equals(normalized)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported DB_TYPE '" + value + "'. Expected mysql or sqlite.");
    }
}
