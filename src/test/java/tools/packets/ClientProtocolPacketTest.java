package tools.packets;

import config.YamlConfig;
import io.netty.buffer.Unpooled;
import net.opcodes.SendOpcode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.PacketCreator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientProtocolPacketTest {

    @BeforeAll
    static void loadConfig() {
        YamlConfig.load();
    }

    @Test
    void expTablePacketCarriesServerTable() {
        byte[] bytes = PacketCreator.clientExpTable().getBytes();
        var packet = Unpooled.wrappedBuffer(bytes);

        assertEquals(SendOpcode.CLIENT_EXT.getValue(), packet.readUnsignedShortLE());
        assertEquals(PacketCreator.CLIENT_EXP_TABLE_MAGIC, packet.readIntLE());
        assertEquals(1, packet.readUnsignedByte());
        assertEquals(201, packet.readUnsignedShortLE());
        assertEquals(15, packet.readIntLE());
    }
}
