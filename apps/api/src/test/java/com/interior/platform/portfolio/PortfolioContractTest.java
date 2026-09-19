package com.interior.platform.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.domain.SectionType;
import com.interior.platform.portfolio.domain.TemplateImplementationStatus;
import com.interior.platform.portfolio.dto.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void canonicalSectionTypesAreExact() {
        List<String> actual = Arrays.stream(SectionType.values()).map(Enum::name).toList();
        List<String> expected = List.of(
                "HERO", "ABOUT", "SERVICES", "FEATURED_PROJECTS", "PROJECT_GRID",
                "BEFORE_AFTER", "BEFORE_AI_REALITY", "DESIGN_PROCESS", "TESTIMONIALS",
                "TEAM", "AWARDS", "PRESS", "SERVICE_AREAS", "FAQ", "CONTACT", "CTA",
                "VIDEO", "CUSTOM_NOTE"
        );
        assertEquals(expected, actual, "SectionType enum values must strictly match canonical contract");
        assertEquals(18, actual.size());
    }

    @Test
    void canonicalFontPairingsAreExact() {
        List<String> actual = Arrays.stream(FontPairing.values()).map(Enum::name).toList();
        List<String> expected = List.of(
                "SYSTEM_SANS", "CLASSIC_SERIF", "MODERN_CLEAN",
                "EDITORIAL", "WARM_EDITORIAL", "BOLD_CINEMATIC"
        );
        assertEquals(expected, actual, "FontPairing enum values must strictly match canonical contract");
        assertEquals(6, actual.size());
    }

    @Test
    void templateRegistryVersionsAndStatusAreExact() {
        for (PortfolioTemplateKey key : PortfolioTemplateKey.values()) {
            assertEquals("1.0.0", key.getVersion(), "Template " + key + " version must be 1.0.0");
            assertEquals(TemplateImplementationStatus.SCAFFOLD, key.getStatus(), "Template " + key + " must initially be SCAFFOLD");
        }
    }

    @Test
    void reorderRequestSerializationContract() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String json = "{\"sectionIds\": [\"" + id1 + "\", \"" + id2 + "\"], \"version\": 5}";

        ReorderSectionsRequest req = objectMapper.readValue(json, ReorderSectionsRequest.class);
        assertEquals(List.of(id1, id2), req.sectionIds());
        assertEquals(5L, req.version());

        String serialized = objectMapper.writeValueAsString(req);
        assertTrue(serialized.contains("\"sectionIds\""));
        assertTrue(serialized.contains("\"version\":5"));
        assertFalse(serialized.contains("\"orderedSectionIds\""));
    }

    @Test
    void updateSectionRequestSerializationContract() throws Exception {
        String json = "{\"isVisible\": true, \"content\": {\"badgeText\": \"Design Studio\"}, \"version\": 3}";
        UpdateSectionRequest req = objectMapper.readValue(json, UpdateSectionRequest.class);
        assertTrue(req.isVisible());
        assertEquals(3L, req.version());
        assertEquals("Design Studio", req.content().get("badgeText").asText());
    }

    @Test
    void createSnapshotRequestSerializationContract() throws Exception {
        String json = "{\"label\": \"Milestone 1\", \"version\": 4}";
        CreateVersionSnapshotRequest req = objectMapper.readValue(json, CreateVersionSnapshotRequest.class);
        assertEquals("Milestone 1", req.label());
        assertEquals(4L, req.version());
    }

    @Test
    void restoreVersionRequestSerializationContract() throws Exception {
        String json = "{\"version\": 2}";
        RestoreVersionRequest req = objectMapper.readValue(json, RestoreVersionRequest.class);
        assertEquals(2L, req.version());
    }
}
