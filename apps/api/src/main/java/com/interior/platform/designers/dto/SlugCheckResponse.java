package com.interior.platform.designers.dto;

public record SlugCheckResponse(
        String slug,
        boolean available,
        String reason,
        String suggestedSlug
) {
    public static SlugCheckResponse available(String slug) {
        return new SlugCheckResponse(slug, true, "Slug is available", null);
    }

    public static SlugCheckResponse unavailable(String slug, String reason, String suggestedSlug) {
        return new SlugCheckResponse(slug, false, reason, suggestedSlug);
    }
}
