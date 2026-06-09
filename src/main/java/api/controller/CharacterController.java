package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import client.Character;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.server.Server;
import net.server.world.World;
import org.springframework.web.bind.annotation.*;
import server.maps.MapFactory;
import server.maps.MapleMap;

import java.util.*;

@RestController
@RequestMapping("/api/character")
public class CharacterController {

    @Tag(name = "/character/" + ApiConstant.LATEST)
    @Operation(summary = "Character list by account ID or account name (fuzzy)")
    @GetMapping("/" + ApiConstant.LATEST + "/list")
    public ResultBody<List<Map<String, Object>>> characterList(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) String accountName) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (var handle = tools.DatabaseConnection.getHandle()) {
            if (accountName != null && !accountName.isBlank()) {
                result = handle.createQuery(
                        "SELECT c.id, c.name, c.level, c.job, c.world, c.gm, c.fame, c.meso, " +
                        "c.guildid, c.createdate, c.lastLogoutTime, c.accountid, a.name AS accountName " +
                        "FROM characters c JOIN accounts a ON c.accountid = a.id " +
                        "WHERE a.name LIKE ? ORDER BY c.id")
                        .bind(0, "%" + accountName.trim() + "%")
                        .mapToMap()
                        .list();
            } else if (accountId != null) {
                result = handle.createQuery(
                        "SELECT c.id, c.name, c.level, c.job, c.world, c.gm, c.fame, c.meso, " +
                        "c.guildid, c.createdate, c.lastLogoutTime, c.accountid, a.name AS accountName " +
                        "FROM characters c JOIN accounts a ON c.accountid = a.id " +
                        "WHERE c.accountid = ? ORDER BY c.id")
                        .bind(0, accountId)
                        .mapToMap()
                        .list();
            } else {
                return ResultBody.error(400, "accountId or accountName is required");
            }
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to get character list: " + e.getMessage());
        }
        return ResultBody.success(result);
    }

    @Tag(name = "/character/" + ApiConstant.LATEST)
    @Operation(summary = "Online characters")
    @GetMapping("/" + ApiConstant.LATEST + "/online")
    public ResultBody<List<Map<String, Object>>> onlineCharacters() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            for (var world : Server.getInstance().getWorlds()) {
                for (var chr : world.getPlayerStorage().getAllCharacters()) {
                    Map<String, Object> map = new HashMap<>();
                    int mapId = chr.getMapId();
                    map.put("id", chr.getId());
                    map.put("name", chr.getName());
                    map.put("level", chr.getLevel());
                    map.put("job", chr.getJob().getId());
                    map.put("world", world.getId());
                    map.put("mapId", mapId);
                    map.put("mapName", MapFactory.loadPlaceName(mapId));
                    result.add(map);
                }
            }
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to get online characters: " + e.getMessage());
        }
        return ResultBody.success(result);
    }

    @Tag(name = "/character/" + ApiConstant.LATEST)
    @Operation(summary = "Warp a character to a map")
    @PostMapping("/" + ApiConstant.LATEST + "/warp")
    public ResultBody<String> warpCharacter(@RequestBody Map<String, Object> body) {
        int characterId = (int) body.get("characterId");
        int mapId = (int) body.get("mapId");

        try {
            Character chr = null;
            for (var world : Server.getInstance().getWorlds()) {
                chr = world.getPlayerStorage().getCharacterById(characterId);
                if (chr != null) {
                    break;
                }
            }

            if (chr == null) {
                return ResultBody.error(404, "Character not online: " + characterId);
            }

            MapleMap target = chr.getClient().getChannelServer().getMapFactory().getMap(mapId);
            if (target == null) {
                return ResultBody.error(404, "Map not found: " + mapId);
            }

            chr.saveLocationOnWarp();
            chr.changeMap(target, target.getRandomPlayerSpawnpoint());
            return ResultBody.success("Character " + chr.getName() + " warped to map " + mapId);
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to warp character: " + e.getMessage());
        }
    }
}
