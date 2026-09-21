package com.interior.platform.ai.dto;

import java.util.List;

public record AiJobListResponse(
        List<AiJobDetailResponse> items,
        int total,
        int limit,
        int offset
) {}
