package com.interior.platform.ai.dto;

import java.util.List;

public record AiJobHistoryResponse(
        List<AiJobDetailResponse> items,
        int page,
        int limit,
        long totalItems,
        int totalPages
) {}
