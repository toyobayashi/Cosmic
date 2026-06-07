package api.service;

import org.jdbi.v3.core.Handle;
import tools.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DropService {

    public List<Map<String, Object>> getDropList(Integer dropperId, Integer itemId, Integer page, Integer size) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT d.id, d.dropperid, d.itemid, d.chance, d.minimum_quantity, d.maximum_quantity, d.questid FROM drop_data d WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (dropperId != null) { sql.append(" AND d.dropperid = ?"); params.add(dropperId); }
            if (itemId != null) { sql.append(" AND d.itemid = ?"); params.add(itemId); }

            sql.append(" ORDER BY d.id");
            if (page != null && size != null && size > 0) {
                sql.append(" LIMIT ? OFFSET ?");
                params.add(size);
                params.add((page - 1) * size);
            }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) query.bind(i, params.get(i));
            List<Map<String, Object>> list = query.mapToMap().list();
            for (Map<String, Object> row : list) {
                int dropper = ((Number) row.get("dropperid")).intValue();
                int iid = ((Number) row.get("itemid")).intValue();
                row.put("mobName", NameResolver.getMobName(dropper));
                row.put("itemName", NameResolver.getItemName(iid));
            }
            return list;
        }
    }

    public long getDropCount(Integer dropperId, Integer itemId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM drop_data WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (dropperId != null) { sql.append(" AND dropperid = ?"); params.add(dropperId); }
            if (itemId != null) { sql.append(" AND itemid = ?"); params.add(itemId); }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) query.bind(i, params.get(i));
            return query.mapTo(Long.class).one();
        }
    }

    public void addDropData(int dropperId, int itemId, int chance, int minQty, int maxQty, int questId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate(
                    "INSERT INTO drop_data (dropperid, itemid, chance, minimum_quantity, maximum_quantity, questid) VALUES (?, ?, ?, ?, ?, ?)")
                    .bind(0, dropperId).bind(1, itemId).bind(2, chance).bind(3, minQty).bind(4, maxQty).bind(5, questId)
                    .execute();
        }
    }

    public void updateDropData(int id, Integer dropperId, Integer itemId, Integer chance, Integer minQty, Integer maxQty, Integer questId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            List<String> sets = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (dropperId != null) { sets.add("dropperid = ?"); params.add(dropperId); }
            if (itemId != null) { sets.add("itemid = ?"); params.add(itemId); }
            if (chance != null) { sets.add("chance = ?"); params.add(chance); }
            if (minQty != null) { sets.add("minimum_quantity = ?"); params.add(minQty); }
            if (maxQty != null) { sets.add("maximum_quantity = ?"); params.add(maxQty); }
            if (questId != null) { sets.add("questid = ?"); params.add(questId); }

            if (sets.isEmpty()) return;

            StringBuilder sql = new StringBuilder("UPDATE drop_data SET ");
            sql.append(String.join(", ", sets)).append(" WHERE id = ?");
            params.add(id);

            var update = handle.createUpdate(sql.toString());
            for (int i = 0; i < params.size(); i++) update.bind(i, params.get(i));
            update.execute();
        }
    }

    public void deleteDropData(int id) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("DELETE FROM drop_data WHERE id = ?").bind(0, id).execute();
        }
    }

    // Global drops
    public List<Map<String, Object>> getGlobalDropList(Integer continent, Integer itemId, Integer page, Integer size) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT d.id, d.continent, d.itemid, d.chance, d.minimum_quantity, d.maximum_quantity, d.questid, d.comments FROM drop_data_global d WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (continent != null) { sql.append(" AND d.continent = ?"); params.add(continent); }
            if (itemId != null) { sql.append(" AND d.itemid = ?"); params.add(itemId); }

            sql.append(" ORDER BY d.id");
            if (page != null && size != null && size > 0) {
                sql.append(" LIMIT ? OFFSET ?");
                params.add(size);
                params.add((page - 1) * size);
            }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) query.bind(i, params.get(i));
            List<Map<String, Object>> list = query.mapToMap().list();
            for (Map<String, Object> row : list) {
                int iid = ((Number) row.get("itemid")).intValue();
                row.put("itemName", NameResolver.getItemName(iid));
            }
            return list;
        }
    }

    public long getGlobalDropCount(Integer continent, Integer itemId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM drop_data_global WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (continent != null) { sql.append(" AND continent = ?"); params.add(continent); }
            if (itemId != null) { sql.append(" AND itemid = ?"); params.add(itemId); }

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) query.bind(i, params.get(i));
            return query.mapTo(Long.class).one();
        }
    }

    public void addGlobalDropData(int continent, int itemId, int chance, int minQty, int maxQty, int questId, String comments) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate(
                    "INSERT INTO drop_data_global (continent, itemid, chance, minimum_quantity, maximum_quantity, questid, comments) VALUES (?, ?, ?, ?, ?, ?, ?)")
                    .bind(0, continent).bind(1, itemId).bind(2, chance).bind(3, minQty).bind(4, maxQty).bind(5, questId).bind(6, comments)
                    .execute();
        }
    }

    public void updateGlobalDropData(int id, Integer continent, Integer itemId, Integer chance, Integer minQty, Integer maxQty, Integer questId, String comments) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            List<String> sets = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (continent != null) { sets.add("continent = ?"); params.add(continent); }
            if (itemId != null) { sets.add("itemid = ?"); params.add(itemId); }
            if (chance != null) { sets.add("chance = ?"); params.add(chance); }
            if (minQty != null) { sets.add("minimum_quantity = ?"); params.add(minQty); }
            if (maxQty != null) { sets.add("maximum_quantity = ?"); params.add(maxQty); }
            if (questId != null) { sets.add("questid = ?"); params.add(questId); }
            if (comments != null) { sets.add("comments = ?"); params.add(comments); }

            if (sets.isEmpty()) return;

            StringBuilder sql = new StringBuilder("UPDATE drop_data_global SET ");
            sql.append(String.join(", ", sets)).append(" WHERE id = ?");
            params.add(id);

            var update = handle.createUpdate(sql.toString());
            for (int i = 0; i < params.size(); i++) update.bind(i, params.get(i));
            update.execute();
        }
    }

    public void deleteGlobalDropData(int id) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("DELETE FROM drop_data_global WHERE id = ?").bind(0, id).execute();
        }
    }
}
