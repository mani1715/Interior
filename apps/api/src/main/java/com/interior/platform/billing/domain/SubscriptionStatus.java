package com.interior.platform.billing.domain;

public enum SubscriptionStatus {
    INACTIVE,
    PENDING,
    ACTIVE,
    PAST_DUE,
    CANCEL_AT_PERIOD_END,
    CANCELLED,
    EXPIRED
}
