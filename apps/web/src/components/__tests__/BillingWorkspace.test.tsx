import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
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
    STORAGE_LIMIT_BYTES: null, // Unlimited
    AI_MONTHLY_CREDITS: null, // Unlimited
    TEAM_MEMBERS: null, // Unlimited
    LEAD_MANAGEMENT: true,
    ADVANCED_ANALYTICS: true,
  },
  availablePlans: [], // No commercial plans configured
  billingProviderStatus: 'NOT_CONFIGURED',
  commercialCheckoutEnabled: false,
  recentTransactions: [],
};

describe('Phase 28 — Billing & Plans Subsystem', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Studio Billing Workspace Page', () => {
    it('truthfully renders operational BASE plan and unlimited platform entitlements', async () => {
      vi.mocked(billingApi.getStudioBillingSummary).mockResolvedValue(mockBaseBillingSummary);

      render(<StudioBillingPage />);

      await waitFor(() => {
        expect(screen.getByText('Billing & Plans')).toBeDefined();
      });

      // Active Plan Card
      expect(screen.getByText('Platform Base')).toBeDefined();
      expect(screen.getByText('₹0')).toBeDefined();
      expect(screen.getByText('Active Operational Access')).toBeDefined();

      // Entitlements list
      expect(screen.getByText('Portfolio Projects')).toBeDefined();
      expect(screen.getByText('Unlimited capacity')).toBeDefined();
      expect(screen.getByText('Cloud Storage')).toBeDefined();
      expect(screen.getByText('Unlimited asset storage')).toBeDefined();

      // Verification separation invariant
      expect(screen.getByText('Independent review (not purchasable)')).toBeDefined();

      // Provider not configured banner
      expect(screen.getByText('Billing Provider: NOT_CONFIGURED')).toBeDefined();

      // No payment required badge
      expect(screen.getByText('No Payment Required')).toBeDefined();
    });

    it('displays transaction history when present', async () => {
      const summaryWithTransactions: StudioBillingSummaryDto = {
        ...mockBaseBillingSummary,
        recentTransactions: [
          {
            id: 'txn-001',
            studioId: 'studio-001',
            provider: 'DISABLED',
            providerPaymentId: 'txn_init_001',
            description: 'Operational Baseline Tier Setup',
            amountMinor: 0,
            currency: 'INR',
            status: 'SUCCEEDED',
            occurredAt: '2026-03-20T10:00:00Z',
            createdAt: '2026-03-20T10:00:00Z',
          },
        ],
      };
      vi.mocked(billingApi.getStudioBillingSummary).mockResolvedValue(summaryWithTransactions);

      render(<StudioBillingPage />);

      await waitFor(() => {
        expect(screen.getByText('Billing History')).toBeDefined();
      });

      expect(screen.getByText('Operational Baseline Tier Setup')).toBeDefined();
      expect(screen.getByText(/SUCCEEDED/)).toBeDefined();
    });
  });
});
