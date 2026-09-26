import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import StudioAnalyticsPage from '@/app/workspace/analytics/page';
import { PublicTelemetryTracker } from '@/components/analytics/PublicTelemetryTracker';
import * as analyticsApi from '@/lib/analytics/api';
import { StudioAnalyticsSummaryDto } from '@/lib/analytics/types';

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  usePathname: () => '/workspace/analytics',
}));

// Mock AuthContext
vi.mock('@/lib/auth/auth-context', () => ({
  useAuth: () => ({
    user: { id: 'user-001', displayName: 'Designer Alpha' },
    activeStudioId: 'studio-001',
    activeRole: 'OWNER',
    isLoading: false,
  }),
}));

vi.mock('@/lib/analytics/api', () => ({
  getStudioAnalytics: vi.fn(),
  trackPublicEvent: vi.fn().mockResolvedValue(undefined),
}));

const mockAnalyticsSummary: StudioAnalyticsSummaryDto = {
  studioId: 'studio-001',
  startDate: '2026-02-26',
  endDate: '2026-03-26',
  totalProfileViews: 42,
  totalProjectViews: 18,
  totalDiscoveryImpressions: 120,
  totalDiscoveryClicks: 35,
  totalInquiriesOpened: 14,
  totalLeadsCreated: 8,
  totalLeadsWon: 3,
  totalReviewsSubmitted: 5,
  totalAiGenerations: 20,
  totalWhatsappHandoffs: 6,
  funnel: {
    publicViews: 60,
    inquiriesOpened: 14,
    leadsCreated: 8,
    leadsWon: 3,
    inquiryOpenRatePercent: 23.3,
    leadConversionRatePercent: 57.1,
    leadWinRatePercent: 37.5,
  },
  topProjects: [
    {
      projectId: 'proj-001',
      title: 'Modern Living Room',
      slug: 'modern-living-room',
      viewCount: 18,
      inquiryCount: 4,
    },
  ],
  timeseries: [
    {
      date: '2026-03-25',
      profileViews: 10,
      projectViews: 5,
      discoveryImpressions: 25,
      discoveryClicks: 8,
      inquiriesOpened: 3,
      leadsCreated: 2,
      leadsWon: 1,
      reviewsSubmitted: 1,
      aiGenerations: 4,
      whatsappHandoffs: 1,
    },
  ],
  trackingSince: '2026-01-01',
};

describe('Phase 28 — Analytics Subsystem', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Studio Analytics Workspace Page', () => {
    it('renders dashboard with summary cards and conversion funnel', async () => {
      vi.mocked(analyticsApi.getStudioAnalytics).mockResolvedValue(mockAnalyticsSummary);

      render(<StudioAnalyticsPage />);

      // Verify loading state appears then finishes
      await waitFor(() => {
        expect(screen.getByText(/42 profile · 18 project/i)).toBeDefined();
      });

      // Summary metrics
      expect(screen.getAllByText('14').length).toBeGreaterThanOrEqual(1); // Inquiries Opened & Funnel
      expect(screen.getAllByText('3').length).toBeGreaterThanOrEqual(1); // Leads Won & Funnel

      // Funnel & Rates
      expect(screen.getByText('Client Acquisition Funnel')).toBeDefined();
      expect(screen.getByText('23.3% open rate')).toBeDefined();
      expect(screen.getByText('57.1% lead conversion')).toBeDefined();
      expect(screen.getAllByText('37.5% win rate').length).toBeGreaterThanOrEqual(1);

      // Top projects
      expect(screen.getByText('Modern Living Room')).toBeDefined();
    });

    it('allows changing date range filters', async () => {
      vi.mocked(analyticsApi.getStudioAnalytics).mockResolvedValue(mockAnalyticsSummary);

      render(<StudioAnalyticsPage />);

      await waitFor(() => {
        expect(screen.getByText('Studio Analytics')).toBeDefined();
      });

      const button7d = screen.getByText('Last 7 Days');
      fireEvent.click(button7d);

      await waitFor(() => {
        expect(analyticsApi.getStudioAnalytics).toHaveBeenCalledTimes(2);
      });
    });

    it('renders empty zero-state when studio has no telemetry data yet', async () => {
      const emptySummary: StudioAnalyticsSummaryDto = {
        ...mockAnalyticsSummary,
        totalProfileViews: 0,
        totalProjectViews: 0,
        totalDiscoveryImpressions: 0,
        totalInquiriesOpened: 0,
        totalLeadsCreated: 0,
        totalLeadsWon: 0,
        topProjects: [],
        timeseries: [],
      };
      vi.mocked(analyticsApi.getStudioAnalytics).mockResolvedValue(emptySummary);

      render(<StudioAnalyticsPage />);

      await waitFor(() => {
        expect(screen.getByText('No Visitor Traffic Recorded Yet')).toBeDefined();
      });
    });
  });

  describe('PublicTelemetryTracker', () => {
    it('fires trackPublicEvent once on mount with sanitized payload', async () => {
      render(
        <PublicTelemetryTracker
          eventType="PUBLIC_PROFILE_VIEW"
          entityType="STUDIO"
          entitySlug="zen-designs"
          metadata={{ categoryCode: 'LIVING_ROOM' }}
        />
      );

      await waitFor(() => {
        expect(analyticsApi.trackPublicEvent).toHaveBeenCalledWith(
          expect.objectContaining({
            eventType: 'PUBLIC_PROFILE_VIEW',
            entityType: 'STUDIO',
            entitySlug: 'zen-designs',
            metadata: { categoryCode: 'LIVING_ROOM' },
          })
        );
      });
    });
  });
});
