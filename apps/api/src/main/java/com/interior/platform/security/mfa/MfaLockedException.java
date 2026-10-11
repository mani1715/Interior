package com.interior.platform.security.mfa;

import com.interior.platform.common.exception.AccessDeniedException;

public class MfaLockedException extends AccessDeniedException {
    public MfaLockedException(String message) {
        super(message);
    }
}
