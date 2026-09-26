export type AnalyticsEventType =
  | 'PUBLIC_PROFILE_VIEW'
  | 'PUBLIC_PROJECT_VIEW'
  | 'DISCOVERY_RESULT_IMPRESSION'
  | 'DISCOVERY_RESULT_CLICK'
  | 'INQUIRY_OPENED'
  | 'LEAD_CREATED'
  | 'LEAD_STATUS_CHANGED'
  | 'REVIEW_SUBMITTED'
  | 'AI_GENERATION_COMPLETED'
  | 'PORTFOLIO_PUBLISHED'
  | 'WHATSAPP_HANDOFF_OPENED';

export interface PublicAnalyticsEventRequest {
  eventType: AnalyticsEventType;
  entityType?: 'STUDIO' | 'PROJECT';
  entitySlug?: string;
  sessionHash?: string;
  referrer?: string;
  deviceClass?: 'MOBILE' | 'TABLET' | 'DESKTOP' | 'UNKNOWN';
  metadata?: Record<string, unknown>;
}

export interface AggregateFunnelDto {
  publicViews: number;
  inquiriesOpened: number;
  leadsCreated: number;
  leadsWon: number;
  inquiryOpenRatePercent: number;
  leadConversionRatePercent: number;
  leadWinRatePercent: number;
}

export interface TopProjectMetricDto {
  projectId: string;
  title: string;
  slug: string;
  coverImageUrl?: string;
  viewCount: number;
  inquiryCount: number;
}

export interface StudioDailyMetricDto {
  date: string;
  profileViews: number;
  projectViews: number;
  discoveryImpressions: number;
  discoveryClicks: number;
  inquiriesOpened: number;
  leadsCreated: number;
  leadsWon: number;
  reviewsSubmitted: number;
  aiGenerations: number;
  whatsappHandoffs: number;
}

export interface StudioAnalyticsSummaryDto {
  studioId: string;
  startDate: string;
  endDate: string;
  trackingSince: string;
  totalProfileViews: number;
  totalProjectViews: number;
  totalDiscoveryImpressions: number;
  totalDiscoveryClicks: number;
  totalInquiriesOpened: number;
  totalLeadsCreated: number;
  totalLeadsWon: number;
  totalReviewsSubmitted: number;
  totalAiGenerations: number;
  totalWhatsappHandoffs: number;
  funnel: AggregateFunnelDto;
  topProjects: TopProjectMetricDto[];
  timeseries: StudioDailyMetricDto[];
}
