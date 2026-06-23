package constants.string;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharsetConstantsTest {
    @Test
    void serverInternalCharsetIsUtf8() {
        assertEquals(StandardCharsets.UTF_8, CharsetConstants.CHARSET);
    }
}
