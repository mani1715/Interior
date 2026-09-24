package com.interior.platform.leads.domain;

public enum WhatsAppMessageStatus {
    QUEUED(0),
    SUBMITTED(1),
    SENT(2),
    DELIVERED(3),
    READ(4),
    FAILED(-1);

    private final int rank;

    WhatsAppMessageStatus(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean canTransitionTo(WhatsAppMessageStatus next) {
        if (next == null || this == next) {
            return false;
        }
        // FAILED cannot be transitioned out of
        if (this == FAILED) {
            return false;
        }
        // Can fail only prior to delivery (cannot fail once DELIVERED or READ)
        if (next == FAILED) {
            return this != READ && this != DELIVERED;
        }
        // Strict monotonic progression: QUEUED -> SUBMITTED -> SENT -> DELIVERED -> READ
        return next.rank > this.rank;
    }
}
