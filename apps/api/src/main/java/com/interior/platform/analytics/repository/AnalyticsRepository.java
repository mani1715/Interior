package com.interior.platform.analytics.repository;

import com.interior.platform.analytics.domain.AnalyticsEventRecord;
import com.interior.platform.analytics.domain.StudioDailyMetricRecord;
import com.interior.platform.analytics.dto.StudioAnalyticsSummaryDto;
import com.interior.platform.analytics.dto.TopProjectMetricDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository {

    /**
     * Persists an analytics event with deduplication.
     * If the event was successfully inserted, transactional daily rollups are incremented.
     * Returns true if newly recorded, false if deduplicated.
     */
    boolean recordEvent(AnalyticsEventRecord event);

    /**
     * Retrieves aggregated analytics summary for a studio within the date range.
     */
    StudioAnalyticsSummaryDto getSummary(UUID studioId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves raw daily metric records for a studio within the date range.
     */
    List<StudioDailyMetricRecord> getDailyMetrics(UUID studioId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves top projects ordered by views in the given date range.
     */
    List<TopProjectMetricDto> getTopProjects(UUID studioId, LocalDate startDate, LocalDate endDate, int limit);
}
