package com.interior.platform.analytics.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.analytics.domain.AnalyticsEventRecord;
import com.interior.platform.analytics.domain.AnalyticsEventType;
import com.interior.platform.analytics.domain.StudioDailyMetricRecord;
import com.interior.platform.analytics.dto.AggregateFunnelDto;
import com.interior.platform.analytics.dto.StudioAnalyticsSummaryDto;
import com.interior.platform.analytics.dto.StudioDailyMetricDto;
import com.interior.platform.analytics.dto.TopProjectMetricDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAnalyticsRepository implements AnalyticsRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAnalyticsRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private Boolean isPostgres;

    public JdbcAnalyticsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private synchronized boolean isPostgreSql() {
        if (isPostgres == null) {
            try {
                String dbProduct = jdbcTemplate.execute((java.sql.Connection conn) -> conn.getMetaData().getDatabaseProductName());
                isPostgres = dbProduct != null && dbProduct.toLowerCase().contains("postgresql");
            } catch (Exception e) {
                isPostgres = false;
            }
        }
        return Boolean.TRUE.equals(isPostgres);
    }

    private Object toJsonbObject(String jsonString) {
        if (jsonString == null) return null;
        if (!isPostgreSql()) {
            return jsonString;
        }
        try {
            Class<?> clazz = Class.forName("org.postgresql.util.PGobject");
            Object pgo = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setType", String.class).invoke(pgo, "jsonb");
            clazz.getMethod("setValue", String.class).invoke(pgo, jsonString);
            return pgo;
        } catch (Exception ignored) {
            return jsonString;
        }
    }

    @Override
    @Transactional
    public boolean recordEvent(AnalyticsEventRecord event) {
        String insertEventSql = """
            INSERT INTO analytics_events (
                id, studio_id, event_type, entity_type, entity_id, source,
                occurred_at, anonymous_session_hash, metadata, deduplication_key, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            jdbcTemplate.update(insertEventSql,
                    event.id(),
                    event.studioId(),
                    event.eventType().name(),
                    event.entityType(),
                    event.entityId(),
                    event.source(),
                    Timestamp.from(event.occurredAt()),
                    event.anonymousSessionHash(),
                    toJsonbObject(event.metadataJson()),
                    event.deduplicationKey(),
                    Timestamp.from(event.createdAt())
            );
        } catch (DataIntegrityViolationException e) {
            if (event.deduplicationKey() != null) {
                log.debug("Analytics event deduplicated for key: {}", event.deduplicationKey());
                return false;
            }
            throw e;
        }

        // Increment daily metrics on successful insert
        LocalDate metricDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        incrementDailyMetrics(event, metricDate);
        return true;
    }

    private void incrementDailyMetrics(AnalyticsEventRecord event, LocalDate metricDate) {
        long profileViews = event.eventType() == AnalyticsEventType.PUBLIC_PROFILE_VIEW ? 1 : 0;
        long projectViews = event.eventType() == AnalyticsEventType.PUBLIC_PROJECT_VIEW ? 1 : 0;
        long discoveryImpressions = event.eventType() == AnalyticsEventType.DISCOVERY_RESULT_IMPRESSION ? 1 : 0;
        long discoveryClicks = event.eventType() == AnalyticsEventType.DISCOVERY_RESULT_CLICK ? 1 : 0;
        long inquiriesOpened = event.eventType() == AnalyticsEventType.INQUIRY_OPENED ? 1 : 0;
        long leadsCreated = event.eventType() == AnalyticsEventType.LEAD_CREATED ? 1 : 0;
        long leadsWon = 0;
        if (event.eventType() == AnalyticsEventType.LEAD_STATUS_CHANGED && event.metadataJson() != null) {
            try {
                JsonNode root = objectMapper.readTree(event.metadataJson());
                JsonNode toStatus = root.get("toStatus");
                if (toStatus != null && "WON".equalsIgnoreCase(toStatus.asText())) {
                    leadsWon = 1;
                }
            } catch (Exception ignored) {}
        }
        long reviewsSubmitted = event.eventType() == AnalyticsEventType.REVIEW_SUBMITTED ? 1 : 0;
        long aiGenerations = event.eventType() == AnalyticsEventType.AI_GENERATION_COMPLETED ? 1 : 0;
        long whatsappHandoffs = event.eventType() == AnalyticsEventType.WHATSAPP_HANDOFF_OPENED ? 1 : 0;

        if (profileViews == 0 && projectViews == 0 && discoveryImpressions == 0 && discoveryClicks == 0
                && inquiriesOpened == 0 && leadsCreated == 0 && leadsWon == 0 && reviewsSubmitted == 0
                && aiGenerations == 0 && whatsappHandoffs == 0) {
            return;
        }

        String updateSql = """
            UPDATE studio_daily_metrics SET
                profile_views = profile_views + ?,
                project_views = project_views + ?,
                discovery_impressions = discovery_impressions + ?,
                discovery_clicks = discovery_clicks + ?,
                inquiries_opened = inquiries_opened + ?,
                leads_created = leads_created + ?,
                leads_won = leads_won + ?,
                reviews_submitted = reviews_submitted + ?,
                ai_generations = ai_generations + ?,
                whatsapp_handoffs = whatsapp_handoffs + ?,
                updated_at = now()
            WHERE studio_id = ? AND metric_date = ?
        """;

        int rows = jdbcTemplate.update(updateSql,
                profileViews, projectViews, discoveryImpressions, discoveryClicks,
                inquiriesOpened, leadsCreated, leadsWon, reviewsSubmitted,
                aiGenerations, whatsappHandoffs, event.studioId(), Date.valueOf(metricDate));

        if (rows == 0) {
            String insertSql = """
                INSERT INTO studio_daily_metrics (
                    studio_id, metric_date, profile_views, project_views,
                    discovery_impressions, discovery_clicks, inquiries_opened,
                    leads_created, leads_won, reviews_submitted, ai_generations,
                    whatsapp_handoffs, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """;
            try {
                jdbcTemplate.update(insertSql,
                        event.studioId(), Date.valueOf(metricDate),
                        profileViews, projectViews, discoveryImpressions, discoveryClicks,
                        inquiriesOpened, leadsCreated, leadsWon, reviewsSubmitted,
                        aiGenerations, whatsappHandoffs);
            } catch (DataIntegrityViolationException e) {
                // Concurrent insert, retry update once
                jdbcTemplate.update(updateSql,
                        profileViews, projectViews, discoveryImpressions, discoveryClicks,
                        inquiriesOpened, leadsCreated, leadsWon, reviewsSubmitted,
                        aiGenerations, whatsappHandoffs, event.studioId(), Date.valueOf(metricDate));
            }
        }
    }

    @Override
    public StudioAnalyticsSummaryDto getSummary(UUID studioId, LocalDate startDate, LocalDate endDate) {
        String summarySql = """
            SELECT
                COALESCE(SUM(profile_views), 0) AS total_profile_views,
                COALESCE(SUM(project_views), 0) AS total_project_views,
                COALESCE(SUM(discovery_impressions), 0) AS total_discovery_impressions,
                COALESCE(SUM(discovery_clicks), 0) AS total_discovery_clicks,
                COALESCE(SUM(inquiries_opened), 0) AS total_inquiries_opened,
                COALESCE(SUM(leads_created), 0) AS total_leads_created,
                COALESCE(SUM(leads_won), 0) AS total_leads_won,
                COALESCE(SUM(reviews_submitted), 0) AS total_reviews_submitted,
                COALESCE(SUM(ai_generations), 0) AS total_ai_generations,
                COALESCE(SUM(whatsapp_handoffs), 0) AS total_whatsapp_handoffs
            FROM studio_daily_metrics
            WHERE studio_id = ? AND metric_date >= ? AND metric_date <= ?
        """;

        RowMapper<Totals> totalsMapper = (rs, rowNum) -> new Totals(
                rs.getLong("total_profile_views"),
                rs.getLong("total_project_views"),
                rs.getLong("total_discovery_impressions"),
                rs.getLong("total_discovery_clicks"),
                rs.getLong("total_inquiries_opened"),
                rs.getLong("total_leads_created"),
                rs.getLong("total_leads_won"),
                rs.getLong("total_reviews_submitted"),
                rs.getLong("total_ai_generations"),
                rs.getLong("total_whatsapp_handoffs")
        );

        Totals totals = jdbcTemplate.query(summarySql, totalsMapper, studioId, Date.valueOf(startDate), Date.valueOf(endDate))
                .stream().findFirst().orElse(new Totals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        long publicViews = totals.profileViews + totals.projectViews;
        double inquiryOpenRate = publicViews > 0 ? (totals.inquiriesOpened * 100.0 / publicViews) : 0.0;
        double leadConversionRate = totals.inquiriesOpened > 0 ? (totals.leadsCreated * 100.0 / totals.inquiriesOpened) : (publicViews > 0 ? (totals.leadsCreated * 100.0 / publicViews) : 0.0);
        double leadWinRate = totals.leadsCreated > 0 ? (totals.leadsWon * 100.0 / totals.leadsCreated) : 0.0;

        AggregateFunnelDto funnel = new AggregateFunnelDto(
                publicViews,
                totals.inquiriesOpened,
                totals.leadsCreated,
                totals.leadsWon,
                Math.round(inquiryOpenRate * 10.0) / 10.0,
                Math.round(leadConversionRate * 10.0) / 10.0,
                Math.round(leadWinRate * 10.0) / 10.0
        );

        String sinceSql = "SELECT MIN(metric_date) FROM studio_daily_metrics WHERE studio_id = ?";
        Date minDate = jdbcTemplate.queryForObject(sinceSql, Date.class, studioId);
        String trackingSince = minDate != null ? minDate.toLocalDate().toString() : startDate.toString();

        List<TopProjectMetricDto> topProjects = getTopProjects(studioId, startDate, endDate, 10);
        List<StudioDailyMetricDto> timeseries = getTimeseries(studioId, startDate, endDate);

        return new StudioAnalyticsSummaryDto(
                studioId,
                startDate,
                endDate,
                trackingSince,
                totals.profileViews,
                totals.projectViews,
                totals.discoveryImpressions,
                totals.discoveryClicks,
                totals.inquiriesOpened,
                totals.leadsCreated,
                totals.leadsWon,
                totals.reviewsSubmitted,
                totals.aiGenerations,
                totals.whatsappHandoffs,
                funnel,
                topProjects,
                timeseries
        );
    }

    private List<StudioDailyMetricDto> getTimeseries(UUID studioId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT metric_date, profile_views, project_views, discovery_impressions,
                   discovery_clicks, inquiries_opened, leads_created, leads_won,
                   reviews_submitted, ai_generations, whatsapp_handoffs
            FROM studio_daily_metrics
            WHERE studio_id = ? AND metric_date >= ? AND metric_date <= ?
            ORDER BY metric_date ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDailyMetricDto(
                rs.getDate("metric_date").toLocalDate(),
                rs.getLong("profile_views"),
                rs.getLong("project_views"),
                rs.getLong("discovery_impressions"),
                rs.getLong("discovery_clicks"),
                rs.getLong("inquiries_opened"),
                rs.getLong("leads_created"),
                rs.getLong("leads_won"),
                rs.getLong("reviews_submitted"),
                rs.getLong("ai_generations"),
                rs.getLong("whatsapp_handoffs")
        ), studioId, Date.valueOf(startDate), Date.valueOf(endDate));
    }

    @Override
    public List<StudioDailyMetricRecord> getDailyMetrics(UUID studioId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM studio_daily_metrics
            WHERE studio_id = ? AND metric_date >= ? AND metric_date <= ?
            ORDER BY metric_date ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDailyMetricRecord(
                getUuid(rs, "studio_id"),
                rs.getDate("metric_date").toLocalDate(),
                rs.getLong("profile_views"),
                rs.getLong("project_views"),
                rs.getLong("discovery_impressions"),
                rs.getLong("discovery_clicks"),
                rs.getLong("inquiries_opened"),
                rs.getLong("leads_created"),
                rs.getLong("leads_won"),
                rs.getLong("reviews_submitted"),
                rs.getLong("ai_generations"),
                rs.getLong("whatsapp_handoffs"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        ), studioId, Date.valueOf(startDate), Date.valueOf(endDate));
    }

    @Override
    public List<TopProjectMetricDto> getTopProjects(UUID studioId, LocalDate startDate, LocalDate endDate, int limit) {
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        String sql = """
            SELECT
                ae.entity_id AS project_id,
                COALESCE(sp.title, 'Project') AS project_title,
                COALESCE(sp.slug, '') AS project_slug,
                COUNT(*) AS view_count
            FROM analytics_events ae
            LEFT JOIN studio_projects sp ON sp.id = ae.entity_id
            WHERE ae.studio_id = ?
              AND ae.event_type = 'PUBLIC_PROJECT_VIEW'
              AND ae.occurred_at >= ?
              AND ae.occurred_at < ?
              AND ae.entity_id IS NOT NULL
            GROUP BY ae.entity_id, sp.title, sp.slug
            ORDER BY view_count DESC
            LIMIT ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TopProjectMetricDto(
                getUuid(rs, "project_id"),
                rs.getString("project_title"),
                rs.getString("project_slug"),
                null,
                rs.getLong("view_count"),
                0L
        ), studioId, Timestamp.from(startInstant), Timestamp.from(endInstant), limit);
    }

    private static UUID getUuid(ResultSet rs, String col) throws SQLException {
        Object val = rs.getObject(col);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private record Totals(
            long profileViews,
            long projectViews,
            long discoveryImpressions,
            long discoveryClicks,
            long inquiriesOpened,
            long leadsCreated,
            long leadsWon,
            long reviewsSubmitted,
            long aiGenerations,
            long whatsappHandoffs
    ) {}
}
