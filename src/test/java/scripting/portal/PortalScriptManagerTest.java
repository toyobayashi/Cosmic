package scripting.portal;

import client.Client;
import org.junit.jupiter.api.Test;
import server.maps.GenericPortal;
import server.maps.Portal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalScriptManagerTest {
    @Test
    void passesPortalInteractionAsLegacyEnterArgument() throws IOException {
        Path script = Path.of("scripts", "portal", "codex_legacy_portal_argument_test.js");
        Files.writeString(script, """
                function enter(argument) {
                    if (argument === undefined) {
                        throw new Error('missing legacy portal argument');
                    }
                    if (argument !== pi) {
                        throw new Error('legacy argument should match pi global');
                    }
                    return true;
                }
                """, StandardCharsets.UTF_8);

        PortalScriptManager manager = PortalScriptManager.getInstance();
        try {
            Portal portal = new GenericPortal(Portal.TELEPORT_PORTAL);
            portal.setScriptName("codex_legacy_portal_argument_test");

            assertTrue(manager.executePortalScript(portal, Client.createMock()));
            assertTrue(manager.executePortalScript(portal, Client.createMock()));
        } finally {
            manager.reloadPortalScripts();
            Files.deleteIfExists(script);
        }
    }
}
