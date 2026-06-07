package api.model.dto;

import java.util.List;

public class ChannelListRtnDTO {
    private int worldId;
    private int channelId;
    private int onlineCount;

    public ChannelListRtnDTO() {}

    public ChannelListRtnDTO(int worldId, int channelId, int onlineCount) {
        this.worldId = worldId;
        this.channelId = channelId;
        this.onlineCount = onlineCount;
    }

    public int getWorldId() { return worldId; }
    public void setWorldId(int worldId) { this.worldId = worldId; }
    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }
}
