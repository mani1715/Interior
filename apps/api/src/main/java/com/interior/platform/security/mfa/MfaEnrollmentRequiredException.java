package com.interior.platform.security.mfa;

import com.interior.platform.common.exception.AccessDeniedException;

public class MfaEnrollmentRequiredException extends AccessDeniedException {
    public MfaEnrollmentRequiredException(String message) {
        super(message);
    }
}
