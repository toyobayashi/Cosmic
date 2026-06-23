package net.packet;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PacketCharsetsTest {
    @Test
    void resolvesWindowsCodePage() {
        assertEquals(Charset.forName("windows-936"), PacketCharsets.forWindowsCodePage(936));
        assertEquals(Charset.forName("windows-932"), PacketCharsets.forWindowsCodePage(932));
        assertEquals(Charset.forName("IBM437"), PacketCharsets.forWindowsCodePage(437));
    }

    @Test
    void rejectsUnsupportedCodePage() {
        assertFalse(PacketCharsets.isSupportedWindowsCodePage(-1));
        assertFalse(PacketCharsets.isSupportedWindowsCodePage(65001));
    }
}
