package client;

import constants.string.CharsetConstants;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterNameValidationTest {

    @Test
    void validatesChineseCharacterNamesAccordingToConfiguredCharset() {
        assertEquals(canBeValidCharacterName("测试"), Character.isValidNewCharacterName("测试"));
        assertEquals(canBeValidCharacterName("小明123"), Character.isValidNewCharacterName("小明123"));
    }

    @Test
    void preservesExistingAsciiCharacterNameRules() {
        assertTrue(Character.isValidNewCharacterName("Test123"));
        assertFalse(Character.isValidNewCharacterName("ab"));
        assertFalse(Character.isValidNewCharacterName("name_with_symbol"));
    }

    @Test
    void rejectsNamesThatExceedClientFixedNameBytes() {
        assertFalse(Character.isValidNewCharacterName(overlongNameFor(CharsetConstants.CHARSET)));
    }

    @Test
    void rejectsNamesThatConfiguredCharsetCannotEncode() {
        if (!CharsetConstants.CHARSET.newEncoder().canEncode("小明123")) {
            assertFalse(Character.isValidNewCharacterName("小明123"));
        }
    }

    private static boolean canBeValidCharacterName(String name) {
        return CharsetConstants.CHARSET.newEncoder().canEncode(name)
                && name.getBytes(CharsetConstants.CHARSET).length >= 3
                && name.getBytes(CharsetConstants.CHARSET).length <= 12;
    }

    private static String overlongNameFor(Charset charset) {
        if (charset.newEncoder().canEncode("一二三四五六七")) {
            return "一二三四五六七";
        }
        return "abcdefghijklm";
    }
}
