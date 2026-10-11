package com.interior.platform.security.mfa;

import com.interior.platform.common.exception.AccessDeniedException;

public class MfaRequiredException extends AccessDeniedException {
    public MfaRequiredException(String message) {
        super(message);
    }
}
