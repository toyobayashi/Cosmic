package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.server.Server;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/character")
public class CharacterController {

    @Tag(name = "/character/" + ApiConstant.LATEST)
    @Operation(summary = "Character list by account ID")
    @GetMapping("/" + ApiConstant.LATEST + "/list")
    public ResultBody<List<Map<String, Object>>> characterList(@RequestParam int accountId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (var handle = tools.DatabaseConnection.getHandle()) {
            result = handle.createQuery(
                    "SELECT c.id, c.name, c.level, c.job, c.world, c.gm, c.fame, c.meso, " +
                    "c.guildid, c.createdate, c.lastLogoutTime " +
                    "FROM characters c WHERE c.accountid = ? ORDER BY c.id")
                    .bind(0, accountId)
                    .mapToMap()
                    .list();
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
                    map.put("id", chr.getId());
                    map.put("name", chr.getName());
                    map.put("level", chr.getLevel());
                    map.put("job", chr.getJob().getId());
                    map.put("world", world.getId());
                    map.put("mapId", chr.getMapId());
                    result.add(map);
                }
            }
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to get online characters: " + e.getMessage());
        }
        return ResultBody.success(result);
    }
}
