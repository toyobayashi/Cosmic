package net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.jcip.annotations.NotThreadSafe;
import net.opcodes.SendOpcode;

import java.awt.*;
import java.nio.charset.Charset;

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
        byte[] bytes = value.getBytes(charset);
        writeShort(bytes.length);
        writeBytes(bytes);
    }

    @Override
    public void writeFixedString(String value) {
        writeBytes(value.getBytes(charset));
    }

    @Override
    public void writeFixedString(String value, int byteLength) {
        byte[] raw = value.getBytes(charset);
        byte[] fixed = new byte[byteLength];
        System.arraycopy(raw, 0, fixed, 0, Math.min(raw.length, Math.max(byteLength - 1, 0)));
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
}
