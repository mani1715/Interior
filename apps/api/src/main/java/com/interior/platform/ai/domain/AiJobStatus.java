package com.interior.platform.ai.domain;

public enum AiJobStatus {
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMED_OUT;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == TIMED_OUT;
    }
}
