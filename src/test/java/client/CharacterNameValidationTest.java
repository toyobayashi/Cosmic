package client;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterNameValidationTest {

    @Test
    void validatesChineseCharacterNamesAccordingToClientCharset() {
        Charset charset = Charset.forName("windows-936");

        assertEquals(canBeValidCharacterName("测试", charset), Character.isValidNewCharacterName("测试", charset));
        assertEquals(canBeValidCharacterName("小明123", charset), Character.isValidNewCharacterName("小明123", charset));
    }

    @Test
    void preservesExistingAsciiCharacterNameRules() {
        Charset charset = Charset.forName("windows-1252");

        assertTrue(Character.isValidNewCharacterName("Test123", charset));
        assertFalse(Character.isValidNewCharacterName("ab", charset));
        assertFalse(Character.isValidNewCharacterName("name_with_symbol", charset));
    }

    @Test
    void rejectsNamesThatExceedClientFixedNameBytes() {
        Charset charset = Charset.forName("windows-936");

        assertFalse(Character.isValidNewCharacterName(overlongNameFor(charset), charset));
    }

    @Test
    void rejectsNamesThatClientCharsetCannotEncode() {
        assertFalse(Character.isValidNewCharacterName("小明123", Charset.forName("windows-1252")));
    }

    private static boolean canBeValidCharacterName(String name, Charset charset) {
        return charset.newEncoder().canEncode(name)
                && name.getBytes(charset).length >= 3
                && name.getBytes(charset).length <= 12;
    }

    private static String overlongNameFor(Charset charset) {
        if (charset.newEncoder().canEncode("一二三四五六七")) {
            return "一二三四五六七";
        }
        return "abcdefghijklm";
    }
}
