package api.model.dto;

import java.util.List;

public class WorldListRtnDTO {
    private int id;
    private String name;
    private String eventMessage;
    private int flag;
    private int channels;
    private List<ChannelListRtnDTO> channelList;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEventMessage() { return eventMessage; }
    public void setEventMessage(String eventMessage) { this.eventMessage = eventMessage; }
    public int getFlag() { return flag; }
    public void setFlag(int flag) { this.flag = flag; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public List<ChannelListRtnDTO> getChannelList() { return channelList; }
    public void setChannelList(List<ChannelListRtnDTO> channelList) { this.channelList = channelList; }
}
