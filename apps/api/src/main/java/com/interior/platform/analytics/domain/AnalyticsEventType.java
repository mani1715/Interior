package com.interior.platform.analytics.domain;

import java.util.Set;

public enum AnalyticsEventType {
    // Low-trust client events (may be submitted via public ingestion with strict validation)
    PUBLIC_PROFILE_VIEW,
    PUBLIC_PROJECT_VIEW,
    DISCOVERY_RESULT_IMPRESSION,
    DISCOVERY_RESULT_CLICK,
    INQUIRY_OPENED,
    WHATSAPP_HANDOFF_OPENED,

    // High-trust server business events (strictly server-side only; rejected by public ingestion)
    LEAD_CREATED,
    LEAD_STATUS_CHANGED,
    REVIEW_SUBMITTED,
    AI_GENERATION_COMPLETED,
    PORTFOLIO_PUBLISHED;

    private static final Set<AnalyticsEventType> CLIENT_ALLOWLIST = Set.of(
            PUBLIC_PROFILE_VIEW,
            PUBLIC_PROJECT_VIEW,
            DISCOVERY_RESULT_IMPRESSION,
            DISCOVERY_RESULT_CLICK,
            INQUIRY_OPENED,
            WHATSAPP_HANDOFF_OPENED
    );

    public boolean isAllowedFromClient() {
        return CLIENT_ALLOWLIST.contains(this);
    }
}
