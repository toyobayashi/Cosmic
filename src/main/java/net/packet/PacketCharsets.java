package net.packet;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

public final class PacketCharsets {
    public static final int DEFAULT_CODEPAGE = 936;
    public static final Charset DEFAULT_CHARSET = Charset.forName("windows-" + DEFAULT_CODEPAGE);

    private static final Map<Integer, String> SUPPORTED_CODEPAGES = Map.ofEntries(
            Map.entry(437, "IBM437"),
            Map.entry(932, "windows-932"),
            Map.entry(936, "windows-936"),
            Map.entry(949, "windows-949"),
            Map.entry(950, "windows-950"),
            Map.entry(1250, "windows-1250"),
            Map.entry(1251, "windows-1251"),
            Map.entry(1252, "windows-1252"),
            Map.entry(1253, "windows-1253"),
            Map.entry(1254, "windows-1254"),
            Map.entry(1255, "windows-1255"),
            Map.entry(1256, "windows-1256"),
            Map.entry(1257, "windows-1257"),
            Map.entry(1258, "windows-1258")
    );

    private PacketCharsets() {
    }

    public static boolean isSupportedWindowsCodePage(int codePage) {
        String charsetName = SUPPORTED_CODEPAGES.get(codePage);
        if (charsetName == null) {
            return false;
        }
        try {
            Charset.forName(charsetName);
            return true;
        } catch (UnsupportedCharsetException ex) {
            return false;
        }
    }

    public static Charset forWindowsCodePage(int codePage) {
        if (!isSupportedWindowsCodePage(codePage)) {
            return DEFAULT_CHARSET;
        }
        return Charset.forName(SUPPORTED_CODEPAGES.get(codePage));
    }
}
