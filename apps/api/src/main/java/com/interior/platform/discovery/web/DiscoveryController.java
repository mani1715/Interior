package com.interior.platform.discovery.web;

import com.interior.platform.discovery.dto.*;
import com.interior.platform.discovery.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/discovery")
@Tag(name = "Discovery Engine", description = "Canonical public endpoints for search, filtering, autocomplete suggestions, and discovery facets")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/search")
    @Operation(summary = "Unified search", description = "Unified search across published projects and professionals")
    public ResponseEntity<DiscoverySearchResponse> searchAll(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "professionalType", required = false) String professionalType,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "cursor", required = false) String cursor
    ) {
        DiscoverySearchParams params = new DiscoverySearchParams(
                q, category, style, city, state, propertyType, scope, professionalType, sort, limit, offset, cursor
        );
        return ResponseEntity.ok(discoveryService.searchAll(params));
    }

    @GetMapping("/projects")
    @Operation(summary = "Search projects", description = "Search and filter published portfolio projects")
    public ResponseEntity<DiscoverySearchResponse> searchProjects(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "professionalType", required = false) String professionalType,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "cursor", required = false) String cursor
    ) {
        DiscoverySearchParams params = new DiscoverySearchParams(
                q, category, style, city, state, propertyType, scope, professionalType, sort, limit, offset, cursor
        );
        return ResponseEntity.ok(discoveryService.searchProjects(params));
    }

    @GetMapping("/professionals")
    @Operation(summary = "Search professionals", description = "Search and filter published professionals and studios")
    public ResponseEntity<DiscoverySearchResponse> searchProfessionals(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "professionalType", required = false) String professionalType,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "cursor", required = false) String cursor
    ) {
        DiscoverySearchParams params = new DiscoverySearchParams(
                q, category, style, city, state, propertyType, scope, professionalType, sort, limit, offset, cursor
        );
        return ResponseEntity.ok(discoveryService.searchProfessionals(params));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get autocomplete suggestions", description = "Provides rate-limited typeahead suggestions for search queries")
    public ResponseEntity<DiscoverySuggestionsResponse> getSuggestions(
            @RequestParam("q") String query,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(discoveryService.getSuggestions(query, clientIp));
    }

    @GetMapping("/facets")
    @Operation(summary = "Get discovery facets", description = "Retrieve aggregated count facets for categories, cities, styles, and professional types")
    public ResponseEntity<DiscoveryFacetsDto> getFacets(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "city", required = false) String city
    ) {
        DiscoverySearchParams params = new DiscoverySearchParams(
                null, category, null, city, null, null, null, null, null, null, null, null
        );
        return ResponseEntity.ok(discoveryService.getFacets(params));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
