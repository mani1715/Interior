package com.interior.platform.leads.domain;

public enum LeadSource {
    PROJECT_DISCOVERY("Project Discovery"),
    PROFESSIONAL_DISCOVERY("Professional Discovery"),
    PUBLIC_PROJECT("Public Project"),
    PUBLIC_PORTFOLIO("Public Portfolio"),
    DIRECT_INQUIRY("Direct Inquiry"),
    WHATSAPP_HANDOFF("WhatsApp Handoff");

    private final String displayName;

    LeadSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
