package com.interior.platform.ai.domain;

public enum EditingMode {
    FULL_IMAGE("Full Concept"),
    PRECISION_MASK("Precision Edit");

    private final String displayName;

    EditingMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
