package com.interior.platform.leads.domain;

public enum LeadStatus {
    NEW("New"),
    CONTACTED("Contacted"),
    QUALIFIED("Qualified"),
    SITE_VISIT_PLANNED("Site Visit Planned"),
    IN_DISCUSSION("In Discussion"),
    WON("Won"),
    LOST("Lost"),
    ARCHIVED("Archived");

    private final String displayName;

    LeadStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == NEW || this == CONTACTED || this == QUALIFIED ||
               this == SITE_VISIT_PLANNED || this == IN_DISCUSSION;
    }

    public boolean isTerminal() {
        return this == WON || this == LOST || this == ARCHIVED;
    }

    public boolean canTransitionTo(LeadStatus next) {
        if (next == null || this == next) {
            return true;
        }
        // Archived can be reopened
        if (this == ARCHIVED) {
            return next != ARCHIVED;
        }
        // Any status can be archived
        if (next == ARCHIVED) {
            return true;
        }
        // Active leads can transition across CRM pipeline, won, or lost
        if (isActive()) {
            return true;
        }
        // Won or Lost leads can be reopened intentionally into active discussion
        return next == IN_DISCUSSION || next == QUALIFIED || next == NEW;
    }
}
