package api.service;

import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import tools.StringUtil;

import java.util.*;

public class MapService {

    private static final DataProvider mapSource = DataProviderFactory.getDataProvider(WZFiles.MAP);
    private static final Data mapStringData;
    private static final Map<String, String> mobNameCache = new HashMap<>();

    static {
        DataProvider stringProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
        mapStringData = stringProvider.getData("Map.img");
        Data mobData = stringProvider.getData("Mob.img");
        if (mobData != null) {
            for (Data child : mobData.getChildren()) {
                try {
                    String name = DataTool.getString(child.getChildByPath("name"), "");
                    if (!name.isEmpty()) {
                        mobNameCache.put(child.getName(), name);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public record MapDetail(int id, String name, String streetName, String bgm,
                             List<MapMonster> monsters) {}

    public record MapMonster(int id, String name, int level, int maxHP, int maxMP, int exp,
                              int ice, int lightning, int fire, int poison, int holy, int dark, int physical) {}

    private static final DataProvider mobWz = DataProviderFactory.getDataProvider(WZFiles.MOB);

    public static MapDetail getMapDetail(int mapid) {
        String mapStringPath = getMapStringPath(mapid);
        Data mapStrEntry = mapStringData.getChildByPath(mapStringPath);
        String mapName = "";
        String streetName = "";
        if (mapStrEntry != null) {
            mapName = DataTool.getString(mapStrEntry.getChildByPath("mapName"), "");
            streetName = DataTool.getString(mapStrEntry.getChildByPath("streetName"), "");
        }

        String bgm = "";
        List<MapMonster> monsters = new ArrayList<>();
        Set<Integer> seenMobs = new HashSet<>();

        try {
            Data mapData = mapSource.getData(getMapWzName(mapid));
            if (mapData != null) {
                Data infoData = mapData.getChildByPath("info");
                if (infoData != null) {
                    String link = DataTool.getString(infoData.getChildByPath("link"), "");
                    if (!link.isEmpty()) {
                        int linkId = Integer.parseInt(link);
                        Data linkData = mapSource.getData(getMapWzName(linkId));
                        if (linkData != null) {
                            infoData = linkData.getChildByPath("info");
                            mapData = linkData;
                        }
                    }
                    bgm = DataTool.getString(infoData.getChildByPath("bgm"), "");
                }

                Data lifeData = mapData.getChildByPath("life");
                if (lifeData != null) {
                    for (Data life : lifeData.getChildren()) {
                        String type = DataTool.getString(life.getChildByPath("type"), "");
                        if ("m".equals(type)) {
                            String idStr = DataTool.getString(life.getChildByPath("id"), "");
                            try {
                                int mobId = Integer.parseInt(idStr);
                                if (seenMobs.add(mobId)) {
                                    monsters.add(buildMonster(mobId));
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map " + mapid, e);
        }

        return new MapDetail(mapid, mapName, streetName, bgm, monsters);
    }

    private static MapMonster buildMonster(int mobId) {
        String name = mobNameCache.getOrDefault(Integer.toString(mobId), "Mob " + mobId);
        int maxHP = 0, maxMP = 0, exp = 0, level = 0;
        int ice = 0, lightning = 0, fire = 0, poison = 0, holy = 0, dark = 0, physical = 0;
        try {
            Data mobData = mobWz.getData(mobId + ".img");
            if (mobData == null) {
                String padded = StringUtil.getLeftPaddedStr(Integer.toString(mobId), '0', 7);
                mobData = mobWz.getData(padded + ".img");
            }
            if (mobData != null) {
                Data info = mobData.getChildByPath("info");
                if (info != null) {
                    maxHP = DataTool.getInt(info.getChildByPath("maxHP"), 0);
                    maxMP = DataTool.getInt(info.getChildByPath("maxMP"), 0);
                    exp = DataTool.getInt(info.getChildByPath("exp"), 0);
                    level = DataTool.getInt(info.getChildByPath("level"), 0);
                    Data elem = info.getChildByPath("elemAttr");
                    if (elem != null) {
                        String attr = DataTool.getString(elem, "");
                        for (int i = 0; i + 1 < attr.length(); i += 2) {
                            char e = attr.charAt(i);
                            int lvl = attr.charAt(i + 1) - '0';
                            switch (e) {
                                case 'I': ice = lvl; break;
                                case 'L': lightning = lvl; break;
                                case 'F': fire = lvl; break;
                                case 'P': poison = lvl; break;
                                case 'S': holy = lvl; break;
                                case 'D': dark = lvl; break;
                                case 'H': physical = lvl; break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new MapMonster(mobId, name, level, maxHP, maxMP, exp, ice, lightning, fire, poison, holy, dark, physical);
    }

    private static String getMapWzName(int mapid) {
        String padded = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        return "Map/Map" + (mapid / 100000000) + "/" + padded + ".img";
    }

    private static String getMapStringPath(int mapid) {
        String region;
        if (mapid < 100000000) {
            region = "maple";
        } else if (mapid < 200000000) {
            region = "victoria";
        } else if (mapid < 300000000) {
            region = "ossyria";
        } else if (mapid < 400000000) {
            region = "elin";
        } else if (mapid >= 540000000 && mapid < 560000000) {
            region = "singapore";
        } else if (mapid >= 600000000 && mapid < 620000000) {
            region = "MasteriaGL";
        } else if (mapid >= 670000000 && mapid < 682000000) {
            region = (mapid >= 674030000 && mapid < 674040000)
                    || (mapid >= 680100000 && mapid < 680200000) ? "etc" : "weddingGL";
        } else if (mapid >= 682000000 && mapid < 683000000) {
            region = "HalloweenGL";
        } else if (mapid >= 683000000 && mapid < 684000000) {
            region = "event";
        } else if (mapid >= 800000000 && mapid < 900000000) {
            region = (mapid >= 889100000 && mapid < 889200000) ? "etc" : "jp";
        } else {
            region = "etc";
        }
        return region + "/" + mapid;
    }
}
