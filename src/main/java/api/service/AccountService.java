package api.service;

import api.model.dto.AccountInfoDTO;
import api.model.dto.AddAccountDTO;
import api.model.dto.UpdateAccountByUserDTO;
import api.model.dto.UpdateAccountByGmDTO;
import client.DefaultDates;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import tools.BCrypt;
import tools.DatabaseConnection;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountService {

    public AccountInfoDTO getAccountById(int accountId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Map<String, Object> row = handle.createQuery(
                    "SELECT id, name, email, gender, loggedin, lastlogin, createdat, " +
                    "birthday, banned, banreason, characterslots, webadmin, nick, mute, " +
                    "nxCredit, maplePoint, nxPrepaid, rewardpoints, votepoints, language " +
                    "FROM accounts WHERE id = ?")
                    .bind(0, accountId)
                    .mapToMap()
                    .findOne()
                    .orElse(null);
            if (row == null) return null;
            return mapToAccountInfo(row);
        }
    }

    public AccountInfoDTO getAccountByName(String name) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Map<String, Object> row = handle.createQuery(
                    "SELECT id, name, email, gender, loggedin, lastlogin, createdat, " +
                    "birthday, banned, banreason, characterslots, webadmin, nick, mute, " +
                    "nxCredit, maplePoint, nxPrepaid, rewardpoints, votepoints, language " +
                    "FROM accounts WHERE name = ?")
                    .bind(0, name)
                    .mapToMap()
                    .findOne()
                    .orElse(null);
            if (row == null) return null;
            return mapToAccountInfo(row);
        }
    }

    public List<AccountInfoDTO> getAccountList(Integer page, Integer size, Integer id,
                                                String name, String lastLoginStart,
                                                String lastLoginEnd, String createdAtStart,
                                                String createdAtEnd) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT id, name, email, gender, loggedin, lastlogin, createdat, " +
                    "birthday, banned, banreason, characterslots, webadmin, nick, mute, " +
                    "nxCredit, maplePoint, nxPrepaid, rewardpoints, votepoints, language " +
                    "FROM accounts WHERE 1=1");

            List<Object> params = new ArrayList<>();
            if (id != null) {
                sql.append(" AND id = ?");
                params.add(id);
            }
            if (name != null && !name.isEmpty()) {
                sql.append(" AND name LIKE ?");
                params.add("%" + name + "%");
            }
            if (lastLoginStart != null && !lastLoginStart.isEmpty()) {
                sql.append(" AND lastlogin >= ?");
                params.add(lastLoginStart);
            }
            if (lastLoginEnd != null && !lastLoginEnd.isEmpty()) {
                sql.append(" AND lastlogin <= ?");
                params.add(lastLoginEnd);
            }
            if (createdAtStart != null && !createdAtStart.isEmpty()) {
                sql.append(" AND createdat >= ?");
                params.add(createdAtStart);
            }
            if (createdAtEnd != null && !createdAtEnd.isEmpty()) {
                sql.append(" AND createdat <= ?");
                params.add(createdAtEnd);
            }

            sql.append(" ORDER BY id DESC");

            if (page != null && size != null && page > 0 && size > 0) {
                sql.append(" LIMIT ? OFFSET ?");
                params.add(size);
                params.add((page - 1) * size);
            }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }

            return query.mapToMap().list().stream()
                    .map(AccountService::mapToAccountInfo)
                    .toList();
        }
    }

    public long getAccountCount(Integer id, String name, String lastLoginStart,
                                String lastLoginEnd, String createdAtStart, String createdAtEnd) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM accounts WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (id != null) {
                sql.append(" AND id = ?");
                params.add(id);
            }
            if (name != null && !name.isEmpty()) {
                sql.append(" AND name LIKE ?");
                params.add("%" + name + "%");
            }
            if (lastLoginStart != null && !lastLoginStart.isEmpty()) {
                sql.append(" AND lastlogin >= ?");
                params.add(lastLoginStart);
            }
            if (lastLoginEnd != null && !lastLoginEnd.isEmpty()) {
                sql.append(" AND lastlogin <= ?");
                params.add(lastLoginEnd);
            }
            if (createdAtStart != null && !createdAtStart.isEmpty()) {
                sql.append(" AND createdat >= ?");
                params.add(createdAtStart);
            }
            if (createdAtEnd != null && !createdAtEnd.isEmpty()) {
                sql.append(" AND createdat <= ?");
                params.add(createdAtEnd);
            }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }
            return query.mapTo(Long.class).one();
        }
    }

    public void addAccount(AddAccountDTO dto) {
        if (getAccountByName(dto.getName()) != null) {
            throw new IllegalArgumentException("Account name already exists");
        }
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO accounts (name, password, email, birthday, tempban, gender, tos) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 0)")) {
            String hashedPwd = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12));
            String birthday = dto.getBirthday() != null ? dto.getBirthday() : "2005-05-11";
            int gender = dto.getGender() != null ? dto.getGender() : 10;

            statement.setString(1, dto.getName());
            statement.setString(2, hashedPwd);
            statement.setString(3, dto.getEmail());
            statement.setString(4, birthday);
            DatabaseConnection.setLocalDateTime(statement, 5, DefaultDates.getTempban());
            statement.setInt(6, gender);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create account", e);
        }
    }

    public void updateAccountByUser(UpdateAccountByUserDTO dto) {
        AccountInfoDTO account = getAccountById(getCurrentUserId());
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        try (Handle handle = DatabaseConnection.getHandle()) {
            String storedHash = handle.createQuery("SELECT password FROM accounts WHERE id = ?")
                    .bind(0, account.getId())
                    .mapTo(String.class)
                    .one();

            if (!BCrypt.checkpw(dto.getOldPassword(), storedHash)) {
                throw new IllegalArgumentException("Old password is incorrect");
            }

            if (dto.getNewPassword() != null && !dto.getNewPassword().isEmpty()) {
                String newHash = BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt(12));
                handle.createUpdate("UPDATE accounts SET password = ? WHERE id = ?")
                        .bind(0, newHash)
                        .bind(1, account.getId())
                        .execute();
            }

            if (dto.getEmail() != null) {
                handle.createUpdate("UPDATE accounts SET email = ? WHERE id = ?")
                        .bind(0, dto.getEmail())
                        .bind(1, account.getId())
                        .execute();
            }
        }
    }

    public void updateAccountByGM(int accountId, UpdateAccountByGmDTO dto) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            List<String> sets = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (dto.getName() != null) {
                sets.add("name = ?");
                params.add(dto.getName());
            }
            if (dto.getEmail() != null) {
                sets.add("email = ?");
                params.add(dto.getEmail());
            }
            if (dto.getGender() != null) {
                sets.add("gender = ?");
                params.add(dto.getGender());
            }
            if (dto.getBanned() != null) {
                sets.add("banned = ?");
                params.add(dto.getBanned());
            }
            if (dto.getBanreason() != null) {
                sets.add("banreason = ?");
                params.add(dto.getBanreason());
            }
            if (dto.getWebadmin() != null) {
                sets.add("webadmin = ?");
                params.add(dto.getWebadmin());
            }
            if (dto.getNxCredit() != null) {
                sets.add("nxCredit = ?");
                params.add(dto.getNxCredit());
            }
            if (dto.getMaplePoint() != null) {
                sets.add("maplePoint = ?");
                params.add(dto.getMaplePoint());
            }
            if (dto.getNxPrepaid() != null) {
                sets.add("nxPrepaid = ?");
                params.add(dto.getNxPrepaid());
            }
            if (dto.getCharacterslots() != null) {
                sets.add("characterslots = ?");
                params.add(dto.getCharacterslots());
            }
            if (dto.getPin() != null) {
                sets.add("pin = ?");
                params.add(dto.getPin());
            }
            if (dto.getPic() != null) {
                sets.add("pic = ?");
                params.add(dto.getPic());
            }
            if (dto.getNick() != null) {
                sets.add("nick = ?");
                params.add(dto.getNick());
            }
            if (dto.getMute() != null) {
                sets.add("mute = ?");
                params.add(dto.getMute());
            }
            if (dto.getLanguage() != null) {
                sets.add("language = ?");
                params.add(dto.getLanguage());
            }
            if (dto.getRewardpoints() != null) {
                sets.add("rewardpoints = ?");
                params.add(dto.getRewardpoints());
            }
            if (dto.getVotepoints() != null) {
                sets.add("votepoints = ?");
                params.add(dto.getVotepoints());
            }
            if (dto.getBirthday() != null) {
                sets.add("birthday = ?");
                params.add(dto.getBirthday());
            }

            if (!sets.isEmpty()) {
                StringBuilder sql = new StringBuilder("UPDATE accounts SET ");
                sql.append(String.join(", ", sets));
                sql.append(" WHERE id = ?");
                params.add(accountId);

                var update = handle.createUpdate(sql.toString());
                for (int i = 0; i < params.size(); i++) {
                    update.bind(i, params.get(i));
                }
                update.execute();
            }
        }
    }

    public void banAccount(int accountId, String reason) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")
                    .bind(0, reason)
                    .bind(1, accountId)
                    .execute();
        }
    }

    public void unbanAccount(int accountId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("UPDATE accounts SET banned = 0, banreason = NULL WHERE id = ?")
                    .bind(0, accountId)
                    .execute();
        }
    }

    public void deleteAccount(int accountId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("DELETE FROM accounts WHERE id = ?")
                    .bind(0, accountId)
                    .execute();
        }
    }

    public void resetLoggedIn(int accountId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("UPDATE accounts SET loggedin = 0 WHERE id = ?")
                    .bind(0, accountId)
                    .execute();
        }
    }

    private static int currentUserId = -1;

    public static void setCurrentUserId(int id) {
        currentUserId = id;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    private static AccountInfoDTO mapToAccountInfo(Map<String, Object> row) {
        AccountInfoDTO dto = new AccountInfoDTO();
        dto.setId(((Number) row.get("id")).intValue());
        dto.setName((String) row.get("name"));
        dto.setEmail((String) row.get("email"));
        dto.setGender(((Number) row.get("gender")).intValue());
        dto.setLoggedin(((Number) row.get("loggedin")).intValue());
        dto.setLastlogin(toStringOrNull(row.get("lastlogin")));
        dto.setCreatedat(toStringOrNull(row.get("createdat")));
        dto.setBirthday(toStringOrNull(row.get("birthday")));
        dto.setBanned(((Number) row.get("banned")).intValue());
        dto.setBanreason((String) row.get("banreason"));
        dto.setCharacterslots(((Number) row.get("characterslots")).intValue());
        dto.setWebadmin(((Number) row.get("webadmin")).intValue());
        dto.setNick((String) row.get("nick"));
        dto.setMute(((Number) row.get("mute")).intValue());
        dto.setNxCredit(row.get("nxCredit") != null ? ((Number) row.get("nxCredit")).intValue() : null);
        dto.setMaplePoint(row.get("maplePoint") != null ? ((Number) row.get("maplePoint")).intValue() : null);
        dto.setNxPrepaid(row.get("nxPrepaid") != null ? ((Number) row.get("nxPrepaid")).intValue() : null);
        dto.setRewardpoints(((Number) row.get("rewardpoints")).intValue());
        dto.setVotepoints(((Number) row.get("votepoints")).intValue());
        dto.setLanguage(((Number) row.get("language")).intValue());
        return dto;
    }

    private static String toStringOrNull(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}
