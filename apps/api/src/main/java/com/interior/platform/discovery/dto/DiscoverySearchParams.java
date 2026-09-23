package com.interior.platform.discovery.dto;

public record DiscoverySearchParams(
        String q,
        String category,
        String style,
        String city,
        String state,
        String propertyType,
        String scope,
        String professionalType,
        String sort,
        Integer limit,
        Integer offset,
        String cursor
) {
    public int getSafeLimit() {
        if (limit == null || limit <= 0) return 12;
        return Math.min(limit, 50);
    }

    public int getSafeOffset() {
        if (offset == null || offset < 0) return 0;
        return Math.min(offset, 1000); // Guard against deep pagination DoS
    }
}
