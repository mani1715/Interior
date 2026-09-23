package com.interior.platform.portfolio.domain;

public enum PortfolioTemplateKey {
    BASIC("Basic Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE),
    MODERN("Modern Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE),
    LUXURY("Luxury Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE),
    ARCHITECTURAL("Architectural Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE),
    WARM_NATURAL("Warm / Natural Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE),
    DARK_CINEMATIC("Dark Cinematic Portfolio", "1.0.0", TemplateImplementationStatus.AVAILABLE);

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
