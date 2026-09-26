import { apiFetch } from '../api-client';
import { PublicAnalyticsEventRequest, StudioAnalyticsSummaryDto } from './types';

/**
 * Fires a low-trust public telemetry event.
 * Never throws or blocks page execution.
 */
export async function trackPublicEvent(request: PublicAnalyticsEventRequest): Promise<void> {
  try {
    await apiFetch<void>('/public/analytics/events', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  } catch {
    // Graceful swallow of telemetry failure
  }
}

/**
 * Retrieves the aggregated analytics summary for a studio within an optional date range.
 */
export async function getStudioAnalytics(
  studioId?: string,
  startDate?: string,
  endDate?: string
): Promise<StudioAnalyticsSummaryDto> {
  const params = new URLSearchParams();
  if (startDate) params.set('startDate', startDate);
  if (endDate) params.set('endDate', endDate);
  if (studioId) params.set('studioId', studioId);

  const query = params.toString() ? `?${params.toString()}` : '';
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }

  return apiFetch<StudioAnalyticsSummaryDto>(`/studio/analytics${query}`, {
    headers,
  });
}
