package com.interior.platform.designers.domain;

import com.interior.platform.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Optional;

/**
 * Controlled canonical style / aesthetic specialty taxonomy.
 * 
 * Canonical Source:
 * Maps directly to the Phase 02 Taxonomy Strategy (docs/02_TAXONOMY_STRATEGY.md, Section 2)
 * 'STYLE' dimension (MODERN, CONTEMPORARY, MINIMAL, LUXURY, TRADITIONAL, SCANDINAVIAN, INDUSTRIAL, CLASSIC),
 * adapted with composite aesthetic specializations for Indian design practices (e.g. Indian Traditional / Chettinad,
 * Neo-Classical, Biophilic).
 * 
 * This controlled enum serves as the canonical V1 controlled taxonomy for studio specialties to
 * prevent arbitrary free-text tags and ensure consistent discoverability, filtering, and indexing.
 */
public enum CanonicalSpecialty {
    MODERN_MINIMALIST("MODERN_MINIMALIST", "Modern Minimalist"),
    WARM_CONTEMPORARY("WARM_CONTEMPORARY", "Warm Contemporary"),
    INDIAN_TRADITIONAL("INDIAN_TRADITIONAL", "Indian Traditional / Chettinad"),
    NEO_CLASSICAL("NEO_CLASSICAL", "Neo-Classical & Heritage"),
    SCANDINAVIAN("SCANDINAVIAN", "Scandinavian Natural"),
    INDUSTRIAL("INDUSTRIAL", "Industrial & Loft"),
    LUXURY_ECLECTIC("LUXURY_ECLECTIC", "Luxury Eclectic"),
    BIOPHILIC("BIOPHILIC", "Biophilic & Sustainable");

    private final String code;
    private final String displayName;

    CanonicalSpecialty(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static CanonicalSpecialty from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Specialty cannot be empty");
        }
        String trimmed = raw.trim();

        // Match by enum name/code
        for (CanonicalSpecialty s : values()) {
            if (s.code.equalsIgnoreCase(trimmed) || s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }

        // Match by display name
        for (CanonicalSpecialty s : values()) {
            if (s.displayName.equalsIgnoreCase(trimmed)) {
                return s;
            }
        }

        // Match partial normalized code (e.g. "indian-traditional" -> "INDIAN_TRADITIONAL")
        String normalized = trimmed.toUpperCase().replace('-', '_').replace(' ', '_');
        for (CanonicalSpecialty s : values()) {
            if (s.code.equalsIgnoreCase(normalized) || s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }

        throw new BadRequestException("Invalid design specialty: '" + trimmed + "'. Must belong to canonical taxonomy.");
    }
}
