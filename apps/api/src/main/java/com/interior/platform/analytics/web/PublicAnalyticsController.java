package com.interior.platform.analytics.web;

import com.interior.platform.analytics.dto.PublicAnalyticsEventRequest;
import com.interior.platform.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/analytics")
@Tag(name = "Public Telemetry & Analytics", description = "Privacy-safe low-trust telemetry event ingestion")
public class PublicAnalyticsController {

    private final AnalyticsService analyticsService;

    public PublicAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events")
    @Operation(summary = "Ingest public telemetry event", description = "Accepts privacy-safe client telemetry events with sanitization and bounded deduplication.")
    public ResponseEntity<Void> recordPublicEvent(
            @RequestBody(required = false) PublicAnalyticsEventRequest request,
            HttpServletRequest servletRequest
    ) {
        if (request == null) {
            return ResponseEntity.noContent().build();
        }

        String clientIp = resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader(HttpHeaders.USER_AGENT);

        analyticsService.recordPublicEvent(request, clientIp, userAgent);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
