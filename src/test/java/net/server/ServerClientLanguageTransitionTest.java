package net.server;

import client.Client;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerClientLanguageTransitionTest {

    @Test
    void carriesClientLanguageAcrossChannelTransition() {
        Client client = Client.createMock();
        client.setClientLanguage("zh-CN");

        int charId = 987654321;
        Server.getInstance().setCharacteridInTransition(client, charId);

        assertEquals("zhCN", Server.getInstance().takeCharacterClientLanguageInTransition(charId));
    }
}
