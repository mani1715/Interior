package com.interior.platform.support.dto;

import com.interior.platform.support.domain.SupportPriority;
import com.interior.platform.support.domain.SupportStatus;

public record UpdateSupportStatusRequest(
        SupportStatus status,
        SupportPriority priority
) {}
