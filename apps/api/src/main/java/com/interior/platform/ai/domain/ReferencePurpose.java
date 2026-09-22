package com.interior.platform.ai.domain;

public enum ReferencePurpose {
    COLOR("Color Palette", "Reference for wall or accent colors, palettes, and tone."),
    MATERIAL("Material", "General material guidance."),
    WOOD("Wood & Laminate", "Reference for wood veneer, timber species, grain, or laminate finish."),
    STONE("Stone, Granite & Marble", "Reference for marble, granite, quartz, or natural stone."),
    TILE("Tile & Backsplash", "Reference for floor, wall, or backsplash tiles and patterns."),
    FABRIC("Fabric & Upholstery", "Reference for fabric texture, weave, curtains, and upholstery."),
    HARDWARE("Hardware & Fixtures", "Reference for handles, knobs, faucets, and metal hardware."),
    FURNITURE_STYLE("Furniture Style", "Reference for seating, tables, or freestanding furniture style."),
    CABINET_STYLE("Cabinetry & Wardrobes", "Reference for shutters, wardrobe doors, groove detailing, and modular units."),
    ROOM_STYLE("Room Style", "Inspiration for spatial ambiance and room style theme."),
    WALL_FINISH("Wall Finish", "Reference for wallpaper, texture paint, fluted panels, or moulding."),
    CEILING_STYLE("Ceiling & Lighting", "Reference for false ceiling design, coves, and lighting fixtures."),
    GENERAL_STYLE("General Inspiration", "Broad design inspiration or aesthetic mood.");

    private final String displayName;
    private final String description;

    ReferencePurpose(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
