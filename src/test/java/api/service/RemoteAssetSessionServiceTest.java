package api.service;

import client.Client;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteAssetSessionServiceTest {

    @Test
    void validatesSessionBoundToClientIdentity() {
        RemoteAssetSessionService service = new RemoteAssetSessionService("http://127.0.0.1:8686/remote-assets", 900);
        Client client = Client.createMock();
        client.setAccID(100);
        client.setAccountName("toyobayashi");

        RemoteAssetSession session = service.issueSession(client, 2000);

        assertTrue(service.isValid(session.session(), client.getRemoteAddress(), client.getAccID(), client.getSessionId()));
        assertFalse(service.isValid(session.session(), "other-host", client.getAccID(), client.getSessionId()));
        assertFalse(service.isValid(session.session(), client.getRemoteAddress(), 101, client.getSessionId()));
        assertFalse(service.isValid(session.session(), client.getRemoteAddress(), client.getAccID(), 9999));
    }

    @Test
    void rejectsExpiredSession() {
        RemoteAssetSessionService service = new RemoteAssetSessionService("http://127.0.0.1:8686/remote-assets", 0);
        Client client = Client.createMock();
        client.setAccID(100);

        RemoteAssetSession session = service.issueSession(client, 1);

        assertFalse(service.isValid(session.session(), client.getRemoteAddress(), client.getAccID(), client.getSessionId()));
    }
}
