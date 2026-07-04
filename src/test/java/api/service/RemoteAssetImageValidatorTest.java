package api.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteAssetImageValidatorTest {

    @Test
    void acceptsPngWithinNpcImageLimit() throws Exception {
        byte[] png = png(250, 250);

        assertDoesNotThrow(() -> RemoteAssetImageValidator.validatePng(png, 250, 250, 128 * 1024));
    }

    @Test
    void rejectsPngOverNpcImageLimit() throws Exception {
        byte[] png = png(251, 250);

        assertThrows(IllegalArgumentException.class, () -> RemoteAssetImageValidator.validatePng(png, 250, 250, 128 * 1024));
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
