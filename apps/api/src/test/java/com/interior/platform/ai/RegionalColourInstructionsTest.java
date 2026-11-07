package com.interior.platform.ai;

import com.interior.platform.ai.service.RegionalColourInstructions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionalColourInstructionsTest {
    @Test void detectsTheReportedFailureAndLegacyEnhancement() {
        assertTrue(RegionalColourInstructions.needsSeparateSelections("make the upper as the yellow and down black cupboards"));
        assertTrue(RegionalColourInstructions.needsSeparateSelections("Photorealistic architectural interior transformation: in modular kitchen space. make the upper as the yellow and down black cupboards. Preserve room perspective."));
        assertTrue(RegionalColourInstructions.needsSeparateSelections("Upper cupboards white; lower cupboards black."));
    }
    @Test void allowsSingleColourAndSingleRegionEdits() {
        assertFalse(RegionalColourInstructions.needsSeparateSelections("Upper cupboards yellow. Preserve lower black cupboards."));
        assertFalse(RegionalColourInstructions.needsSeparateSelections("Upper cupboards yellow and lower cupboards yellow."));
        assertFalse(RegionalColourInstructions.needsSeparateSelections("Recolour selected upper doors yellow. Preserve handles."));
        assertFalse(RegionalColourInstructions.needsSeparateSelections(null));
    }
}
