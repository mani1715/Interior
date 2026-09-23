package com.interior.platform.discovery.repository;

import com.interior.platform.discovery.dto.*;

import java.util.List;

public interface DiscoveryRepository {

    List<DiscoveryProjectCardDto> searchProjects(DiscoverySearchParams params);

    long countProjects(DiscoverySearchParams params);

    List<DiscoveryProfessionalCardDto> searchProfessionals(DiscoverySearchParams params);

    long countProfessionals(DiscoverySearchParams params);

    DiscoverySuggestionsResponse getSuggestions(String query, int limit);

    DiscoveryFacetsDto getFacets(DiscoverySearchParams params);
}
