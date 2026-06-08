package api.service;

import api.model.dto.InventoryItemDTO;
import org.jdbi.v3.core.Handle;
import server.ItemInformationProvider;
import tools.DatabaseConnection;

import java.util.*;

public class InventoryService {

    public List<Map<String, Object>> getInventoryTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        String[] typeNames = {"Equip", "Use", "Setup", "Etc", "Cash"};
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> type = new LinkedHashMap<>();
            type.put("inventoryType", i);
            type.put("name", typeNames[i - 1]);
            types.add(type);
        }
        return types;
    }

    public List<Map<String, Object>> getCharacterList(String keyword) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.name, c.level, c.job, c.world, c.gm, " +
                "a.loggedin AS online " +
                "FROM characters c LEFT JOIN accounts a ON c.accountid = a.id WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND (c.name LIKE ? OR c.id = ?)");
                params.add("%" + keyword.trim() + "%");
                try {
                    params.add(Integer.parseInt(keyword.trim()));
                } catch (NumberFormatException e) {
                    params.add(-1);
                }
            }
            sql.append(" ORDER BY c.name LIMIT 100");
            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }
            return query.mapToMap().list();
        }
    }

    public List<Map<String, Object>> getInventoryList(Integer characterId, String characterName,
                                                       Integer accountId, Integer inventoryType,
                                                       Integer page, Integer size) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            // First resolve character ID if name or account ID is provided
            Integer resolvedCharId = characterId;
            if (resolvedCharId == null && characterName != null) {
                var row = handle.createQuery("SELECT id FROM characters WHERE name = ?")
                        .bind(0, characterName)
                        .mapToMap()
                        .findOne()
                        .orElse(null);
                if (row != null) {
                    resolvedCharId = (Integer) row.get("id");
                }
            }
            if (resolvedCharId == null && accountId != null) {
                resolvedCharId = getFirstCharacterId(handle, accountId);
            }

            if (resolvedCharId == null) {
                return Collections.emptyList();
            }

            StringBuilder sql = new StringBuilder(
                "SELECT i.inventoryitemid, i.itemid, i.inventorytype, i.position, i.quantity, " +
                "i.owner, i.petid, i.expiration, i.giftFrom, i.characterid, " +
                "e.upgradeslots, e.level AS eqLevel, e.str, e.dex, e.`int` AS item_int, e.luk, " +
                "e.hp, e.mp, e.watk, e.matk, e.wdef, e.mdef, e.acc, e.avoid, e.hands, " +
                "e.speed, e.jump, e.locked, e.vicious, e.itemlevel, e.itemexp, e.ringid " +
                "FROM inventoryitems i LEFT JOIN inventoryequipment e ON i.inventoryitemid = e.inventoryitemid " +
                "WHERE i.characterid = ?");

            List<Object> params = new ArrayList<>();
            params.add(resolvedCharId);

            if (inventoryType != null && inventoryType > 0 && inventoryType <= 5) {
                sql.append(" AND i.inventorytype = ?");
                params.add(inventoryType);
            }

            sql.append(" ORDER BY i.inventorytype, i.position");

            if (page != null && size != null && size > 0) {
                sql.append(" LIMIT ? OFFSET ?");
                params.add(size);
                params.add((page - 1) * size);
            }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }

            return query.mapToMap().list().stream().map(row -> {
                Map<String, Object> result = new LinkedHashMap<>();
                int itemId = (int) row.get("itemid");
                result.put("id", row.get("inventoryitemid"));
                result.put("characterId", row.get("characterid"));
                result.put("itemId", itemId);
                result.put("itemName", tryGetItemName(itemId));
                result.put("inventoryType", row.get("inventorytype"));
                result.put("position", row.get("position"));
                result.put("quantity", row.get("quantity"));
                result.put("owner", row.get("owner"));
                result.put("petId", row.get("petid"));
                result.put("expiration", row.get("expiration"));
                result.put("giftFrom", row.get("giftFrom"));

                if (row.get("upgradeslots") != null) {
                    Map<String, Object> equipment = new LinkedHashMap<>();
                    equipment.put("upgradeslots", row.get("upgradeslots"));
                    equipment.put("level", row.get("eqLevel"));
                    equipment.put("str", row.get("str"));
                    equipment.put("dex", row.get("dex"));
                    equipment.put("item_int", row.get("item_int"));
                    equipment.put("luk", row.get("luk"));
                    equipment.put("hp", row.get("hp"));
                    equipment.put("mp", row.get("mp"));
                    equipment.put("watk", row.get("watk"));
                    equipment.put("matk", row.get("matk"));
                    equipment.put("wdef", row.get("wdef"));
                    equipment.put("mdef", row.get("mdef"));
                    equipment.put("acc", row.get("acc"));
                    equipment.put("avoid", row.get("avoid"));
                    equipment.put("hands", row.get("hands"));
                    equipment.put("speed", row.get("speed"));
                    equipment.put("jump", row.get("jump"));
                    equipment.put("locked", row.get("locked"));
                    equipment.put("vicious", row.get("vicious"));
                    equipment.put("itemlevel", row.get("itemlevel"));
                    equipment.put("itemexp", row.get("itemexp"));
                    equipment.put("ringid", row.get("ringid"));
                    result.put("equipment", equipment);
                }
                return result;
            }).toList();
        }
    }

    public long getInventoryCount(Integer characterId, String characterName,
                                   Integer accountId, Integer inventoryType) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Integer resolvedCharId = characterId;
            if (resolvedCharId == null && characterName != null) {
                var row = handle.createQuery("SELECT id FROM characters WHERE name = ?")
                        .bind(0, characterName)
                        .mapToMap()
                        .findOne()
                        .orElse(null);
                if (row != null) {
                    resolvedCharId = (Integer) row.get("id");
                }
            }
            if (resolvedCharId == null && accountId != null) {
                resolvedCharId = getFirstCharacterId(handle, accountId);
            }
            if (resolvedCharId == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM inventoryitems WHERE characterid = ?");
            if (inventoryType != null && inventoryType > 0) {
                sql.append(" AND inventorytype = ?");
                return handle.createQuery(sql.toString())
                    .bind(0, resolvedCharId)
                    .bind(1, inventoryType)
                    .mapTo(Long.class)
                    .one();
            }
            return handle.createQuery(sql.toString())
                .bind(0, resolvedCharId)
                .mapTo(Long.class)
                .one();
        }
    }

    public void updateInventoryItem(int inventoryItemId, InventoryItemDTO dto) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            // Update inventoryitems table
            if (dto.getQuantity() != null || dto.getPosition() != null) {
                StringBuilder sql = new StringBuilder("UPDATE inventoryitems SET ");
                List<Object> params = new ArrayList<>();
                boolean first = true;

                if (dto.getQuantity() != null) {
                    sql.append("quantity = ?");
                    params.add(dto.getQuantity());
                    first = false;
                }
                if (dto.getPosition() != null) {
                    if (!first) sql.append(", ");
                    sql.append("position = ?");
                    params.add(dto.getPosition());
                }

                sql.append(" WHERE inventoryitemid = ?");
                params.add(inventoryItemId);

                var update = handle.createUpdate(sql.toString());
                for (int i = 0; i < params.size(); i++) {
                    update.bind(i, params.get(i));
                }
                update.execute();
            }

            // Update inventoryequipment table if equipment fields are set
            boolean hasEquipFields = dto.getUpgradeslots() != null || dto.getLevel() != null ||
                    dto.getStr() != null || dto.getDex() != null || dto.getItem_int() != null ||
                    dto.getLuk() != null || dto.getHp() != null || dto.getMp() != null ||
                    dto.getWatk() != null || dto.getMatk() != null || dto.getWdef() != null ||
                    dto.getMdef() != null || dto.getAcc() != null || dto.getAvoid() != null ||
                    dto.getHands() != null || dto.getSpeed() != null || dto.getJump() != null;

            if (hasEquipFields) {
                // Check if equipment row exists
                var existing = handle.createQuery("SELECT inventoryequipmentid FROM inventoryequipment WHERE inventoryitemid = ?")
                        .bind(0, inventoryItemId)
                        .mapTo(Integer.class)
                        .findOne();

                if (existing.isPresent()) {
                    StringBuilder sql = new StringBuilder("UPDATE inventoryequipment SET ");
                    List<Object> params = new ArrayList<>();
                    boolean first = true;

                    first = appendField(sql, params, "upgradeslots", dto.getUpgradeslots(), first);
                    first = appendField(sql, params, "level", dto.getLevel(), first);
                    first = appendField(sql, params, "str", dto.getStr(), first);
                    first = appendField(sql, params, "dex", dto.getDex(), first);
                    first = appendField(sql, params, "`int`", dto.getItem_int(), first);
                    first = appendField(sql, params, "luk", dto.getLuk(), first);
                    first = appendField(sql, params, "hp", dto.getHp(), first);
                    first = appendField(sql, params, "mp", dto.getMp(), first);
                    first = appendField(sql, params, "watk", dto.getWatk(), first);
                    first = appendField(sql, params, "matk", dto.getMatk(), first);
                    first = appendField(sql, params, "wdef", dto.getWdef(), first);
                    first = appendField(sql, params, "mdef", dto.getMdef(), first);
                    first = appendField(sql, params, "acc", dto.getAcc(), first);
                    first = appendField(sql, params, "avoid", dto.getAvoid(), first);
                    first = appendField(sql, params, "hands", dto.getHands(), first);
                    first = appendField(sql, params, "speed", dto.getSpeed(), first);
                    first = appendField(sql, params, "jump", dto.getJump(), first);

                    sql.append(" WHERE inventoryitemid = ?");
                    params.add(inventoryItemId);

                    var update = handle.createUpdate(sql.toString());
                    for (int i = 0; i < params.size(); i++) {
                        update.bind(i, params.get(i));
                    }
                    update.execute();
                }
            }
        }
    }

    public void deleteInventoryItem(int inventoryItemId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("DELETE FROM inventoryequipment WHERE inventoryitemid = ?")
                .bind(0, inventoryItemId)
                .execute();
            handle.createUpdate("DELETE FROM inventoryitems WHERE inventoryitemid = ?")
                .bind(0, inventoryItemId)
                .execute();
        }
    }

    private Integer getFirstCharacterId(Handle handle, int accountId) {
        var row = handle.createQuery("SELECT id FROM characters WHERE accountid = ? ORDER BY id LIMIT 1")
                .bind(0, accountId)
                .mapTo(Integer.class)
                .findOne();
        return row.orElse(null);
    }

    private String tryGetItemName(int itemId) {
        try {
            return ItemInformationProvider.getInstance().getName(itemId);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean appendField(StringBuilder sql, List<Object> params,
                                 String fieldName, Integer value, boolean first) {
        if (value == null) return first;
        if (!first) sql.append(", ");
        sql.append(fieldName).append(" = ?");
        params.add(value);
        return false;
    }
}
