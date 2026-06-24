package net.server.handlers;

import client.Client;
import config.YamlConfig;
import net.packet.InPacket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutil.Packets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHelloHandlerTest {

    @BeforeAll
    static void loadConfig() {
        YamlConfig.load();
    }

    @Test
    void setsClientCodePageAndCapabilities() {
        Client client = Client.createMock();
        InPacket packet = Packets.buildInPacket(p -> {
            p.writeInt(ClientHelloHandler.MAGIC);
            p.writeByte(ClientHelloHandler.VERSION);
            p.writeInt(0);
            p.writeShort(932);
        });

        new ClientHelloHandler().handlePacket(packet, client);

        assertTrue(client.isExtendedClient());
        assertEquals(932, client.getPacketCodePage());
        assertEquals(0, client.getClientCapabilities());
    }
}
