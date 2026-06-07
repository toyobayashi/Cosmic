package api.service;

import server.ItemInformationProvider;
import server.life.MonsterInformationProvider;
import server.life.LifeFactory;

public class NameResolver {

    private static final ItemInformationProvider itemProvider = ItemInformationProvider.getInstance();
    private static final MonsterInformationProvider mobProvider = MonsterInformationProvider.getInstance();

    public static String getItemName(int itemId) {
        try {
            String name = itemProvider.getName(itemId);
            return name != null ? name : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getMobName(int mobId) {
        try {
            String name = mobProvider.getMobNameFromId(mobId);
            return name != null ? name : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getNpcName(int npcId) {
        try {
            var life = LifeFactory.getNPC(npcId);
            return life != null && life.getName() != null ? life.getName() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
