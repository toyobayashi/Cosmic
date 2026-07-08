package net.server.guild;

import client.Client;
import io.netty.buffer.Unpooled;
import net.packet.ByteBufInPacket;
import net.packet.Packet;
import net.packet.PerClientPacket;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildPacketsCharsetTest {
    private static final Charset JAPANESE_CHARSET = Charset.forName("windows-932");

    @Test
    void guildInviteUsesTargetClientCharsetForInviteeName() {
        ByteBufInPacket packet = packetReader(resolvePacket(GuildPackets.guildInvite(77, "汉"), japaneseClient()));

        packet.skip(2);
        assertEquals(0x05, packet.readByte());
        assertEquals(77, packet.readInt());
        assertEquals("?", packet.readString());
    }

    @Test
    void bbsThreadListUsesClientCharsetForThreadTitle() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.last()).thenReturn(true);
        when(rs.getRow()).thenReturn(1);
        when(rs.getInt("localthreadid")).thenReturn(1);
        when(rs.absolute(1)).thenReturn(true);
        when(rs.getInt("postercid")).thenReturn(77);
        when(rs.getString("name")).thenReturn("汉");
        when(rs.getLong("timestamp")).thenReturn(0L);
        when(rs.getInt("icon")).thenReturn(3);
        when(rs.getInt("replycount")).thenReturn(0);
        when(rs.next()).thenReturn(false);

        ByteBufInPacket packet = packetReader(GuildPackets.BBSThreadList(japaneseClient(), rs, 0));

        packet.skip(2);
        assertEquals(0x06, packet.readByte());
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(1, packet.readInt());
        assertEquals(1, packet.readInt());
        assertEquals(1, packet.readInt());
        assertEquals(77, packet.readInt());
        assertEquals("?", packet.readString());
    }

    @Test
    void showThreadUsesClientCharsetForTitleAndReplies() throws SQLException {
        ResultSet threadRS = mock(ResultSet.class);
        when(threadRS.getInt("postercid")).thenReturn(77);
        when(threadRS.getLong("timestamp")).thenReturn(0L);
        when(threadRS.getString("name")).thenReturn("汉");
        when(threadRS.getString("startpost")).thenReturn("汉汉汉汉");
        when(threadRS.getInt("icon")).thenReturn(2);
        when(threadRS.getInt("replycount")).thenReturn(1);

        ResultSet repliesRS = mock(ResultSet.class);
        when(repliesRS.next()).thenReturn(true, false);
        when(repliesRS.getInt("replyid")).thenReturn(9);
        when(repliesRS.getInt("postercid")).thenReturn(88);
        when(repliesRS.getLong("timestamp")).thenReturn(0L);
        when(repliesRS.getString("content")).thenReturn("汉汉汉汉");

        ByteBufInPacket packet = packetReader(GuildPackets.showThread(japaneseClient(), 5, threadRS, repliesRS));

        packet.skip(2);
        assertEquals(0x07, packet.readByte());
        assertEquals(5, packet.readInt());
        assertEquals(77, packet.readInt());
        packet.readLong();
        assertEquals("?", packet.readString());
        assertEquals("????", packet.readString());
        assertEquals(2, packet.readInt());
        assertEquals(1, packet.readInt());
        assertEquals(9, packet.readInt());
        assertEquals(88, packet.readInt());
        packet.readLong();
        assertEquals("????", packet.readString());
    }

    private static Client japaneseClient() {
        Client client = Client.createMock();
        client.setPacketCodePage(932);
        return client;
    }

    private static Packet resolvePacket(Packet packet, Client client) {
        if (packet instanceof PerClientPacket perClientPacket) {
            return perClientPacket.forClient(client);
        }
        return packet;
    }

    private static ByteBufInPacket packetReader(Packet packet) {
        return new ByteBufInPacket(Unpooled.wrappedBuffer(packet.getBytes()), JAPANESE_CHARSET);
    }
}
