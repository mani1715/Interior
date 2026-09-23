package com.interior.platform.discovery.dto;

import java.util.List;

public record DiscoveryFacetsDto(
        List<DiscoveryFacetItemDto> categories,
        List<DiscoveryFacetItemDto> cities,
        List<DiscoveryFacetItemDto> styles,
        List<DiscoveryFacetItemDto> professionalTypes
) {}
