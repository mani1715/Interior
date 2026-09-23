package com.interior.platform.discovery.dto;

import java.util.List;

public record DiscoverySearchResponse(
        List<DiscoveryProjectCardDto> projects,
        List<DiscoveryProfessionalCardDto> professionals,
        long totalProjects,
        long totalProfessionals,
        DiscoveryFacetsDto facets,
        String nextCursor,
        boolean hasMore
) {}
