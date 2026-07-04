package api.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class RemoteAssetImageValidator {
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public static void validatePng(byte[] data, int maxWidth, int maxHeight, int maxBytes) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("remote asset image is empty");
        }
        if (data.length > maxBytes) {
            throw new IllegalArgumentException("remote asset image is too large");
        }
        if (!hasPngSignature(data)) {
            throw new IllegalArgumentException("remote asset image must be PNG");
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new IllegalArgumentException("remote asset image could not be decoded");
        }
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            throw new IllegalArgumentException("remote asset image dimensions exceed " + maxWidth + "x" + maxHeight);
        }
    }

    private static boolean hasPngSignature(byte[] data) {
        if (data.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }
}
