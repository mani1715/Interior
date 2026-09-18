package com.interior.platform.portfolio.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.portfolio.domain.SectionType;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class PortfolioSectionValidator {

    private static final int MAX_SECTION_PAYLOAD_BYTES = 32 * 1024; // 32 KB
    private static final Pattern DISALLOWED_HTML_TAGS = Pattern.compile(
            "<(?i)(script|iframe|object|embed|applet|meta|link|style)[^>]*>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DISALLOWED_HTML_ATTRIBUTES = Pattern.compile(
            "(?i)(\\bon[a-z]{3,20}\\s*=|javascript:|data:text/html)",
            Pattern.CASE_INSENSITIVE
    );

    public void validateSectionContent(SectionType type, JsonNode content) {
        if (content == null || content.isNull() || content.isMissingNode()) {
            return;
        }

        String serialized = content.toString();
        if (serialized.getBytes().length > MAX_SECTION_PAYLOAD_BYTES) {
            throw new BadRequestException("Section content exceeds maximum payload size of 32 KB");
        }

        if (DISALLOWED_HTML_TAGS.matcher(serialized).find() || DISALLOWED_HTML_ATTRIBUTES.matcher(serialized).find()) {
            throw new BadRequestException("Section content contains disallowed HTML or script attributes");
        }

        if (!content.isObject()) {
            throw new BadRequestException("Section content must be a JSON object");
        }

        switch (type) {
            case HERO -> validateHero(content);
            case ABOUT -> validateAbout(content);
            case SERVICES -> validateServices(content);
            case SERVICE_AREAS -> validateServiceAreas(content);
            case CONTACT -> validateContact(content);
            case CTA -> validateCta(content);
            case TESTIMONIALS -> validateTestimonials(content);
            case FAQ -> validateFaq(content);
            case TEAM -> validateTeam(content);
            default -> validateGeneric(content);
        }
    }

    private void validateHero(JsonNode content) {
        checkStringField(content, "badgeText", 100);
        checkStringField(content, "headlineOverride", 200);
        checkStringField(content, "subheadlineOverride", 300);
        checkStringField(content, "ctaText", 50);
        checkStringField(content, "ctaLink", 200);
    }

    private void validateAbout(JsonNode content) {
        checkStringField(content, "narrativeOverride", 4000);
        checkStringField(content, "philosophyOverride", 2000);
        checkStringField(content, "quote", 500);
    }

    private void validateServices(JsonNode content) {
        checkStringField(content, "sectionHeadline", 200);
        checkStringField(content, "sectionDescription", 1000);
    }

    private void validateServiceAreas(JsonNode content) {
        checkStringField(content, "sectionHeadline", 200);
        checkStringField(content, "coverageNote", 500);
    }

    private void validateContact(JsonNode content) {
        checkStringField(content, "contactIntro", 500);
        checkStringField(content, "preferredChannel", 50);
    }

    private void validateCta(JsonNode content) {
        checkStringField(content, "headline", 200);
        checkStringField(content, "description", 500);
        checkStringField(content, "buttonText", 50);
        checkStringField(content, "buttonLink", 200);
    }

    private void validateTestimonials(JsonNode content) {
        JsonNode items = content.get("items");
        if (items != null) {
            if (!items.isArray()) {
                throw new BadRequestException("Testimonials items must be an array");
            }
            if (items.size() > 20) {
                throw new BadRequestException("Testimonials section cannot exceed 20 entries");
            }
        }
    }

    private void validateFaq(JsonNode content) {
        JsonNode items = content.get("items");
        if (items != null) {
            if (!items.isArray()) {
                throw new BadRequestException("FAQ items must be an array");
            }
            if (items.size() > 20) {
                throw new BadRequestException("FAQ section cannot exceed 20 items");
            }
        }
    }

    private void validateTeam(JsonNode content) {
        JsonNode members = content.get("members");
        if (members != null) {
            if (!members.isArray()) {
                throw new BadRequestException("Team members must be an array");
            }
            if (members.size() > 20) {
                throw new BadRequestException("Team section cannot exceed 20 members");
            }
        }
    }

    private void validateGeneric(JsonNode content) {
        Iterator<Map.Entry<String, JsonNode>> fields = content.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getValue().isTextual() && entry.getValue().asText().length() > 4000) {
                throw new BadRequestException("Field '" + entry.getKey() + "' exceeds 4000 characters limit");
            }
        }
    }

    private void checkStringField(JsonNode node, String fieldName, int maxLength) {
        JsonNode field = node.get(fieldName);
        if (field != null && field.isTextual() && field.asText().length() > maxLength) {
            throw new BadRequestException("Field '" + fieldName + "' exceeds maximum length of " + maxLength + " characters");
        }
    }
}
