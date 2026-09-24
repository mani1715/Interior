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
        // Any status can be archived
        if (next == ARCHIVED) {
            return true;
        }
        // Archived can only be reopened to NEW or IN_DISCUSSION
        if (this == ARCHIVED) {
            return next == NEW || next == IN_DISCUSSION;
        }
        // Active leads can transition across CRM pipeline stages or resolve to WON / LOST
        if (isActive()) {
            return true;
        }
        // Won leads cannot jump back to NEW or LOST, but can reopen to IN_DISCUSSION for added scope
        if (this == WON) {
            return next == IN_DISCUSSION;
        }
        // Lost leads cannot jump directly to WON, but can reopen to IN_DISCUSSION or QUALIFIED
        if (this == LOST) {
            return next == IN_DISCUSSION || next == QUALIFIED;
        }
        return false;
    }
}
