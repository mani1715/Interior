package com.interior.platform.media.domain;

public enum MediaVisibility {
    PRIVATE("Private / Studio Only"),
    PORTFOLIO("Portfolio Website"),
    PUBLIC("Public & Discoverable");

    private final String displayName;

    MediaVisibility(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
