package com.interior.platform.discovery.service;

import com.interior.platform.discovery.dto.*;
import com.interior.platform.discovery.repository.DiscoveryRepository;
import com.interior.platform.security.service.RateLimiterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class DiscoveryService {

    private final DiscoveryRepository discoveryRepository;
    private final RateLimiterService rateLimiterService;

    public DiscoveryService(DiscoveryRepository discoveryRepository, RateLimiterService rateLimiterService) {
        this.discoveryRepository = discoveryRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @Transactional(readOnly = true)
    public DiscoverySearchResponse searchProjects(DiscoverySearchParams rawParams) {
        DiscoverySearchParams params = sanitizeParams(rawParams);

        List<DiscoveryProjectCardDto> projects = discoveryRepository.searchProjects(params);
        long totalProjects = discoveryRepository.countProjects(params);
        DiscoveryFacetsDto facets = discoveryRepository.getFacets(params);

        boolean hasMore = (params.getSafeOffset() + projects.size()) < totalProjects;
        String nextCursor = hasMore ? String.valueOf(params.getSafeOffset() + params.getSafeLimit()) : null;

        return new DiscoverySearchResponse(
                projects,
                List.of(),
                totalProjects,
                0,
                facets,
                nextCursor,
                hasMore
        );
    }

    @Transactional(readOnly = true)
    public DiscoverySearchResponse searchProfessionals(DiscoverySearchParams rawParams) {
        DiscoverySearchParams params = sanitizeParams(rawParams);

        List<DiscoveryProfessionalCardDto> professionals = discoveryRepository.searchProfessionals(params);
        long totalProfessionals = discoveryRepository.countProfessionals(params);
        DiscoveryFacetsDto facets = discoveryRepository.getFacets(params);

        boolean hasMore = (params.getSafeOffset() + professionals.size()) < totalProfessionals;
        String nextCursor = hasMore ? String.valueOf(params.getSafeOffset() + params.getSafeLimit()) : null;

        return new DiscoverySearchResponse(
                List.of(),
                professionals,
                0,
                totalProfessionals,
                facets,
                nextCursor,
                hasMore
        );
    }

    @Transactional(readOnly = true)
    public DiscoverySearchResponse searchAll(DiscoverySearchParams rawParams) {
        DiscoverySearchParams params = sanitizeParams(rawParams);

        List<DiscoveryProjectCardDto> projects = discoveryRepository.searchProjects(params);
        long totalProjects = discoveryRepository.countProjects(params);

        List<DiscoveryProfessionalCardDto> professionals = discoveryRepository.searchProfessionals(params);
        long totalProfessionals = discoveryRepository.countProfessionals(params);

        DiscoveryFacetsDto facets = discoveryRepository.getFacets(params);

        boolean hasMore = (params.getSafeOffset() + projects.size()) < totalProjects ||
                          (params.getSafeOffset() + professionals.size()) < totalProfessionals;
        String nextCursor = hasMore ? String.valueOf(params.getSafeOffset() + params.getSafeLimit()) : null;

        return new DiscoverySearchResponse(
                projects,
                professionals,
                totalProjects,
                totalProfessionals,
                facets,
                nextCursor,
                hasMore
        );
    }

    @Transactional(readOnly = true)
    public DiscoverySuggestionsResponse getSuggestions(String rawQuery, String clientIp) {
        String ipKey = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "anonymous";
        rateLimiterService.acquire("discovery_sugg:" + ipKey, 20, Duration.ofMinutes(1));

        String sanitized = sanitizeQueryString(rawQuery);
        if (sanitized == null || sanitized.length() < 2) {
            return new DiscoverySuggestionsResponse(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        return discoveryRepository.getSuggestions(sanitized, 5);
    }

    @Transactional(readOnly = true)
    public DiscoveryFacetsDto getFacets(DiscoverySearchParams rawParams) {
        DiscoverySearchParams params = sanitizeParams(rawParams);
        return discoveryRepository.getFacets(params);
    }

    private DiscoverySearchParams sanitizeParams(DiscoverySearchParams p) {
        if (p == null) {
            return new DiscoverySearchParams(null, null, null, null, null, null, null, null, null, 12, 0, null);
        }
        return new DiscoverySearchParams(
                sanitizeQueryString(p.q()),
                sanitizeFilterValue(p.category()),
                sanitizeFilterValue(p.style()),
                sanitizeFilterValue(p.city()),
                sanitizeFilterValue(p.state()),
                sanitizeFilterValue(p.propertyType()),
                sanitizeFilterValue(p.scope()),
                sanitizeFilterValue(p.professionalType()),
                sanitizeFilterValue(p.sort()),
                p.limit(),
                p.offset(),
                p.cursor()
        );
    }

    private String sanitizeQueryString(String q) {
        if (q == null) return null;
        String trimmed = q.trim();
        if (trimmed.isEmpty()) return null;
        // Cap query length at 100 characters to prevent query amplification
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        // Collapse multiple whitespace
        return trimmed.replaceAll("\\s+", " ");
    }

    private String sanitizeFilterValue(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed;
    }
}
