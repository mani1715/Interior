package com.interior.platform.media.domain;

public enum MediaType {
    REAL_PROJECT("Real Project Photo", true),
    BEFORE("Before Renovation Photo", true),
    AFTER("After Renovation Photo", true),
    AI_CONCEPT("AI Concept Visualization", true),
    REFERENCE("Inspirational Reference", false),
    CLIENT_PRIVATE("Client Confidential Document", false);

    private final String displayName;
    private final boolean publicEligible;

    MediaType(String displayName, boolean publicEligible) {
        this.displayName = displayName;
        this.publicEligible = publicEligible;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPublicEligible() {
        return publicEligible;
    }
}
