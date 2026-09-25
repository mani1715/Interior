package com.interior.platform.reviews.dto;

import java.util.List;

public record PublicStudioReviewsResponse(
        PublicReviewAggregateDto aggregate,
        List<PublicStudioReviewDto> reviews,
        int total,
        int limit,
        int offset
) {
}
