package com.interior.platform.projects.domain;

public enum PropertyType {
    APARTMENT("Apartment / Flat"),
    INDEPENDENT_HOUSE("Independent House"),
    VILLA("Villa"),
    OFFICE("Office"),
    RETAIL("Retail / Showroom"),
    RESTAURANT("Restaurant / Cafe"),
    COMMERCIAL("Commercial Space"),
    OTHER("Other");

    private final String displayName;

    PropertyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
