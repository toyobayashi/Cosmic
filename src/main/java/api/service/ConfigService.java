package api.service;

import config.ServerConfig;
import config.WorldConfig;
import config.YamlConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigService {

    private static final String CONFIG_FILE = YamlConfig.getConfigFileName();
    private final Map<String, Object> pendingChanges = new LinkedHashMap<>();

    public Map<String, Object> getConfigTypes() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> types = List.of("server", "world");
        List<String> subTypes = List.of("Database", "Network", "Rates", "Features", "GM Security",
                "Events & PQs", "Cash Shop", "Scroll & Skill", "Quest", "Character", "Equipment",
                "Guild", "Family", "Wedding", "Pet", "Maker", "Miscellaneous");
        result.put("types", types);
        result.put("subTypes", subTypes);
        return result;
    }

    public Map<String, Object> getConfigList(String type, String subType, String filter,
                                              Integer page, Integer size) {
        List<Map<String, Object>> allConfigs = new ArrayList<>();
        YamlConfig yamlConfig = YamlConfig.config;

        if (yamlConfig == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", Collections.emptyList());
            result.put("totalRow", 0);
            return result;
        }

        if (type == null || type.isEmpty() || "server".equals(type)) {
            if (yamlConfig.server != null) {
                collectServerConfig(allConfigs, yamlConfig.server, subType);
            }
        }

        if (type == null || type.isEmpty() || "world".equals(type)) {
            if (yamlConfig.worlds != null) {
                for (int i = 0; i < yamlConfig.worlds.size(); i++) {
                    // For world type, subType is the world index; for mixed (All), pass null to show all
                    String worldSubType = (type == null || type.isEmpty()) ? null : subType;
                    if (worldSubType != null && !worldSubType.isEmpty() && !String.valueOf(i).equals(worldSubType)) {
                        continue;
                    }
                    WorldConfig wc = yamlConfig.worlds.get(i);
                    collectWorldConfig(allConfigs, wc, i);
                }
            }
        }

        // Apply filter
        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.trim().toLowerCase();
            allConfigs = allConfigs.stream()
                .filter(c -> {
                    String code = (String) c.getOrDefault("configCode", "");
                    String val = (String) c.getOrDefault("configValue", "");
                    return code.toLowerCase().contains(lowerFilter) || val.toLowerCase().contains(lowerFilter);
                })
                .toList();
        }

        long total = allConfigs.size();
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allConfigs.size());

        List<Map<String, Object>> records;
        if (start >= allConfigs.size()) {
            records = Collections.emptyList();
        } else {
            records = new ArrayList<>(allConfigs.subList(start, end));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("totalRow", total);
        return result;
    }

    public boolean updateConfig(String configType, String configSubType, String configCode, String configValue) {
        YamlConfig yamlConfig = YamlConfig.config;
        if (yamlConfig == null) return false;

        try {
            if ("server".equals(configType)) {
                ServerConfig sc = yamlConfig.server;
                Field field = ServerConfig.class.getDeclaredField(configCode);
                field.setAccessible(true);
                Object typedValue = parseValue(configValue, field.getType());
                field.set(sc, typedValue);
            } else if ("world".equals(configType)) {
                int worldIndex = Integer.parseInt(configSubType);
                if (worldIndex >= 0 && worldIndex < yamlConfig.worlds.size()) {
                    WorldConfig wc = yamlConfig.worlds.get(worldIndex);
                    Field field = WorldConfig.class.getDeclaredField(configCode);
                    field.setAccessible(true);
                    Object typedValue = parseValue(configValue, field.getType());
                    field.set(wc, typedValue);
                } else {
                    return false;
                }
            } else {
                return false;
            }
            // Mark as pending save
            pendingChanges.put(configType + "/" + configSubType + "/" + configCode, true);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

    public void saveConfigToDisk() throws IOException {
        YamlConfig yamlConfig = YamlConfig.config;
        if (yamlConfig == null) return;

        try (BufferedWriter w = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            w.write("#World Name: (0 \"Scania\", 1 \"Bera\", 2 \"Broa\", 3 \"Windia\", 4 \"Khaini\", 5 \"Bellocan\", 6 \"Mardia\", 7 \"Kradia\", 8 \"Yellonde\", 9 \"Demethos\", 10 \"Galicia\", 11 \"El Nido\", 12 \"Zenith\", 13 \"Arcenia\", 14 \"Kastia\", 15 \"Judis\", 16 \"Plana\", 17 \"Kalluna\", 18 \"Stius\", 19 \"Croa\", 20 \"Medere\")\n");
            w.write("#Flag types: (0 = nothing, 1 = event, 2 = new, 3 = hot)\n");
            w.write("#Recommended to use only up to 15 worlds\n");
            w.write("worlds:\n");

            if (yamlConfig.worlds != null) {
                String[] worldNames = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde",
                        "Demethos", "Galicia", "Kastia", "Judis", "Arcenia", "Plana", "El Nido", "Kalluna", "Stius", "Croa", "Zenith", "Medere"};
                for (int i = 0; i < yamlConfig.worlds.size(); i++) {
                    WorldConfig wc = yamlConfig.worlds.get(i);
                    String worldLabel = i < worldNames.length ? worldNames[i] : "World" + i;
                    w.write("    #Properties for " + worldLabel + " " + i + "\n");
                    w.write("  - flag: " + wc.flag + "\n");
                    w.write("    server_message: '" + escapeYaml(wc.server_message) + "'\n");
                    w.write("    event_message: '" + escapeYaml(wc.event_message) + "'\n");
                    w.write("    why_am_i_recommended: '" + escapeYaml(wc.why_am_i_recommended) + "'\n");
                    w.write("    channels: " + wc.channels + "\n");
                    w.write("    exp_rate: " + formatFloat(wc.exp_rate) + "\n");
                    w.write("    meso_rate: " + formatFloat(wc.meso_rate) + "\n");
                    w.write("    drop_rate: " + formatFloat(wc.drop_rate) + "\n");
                    w.write("    boss_drop_rate: " + formatFloat(wc.boss_drop_rate) + "\n");
                    w.write("    quest_rate: " + formatFloat(wc.quest_rate) + "\n");
                    w.write("    fishing_rate: " + formatFloat(wc.fishing_rate) + "\n");
                    w.write("    travel_rate: " + formatFloat(wc.travel_rate) + "\n");
                    w.write("\n");
                }
            }

            w.write("\nserver:\n");
            if (yamlConfig.server != null) {
                writeServerConfig(w, yamlConfig.server);
            }
        }
        pendingChanges.clear();
    }

    private void writeServerConfig(BufferedWriter w, ServerConfig sc) throws IOException {
        w.write("    #Database Configuration\n");
        w.write("    DB_URL_FORMAT: \"" + escapeYaml(sc.DB_URL_FORMAT) + "\"\n");
        w.write("    DB_HOST: \"" + escapeYaml(sc.DB_HOST) + "\"\n");
        w.write("    DB_USER: \"" + escapeYaml(sc.DB_USER) + "\"\n");
        w.write("    DB_PASS: \"" + escapeYaml(sc.DB_PASS) + "\"\n");
        w.write("    INIT_CONNECTION_POOL_TIMEOUT: " + sc.INIT_CONNECTION_POOL_TIMEOUT + "\n");
        w.write("\n    #Login Configuration\n");
        writeField(w, "WORLDS", sc.WORLDS);
        writeField(w, "WLDLIST_SIZE", sc.WLDLIST_SIZE);
        writeField(w, "CHANNEL_SIZE", sc.CHANNEL_SIZE);
        writeField(w, "CHANNEL_LOAD", sc.CHANNEL_LOAD);
        writeField(w, "CHANNEL_LOCKS", sc.CHANNEL_LOCKS);
        writeField(w, "RESPAWN_INTERVAL", sc.RESPAWN_INTERVAL);
        writeField(w, "PURGING_INTERVAL", sc.PURGING_INTERVAL);
        writeField(w, "RANKING_INTERVAL", sc.RANKING_INTERVAL);
        writeField(w, "COUPON_INTERVAL", sc.COUPON_INTERVAL);
        writeField(w, "UPDATE_INTERVAL", sc.UPDATE_INTERVAL);
        writeField(w, "ENABLE_PIC", sc.ENABLE_PIC);
        writeField(w, "ENABLE_PIN", sc.ENABLE_PIN);
        writeField(w, "BYPASS_PIC_EXPIRATION", sc.BYPASS_PIC_EXPIRATION);
        writeField(w, "BYPASS_PIN_EXPIRATION", sc.BYPASS_PIN_EXPIRATION);
        writeField(w, "AUTOMATIC_REGISTER", sc.AUTOMATIC_REGISTER);
        writeField(w, "BCRYPT_MIGRATION", sc.BCRYPT_MIGRATION);
        writeField(w, "COLLECTIVE_CHARSLOT", sc.COLLECTIVE_CHARSLOT);
        writeField(w, "DETERRED_MULTICLIENT", sc.DETERRED_MULTICLIENT);
        writeField(w, "MAX_ALLOWED_ACCOUNT_HWID", sc.MAX_ALLOWED_ACCOUNT_HWID);
        writeField(w, "MAX_ACCOUNT_LOGIN_ATTEMPT", sc.MAX_ACCOUNT_LOGIN_ATTEMPT);
        writeField(w, "LOGIN_ATTEMPT_DURATION", sc.LOGIN_ATTEMPT_DURATION);

        w.write("\n    #Ip Configuration\n");
        writeField(w, "HOST", sc.HOST);
        writeField(w, "LANHOST", sc.LANHOST);
        writeField(w, "LOCALHOST", sc.LOCALHOST);
        writeField(w, "LOGIN_PORT", sc.LOGIN_PORT);
        writeField(w, "CHANNEL_BASE_PORT", sc.CHANNEL_BASE_PORT);
        writeField(w, "API_PORT", sc.API_PORT);
        writeField(w, "GMSERVER", sc.GMSERVER);
        writeField(w, "SHUTDOWNHOOK", sc.SHUTDOWNHOOK);

        w.write("\n    #Server Flags\n");
        writeAllServerFields(w, sc, List.of(
            "USE_CUSTOM_KEYSET","USE_DEBUG","USE_DEBUG_SHOW_INFO_EQPEXP","USE_DEBUG_SHOW_RCVD_PACKET",
            "USE_DEBUG_SHOW_RCVD_MVLIFE","USE_DEBUG_SHOW_PACKET","USE_SUPPLY_RATE_COUPONS",
            "USE_IP_VALIDATION","USE_CHARACTER_ACCOUNT_CHECK","USE_MAXRANGE",
            "USE_MAXRANGE_ECHO_OF_HERO","USE_MTS","USE_CPQ","USE_AUTOHIDE_GM",
            "USE_FIXED_RATIO_HPMP_UPDATE","USE_FAMILY_SYSTEM","USE_DUEY",
            "USE_RANDOMIZE_HPMP_GAIN","USE_STORAGE_ITEM_SORT","USE_ITEM_SORT",
            "USE_ITEM_SORT_BY_NAME","USE_PARTY_FOR_STARTERS","USE_AUTOASSIGN_STARTERS_AP",
            "USE_AUTOASSIGN_SECONDARY_CAP","USE_STARTING_AP_4","USE_AUTOBAN",
            "USE_AUTOBAN_LOG","USE_EXP_GAIN_LOG","USE_AUTOSAVE","USE_SERVER_AUTOASSIGNER",
            "USE_REFRESH_RANK_MOVE","USE_ENFORCE_ADMIN_ACCOUNT","USE_ENFORCE_NOVICE_EXPRATE",
            "USE_ENFORCE_HPMP_SWAP","USE_ENFORCE_MOB_LEVEL_RANGE","USE_ENFORCE_JOB_LEVEL_RANGE",
            "USE_ENFORCE_JOB_SP_RANGE","USE_ENFORCE_ITEM_SUGGESTION","USE_ENFORCE_UNMERCHABLE_CASH",
            "USE_ENFORCE_UNMERCHABLE_PET","USE_ENFORCE_MERCHANT_SAVE","USE_ENFORCE_MDOOR_POSITION",
            "USE_SPAWN_CLEAN_MDOOR","USE_SPAWN_RELEVANT_LOOT","USE_ERASE_PERMIT_ON_OPENSHOP",
            "USE_ERASE_UNTRADEABLE_DROP","USE_ERASE_PET_ON_EXPIRATION","USE_BUFF_MOST_SIGNIFICANT",
            "USE_BUFF_EVERLASTING","USE_MULTIPLE_SAME_EQUIP_DROP","USE_ENABLE_FULL_RESPAWN",
            "USE_ENABLE_CHAT_LOG","USE_MAP_OWNERSHIP_SYSTEM","USE_FISHING_SYSTEM",
            "USE_NPCS_SCRIPTABLE"
        ));

        w.write("\n    #Events/PQs Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_OLD_GMS_STYLED_PQ_NPCS","USE_ENABLE_SOLO_EXPEDITIONS","USE_ENABLE_DAILY_EXPEDITIONS",
            "USE_ENABLE_RECALL_EVENT"
        ));

        w.write("\n    #Announcement Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_ANNOUNCE_SHOPITEMSOLD","USE_ANNOUNCE_CHANGEJOB","USE_ANNOUNCE_NX_COUPON_LOOT"
        ));

        w.write("\n    #Cash Shop Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_JOINT_CASHSHOP_INVENTORY","USE_CLEAR_OUTDATED_COUPONS",
            "ALLOW_CASHSHOP_NAME_CHANGE","ALLOW_CASHSHOP_WORLD_TRANSFER"
        ));

        w.write("\n    #Maker Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_MAKER_PERMISSIVE_ATKUP","USE_MAKER_FEE_HEURISTICS"
        ));

        w.write("\n    #Commands Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "BLOCK_GENERATE_CASH_ITEM","USE_WHOLE_SERVER_RANKING"
        ));

        w.write("\n    EQUIP_EXP_RATE: " + formatFloat(sc.EQUIP_EXP_RATE) + "\n");
        w.write("    PQ_BONUS_EXP_RATE: " + formatFloat(sc.PQ_BONUS_EXP_RATE) + "\n");
        writeField(w, "EXP_SPLIT_LEVEL_INTERVAL", sc.EXP_SPLIT_LEVEL_INTERVAL);
        writeField(w, "EXP_SPLIT_LEECH_INTERVAL", sc.EXP_SPLIT_LEECH_INTERVAL);
        w.write("    EXP_SPLIT_MVP_MOD: " + formatFloat(sc.EXP_SPLIT_MVP_MOD) + "\n");
        w.write("    EXP_SPLIT_COMMON_MOD: " + formatFloat(sc.EXP_SPLIT_COMMON_MOD) + "\n");
        w.write("    PARTY_BONUS_EXP_RATE: " + formatFloat(sc.PARTY_BONUS_EXP_RATE) + "\n");
        writeField(w, "ALLOW_CONSECUTIVE_LEVEL_UP", sc.ALLOW_CONSECUTIVE_LEVEL_UP);
        writeField(w, "USE_MODERN_EXP_TABLE", sc.USE_MODERN_EXP_TABLE);

        w.write("\n    #Miscellaneous Configuration\n");
        writeField(w, "TIMEZONE", sc.TIMEZONE);
        writeField(w, "USE_DISPLAY_NUMBERS_WITH_COMMA", sc.USE_DISPLAY_NUMBERS_WITH_COMMA);
        writeField(w, "USE_UNITPRICE_WITH_COMMA", sc.USE_UNITPRICE_WITH_COMMA);
        writeField(w, "MAX_MONITORED_BUFFSTATS", sc.MAX_MONITORED_BUFFSTATS);
        writeField(w, "MAX_AP", sc.MAX_AP);
        writeField(w, "MAX_EVENT_LEVELS", sc.MAX_EVENT_LEVELS);
        writeField(w, "BLOCK_NPC_RACE_CONDT", sc.BLOCK_NPC_RACE_CONDT);
        writeField(w, "TOT_MOB_QUEST_REQUIREMENT", sc.TOT_MOB_QUEST_REQUIREMENT);
        writeField(w, "MOB_REACTOR_REFRESH_TIME", sc.MOB_REACTOR_REFRESH_TIME);
        writeField(w, "PARTY_SEARCH_REENTRY_LIMIT", sc.PARTY_SEARCH_REENTRY_LIMIT);
        writeField(w, "NAME_CHANGE_COOLDOWN", sc.NAME_CHANGE_COOLDOWN);
        writeField(w, "WORLD_TRANSFER_COOLDOWN", sc.WORLD_TRANSFER_COOLDOWN);
        writeField(w, "INSTANT_NAME_CHANGE", sc.INSTANT_NAME_CHANGE);

        w.write("\n    #Dangling Items/Locks Configuration\n");
        writeField(w, "ITEM_EXPIRE_TIME", sc.ITEM_EXPIRE_TIME);
        writeField(w, "KITE_EXPIRE_TIME", sc.KITE_EXPIRE_TIME);
        writeField(w, "ITEM_MONITOR_TIME", sc.ITEM_MONITOR_TIME);
        writeField(w, "LOCK_MONITOR_TIME", sc.LOCK_MONITOR_TIME);
        writeField(w, "ITEM_EXPIRE_CHECK", sc.ITEM_EXPIRE_CHECK);
        writeField(w, "ITEM_LIMIT_ON_MAP", sc.ITEM_LIMIT_ON_MAP);
        writeField(w, "MAP_VISITED_SIZE", sc.MAP_VISITED_SIZE);
        writeField(w, "MAP_DAMAGE_OVERTIME_INTERVAL", sc.MAP_DAMAGE_OVERTIME_INTERVAL);
        writeField(w, "MAP_DAMAGE_OVERTIME_COUNT", sc.MAP_DAMAGE_OVERTIME_COUNT);

        w.write("\n    #Channel Mob Disease Monitor Configuration\n");
        writeField(w, "MOB_STATUS_MONITOR_PROC", sc.MOB_STATUS_MONITOR_PROC);
        writeField(w, "MOB_STATUS_MONITOR_LIFE", sc.MOB_STATUS_MONITOR_LIFE);
        writeField(w, "MOB_STATUS_AGGRO_PERSISTENCE", sc.MOB_STATUS_AGGRO_PERSISTENCE);
        writeField(w, "MOB_STATUS_AGGRO_INTERVAL", sc.MOB_STATUS_AGGRO_INTERVAL);
        writeField(w, "USE_AUTOAGGRO_NEARBY", sc.USE_AUTOAGGRO_NEARBY);

        w.write("\n    #Scroll Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_PERFECT_GM_SCROLL","USE_PERFECT_SCROLLING","USE_ENHANCED_CHSCROLL",
            "USE_ENHANCED_CRAFTING","SCROLL_CHANCE_ROLLS","CHSCROLL_STAT_RATE","CHSCROLL_STAT_RANGE"
        ));

        w.write("\n    #Beginner/Other Skills Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_ULTRA_NIMBLE_FEET","USE_ULTRA_RECOVERY","USE_ULTRA_THREE_SNAILS",
            "USE_FULL_ARAN_SKILLSET","USE_FAST_REUSE_HERO_WILL","USE_ANTI_IMMUNITY_CRASH",
            "USE_UNDISPEL_HOLY_SHIELD","USE_FULL_HOLY_SYMBOL"
        ));

        w.write("\n    #Character/Quest Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_ADD_SLOTS_BY_LEVEL","USE_ADD_RATES_BY_LEVEL","USE_STACK_COUPON_RATES",
            "USE_PERFECT_PITCH","USE_QUEST_RATE","QUEST_POINT_REPEATABLE_INTERVAL",
            "QUEST_POINT_REQUIREMENT","QUEST_POINT_PER_QUEST_COMPLETE","QUEST_POINT_PER_EVENT_CLEAR"
        ));

        w.write("\n    #Guild Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "CREATE_GUILD_MIN_PARTNERS","CREATE_GUILD_COST","CHANGE_EMBLEM_COST",
            "EXPAND_GUILD_BASE_COST","EXPAND_GUILD_TIER_COST","EXPAND_GUILD_MAX_COST"
        ));

        w.write("\n    #Family Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "FAMILY_REP_PER_KILL","FAMILY_REP_PER_BOSS_KILL","FAMILY_REP_PER_LEVELUP","FAMILY_MAX_GENERATIONS"
        ));

        w.write("\n    #Equipment Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_EQUIPMNT_LVLUP_SLOTS","USE_EQUIPMNT_LVLUP_POWER","USE_EQUIPMNT_LVLUP_CASH",
            "MAX_EQUIPMNT_LVLUP_STAT_UP","MAX_EQUIPMNT_STAT","USE_EQUIPMNT_LVLUP"
        ));

        w.write("\n    #Map-Chair/Player NPC Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_CHAIR_EXTRAHEAL","CHAIR_EXTRA_HEAL_MULTIPLIER","CHAIR_EXTRA_HEAL_MAX_DELAY",
            "PLAYERNPC_INITIAL_X","PLAYERNPC_INITIAL_Y","PLAYERNPC_AREA_X","PLAYERNPC_AREA_Y",
            "PLAYERNPC_AREA_STEPS","PLAYERNPC_ORGANIZE_AREA","PLAYERNPC_AUTODEPLOY"
        ));

        w.write("\n    #Pet Auto-Pot/Mount Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_COMPULSORY_AUTOPOT","USE_EQUIPS_ON_AUTOPOT","PET_AUTOHP_RATIO","PET_AUTOMP_RATIO",
            "PET_EXHAUST_COUNT","MOUNT_EXHAUST_COUNT","PETS_NEVER_HUNGRY","GM_PETS_NEVER_HUNGRY"
        ));

        w.write("\n    #Event Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "EVENT_MAX_GUILD_QUEUE","EVENT_LOBBY_DELAY"
        ));

        w.write("\n    #Dojo Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "USE_FAST_DOJO_UPGRADE","USE_DEADLY_DOJO","DOJO_ENERGY_ATK","DOJO_ENERGY_DMG"
        ));

        w.write("\n    #Wedding Configuration\n");
        writeAllServerFields(w, sc, List.of(
            "WEDDING_RESERVATION_DELAY","WEDDING_RESERVATION_TIMEOUT","WEDDING_RESERVATION_INTERVAL",
            "WEDDING_BLESS_EXP","WEDDING_GIFT_LIMIT","WEDDING_BLESSER_SHOWFX"
        ));

        w.write("\n    #Timeout/Event End\n");
        writeField(w, "TIMEOUT_DURATION", sc.TIMEOUT_DURATION);
        writeField(w, "EVENT_END_TIMESTAMP", sc.EVENT_END_TIMESTAMP);

        w.write("\n    # GM Security Configuration\n");
        writeField(w, "MINIMUM_GM_LEVEL_TO_TRADE", sc.MINIMUM_GM_LEVEL_TO_TRADE);
        writeField(w, "MINIMUM_GM_LEVEL_TO_USE_STORAGE", sc.MINIMUM_GM_LEVEL_TO_USE_STORAGE);
        writeField(w, "MINIMUM_GM_LEVEL_TO_USE_DUEY", sc.MINIMUM_GM_LEVEL_TO_USE_DUEY);
        writeField(w, "MINIMUM_GM_LEVEL_TO_DROP", sc.MINIMUM_GM_LEVEL_TO_DROP);
    }

    private void writeAllServerFields(BufferedWriter w, ServerConfig sc, List<String> fields) throws IOException {
        for (String name : fields) {
            try {
                Field f = ServerConfig.class.getDeclaredField(name);
                f.setAccessible(true);
                Object val = f.get(sc);
                writeValue(w, name, val);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
    }

    private void writeField(BufferedWriter w, String name, Object val) throws IOException {
        writeValue(w, name, val);
    }

    private void writeValue(BufferedWriter w, String name, Object val) throws IOException {
        String strVal;
        if (val instanceof String) {
            strVal = "\"" + escapeYaml((String) val) + "\"";
        } else if (val instanceof Boolean) {
            strVal = val.toString();
        } else if (val instanceof Float || val instanceof Double) {
            strVal = formatFloat(((Number) val).doubleValue());
        } else {
            strVal = String.valueOf(val);
        }
        w.write("    " + name + ": " + strVal + "\n");
    }

    private String formatFloat(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((int) v) + ".0";
        }
        return String.valueOf(v);
    }

    private String escapeYaml(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Object parseValue(String value, Class<?> targetType) {
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        return value;
    }

    private void collectServerConfig(List<Map<String, Object>> list, ServerConfig config, String subType) {
        Map<String, String> categoryMap = getCategoryMap();
        for (Field field : ServerConfig.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(config);
                String name = field.getName();
                String category = categoryMap.getOrDefault(name, "Miscellaneous");

                if (subType != null && !subType.isEmpty() && !category.equals(subType)) continue;

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", list.size() + 1);
                entry.put("configType", "server");
                entry.put("configSubType", category);
                entry.put("configClazz", getTypeName(value));
                entry.put("configCode", name);
                entry.put("configValue", value != null ? String.valueOf(value) : "null");
                entry.put("configDesc", "");
                list.add(entry);
            } catch (IllegalAccessException ignored) {}
        }
    }

    private void collectWorldConfig(List<Map<String, Object>> list, WorldConfig config, int worldIndex) {
        for (Field field : WorldConfig.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(config);
                String name = field.getName();

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", list.size() + 1);
                entry.put("configType", "world");
                entry.put("configSubType", String.valueOf(worldIndex));
                entry.put("configClazz", getTypeName(value));
                entry.put("configCode", name);
                entry.put("configValue", value != null ? String.valueOf(value) : "null");
                entry.put("configDesc", "");
                list.add(entry);
            } catch (IllegalAccessException ignored) {}
        }
    }

    private String getTypeName(Object value) {
        if (value == null) return "java.lang.String";
        if (value instanceof Boolean) return "java.lang.Boolean";
        if (value instanceof Integer) return "java.lang.Integer";
        if (value instanceof Long) return "java.lang.Long";
        if (value instanceof Float) return "java.lang.Float";
        if (value instanceof Double) return "java.lang.Double";
        if (value instanceof String) return "java.lang.String";
        return "java.lang.String";
    }

    private Map<String, String> getCategoryMap() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("DB_URL_FORMAT", "Database");
        map.put("DB_HOST", "Database");
        map.put("DB_USER", "Database");
        map.put("DB_PASS", "Database");

        map.put("HOST", "Network");
        map.put("LANHOST", "Network");
        map.put("LOCALHOST", "Network");
        map.put("LOGIN_PORT", "Network");
        map.put("CHANNEL_BASE_PORT", "Network");
        map.put("API_PORT", "Network");
        map.put("WORLDS", "Network");
        map.put("CHANNEL_SIZE", "Network");
        map.put("CHANNEL_LOAD", "Network");

        map.put("EQUIP_EXP_RATE", "Rates");
        map.put("PQ_BONUS_EXP_RATE", "Rates");
        map.put("PARTY_BONUS_EXP_RATE", "Rates");
        map.put("EXP_SPLIT_MVP_MOD", "Rates");
        map.put("EXP_SPLIT_COMMON_MOD", "Rates");

        map.put("USE_CUSTOM_KEYSET", "Features");
        map.put("USE_AUTOHIDE_GM", "Features");
        map.put("USE_FAMILY_SYSTEM", "Features");
        map.put("USE_DUEY", "Features");
        map.put("USE_ITEM_SORT", "Features");
        map.put("USE_ITEM_SORT_BY_NAME", "Features");
        map.put("USE_STORAGE_ITEM_SORT", "Features");
        map.put("USE_AUTOBAN", "Features");
        map.put("USE_AUTOSAVE", "Features");
        map.put("USE_FISHING_SYSTEM", "Features");
        map.put("USE_MAP_OWNERSHIP_SYSTEM", "Features");
        map.put("USE_NPCS_SCRIPTABLE", "Features");
        map.put("USE_RANDOMIZE_HPMP_GAIN", "Features");
        map.put("USE_FIXED_RATIO_HPMP_UPDATE", "Features");
        map.put("USE_BUFF_MOST_SIGNIFICANT", "Features");

        map.put("MINIMUM_GM_LEVEL_TO_TRADE", "GM Security");
        map.put("MINIMUM_GM_LEVEL_TO_USE_STORAGE", "GM Security");
        map.put("MINIMUM_GM_LEVEL_TO_USE_DUEY", "GM Security");
        map.put("MINIMUM_GM_LEVEL_TO_DROP", "GM Security");

        map.put("USE_OLD_GMS_STYLED_PQ_NPCS", "Events & PQs");
        map.put("USE_ENABLE_SOLO_EXPEDITIONS", "Events & PQs");
        map.put("USE_ENABLE_DAILY_EXPEDITIONS", "Events & PQs");
        map.put("USE_ENABLE_RECALL_EVENT", "Events & PQs");
        map.put("USE_CPQ", "Events & PQs");

        map.put("USE_JOINT_CASHSHOP_INVENTORY", "Cash Shop");
        map.put("ALLOW_CASHSHOP_NAME_CHANGE", "Cash Shop");
        map.put("ALLOW_CASHSHOP_WORLD_TRANSFER", "Cash Shop");

        map.put("USE_PERFECT_GM_SCROLL", "Scroll & Skill");
        map.put("USE_PERFECT_SCROLLING", "Scroll & Skill");
        map.put("SCROLL_CHANCE_ROLLS", "Scroll & Skill");
        map.put("CHSCROLL_STAT_RATE", "Scroll & Skill");

        map.put("USE_QUEST_RATE", "Quest");

        map.put("USE_ADD_SLOTS_BY_LEVEL", "Character");
        map.put("USE_ADD_RATES_BY_LEVEL", "Character");
        map.put("USE_STACK_COUPON_RATES", "Character");
        map.put("USE_ENFORCE_MOB_LEVEL_RANGE", "Character");

        map.put("USE_EQUIPMNT_LVLUP", "Equipment");
        map.put("USE_EQUIPMNT_LVLUP_SLOTS", "Equipment");
        map.put("MAX_EQUIPMNT_STAT", "Equipment");

        map.put("CREATE_GUILD_COST", "Guild");
        map.put("EXPAND_GUILD_BASE_COST", "Guild");

        map.put("FAMILY_REP_PER_KILL", "Family");
        map.put("FAMILY_REP_PER_BOSS_KILL", "Family");

        map.put("WEDDING_GIFT_LIMIT", "Wedding");
        map.put("WEDDING_RESERVATION_DELAY", "Wedding");

        map.put("PETS_NEVER_HUNGRY", "Pet");
        map.put("PET_EXHAUST_COUNT", "Pet");

        map.put("USE_MAKER_PERMISSIVE_ATKUP", "Maker");

        return map;
    }
}
