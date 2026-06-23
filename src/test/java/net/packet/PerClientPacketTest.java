package net.packet;

import client.Client;
import org.junit.jupiter.api.Test;
import tools.PacketCreator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerClientPacketTest {
    @Test
    void serverNoticeIsEncodedForRecipientCodePage() {
        Packet packet = PacketCreator.serverNotice(5, "中文");
        assertTrue(packet instanceof PerClientPacket);

        Client chineseClient = Client.createMock();
        chineseClient.setPacketCodePage(936);
        Client japaneseClient = Client.createMock();
        japaneseClient.setPacketCodePage(932);

        byte[] chineseBytes = ((PerClientPacket) packet).forClient(chineseClient).getBytes();
        byte[] japaneseBytes = ((PerClientPacket) packet).forClient(japaneseClient).getBytes();

        assertArrayEquals(new byte[]{
                (byte) 0xD6, (byte) 0xD0, (byte) 0xCE, (byte) 0xC4
        }, new byte[]{
                chineseBytes[5], chineseBytes[6], chineseBytes[7], chineseBytes[8]
        });
        assertNotEquals(chineseBytes[5], japaneseBytes[5]);
    }
}
