package com.interior.platform.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.analytics.domain.AnalyticsEventRecord;
import com.interior.platform.analytics.domain.AnalyticsEventType;
import com.interior.platform.analytics.dto.PublicAnalyticsEventRequest;
import com.interior.platform.analytics.dto.StudioAnalyticsSummaryDto;
import com.interior.platform.analytics.repository.AnalyticsRepository;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.security.domain.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final Set<AnalyticsEventType> ALLOWED_PUBLIC_EVENTS = Set.of(
            AnalyticsEventType.PUBLIC_PROFILE_VIEW,
            AnalyticsEventType.PUBLIC_PROJECT_VIEW,
            AnalyticsEventType.DISCOVERY_RESULT_IMPRESSION,
            AnalyticsEventType.DISCOVERY_RESULT_CLICK,
            AnalyticsEventType.INQUIRY_OPENED,
            AnalyticsEventType.WHATSAPP_HANDOFF_OPENED
    );

    private final AnalyticsRepository analyticsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsRepository analyticsRepository, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.analyticsRepository = analyticsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingests low-trust client telemetry events.
     * Verifies target studio/project is active and published, derives studio ID server-side,
     * strips query parameters and tokens, bounds deduplication, and persists.
     */
    public boolean recordPublicEvent(PublicAnalyticsEventRequest request, String clientIp, String userAgent) {
        if (request == null || request.eventType() == null) {
            return false;
        }

        AnalyticsEventType eventType = request.eventType();
        if (!ALLOWED_PUBLIC_EVENTS.contains(eventType)) {
            log.warn("Rejected high-trust or disallowed event type from public endpoint: {}", eventType);
            return false;
        }

        ResolvedEntity entity = resolveEntity(request.entityType(), request.entitySlug());
        if (entity == null) {
            log.debug("Discarding public event for unresolvable entity: type={}, slug={}",
                    request.entityType(), request.entitySlug());
            return false;
        }

        Instant occurredAt = Instant.now();
        String source = "client";

        String sessionHash = request.sessionHash();
        if (sessionHash == null || sessionHash.isBlank()) {
            sessionHash = AnalyticsSanitizer.hashSession(
                    (clientIp != null ? clientIp : "unknown-ip") + ":" +
                    (userAgent != null ? userAgent : "unknown-ua") + ":" +
                    LocalDate.now(ZoneOffset.UTC)
            );
        } else {
            sessionHash = AnalyticsSanitizer.hashSession(sessionHash);
        }

        long hourBucket = occurredAt.getEpochSecond() / 3600;
        String deduplicationKey = String.format("pub:%s:%s:%s:%d",
                eventType.name(),
                entity.entityId() != null ? entity.entityId() : entity.studioId(),
                sessionHash,
                hourBucket
        );

        Map<String, Object> cleanMetadata = new HashMap<>();
        String referrer = AnalyticsSanitizer.sanitizeReferrer(request.referrer());
        if (referrer != null) {
            cleanMetadata.put("referrerDomain", referrer);
        }
        if (request.deviceClass() != null) {
            cleanMetadata.put("deviceClass", AnalyticsSanitizer.sanitizeDeviceClass(request.deviceClass()));
        }
        if (request.metadata() != null) {
            cleanMetadata.putAll(AnalyticsSanitizer.sanitizeMetadata(eventType, request.metadata()));
        }

        String metadataJson = null;
        if (!cleanMetadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(cleanMetadata);
            } catch (JsonProcessingException ignored) {}
        }

        AnalyticsEventRecord record = new AnalyticsEventRecord(
                UuidV7.randomUuid(),
                entity.studioId(),
                eventType,
                entity.entityType(),
                entity.entityId(),
                source,
                occurredAt,
                sessionHash,
                metadataJson,
                deduplicationKey,
                occurredAt
        );

        return analyticsRepository.recordEvent(record);
    }

    /**
     * Records high-trust server-side domain business events (e.g. leads created, leads won, reviews submitted).
     */
    public boolean recordServerEvent(
            UUID studioId,
            AnalyticsEventType eventType,
            String entityType,
            UUID entityId,
            String source,
            Map<String, Object> metadata,
            String deduplicationKey
    ) {
        if (studioId == null || eventType == null) {
            return false;
        }

        Instant now = Instant.now();
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException ignored) {}
        }

        AnalyticsEventRecord record = new AnalyticsEventRecord(
                UuidV7.randomUuid(),
                studioId,
                eventType,
                entityType,
                entityId,
                source != null ? source : "server",
                now,
                null,
                metadataJson,
                deduplicationKey,
                now
        );

        return analyticsRepository.recordEvent(record);
    }

    /**
     * Retrieves analytics dashboard summary for studio workspace.
     */
    public StudioAnalyticsSummaryDto getStudioAnalytics(ActorContext actor, UUID studioId, LocalDate startDate, LocalDate endDate) {
        validateStudioAccess(actor, studioId);

        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now(ZoneOffset.UTC);
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate temp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = temp;
        }

        // Limit range to max 365 days
        if (effectiveStart.isBefore(effectiveEnd.minusDays(365))) {
            effectiveStart = effectiveEnd.minusDays(365);
        }

        return analyticsRepository.getSummary(studioId, effectiveStart, effectiveEnd);
    }

    private void validateStudioAccess(ActorContext actor, UUID studioId) {
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (studioId == null || !actor.isStudioMember(studioId)) {
            throw new AccessDeniedException("User is not a member of studio: " + studioId);
        }
    }

    private ResolvedEntity resolveEntity(String entityType, String entitySlug) {
        if (entitySlug == null || entitySlug.isBlank()) {
            return null;
        }

        String slug = entitySlug.trim();

        // 1. Try project if requested as PROJECT or unspecified
        if (entityType == null || "PROJECT".equalsIgnoreCase(entityType)) {
            try {
                String sql = """
                    SELECT sp.id AS project_id, sp.studio_id, sp.project_status,
                           sp.visibility_status, sp.archived_at,
                           sp.slug AS project_slug, ds.status AS studio_status, ds.publication_status
                    FROM studio_projects sp
                    JOIN designer_studios ds ON ds.id = sp.studio_id
                    WHERE sp.slug = ?
                """;
                List<ResolvedEntity> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    String projStatus = rs.getString("project_status");
                    String visStatus = rs.getString("visibility_status");
                    Object archivedAt = rs.getObject("archived_at");
                    String studioStatus = rs.getString("studio_status");
                    String pubStatus = rs.getString("publication_status");

                    if (!"READY".equalsIgnoreCase(projStatus) ||
                        !"PORTFOLIO".equalsIgnoreCase(visStatus) ||
                        archivedAt != null ||
                        !"ACTIVE".equalsIgnoreCase(studioStatus) ||
                        !"PUBLISHED".equalsIgnoreCase(pubStatus)) {
                        return null;
                    }

                    UUID projId = getUuid(rs, "project_id");
                    UUID sId = getUuid(rs, "studio_id");
                    return new ResolvedEntity(sId, "PROJECT", projId, rs.getString("project_slug"));
                }, slug);
                for (ResolvedEntity re : list) {
                    if (re != null) return re;
                }
            } catch (Exception e) {
                log.warn("Error resolving project entity for analytics: {}", e.getMessage());
            }
        }

        // 2. Try studio if requested as STUDIO or unspecified
        if (entityType == null || "STUDIO".equalsIgnoreCase(entityType)) {
            try {
                String sql = """
                    SELECT id, status, publication_status
                    FROM designer_studios
                    WHERE slug = ?
                """;
                List<ResolvedEntity> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    String studioStatus = rs.getString("status");
                    String pubStatus = rs.getString("publication_status");

                    if (!"ACTIVE".equalsIgnoreCase(studioStatus) ||
                        !"PUBLISHED".equalsIgnoreCase(pubStatus)) {
                        return null;
                    }

                    UUID sId = getUuid(rs, "id");
                    return new ResolvedEntity(sId, "STUDIO", sId, null);
                }, slug);
                for (ResolvedEntity re : list) {
                    if (re != null) return re;
                }
            } catch (Exception e) {
                log.warn("Error resolving studio entity for analytics: {}", e.getMessage());
            }
        }

        return null;
    }

    private static UUID getUuid(ResultSet rs, String col) throws SQLException {
        Object val = rs.getObject(col);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private record ResolvedEntity(
            UUID studioId,
            String entityType,
            UUID entityId,
            String projectSlug
    ) {}
}
