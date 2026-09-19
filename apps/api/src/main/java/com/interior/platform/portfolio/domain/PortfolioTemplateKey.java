package com.interior.platform.portfolio.domain;

public enum PortfolioTemplateKey {
    BASIC("Basic Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD),
    MODERN("Modern Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD),
    LUXURY("Luxury Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD),
    ARCHITECTURAL("Architectural Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD),
    WARM_NATURAL("Warm / Natural Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD),
    DARK_CINEMATIC("Dark Cinematic Portfolio", "1.0.0", TemplateImplementationStatus.SCAFFOLD);

    private final String displayName;
    private final String version;
    private final TemplateImplementationStatus status;

    PortfolioTemplateKey(String displayName, String version, TemplateImplementationStatus status) {
        this.displayName = displayName;
        this.version = version;
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public TemplateImplementationStatus getStatus() {
        return status;
    }
}
