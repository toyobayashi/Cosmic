package api.service;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import tools.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopService {

    public List<Map<String, Object>> getShopList(Integer shopId, Integer npcId, String npcName, String itemName) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder("SELECT s.shopid, s.npcid FROM shops s WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (shopId != null) { sql.append(" AND s.shopid = ?"); params.add(shopId); }
            if (npcId != null) { sql.append(" AND s.npcid = ?"); params.add(npcId); }

            sql.append(" ORDER BY s.shopid");
            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) query.bind(i, params.get(i));

            List<Map<String, Object>> list = query.mapToMap().list();
            for (Map<String, Object> row : list) {
                int nid = ((Number) row.get("npcid")).intValue();
                row.put("npcName", NameResolver.getNpcName(nid));
            }
            return list;
        }
    }

    public List<Map<String, Object>> getShopItems(int shopId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            List<Map<String, Object>> list = handle.createQuery(
                    "SELECT si.shopitemid, si.shopid, si.itemid, si.price, si.pitch, si.position " +
                    "FROM shopitems si WHERE si.shopid = ? ORDER BY si.position")
                    .bind(0, shopId)
                    .mapToMap().list();
            for (Map<String, Object> row : list) {
                int iid = ((Number) row.get("itemid")).intValue();
                row.put("itemName", NameResolver.getItemName(iid));
            }
            return list;
        }
    }

    public void addShopItem(int shopId, int itemId, int price, int pitch, int position) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate(
                    "INSERT INTO shopitems (shopid, itemid, price, pitch, position) VALUES (?, ?, ?, ?, ?)")
                    .bind(0, shopId).bind(1, itemId).bind(2, price).bind(3, pitch).bind(4, position)
                    .execute();
        }
    }

    public void updateShopItem(int shopItemId, Integer itemId, Integer price, Integer pitch, Integer position) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            List<String> sets = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (itemId != null) { sets.add("itemid = ?"); params.add(itemId); }
            if (price != null) { sets.add("price = ?"); params.add(price); }
            if (pitch != null) { sets.add("pitch = ?"); params.add(pitch); }
            if (position != null) { sets.add("position = ?"); params.add(position); }

            if (sets.isEmpty()) return;

            StringBuilder sql = new StringBuilder("UPDATE shopitems SET ");
            sql.append(String.join(", ", sets)).append(" WHERE shopitemid = ?");
            params.add(shopItemId);

            var update = handle.createUpdate(sql.toString());
            for (int i = 0; i < params.size(); i++) update.bind(i, params.get(i));
            update.execute();
        }
    }

    public void deleteShopItem(int shopItemId) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("DELETE FROM shopitems WHERE shopitemid = ?")
                    .bind(0, shopItemId).execute();
        }
    }
}
