package com.interior.platform.projects.domain;

public enum ProjectScope {
    FULL_INTERIOR("Full Home / Office Interior"),
    PARTIAL_INTERIOR("Partial Interior"),
    SINGLE_ROOM("Single Room"),
    CUSTOM_FURNITURE("Custom Furniture"),
    WOODWORK("Woodwork & Carpentry"),
    RENOVATION("Renovation & Remodeling"),
    ARCHITECTURAL("Architectural Interior"),
    TURNKEY("Turnkey Project"),
    OTHER("Other");

    private final String displayName;

    ProjectScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
