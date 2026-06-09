package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.dto.ChannelListRtnDTO;
import api.model.dto.WorldListRtnDTO;
import api.service.ServerService;
import constants.net.ServerConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.server.Server;
import scripting.event.EventScriptManager;
import scripting.portal.PortalScriptManager;
import scripting.map.MapScriptManager;
import server.life.MonsterInformationProvider;
import server.ShopFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerController {

    private static final ServerService serverService = new ServerService();

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Query server online status")
    @GetMapping("/" + ApiConstant.LATEST + "/online")
    public ResultBody<Boolean> online() {
        return ResultBody.success(Server.getInstance().isOnline());
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "World list")
    @GetMapping("/" + ApiConstant.LATEST + "/world/list")
    public ResultBody<List<WorldListRtnDTO>> worldList() {
        return ResultBody.success(serverService.worldList());
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Channel list")
    @GetMapping("/" + ApiConstant.LATEST + "/channel/list")
    public ResultBody<List<ChannelListRtnDTO>> channelList(@RequestParam int worldId) {
        return ResultBody.success(serverService.channelList(worldId));
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Online player count")
    @GetMapping("/" + ApiConstant.LATEST + "/playerCount")
    public ResultBody<Integer> playerCount() {
        int count = 0;
        try {
            for (var world : Server.getInstance().getWorlds()) {
                count += world.getPlayerStorage().getAllCharacters().size();
            }
        } catch (Exception ignored) {}
        return ResultBody.success(count);
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Server version")
    @GetMapping("/" + ApiConstant.LATEST + "/version")
    public ResultBody<String> version() {
        return ResultBody.success(ServerConstants.VERSION + "");
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Total account count")
    @GetMapping("/" + ApiConstant.LATEST + "/accountCount")
    public ResultBody<Long> accountCount() {
        long count = 0;
        try (var handle = tools.DatabaseConnection.getHandle()) {
            count = handle.createQuery("SELECT COUNT(*) FROM accounts")
                    .mapTo(Long.class).one();
        } catch (Exception ignored) {}
        return ResultBody.success(count);
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Reload events")
    @PostMapping("/" + ApiConstant.LATEST + "/reload/events")
    public ResultBody<Object> reloadEvents() {
        for (var ch : Server.getInstance().getAllChannels()) {
            ch.reloadEventScriptManager();
        }
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Reload portals")
    @PostMapping("/" + ApiConstant.LATEST + "/reload/portals")
    public ResultBody<Object> reloadPortals() {
        PortalScriptManager.getInstance().reloadPortalScripts();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Reload maps")
    @PostMapping("/" + ApiConstant.LATEST + "/reload/maps")
    public ResultBody<Object> reloadMaps() {
        MapScriptManager.getInstance().reloadScripts();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Reload drops")
    @PostMapping("/" + ApiConstant.LATEST + "/reload/drops")
    public ResultBody<Object> reloadDrops() {
        MonsterInformationProvider.getInstance().clearDrops();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Reload shops")
    @PostMapping("/" + ApiConstant.LATEST + "/reload/shops")
    public ResultBody<Object> reloadShops() {
        ShopFactory.getInstance().reloadShops();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Stop server")
    @PostMapping("/" + ApiConstant.LATEST + "/stopServer")
    public ResultBody<Object> stopServer() {
        Server.getInstance().stopGameServer();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Start server")
    @PostMapping("/" + ApiConstant.LATEST + "/startServer")
    public ResultBody<Object> startServer() {
        if (!Server.getInstance().isOnline()) {
            Server.getInstance().init();
        }
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Restart server")
    @PostMapping("/" + ApiConstant.LATEST + "/restartServer")
    public ResultBody<Object> restartServer() {
        new Thread(() -> Server.getInstance().restartGameServer()).start();
        return ResultBody.success();
    }

    @Tag(name = "/server/" + ApiConstant.LATEST)
    @Operation(summary = "Shutdown server and exit")
    @PostMapping("/" + ApiConstant.LATEST + "/shutdown")
    public void shutdown() {
        System.exit(0);
    }
}
