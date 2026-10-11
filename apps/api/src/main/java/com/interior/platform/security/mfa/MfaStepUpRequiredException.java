package com.interior.platform.security.mfa;

import com.interior.platform.common.exception.AccessDeniedException;

public class MfaStepUpRequiredException extends AccessDeniedException {
    public MfaStepUpRequiredException(String message) {
        super(message);
    }
}
