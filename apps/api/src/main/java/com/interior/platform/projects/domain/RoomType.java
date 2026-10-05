package com.interior.platform.projects.domain;

public enum RoomType {
    LIVING_ROOM("Living Room"),
    KITCHEN("Modular Kitchen"),
    BEDROOM("Bedroom"),
    DINING("Dining Room"),
    BATHROOM("Bathroom"),
    POOJA("Pooja Room & Mandir"),
    HOME_OFFICE("Home Office & Study"),
    BALCONY_TERRACE("Balcony & Terrace"),
    FOYER("Foyer & Entryway"),
    WARDROBE_DRESSER("Wardrobe & Dressing Suite"),
    OTHER("Other Space");

    private final String defaultDisplayName;

    RoomType(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }
}
