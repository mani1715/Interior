package com.interior.platform.designers.domain;

public enum ProfessionalType {
    INDIVIDUAL_DESIGNER("Individual Designer"),
    INTERIOR_STUDIO("Interior Studio"),
    ARCHITECT("Architect"),
    ARCHITECTURE_STUDIO("Architecture Studio"),
    CUSTOM_FURNITURE_STUDIO("Custom Furniture Studio"),
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
        for (ProfessionalType type : values()) {
            if (type.name().equalsIgnoreCase(code.trim()) || type.displayName.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return INTERIOR_STUDIO;
    }
}
