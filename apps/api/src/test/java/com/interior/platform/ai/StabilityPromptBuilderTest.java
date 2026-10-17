package com.interior.platform.ai;

import com.interior.platform.ai.service.StabilityPromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StabilityPromptBuilder Unit Tests")
class StabilityPromptBuilderTest {

    @Test
    @DisplayName("extractSelectPrompt detects wardrobe, kitchen cabinets, TV unit, cupboard and falls back safely")
    void testExtractSelectPrompt() {
        assertEquals("wardrobe", StabilityPromptBuilder.extractSelectPrompt("Change the wardrobe to matte walnut brown"));
        assertEquals("kitchen cabinets", StabilityPromptBuilder.extractSelectPrompt("Recolor kitchen cabinets to sage green"));
        assertEquals("cupboard", StabilityPromptBuilder.extractSelectPrompt("Update the cupboard finish"));
        assertEquals("cabinet doors", StabilityPromptBuilder.extractSelectPrompt("Paint cabinet doors white"));
        assertEquals("TV cabinet", StabilityPromptBuilder.extractSelectPrompt("Refinish the TV unit console"));
        assertEquals("wardrobe", StabilityPromptBuilder.extractSelectPrompt(""));
        assertEquals("wardrobe", StabilityPromptBuilder.extractSelectPrompt(null));
    }

    @Test
    @DisplayName("extractTargetFinish expands hex colors and normalizes natural text")
    void testExtractTargetFinish() {
        String walnutHex = StabilityPromptBuilder.extractTargetFinish("Change finish to #7A5C45");
        assertTrue(walnutHex.contains("warm walnut brown approximately #7A5C45"));

        String sageHex = StabilityPromptBuilder.extractTargetFinish("Color: #9CAF88");
        assertTrue(sageHex.contains("muted sage green approximately #9CAF88"));

        String natural = StabilityPromptBuilder.extractTargetFinish("matte sage green with brass hardware");
        assertEquals("matte sage green with brass hardware", natural);
    }

    @Test
    @DisplayName("buildQuickRecolorPrompt preserves geometry, doors, handles, room, perspective")
    void testBuildQuickRecolorPrompt() {
        String prompt = StabilityPromptBuilder.buildQuickRecolorPrompt("wardrobe", "matte walnut brown");
        assertNotNull(prompt);
        assertTrue(prompt.contains("Recolor the selected wardrobe panels to a realistic matte walnut brown"));
        assertTrue(prompt.contains("Preserve the exact wardrobe geometry, doors, handles, grooves, edges"));
        assertTrue(prompt.contains("Do not modify walls, ceiling, floor, furniture, lights"));
        assertTrue(prompt.contains("Maintain realistic interior photography and the original camera perspective"));
    }

    @Test
    @DisplayName("buildInpaintPrompt adheres to Part 14 precise editing specification")
    void testBuildInpaintPrompt() {
        String prompt = StabilityPromptBuilder.buildInpaintPrompt("natural oak wood veneer");
        assertNotNull(prompt);
        assertTrue(prompt.contains("Change ONLY the masked cupboard/wardrobe surface to natural oak wood veneer"));
        assertTrue(prompt.contains("Preserve handles, hinges, grooves, borders, door divisions, shadows, reflections, lighting"));
        assertTrue(prompt.contains("Preserve every unmasked pixel and surrounding object"));
        assertTrue(prompt.contains("Do not add or remove doors, handles, furniture or decorations"));
        assertTrue(prompt.contains("Keep the result photorealistic and consistent with the original interior photograph"));
    }

    @Test
    @DisplayName("getPreservationNegativePrompt adheres to Part 15 negative prompt specification")
    void testPreservationNegativePrompt() {
        String neg = StabilityPromptBuilder.getPreservationNegativePrompt();
        assertNotNull(neg);
        assertTrue(neg.contains("redesigned furniture"));
        assertTrue(neg.contains("changed geometry"));
        assertTrue(neg.contains("extra handles"));
        assertTrue(neg.contains("missing handles"));
        assertTrue(neg.contains("changed wall"));
        assertTrue(neg.contains("distorted perspective"));
    }

    @Test
    @DisplayName("normalizeMaskToBinary outputs pure binary black (preserve) and white (inpaint) PNG")
    void testNormalizeMaskToBinary() throws IOException {
        // Create 100x100 transparent image with a 20x20 bronze painted box
        BufferedImage src = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        g.setColor(new Color(184, 138, 90, 180)); // Bronze brush stroke
        g.fillRect(10, 10, 20, 20);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(src, "png", baos);

        byte[] binaryPng = StabilityPromptBuilder.normalizeMaskToBinary(baos.toByteArray(), 100, 100);
        assertNotNull(binaryPng);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(binaryPng));
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());

        // Pixel outside box (0, 0) must be black (preserve: 0x000000)
        int outsideRgb = result.getRGB(0, 0) & 0x00FFFFFF;
        assertEquals(0, outsideRgb, "Unpainted pixel must be pure black (preserve)");

        // Pixel inside box (15, 15) must be white (inpaint: 0xFFFFFF)
        int insideRgb = result.getRGB(15, 15) & 0x00FFFFFF;
        assertEquals(0x00FFFFFF, insideRgb, "Painted pixel must be pure white (inpaint)");
    }
}
