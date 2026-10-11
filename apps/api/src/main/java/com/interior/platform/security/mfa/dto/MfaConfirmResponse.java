package com.interior.platform.security.mfa.dto;

import java.util.List;

public record MfaConfirmResponse(
    boolean success,
    List<String> recoveryCodes,
    boolean sessionElevated
) {}
