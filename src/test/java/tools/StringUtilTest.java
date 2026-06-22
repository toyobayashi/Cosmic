package tools;

import constants.string.CharsetConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilTest {

    @Test
    void rightPaddingUsesEncodedByteLength() {
        String padded = StringUtil.getRightPaddedStr("测试", '\0', 13);

        assertEquals(13, padded.getBytes(CharsetConstants.CHARSET).length);
    }
}
