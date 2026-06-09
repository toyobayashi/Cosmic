package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import server.ItemInformationProvider;

import java.util.*;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    private static List<Map.Entry<Integer, String>> mobNameCache = null;
    private static List<Map.Entry<Integer, String>> mapNameCache = null;

    private static synchronized List<Map.Entry<Integer, String>> getMobNameList() {
        if (mobNameCache != null) return mobNameCache;

        List<Map.Entry<Integer, String>> list = new ArrayList<>();
        try {
            DataProvider stringProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
            Data mobData = stringProvider.getData("Mob.img");
            for (Data child : mobData.getChildren()) {
                try {
                    int id = Integer.parseInt(child.getName());
                    String name = DataTool.getString(child.getChildByPath("name"), "");
                    if (!name.isEmpty()) {
                        list.add(new AbstractMap.SimpleEntry<>(id, name));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        mobNameCache = list;
        return list;
    }

    private static synchronized List<Map.Entry<Integer, String>> getMapNameList() {
        if (mapNameCache != null) return mapNameCache;

        List<Map.Entry<Integer, String>> list = new ArrayList<>();
        try {
            DataProvider stringProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
            Data mapData = stringProvider.getData("Map.img");
            for (Data region : mapData.getChildren()) {
                for (Data mapEntry : region.getChildren()) {
                    try {
                        int id = Integer.parseInt(mapEntry.getName());
                        String name = DataTool.getString(mapEntry.getChildByPath("mapName"), "");
                        String streetName = DataTool.getString(mapEntry.getChildByPath("streetName"), "");
                        String fullName = name;
                        if (!streetName.isEmpty()) {
                            fullName = streetName + " : " + name;
                        }
                        if (!fullName.isEmpty()) {
                            list.add(new AbstractMap.SimpleEntry<>(id, fullName));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        mapNameCache = list;
        return list;
    }

    @Tag(name = "/common/" + ApiConstant.LATEST)
    @Operation(summary = "Search game data by keyword")
    @GetMapping("/" + ApiConstant.LATEST + "/informationSearch")
    public ResultBody<List<Map<String, Object>>> informationSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) List<String> types) {

        List<Map<String, Object>> results = new ArrayList<>();
        int maxResults = 200;
        String kw = keyword.toLowerCase();

        boolean searchAll = types == null || types.isEmpty();
        boolean searchItems = searchAll || types.stream().anyMatch(t ->
                List.of("cash","equipment","consume","etc","install","pet","item").contains(t.toLowerCase()));
        boolean searchMobs = searchAll || types.stream().anyMatch(t ->
                List.of("mob","monster").contains(t.toLowerCase()));

        if (searchItems) {
            for (var pair : ItemInformationProvider.getInstance().getAllItems()) {
                int id = pair.getLeft();
                String name = pair.getRight();
                if (name.toLowerCase().contains(kw) || String.valueOf(id).contains(kw)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "Item");
                    item.put("id", id);
                    item.put("name", name);
                    results.add(item);
                    if (results.size() >= maxResults) break;
                }
            }
        }

        if (results.size() < maxResults && searchMobs) {
            for (var entry : getMobNameList()) {
                int id = entry.getKey();
                String name = entry.getValue();
                if (name.toLowerCase().contains(kw) || String.valueOf(id).contains(kw)) {
                    Map<String, Object> mob = new LinkedHashMap<>();
                    mob.put("type", "Mob");
                    mob.put("id", id);
                    mob.put("name", name);
                    results.add(mob);
                    if (results.size() >= maxResults) break;
                }
            }
        }

        return ResultBody.success(results);
    }

    @Tag(name = "/common/" + ApiConstant.LATEST)
    @Operation(summary = "Search maps by name or ID (fuzzy)")
    @GetMapping("/" + ApiConstant.LATEST + "/mapSearch")
    public ResultBody<List<Map<String, Object>>> mapSearch(@RequestParam String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
        int maxResults = 50;
        String kw = keyword.toLowerCase().trim();

        if (kw.isEmpty()) {
            return ResultBody.error(400, "keyword is required");
        }

        for (var entry : getMapNameList()) {
            int id = entry.getKey();
            String name = entry.getValue();
            if (name.toLowerCase().contains(kw) || String.valueOf(id).contains(kw)) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", id);
                map.put("name", name);
                results.add(map);
                if (results.size() >= maxResults) break;
            }
        }

        return ResultBody.success(results);
    }
}
