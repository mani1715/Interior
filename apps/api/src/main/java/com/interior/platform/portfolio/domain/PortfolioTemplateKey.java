package com.interior.platform.portfolio.domain;

public enum PortfolioTemplateKey {
    BASIC("Basic Portfolio", TemplateImplementationStatus.SCAFFOLD),
    MODERN("Modern Portfolio", TemplateImplementationStatus.SCAFFOLD),
    LUXURY("Luxury Portfolio", TemplateImplementationStatus.SCAFFOLD),
    ARCHITECTURAL("Architectural Portfolio", TemplateImplementationStatus.SCAFFOLD),
    WARM_NATURAL("Warm / Natural Portfolio", TemplateImplementationStatus.SCAFFOLD),
    DARK_CINEMATIC("Dark Cinematic Portfolio", TemplateImplementationStatus.SCAFFOLD);

    private final String displayName;
    private final TemplateImplementationStatus status;

    PortfolioTemplateKey(String displayName, TemplateImplementationStatus status) {
        this.displayName = displayName;
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TemplateImplementationStatus getStatus() {
        return status;
    }
}
