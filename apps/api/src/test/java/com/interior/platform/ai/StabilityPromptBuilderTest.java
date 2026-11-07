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
    void retainsRegionWhenColourSeparatesPositionFromCupboards() {
        assertEquals("upper cupboard", StabilityPromptBuilder.extractSelectPrompt("Make upper yellow cupboards"));
        assertEquals("lower cabinet doors", StabilityPromptBuilder.extractSelectPrompt("Paint lower matte black cabinet doors"));
        assertEquals("Make upper cupboards yellow.", StabilityPromptBuilder.extractTargetFinish(
                "Photorealistic architectural interior transformation: in modular kitchen space. Make upper cupboards yellow."));
    }
    @Test
    void ignoresLegacyCategoryPrefixAndRecognizesDownAsLowerRegion() {
        assertEquals("cupboard", StabilityPromptBuilder.extractSelectPrompt(
                "Photorealistic architectural interior transformation: in wardrobe space. give me the top white cupboards and down i need black cupboards"));
        assertEquals("cabinetry", StabilityPromptBuilder.extractSelectPrompt(
                "Photorealistic architectural interior transformation: in wardrobe space. At top i need the white and down i need the black."));
        assertEquals("cupboard", StabilityPromptBuilder.extractSelectPrompt(
                "Paint top cupboards white and down cupboards black"));
    }
    @Test
    void blackAndWhiteCombinationKeepsTheUsersRequestWithoutInventingPositions() {
        String raw = "give the cupboards in white and black combination";
        String prompt = StabilityPromptBuilder.buildQuickRecolorPrompt(
                StabilityPromptBuilder.extractSelectPrompt(raw), raw);
        assertTrue(prompt.startsWith(raw));
        assertTrue(prompt.contains("distinct solid white and solid black panels"));
        assertTrue(prompt.contains("not a grey blend"));
        assertFalse(prompt.contains("upper white"));
        assertFalse(prompt.contains("lower black"));
    }

    @Test
    void selectsRequestedSurfaceRatherThanPreservedSurface() {
        assertEquals("cupboard", StabilityPromptBuilder.extractSelectPrompt("Don't change the walls; paint the cupboards white"));
        assertEquals("upper cabinet doors", StabilityPromptBuilder.extractSelectPrompt("Paint upper cabinet doors black. Preserve lower doors."));
        assertEquals("cabinet doors", StabilityPromptBuilder.extractSelectPrompt("Paint upper cabinet doors black and lower cabinet doors white"));
    }

    @Test
    void preservationDoesNotForbidTheRequestedSurfaceChange() {
        assertFalse(StabilityPromptBuilder.getPreservationNegativePrompt().contains("changed wall"));
        assertFalse(StabilityPromptBuilder.getPreservationNegativePrompt().contains("changed furniture"));
        String prompt = StabilityPromptBuilder.buildQuickRecolorPrompt("walls", "Paint the wall white");
        assertTrue(prompt.startsWith("Paint the wall white"));
        assertFalse(prompt.contains("panels to a realistic Paint"));
    }
    @Test
    void preservesSeparateColourAssignmentsAndTarget() {
        String instruction = "Recolor upper cupboards black and lower cupboards white. Do not change the wall.";
        assertEquals("cupboard", StabilityPromptBuilder.extractSelectPrompt(instruction));
        assertEquals("walls", StabilityPromptBuilder.extractSelectPrompt("Paint the wall white, preserve the wardrobe"));
        assertEquals("TV cabinet", StabilityPromptBuilder.extractSelectPrompt("Recolor the TV cabinet black"));
        assertTrue(StabilityPromptBuilder.buildQuickRecolorPrompt("cupboard", instruction).contains(instruction));
        assertTrue(StabilityPromptBuilder.buildInpaintPrompt(instruction).contains(instruction));
    }

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
        assertTrue(prompt.startsWith("matte walnut brown"));
        assertTrue(prompt.contains("selected wardrobe"));
        assertTrue(prompt.contains("existing geometry, panel divisions"));
        assertTrue(prompt.contains("lighting and camera perspective"));
    }

    @Test
    @DisplayName("buildInpaintPrompt adheres to Part 14 precise editing specification")
    void testBuildInpaintPrompt() {
        String prompt = StabilityPromptBuilder.buildInpaintPrompt("natural oak wood veneer");
        assertNotNull(prompt);
        assertTrue(prompt.startsWith("natural oak wood veneer"));
        assertTrue(prompt.contains("selected region"));
        assertTrue(prompt.contains("panel divisions"));
        assertFalse(prompt.contains("upper/lower"));
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
        assertFalse(neg.contains("changed wall"));
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
