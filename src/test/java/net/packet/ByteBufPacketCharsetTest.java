package net.packet;

import net.opcodes.SendOpcode;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufPacketCharsetTest {
    @Test
    void fixedStringUsesExactByteLengthWithChineseCodePage() {
        OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS, Charset.forName("windows-936"));
        p.writeFixedString("中文名管", 13);
        byte[] bytes = p.getBytes();

        assertEquals(15, bytes.length);
        assertEquals(0, bytes[14]);
    }

    @Test
    void fixedStringUsesExactByteLengthWithJapaneseCodePage() {
        OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS, Charset.forName("windows-932"));
        p.writeFixedString("テスト名前", 13);
        byte[] bytes = p.getBytes();

        assertEquals(15, bytes.length);
        assertEquals(0, bytes[14]);
    }

    @Test
    void stringRoundTripsWithPacketCharset() {
        ByteBufOutPacket out = new ByteBufOutPacket(Charset.forName("windows-936"));
        out.writeString("你好");

        ByteBufInPacket in = new ByteBufInPacket(io.netty.buffer.Unpooled.wrappedBuffer(out.getBytes()), Charset.forName("windows-936"));

        assertEquals("你好", in.readString());
    }

    @Test
    void fixedStringTruncatesBeforeTerminatorByte() {
        OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS, Charset.forName("windows-936"));
        p.writeFixedString("中文名字管够", 13);
        byte[] bytes = p.getBytes();
        byte[] fixed = new byte[13];
        System.arraycopy(bytes, 2, fixed, 0, fixed.length);

        assertArrayEquals(new byte[]{
                (byte) 0xD6, (byte) 0xD0, (byte) 0xCE, (byte) 0xC4,
                (byte) 0xC3, (byte) 0xFB, (byte) 0xD7, (byte) 0xD6,
                (byte) 0xB9, (byte) 0xDC, (byte) 0xB9, (byte) 0xBB,
                0
        }, fixed);
    }
}
