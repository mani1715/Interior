package com.interior.platform.ai.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Keeps decoded source pixels outside the user's mask; never claims colour fidelity inside it. */
public final class PrecisionImageComposite {
    private PrecisionImageComposite() { }

    public static byte[] preserveUnselected(byte[] sourceBytes, byte[] generatedBytes, byte[] maskBytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourceBytes));
        BufferedImage generated = ImageIO.read(new ByteArrayInputStream(generatedBytes));
        BufferedImage mask = ImageIO.read(new ByteArrayInputStream(maskBytes));
        if (source == null || generated == null || mask == null) throw new IOException("Invalid precision edit image");
        int width = source.getWidth(), height = source.getHeight();
        if (generated.getWidth() != width || generated.getHeight() != height || mask.getWidth() != width || mask.getHeight() != height) {
            throw new IOException("Precision output dimensions differ from source; cannot safely align selected region");
        }
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = mask.getRGB(x, y);
                boolean selected = ((pixel >>> 24) & 255) > 20 && (pixel & 0xffffff) != 0;
                output.setRGB(x, y, selected ? generated.getRGB(x, y) : source.getRGB(x, y));
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(output, "png", bytes);
        return bytes.toByteArray();
    }
}
