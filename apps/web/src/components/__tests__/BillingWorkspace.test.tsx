import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import StudioBillingPage from '@/app/workspace/billing/page';
import * as billingApi from '@/lib/billing/api';
import { StudioBillingSummaryDto } from '@/lib/billing/types';

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  usePathname: () => '/workspace/billing',
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

vi.mock('@/lib/billing/api', () => ({
  getStudioBillingSummary: vi.fn(),
  createCheckoutSession: vi.fn(),
  cancelSubscription: vi.fn(),
}));

const mockBaseBillingSummary: StudioBillingSummaryDto = {
  studioId: 'studio-001',
  activeSubscription: null,
  currentPlan: {
    id: 'plan-base-001',
    code: 'BASE',
    name: 'Platform Base',
    description: 'Internal non-commercial platform baseline tier.',
    billingPeriod: 'NONE',
    currency: 'INR',
    priceMinor: 0,
    active: true,
    purchasable: false,
    displayOrder: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  effectiveEntitlements: {
    PROJECT_LIMIT: null, // Unlimited
    PROJECT_PHOTO_LIMIT: null, // Unlimited
    CINEMATIC_PORTFOLIO: true,
    CINEMATIC_PROJECT_LIMIT: null, // Unlimited
    STORAGE_LIMIT_BYTES: null, // Unlimited
    AI_MONTHLY_CREDITS: null, // Unlimited
    LEADS_CRM: true,
    ANALYTICS_BASIC: true,
    ANALYTICS_ADVANCED: true,
  },
  availablePlans: [
    {
      id: 'plan-std-002',
      code: 'STANDARD',
      name: 'Standard',
      description: 'Essential portfolio presence for independent interior designers.',
      currency: 'INR',
      priceMinor: 0,
      active: true,
      purchasable: false,
      displayOrder: 1,
      entitlements: {
        PROJECT_LIMIT: 10,
        PROJECT_PHOTO_LIMIT: 15,
        CINEMATIC_PORTFOLIO: false,
        CINEMATIC_PROJECT_LIMIT: 0,
      },
    },
    {
      id: 'plan-prem-003',
      code: 'PREMIUM',
      name: 'Premium',
      description: 'Expanded project and media capacity for growing design studios.',
      currency: 'INR',
      priceMinor: 0,
      active: true,
      purchasable: false,
      displayOrder: 2,
      entitlements: {
        PROJECT_LIMIT: 20,
        PROJECT_PHOTO_LIMIT: 25,
        CINEMATIC_PORTFOLIO: false,
        CINEMATIC_PROJECT_LIMIT: 0,
      },
    },
    {
      id: 'plan-pro-004',
      code: 'PRO',
      name: 'Pro',
      description: 'Maximum capacity with cinematic project presentation.',
      currency: 'INR',
      priceMinor: 0,
      active: true,
      purchasable: false,
      displayOrder: 3,
      entitlements: {
        PROJECT_LIMIT: 20,
        PROJECT_PHOTO_LIMIT: 30,
        CINEMATIC_PORTFOLIO: true,
        CINEMATIC_PROJECT_LIMIT: 5,
      },
    },
  ],
  billingProviderStatus: 'NOT_CONFIGURED',
  commercialCheckoutEnabled: false,
  recentTransactions: [],
  usage: {
    projectCount: 4,
    projectLimit: null,
    cinematicProjectCount: 1,
    cinematicProjectLimit: null,
    cinematicPortfolioAllowed: true,
    storageBytesUsed: 1024 * 1024 * 50,
    storageLimitBytes: null,
    aiCreditsUsedThisMonth: 0,
    aiMonthlyCreditLimit: null,
  },
};

describe('Phase 30 / Phase 5 — Subscription & Entitlements Subsystem', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Studio Plan & Usage Workspace Page', () => {
    it('truthfully renders Plan & Usage, legacy Existing Studio Access badge, and capacity meters', async () => {
      vi.mocked(billingApi.getStudioBillingSummary).mockResolvedValue(mockBaseBillingSummary);

      render(<StudioBillingPage />);

      await waitFor(() => {
        expect(screen.getByText('Plan & Usage')).toBeDefined();
      });

      // Legacy BASE badge
      expect(screen.getAllByText('Existing Studio Access').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText(/Tier: BASE/)).toBeDefined();

      // Quotas
      expect(screen.getByText('Portfolio Projects')).toBeDefined();
      expect(screen.getByText('Photos / Project')).toBeDefined();
      expect(screen.getByText('Cinematic Presentations')).toBeDefined();

      // Three commercial plan cards
      expect(screen.getByText('Standard')).toBeDefined();
      expect(screen.getByText('Premium')).toBeDefined();
      expect(screen.getByText('Pro')).toBeDefined();

      // Provider not active banner
      expect(screen.getByText('Commercial Checkout Gateway: Not Active')).toBeDefined();
      expect(screen.getByText('No Payment Required')).toBeDefined();
    });

    it('opens plan review modal and displays safe capacity policy and checkout not available message', async () => {
      vi.mocked(billingApi.getStudioBillingSummary).mockResolvedValue(mockBaseBillingSummary);

      render(<StudioBillingPage />);

      await waitFor(() => {
        expect(screen.getByText('Plan & Usage')).toBeDefined();
      });

      // Click Review Pro button
      const reviewProBtn = screen.getByText('Review Pro');
      fireEvent.click(reviewProBtn);

      // Verify modal content
      expect(screen.getByText('Switch to Pro')).toBeDefined();
      expect(screen.getByText('Safe Capacity Policy')).toBeDefined();
      expect(screen.getByText(/Downgrading or switching plans never deletes/)).toBeDefined();
      expect(screen.getByText('Plan purchases are not available yet.')).toBeDefined();

      // Close review
      const closeBtn = screen.getByText('Close Review');
      fireEvent.click(closeBtn);
      expect(screen.queryByText('Switch to Pro')).toBeNull();
    });
  });
});
