package com.interior.platform.email.domain;

/**
 * Authoritative typed identifiers for all platform transactional email templates.
 * Enforces compile-time safety and prevents unvalidated template name strings.
 */
public enum EmailTemplateType {
    STUDIO_TEAM_INVITATION("Studio Team Invitation", true),
    CLIENT_REVIEW_INVITATION("Client Review Invitation", true),
    NEW_LEAD_ALERT("New Lead CRM Alert", false),
    CLIENT_FEEDBACK_ALERT("Client Concept Feedback Alert", false),
    SUBSCRIPTION_ACTIVATED("Subscription Activated", false),
    PAYMENT_FAILED("Payment Action Required", false),
    SUBSCRIPTION_RENEWED("Subscription Renewed", false),
    CANCEL_AT_PERIOD_END("Subscription Cancellation Scheduled", false),
    SECURITY_ALERT("Account Security Notification", false),
    PRIVACY_EXPORT_READY("Privacy Data Export Ready", true),
    ACCOUNT_CLOSURE_CONFIRMATION("Account Closure Confirmation", false);

    private final String displayTitle;
    private final boolean tokenBearing;

    EmailTemplateType(String displayTitle, boolean tokenBearing) {
        this.displayTitle = displayTitle;
        this.tokenBearing = tokenBearing;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public boolean isTokenBearing() {
        return tokenBearing;
    }
}
