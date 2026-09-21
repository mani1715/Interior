package com.interior.platform.seo.domain;

public record SeoChecklistItem(
        String code,
        String title,
        String description,
        boolean passed,
        boolean critical
) {}
