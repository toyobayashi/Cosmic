package net.server.handlers;

import client.Client;
import net.PacketHandler;
import net.packet.InPacket;
import net.packet.PacketCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

public final class ClientHelloHandler implements PacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ClientHelloHandler.class);

    public static final int MAGIC = 0x4B544B43;
    public static final int VERSION = 1;
    public static final int CAPABILITY_EXP_TABLE = 0x1;

    @Override
    public boolean validateState(Client c) {
        return true;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (p.available() < 11) {
            log.debug("Client hello ignored: packet too short remote={} available={}", c.getRemoteAddress(), p.available());
            return;
        }

        int magic = p.readInt();
        int version = Byte.toUnsignedInt(p.readByte());
        int capabilities = p.readInt();
        int codePage = Short.toUnsignedInt(p.readShort());
        if (magic != MAGIC || version != VERSION) {
            log.debug("Client hello ignored: remote={} magic=0x{} version={}",
                    c.getRemoteAddress(),
                    Integer.toHexString(magic).toUpperCase(),
                    version);
            return;
        }

        boolean supportedCodePage = PacketCharsets.isSupportedWindowsCodePage(codePage);
        c.setExtendedClient(true);
        c.setClientCapabilities(capabilities);
        c.setPacketCodePage(supportedCodePage
                ? codePage
                : PacketCharsets.DEFAULT_CODEPAGE);
        if (!supportedCodePage) {
            log.warn("Client hello used unsupported codepage: remote={} codepage={} effectiveCodepage={}",
                    c.getRemoteAddress(),
                    codePage,
                    c.getPacketCodePage());
        }

        if (c.hasClientCapability(CAPABILITY_EXP_TABLE)) {
            c.sendPacket(PacketCreator.clientExpTable());
        }
    }
}
