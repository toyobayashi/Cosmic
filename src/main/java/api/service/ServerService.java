package api.service;

import api.model.dto.ChannelListRtnDTO;
import api.model.dto.WorldListRtnDTO;
import config.YamlConfig;
import net.server.Server;
import net.server.world.World;
import net.server.PlayerStorage;

import java.util.ArrayList;
import java.util.List;

public class ServerService {

    public boolean isOnline() {
        return Server.getInstance().isOnline();
    }

    public List<WorldListRtnDTO> worldList() {
        List<WorldListRtnDTO> result = new ArrayList<>();
        List<World> worlds = Server.getInstance().getWorlds();
        if (worlds == null || worlds.isEmpty()) return result;

        String[] worldNames = {
            "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan",
            "Mardia", "Kradia", "Yellonde", "Demethos", "Galicia",
            "El Nido", "Zenith", "Arcenia", "Kastia", "Judis",
            "Plana", "Kalluna", "Stius", "Croa", "Medere"
        };
        var worldConfigs = YamlConfig.config.worlds;

        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);
            if (world == null) continue;
            WorldListRtnDTO dto = new WorldListRtnDTO();
            dto.setId(i);
            dto.setName(i < worldNames.length ? worldNames[i] : "World " + i);

            if (i < worldConfigs.size()) {
                var cfg = worldConfigs.get(i);
                dto.setEventMessage(cfg.event_message != null ? cfg.event_message : "");
                dto.setFlag(cfg.flag != 0 ? cfg.flag : 0);
            }

            int channelCount = world.getChannels() != null ? world.getChannels().size() : 0;
            dto.setChannels(channelCount);
            dto.setChannelList(channelListForWorld(i, world));
            result.add(dto);
        }
        return result;
    }

    private List<ChannelListRtnDTO> channelListForWorld(int worldId, World world) {
        List<ChannelListRtnDTO> result = new ArrayList<>();
        if (world == null || world.getChannels() == null) return result;

        PlayerStorage ps = world.getPlayerStorage();
        int playerCount = ps != null ? ps.getAllCharacters().size() : 0;

        for (int i = 0; i < world.getChannels().size(); i++) {
            ChannelListRtnDTO dto = new ChannelListRtnDTO();
            dto.setWorldId(worldId);
            dto.setChannelId(i + 1);
            dto.setOnlineCount(playerCount);
            result.add(dto);
        }
        return result;
    }

    public List<ChannelListRtnDTO> channelList(int worldId) {
        List<ChannelListRtnDTO> result = new ArrayList<>();
        List<World> worlds = Server.getInstance().getWorlds();
        if (worlds == null || worldId < 0 || worldId >= worlds.size()) return result;

        World world = worlds.get(worldId);
        return channelListForWorld(worldId, world);
    }
}
