package com.interior.platform.reviews.dto;

public record PublicReviewAggregateDto(
        double averageRating,
        int totalReviews,
        int[] distribution
) {
    public static PublicReviewAggregateDto empty() {
        return new PublicReviewAggregateDto(0.0, 0, new int[]{0, 0, 0, 0, 0});
    }
}
