package com.interior.platform.designers.domain;

public enum ProfessionalType {
    INDIVIDUAL_DESIGNER("Individual Designer"),
    INTERIOR_STUDIO("Interior Studio"),
    ARCHITECT("Architect"),
    ARCHITECTURE_STUDIO("Architecture Studio"),
    CUSTOM_FURNITURE("Custom Furniture"),
    CUSTOM_FURNITURE_STUDIO("Custom Furniture Studio"),
    WOODWORK_CABINETRY("Woodwork / Cabinetry"),
    WOODWORK_CABINETRY_PROFESSIONAL("Woodwork / Cabinetry Professional"),
    TURNKEY_CONTRACTOR("Turnkey Contractor");

    private final String displayName;

    ProfessionalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProfessionalType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return INTERIOR_STUDIO;
        }
        String trimmed = code.trim().toUpperCase();
        if ("CUSTOM_FURNITURE".equals(trimmed) || "CUSTOM_FURNITURE_STUDIO".equals(trimmed)) {
            return CUSTOM_FURNITURE;
        }
        if ("WOODWORK_CABINETRY".equals(trimmed) || "WOODWORK_CABINETRY_PROFESSIONAL".equals(trimmed)) {
            return WOODWORK_CABINETRY;
        }
        for (ProfessionalType type : values()) {
            if (type.name().equalsIgnoreCase(trimmed) || type.displayName.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return INTERIOR_STUDIO;
    }
}
