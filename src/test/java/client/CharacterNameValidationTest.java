package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterNameValidationTest {

    @Test
    void acceptsChineseCharacterNames() {
        assertTrue(Character.isValidNewCharacterName("测试"));
        assertTrue(Character.isValidNewCharacterName("小明123"));
    }

    @Test
    void preservesExistingAsciiCharacterNameRules() {
        assertTrue(Character.isValidNewCharacterName("Test123"));
        assertFalse(Character.isValidNewCharacterName("ab"));
        assertFalse(Character.isValidNewCharacterName("name_with_symbol"));
    }

    @Test
    void rejectsNamesThatExceedClientFixedNameBytes() {
        assertFalse(Character.isValidNewCharacterName("一二三四五六七"));
    }
}
