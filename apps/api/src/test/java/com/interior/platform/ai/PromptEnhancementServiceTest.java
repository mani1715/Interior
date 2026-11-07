package com.interior.platform.ai;

import com.interior.platform.ai.dto.PromptEnhanceRequest;
import com.interior.platform.ai.dto.PromptEnhanceResponse;
import com.interior.platform.ai.service.PromptEnhancementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Prompt Enhancement Service Unit Tests")
class PromptEnhancementServiceTest {
    @Test
    void yellowAndBlackAreBothReportedWithoutChangingTheirPositions() {
        String raw = "make the upper as the yellow and down black cupboards";
        var result = new PromptEnhancementService().enhancePrompt(new PromptEnhanceRequest(
                raw, "KITCHEN", "FULL_IMAGE", true, null, List.of()));
        assertTrue(result.enhancedPrompt().contains(raw));
        assertTrue(result.detectedColors().containsAll(List.of("yellow", "black")));
    }
    @Test
    void projectCategoryMustNotOverrideTheUploadedPhotoOrRequestedColours() {
        String raw = "give me the top white cupboards and down i need black cupboards";
        var result = new PromptEnhancementService().enhancePrompt(new PromptEnhanceRequest(
                raw, "WARDROBE", "FULL_IMAGE", true, null, List.of()));
        assertTrue(result.enhancedPrompt().contains(raw));
        assertFalse(result.enhancedPrompt().contains("wardrobe space"));
        assertEquals(List.of("white", "black"), result.detectedColors());
        assertEquals("cupboard", com.interior.platform.ai.service.StabilityPromptBuilder.extractSelectPrompt(result.enhancedPrompt()));
    }
    @Test
    void keepsSpatialAssignmentsAndNegationsVerbatim() {
        String prompt = "Keep upper cupboards black and lower cupboards white. Do not recolor handles.";
        var result = new PromptEnhancementService().enhancePrompt(new PromptEnhanceRequest(
                prompt, null, "FULL_IMAGE", true, null, List.of()));
        assertTrue(result.enhancedPrompt().contains(prompt));
    }

    private PromptEnhancementService promptEnhancementService;

    @BeforeEach
    void setUp() {
        promptEnhancementService = new PromptEnhancementService();
    }

    @Test
    @DisplayName("Enhances informal voice dictation into structured architectural prompt")
    void testEnhanceInformalVoicePrompt() {
        PromptEnhanceRequest request = new PromptEnhanceRequest(
                "Change the wardrobe shutters to walnut wood, keep the structure the same and use matte black handles",
                "BEDROOM",
                "PRECISION_MASK",
                true,
                "Contemporary",
                List.of("MATERIAL", "STYLE")
        );

        PromptEnhanceResponse response = promptEnhancementService.enhancePrompt(request);

        assertNotNull(response);
        assertEquals(request.prompt(), response.originalPrompt());
        assertTrue(response.enhancedPrompt().contains("Targeted inpainting edit on selected masked area"));
        assertFalse(response.enhancedPrompt().contains("bedroom space"));
        assertTrue(response.enhancedPrompt().contains("walnut"));
        assertTrue(response.enhancedPrompt().contains("black"));
        assertTrue(response.enhancedPrompt().contains("Surrounding unmasked walls, ceiling, flooring, and adjacent fixtures remain 100% untouched"));
        assertTrue(response.enhancedPrompt().contains("Architectural structural boundaries"));
        assertEquals("DETERMINISTIC_ARCHITECTURAL_PARSER", response.providerName());

        assertTrue(response.detectedElements().stream().anyMatch(e -> e.contains("wardrobe") || e.contains("shutter")));
        assertTrue(response.detectedMaterials().stream().anyMatch(m -> m.contains("walnut")));
        assertTrue(response.detectedColors().stream().anyMatch(c -> c.contains("black")));
        assertTrue(response.detectedHardware().stream().anyMatch(h -> h.contains("handle")));
    }

    @Test
    @DisplayName("Enhances full image transformation with preserve structure enabled")
    void testEnhanceFullImageTransformation() {
        PromptEnhanceRequest request = new PromptEnhanceRequest(
                "make cupboards white and handles gold",
                "KITCHEN",
                "FULL_IMAGE",
                true,
                "Warm Minimalist",
                List.of()
        );

        PromptEnhanceResponse response = promptEnhancementService.enhancePrompt(request);

        assertNotNull(response);
        assertTrue(response.enhancedPrompt().contains("Photorealistic architectural interior transformation"));
        assertFalse(response.enhancedPrompt().contains("kitchen space"));
        assertTrue(response.enhancedPrompt().contains("white"));
        assertTrue(response.enhancedPrompt().contains("gold"));
        assertTrue(response.enhancedPrompt().contains("Warm Minimalist"));
        assertTrue(response.enhancedPrompt().contains("Architectural structural boundaries"));
    }

    @Test
    @DisplayName("Handles custom text without crashing when no keywords match")
    void testEnhanceCustomText() {
        PromptEnhanceRequest request = new PromptEnhanceRequest(
                "A minimalist reading nook with bespoke details",
                null,
                "FULL_IMAGE",
                false,
                null,
                null
        );

        PromptEnhanceResponse response = promptEnhancementService.enhancePrompt(request);

        assertNotNull(response);
        assertTrue(response.enhancedPrompt().contains("A minimalist reading nook with bespoke details"));
        assertTrue(response.enhancedPrompt().contains("diffused ambient illumination"));
    }
}
