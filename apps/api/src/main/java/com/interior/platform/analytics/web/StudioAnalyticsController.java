package com.interior.platform.analytics.web;

import com.interior.platform.analytics.dto.StudioAnalyticsSummaryDto;
import com.interior.platform.analytics.service.AnalyticsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/studio/analytics")
@Tag(name = "Studio Analytics", description = "Tenant-isolated analytics reporting and funnel metrics for interior design studios")
public class StudioAnalyticsController {

    private final AnalyticsService analyticsService;

    public StudioAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get studio analytics summary", description = "Retrieves aggregated views, impressions, funnel metrics, and top performing projects.")
    public ResponseEntity<StudioAnalyticsSummaryDto> getStudioAnalytics(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        StudioAnalyticsSummaryDto summary = analyticsService.getStudioAnalytics(actor, studioId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .body(summary);
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private UUID resolveRequestedStudioId(String header, UUID param, ActorContext actor) {
        if (param != null) return param;
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actor != null && actor.activeStudioId() != null) {
            return actor.activeStudioId();
        }
        return null;
    }
}
