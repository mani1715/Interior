package com.interior.platform.media.service;

import com.interior.platform.media.domain.DerivativeVariant;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.StudioWatermarkSettingsRecord;
import com.interior.platform.media.domain.WatermarkPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    public record ImageDimensions(int width, int height, String format) {}

    public record ProcessedDerivative(
            byte[] content,
            int width,
            int height,
            String format,
            long fileSize,
            boolean isWatermarked
    ) {}

    public ImageDimensions validateAndGetDimensions(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image format");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid image dimensions: " + width + "x" + height);
            }
            return new ImageDimensions(width, height, "jpg");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode image: " + e.getMessage(), e);
        }
    }

    public ProcessedDerivative createDerivative(
            byte[] originalBytes,
            DerivativeVariant variant,
            StudioWatermarkSettingsRecord watermarkSettings,
            boolean watermarkEnabled,
            MediaType mediaType
    ) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                throw new IllegalArgumentException("Failed to read image for derivative generation");
            }

            // Calculate scaled dimensions preserving aspect ratio
            int origW = original.getWidth();
            int origH = original.getHeight();
            double scale = Math.min((double) variant.getMaxWidth() / origW, (double) variant.getMaxHeight() / origH);
            if (scale > 1.0) {
                scale = 1.0; // Do not upscale smaller images
            }
            int targetW = Math.max(1, (int) Math.round(origW * scale));
            int targetH = Math.max(1, (int) Math.round(origH * scale));

            // Create target image (RGB to strip alpha/transparency issues with JPEG export)
            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Draw background white (for transparent PNG conversion)
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetW, targetH);
                g2d.drawImage(original, 0, 0, targetW, targetH, null);

                boolean appliedWatermark = false;

                // 1. Apply Studio Watermark if requested
                if (watermarkEnabled && watermarkSettings != null && watermarkSettings.enabled() && mediaType.isPublicEligible()) {
                    applyStudioWatermark(g2d, targetW, targetH, watermarkSettings);
                    appliedWatermark = true;
                }

                // 2. Apply Mandatory AI Concept Badge if AI_CONCEPT
                if (mediaType == MediaType.AI_CONCEPT) {
                    applyAiConceptBadge(g2d, targetW, targetH);
                    appliedWatermark = true;
                }

                // Convert to JPEG bytes (EXIF is completely stripped)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "jpg", baos);
                byte[] outputBytes = baos.toByteArray();

                return new ProcessedDerivative(
                        outputBytes,
                        targetW,
                        targetH,
                        "jpg",
                        outputBytes.length,
                        appliedWatermark
                );
            } finally {
                g2d.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate image derivative: " + e.getMessage(), e);
        }
    }

    private void applyStudioWatermark(
            Graphics2D g2d,
            int targetW,
            int targetH,
            StudioWatermarkSettingsRecord settings
    ) {
        String watermarkText = settings.fallbackText();
        if (watermarkText == null || watermarkText.trim().isEmpty()) {
            watermarkText = "Interior Studio";
        }
        watermarkText = "© " + watermarkText.trim();

        float opacity = settings.opacity() != null ? settings.opacity().floatValue() : 0.60f;
        opacity = Math.max(0.10f, Math.min(1.00f, opacity));

        // Font size proportional to image width
        int fontSize = Math.max(11, Math.min(36, targetW * settings.sizePercentage() / 250));
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        int textW = fm.stringWidth(watermarkText);
        int textH = fm.getAscent();

        int paddingX = 10;
        int paddingY = 6;
        int badgeW = textW + (paddingX * 2);
        int badgeH = textH + (paddingY * 2);
        int margin = 16;

        int badgeX;
        int badgeY;

        WatermarkPosition pos = settings.position() != null ? settings.position() : WatermarkPosition.BOTTOM_RIGHT;
        switch (pos) {
            case TOP_LEFT -> {
                badgeX = margin;
                badgeY = margin;
            }
            case TOP_RIGHT -> {
                badgeX = targetW - badgeW - margin;
                badgeY = margin;
            }
            case BOTTOM_LEFT -> {
                badgeX = margin;
                badgeY = targetH - badgeH - margin;
            }
            case CENTER -> {
                badgeX = (targetW - badgeW) / 2;
                badgeY = (targetH - badgeH) / 2;
            }
            case BOTTOM_RIGHT -> {
                badgeX = targetW - badgeW - margin;
                badgeY = targetH - badgeH - margin;
            }
            default -> {
                badgeX = targetW - badgeW - margin;
                badgeY = targetH - badgeH - margin;
            }
        }

        // Composite for watermark
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // Draw translucent pill
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, badgeH, 8, 8));

        // Draw crisp text
        g2d.setColor(Color.WHITE);
        g2d.drawString(watermarkText, badgeX + paddingX, badgeY + paddingY + textH - 2);

        g2d.setComposite(originalComposite);
    }

    private void applyAiConceptBadge(Graphics2D g2d, int targetW, int targetH) {
        String badgeText = "✦ AI Concept Visualization";
        int fontSize = Math.max(10, Math.min(22, targetW / 45));
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        int textW = fm.stringWidth(badgeText);
        int textH = fm.getAscent();

        int paddingX = 10;
        int paddingY = 6;
        int badgeW = textW + (paddingX * 2);
        int badgeH = textH + (paddingY * 2);
        int margin = 16;

        // Position AI badge at top-left
        int badgeX = margin;
        int badgeY = margin;

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));

        // Dark charcoal rounded badge with subtle bronze border
        g2d.setColor(new Color(20, 20, 20, 220));
        g2d.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, badgeH, 6, 6));

        g2d.setColor(new Color(184, 138, 90, 240)); // Warm bronze border
        g2d.draw(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, badgeH, 6, 6));

        // White text
        g2d.setColor(Color.WHITE);
        g2d.drawString(badgeText, badgeX + paddingX, badgeY + paddingY + textH - 2);

        g2d.setComposite(originalComposite);
    }
}
