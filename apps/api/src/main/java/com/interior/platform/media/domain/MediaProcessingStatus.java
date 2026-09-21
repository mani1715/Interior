package com.interior.platform.media.domain;

public enum MediaProcessingStatus {
    PENDING_UPLOAD,
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    QUARANTINED,
    DELETED
}
