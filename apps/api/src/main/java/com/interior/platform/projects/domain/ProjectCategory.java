package com.interior.platform.projects.domain;

public enum ProjectCategory {
    LIVING_ROOM("Living Room"),
    TV_UNIT("TV Unit & Entertainment"),
    BEDROOM("Bedroom"),
    WARDROBE("Wardrobe & Storage"),
    MODULAR_KITCHEN("Modular Kitchen"),
    POOJA_UNIT("Pooja Room & Mandir"),
    CROCKERY_UNIT("Crockery Unit & Bar"),
    STUDY_UNIT("Study & Home Office"),
    FALSE_CEILING("False Ceiling & Lighting"),
    WALL_PANELS("Wall Paneling & Cladding"),
    SHOE_RACK("Foyer & Shoe Rack"),
    OFFICE("Corporate Office"),
    COMMERCIAL("Commercial & Retail"),
    CUSTOM_FURNITURE("Bespoke Custom Furniture"),
    COMPLETE_HOME_INTERIOR("Complete Home Interior");

    private final String displayName;

    ProjectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
