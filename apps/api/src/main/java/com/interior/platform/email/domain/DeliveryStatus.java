package com.interior.platform.email.domain;

public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    NOT_CONFIGURED,
    DELIVERED,
    RETRY_SCHEDULED
}
