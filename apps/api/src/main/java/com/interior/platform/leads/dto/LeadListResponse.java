package com.interior.platform.leads.dto;

import java.util.List;

public record LeadListResponse(
    List<LeadSummaryDto> items,
    long total,
    int limit,
    int offset,
    boolean hasMore
) {}
