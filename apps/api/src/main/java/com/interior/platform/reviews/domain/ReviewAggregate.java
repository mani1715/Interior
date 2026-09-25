package com.interior.platform.reviews.domain;

public record ReviewAggregate(
        double averageRating,
        int totalReviews,
        int[] distribution
) {
    public static ReviewAggregate empty() {
        return new ReviewAggregate(0.0, 0, new int[]{0, 0, 0, 0, 0});
    }
}
