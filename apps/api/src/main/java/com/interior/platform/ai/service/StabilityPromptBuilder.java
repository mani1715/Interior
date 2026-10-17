package com.interior.platform.ai.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StabilityPromptBuilder {

    private static final Map<String, String> OBJECT_SYNONYMS = new LinkedHashMap<>();
    private static final Map<String, String> HEX_COLOR_MAP = new LinkedHashMap<>();

    static {
        // Preferred canonical object terms for select_prompt
        OBJECT_SYNONYMS.put("kitchen cabinet", "kitchen cabinets");
        OBJECT_SYNONYMS.put("kitchen cabinets", "kitchen cabinets");
        OBJECT_SYNONYMS.put("wardrobe shutter", "wardrobe");
        OBJECT_SYNONYMS.put("wardrobe shutters", "wardrobe");
        OBJECT_SYNONYMS.put("wardrobe", "wardrobe");
        OBJECT_SYNONYMS.put("cupboard doors", "cabinet doors");
        OBJECT_SYNONYMS.put("cupboard", "cupboard");
        OBJECT_SYNONYMS.put("cabinet doors", "cabinet doors");
        OBJECT_SYNONYMS.put("cabinet", "cabinets");
        OBJECT_SYNONYMS.put("tv unit", "TV cabinet");
        OBJECT_SYNONYMS.put("tv cabinet", "TV cabinet");
        OBJECT_SYNONYMS.put("storage unit", "storage unit");
        OBJECT_SYNONYMS.put("wall cabinet", "wall cabinet");
        OBJECT_SYNONYMS.put("drawers", "drawers");
        OBJECT_SYNONYMS.put("drawer fronts", "drawer fronts");

        // Hex color approximations for plain-language normalization (Part 19)
        HEX_COLOR_MAP.put("#7A5C45", "warm walnut brown approximately #7A5C45");
        HEX_COLOR_MAP.put("#8B5A2B", "rich teak wood approximately #8B5A2B");
        HEX_COLOR_MAP.put("#D2B48C", "warm oak finish approximately #D2B48C");
        HEX_COLOR_MAP.put("#9CAF88", "muted sage green approximately #9CAF88");
        HEX_COLOR_MAP.put("#36454F", "deep charcoal grey approximately #36454F");
        HEX_COLOR_MAP.put("#FFFFFF", "crisp matte white approximately #FFFFFF");
        HEX_COLOR_MAP.put("#F5F5DC", "soft beige laminate approximately #F5F5DC");
        HEX_COLOR_MAP.put("#000080", "classic navy blue approximately #000080");
        HEX_COLOR_MAP.put("#FDFBF7", "matte off-white approximately #FDFBF7");
        HEX_COLOR_MAP.put("#58111A", "deep burgundy approximately #58111A");
    }

    private StabilityPromptBuilder() {
    }

    /**
     * Extracts a concise object target for Stability search-and-recolor select_prompt (Part 8).
     */
    public static String extractSelectPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "wardrobe";
        }
        String lower = userPrompt.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : OBJECT_SYNONYMS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "wardrobe";
    }

    /**
     * Normalizes free-text user prompt or hex color into a rich finish description (Part 18, 19, 20).
     */
    public static String extractTargetFinish(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "matte walnut brown wood finish";
        }
        String cleaned = userPrompt.trim();

        // Convert hex colour references if present (Part 19)
        Matcher hexMatcher = Pattern.compile("#([0-9a-fA-F]{6})").matcher(cleaned);
        if (hexMatcher.find()) {
            String hex = hexMatcher.group().toUpperCase(Locale.ROOT);
            String desc = HEX_COLOR_MAP.getOrDefault(hex, "custom finish approximately " + hex);
            cleaned = cleaned.replace(hexMatcher.group(), desc);
        }

        return cleaned;
    }

    /**
     * Builds canonical controlled prompt for Stability search-and-recolor (Part 7).
     */
    public static String buildQuickRecolorPrompt(String selectPrompt, String targetFinish) {
        String object = (selectPrompt != null && !selectPrompt.isBlank()) ? selectPrompt : "wardrobe";
        String finish = (targetFinish != null && !targetFinish.isBlank()) ? targetFinish : "matte walnut brown finish";

        return String.format(
                "Recolor the selected %s panels to a realistic %s. " +
                "Preserve the exact %s geometry, doors, handles, grooves, edges, proportions, shadows, reflections and surrounding room. " +
                "Do not redesign, replace or move the %s. " +
                "Do not modify walls, ceiling, floor, furniture, lights or any other unselected object. " +
                "Maintain realistic interior photography and the original camera perspective.",
                object, finish, object, object
        );
    }

    /**
     * Builds canonical controlled prompt for Stability inpaint with mask (Part 14).
     */
    public static String buildInpaintPrompt(String targetFinish) {
        String finish = (targetFinish != null && !targetFinish.isBlank()) ? targetFinish : "matte walnut brown wood finish";

        return String.format(
                "Change ONLY the masked cupboard/wardrobe surface to %s. " +
                "Preserve the exact physical geometry and structure. " +
                "Preserve handles, hinges, grooves, borders, door divisions, shadows, reflections, lighting and perspective. " +
                "Preserve every unmasked pixel and surrounding object as closely as possible. " +
                "Do not add or remove doors, handles, furniture or decorations. " +
                "Do not change the room layout. " +
                "Keep the result photorealistic and consistent with the original interior photograph.",
                finish
        );
    }

    /**
     * Furniture-preservation negative prompt (Part 15).
     */
    public static String getPreservationNegativePrompt() {
        return "redesigned furniture, changed geometry, extra handles, missing handles, " +
               "extra doors, missing doors, warped cabinet, distorted perspective, " +
               "changed wall, changed floor, changed ceiling, changed furniture, " +
               "additional objects, removed objects, unrealistic texture, cartoon, " +
               "illustration, CGI appearance, blurry details";
    }

    /**
     * Normalizes a mask into a pure binary black (0 = preserve) and white (255 = inpaint) PNG
     * matching the source image dimensions (Part 11, Part 12).
     */
    public static byte[] normalizeMaskToBinary(byte[] maskBytes, int targetWidth, int targetHeight) throws IOException {
        if (maskBytes == null || maskBytes.length == 0) {
            throw new IllegalArgumentException("Mask bytes cannot be empty");
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(maskBytes));
        if (src == null) {
            throw new IllegalArgumentException("Invalid mask image format");
        }

        int w = targetWidth > 0 ? targetWidth : src.getWidth();
        int h = targetHeight > 0 ? targetHeight : src.getHeight();

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);

        double scaleX = (double) src.getWidth() / w;
        double scaleY = (double) src.getHeight() / h;

        for (int y = 0; y < h; y++) {
            int srcY = Math.min((int) (y * scaleY), src.getHeight() - 1);
            for (int x = 0; x < w; x++) {
                int srcX = Math.min((int) (x * scaleX), src.getWidth() - 1);
                int argb = src.getRGB(srcX, srcY);
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;

                // Stability Inpaint Semantics:
                // Black (0, 0, 0) = PRESERVE
                // White (255, 255, 255) = INPAINT / EDIT
                boolean isInpaint = (a > 20 && (r > 20 || g > 20 || b > 20));
                out.setRGB(x, y, isInpaint ? 0xFFFFFFFF : 0xFF000000);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "png", baos);
        return baos.toByteArray();
    }
}
