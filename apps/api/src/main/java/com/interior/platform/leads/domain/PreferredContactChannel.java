package com.interior.platform.leads.domain;

public enum PreferredContactChannel {
    PHONE("Phone Call"),
    WHATSAPP("WhatsApp"),
    EMAIL("Email"),
    ANY("Any Channel");

    private final String displayName;

    PreferredContactChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
