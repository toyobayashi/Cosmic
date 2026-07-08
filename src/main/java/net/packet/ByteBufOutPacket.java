package net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.jcip.annotations.NotThreadSafe;
import net.opcodes.SendOpcode;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

@NotThreadSafe
public class ByteBufOutPacket implements OutPacket {
    private final ByteBuf byteBuf;
    private final Charset charset;

    public ByteBufOutPacket() {
        this(Unpooled.buffer(), PacketCharsets.DEFAULT_CHARSET);
    }

    public ByteBufOutPacket(Charset charset) {
        this(Unpooled.buffer(), charset);
    }

    public ByteBufOutPacket(SendOpcode op) {
        this(op, PacketCharsets.DEFAULT_CHARSET);
    }

    public ByteBufOutPacket(SendOpcode op, Charset charset) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShortLE((short) op.getValue());
        this.byteBuf = byteBuf;
        this.charset = charset;
    }

    public ByteBufOutPacket(SendOpcode op, int initialCapacity) {
        this(op, initialCapacity, PacketCharsets.DEFAULT_CHARSET);
    }

    public ByteBufOutPacket(SendOpcode op, int initialCapacity, Charset charset) {
        ByteBuf byteBuf = Unpooled.buffer(initialCapacity);
        byteBuf.writeShortLE((short) op.getValue());
        this.byteBuf = byteBuf;
        this.charset = charset;
    }

    private ByteBufOutPacket(ByteBuf byteBuf, Charset charset) {
        this.byteBuf = byteBuf;
        this.charset = charset;
    }

    @Override
    public byte[] getBytes() {
        return ByteBufUtil.getBytes(byteBuf);
    }

    @Override
    public void writeByte(byte value) {
        byteBuf.writeByte(value);
    }

    @Override
    public void writeByte(int value) {
        writeByte((byte) value);
    }

    @Override
    public void writeBytes(byte[] value) {
        byteBuf.writeBytes(value);
    }

    @Override
    public void writeShort(int value) {
        byteBuf.writeShortLE(value);
    }

    @Override
    public void writeInt(int value) {
        byteBuf.writeIntLE(value);
    }

    @Override
    public void writeLong(long value) {
        byteBuf.writeLongLE(value);
    }

    @Override
    public void writeBool(boolean value) {
        byteBuf.writeByte(value ? 1 : 0);
    }

    @Override
    public void writeString(String value) {
        byte[] bytes = encodeString(value);
        writeShort(bytes.length);
        writeBytes(bytes);
    }

    @Override
    public void writeFixedString(String value) {
        writeBytes(encodeString(value));
    }

    @Override
    public void writeFixedString(String value, int byteLength) {
        int maxPayloadLength = Math.max(byteLength - 1, 0);
        int payloadLength = 0;
        StringBuilder payload = new StringBuilder();

        for (int i = 0; i < value.length();) {
            int codePoint = value.codePointAt(i);
            String next = new String(Character.toChars(codePoint));
            int nextEncodedLength = encodeString(next).length;

            if (payloadLength + nextEncodedLength > maxPayloadLength) {
                break;
            }

            payloadLength += nextEncodedLength;
            payload.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }

        byte[] raw = encodeString(payload.toString());
        byte[] fixed = new byte[byteLength];
        System.arraycopy(raw, 0, fixed, 0, Math.min(raw.length, maxPayloadLength));
        writeBytes(fixed);
    }

    @Override
    public void writePos(Point value) {
        writeShort((short) value.getX());
        writeShort((short) value.getY());
    }

    @Override
    public void skip(int numberOfBytes) {
        writeBytes(new byte[numberOfBytes]);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ByteBufOutPacket other && byteBuf.equals(other.byteBuf);
    }

    private byte[] encodeString(String value) {
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            ByteBuffer encoded = encoder.encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException e) {
            return value.getBytes(charset);
        }
    }
}
