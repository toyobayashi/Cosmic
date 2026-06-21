package net.server.channel.handlers;

import io.netty.buffer.Unpooled;
import net.packet.ByteBufInPacket;
import net.packet.InPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialMoveHandlerTest {

    @Test
    void skipSuperGmHealExtraBytes_shouldSkipAvailableBytesForShortPacket() {
        InPacket packet = new ByteBufInPacket(Unpooled.wrappedBuffer(new byte[]{
                0x01, 0x01, 0x00, 0x00, 0x00, 0x58, 0x02
        }));

        SpecialMoveHandler.skipSuperGmHealExtraBytes(packet);

        assertEquals(0, packet.available());
    }

    @Test
    void skipSuperGmHealExtraBytes_shouldKeepPositionBytesForLongPacket() {
        InPacket packet = new ByteBufInPacket(Unpooled.wrappedBuffer(new byte[]{
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00,
                0x58, 0x02, 0x20, 0x03, 0x01
        }));

        SpecialMoveHandler.skipSuperGmHealExtraBytes(packet);

        assertEquals(5, packet.available());
        assertEquals(600, packet.readShort());
        assertEquals(800, packet.readShort());
        assertEquals(1, packet.available());
    }
}
