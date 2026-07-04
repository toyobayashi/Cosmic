package api.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteAssetPathResolverTest {

    @Test
    void resolvesRelativeAssetPathUnderRoot() {
        RemoteAssetPathResolver resolver = new RemoteAssetPathResolver(Path.of("private-assets"));

        Path resolved = resolver.resolve("npc-images/sponsor/qrcode.png");

        assertEquals(Path.of("private-assets").toAbsolutePath().normalize().resolve("npc-images/sponsor/qrcode.png"), resolved);
    }

    @Test
    void rejectsPathTraversalAndWindowsSeparators() {
        RemoteAssetPathResolver resolver = new RemoteAssetPathResolver(Path.of("private-assets"));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("../secret.png"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("npc-images\\sponsor\\qrcode.png"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("C:/secret.png"));
    }
}
