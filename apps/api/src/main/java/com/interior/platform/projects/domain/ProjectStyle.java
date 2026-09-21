package com.interior.platform.projects.domain;

public enum ProjectStyle {
    MODERN_MINIMALIST("Modern Minimalist"),
    WARM_CONTEMPORARY("Warm Contemporary"),
    INDIAN_TRADITIONAL("Indian Traditional"),
    NEO_CLASSICAL("Neo Classical"),
    SCANDINAVIAN("Scandinavian"),
    INDUSTRIAL("Industrial"),
    LUXURY_ECLECTIC("Luxury Eclectic"),
    BIOPHILIC("Biophilic");

    private final String displayName;

    ProjectStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
