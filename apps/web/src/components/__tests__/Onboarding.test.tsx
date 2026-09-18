import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import AccountPage from '@/app/account/page';
import ProfessionalOnboardingPage from '@/app/onboarding/professional/page';
import { AuthContext, AuthContextType } from '@/lib/auth/auth-context';
import * as apiClient from '@/lib/api-client';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
  }),
}));

// Mock window.scrollTo
beforeEach(() => {
  window.scrollTo = vi.fn();
  vi.clearAllMocks();
});

const mockCustomerAuth: AuthContextType = {
  user: {
    id: 'user-cust-123',
    displayName: 'Vijay Reddy',
    email: 'vijay@example.com',
    status: 'ACTIVE',
    roles: ['CUSTOMER'],
    permissions: [],
    activeStudioId: null,
    activeStudioRole: null,
    studios: [],
    assurance: 'PASSWORD',
  },
  isLoading: false,
  isAuthenticated: true,
  refreshUser: vi.fn().mockResolvedValue(undefined),
  loginDevPersona: vi.fn().mockResolvedValue(undefined),
  logout: vi.fn().mockResolvedValue(undefined),
  revokeAllSessions: vi.fn().mockResolvedValue(undefined),
};

const mockDesignerAuth: AuthContextType = {
  user: {
    id: 'user-des-456',
    displayName: 'Srinivasa Rao',
    email: 'srinivasa@interiors.com',
    status: 'ACTIVE',
    roles: ['DESIGNER'],
    permissions: ['project:create', 'project:edit'],
    activeStudioId: 'studio-999',
    activeStudioRole: 'OWNER',
    studios: [
      {
        studioId: 'studio-999',
        studioName: 'Srinivasa Interiors',
        studioSlug: 'srinivasa-interiors',
        role: 'OWNER',
      },
    ],
    assurance: 'PASSWORD',
  },
  isLoading: false,
  isAuthenticated: true,
  refreshUser: vi.fn().mockResolvedValue(undefined),
  loginDevPersona: vi.fn().mockResolvedValue(undefined),
  logout: vi.fn().mockResolvedValue(undefined),
  revokeAllSessions: vi.fn().mockResolvedValue(undefined),
};

describe('Phase 08 — Designer Onboarding Frontend Tests', () => {

  describe('PublicHeader Role Promotion Awareness', () => {
    it('renders "Register as Professional" CTA for authenticated CUSTOMER accounts', () => {
      render(
        <AuthContext.Provider value={mockCustomerAuth}>
          <PublicHeader />
        </AuthContext.Provider>
      );
      // Both desktop and mobile have the CTA
      const ctas = screen.getAllByText('Register as Professional');
      expect(ctas.length).toBeGreaterThanOrEqual(1);
    });

    it('does NOT render "Register as Professional" CTA for accounts with DESIGNER role', () => {
      render(
        <AuthContext.Provider value={mockDesignerAuth}>
          <PublicHeader />
        </AuthContext.Provider>
      );
      expect(screen.queryByText('Register as Professional')).toBeNull();
      expect(screen.getAllByText('DESIGNER').length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('AccountPage Studio Tenancy Context', () => {
    it('displays Customer Account Boundary and onboarding link for customers without studios', () => {
      render(
        <AuthContext.Provider value={mockCustomerAuth}>
          <AccountPage />
        </AuthContext.Provider>
      );
      expect(screen.getByText('Customer Account Boundary')).toBeDefined();
      expect(screen.getByText('Register Studio as Professional')).toBeDefined();
    });

    it('displays studio ownership details with ACTIVE and UNPUBLISHED statuses for designers', () => {
      render(
        <AuthContext.Provider value={mockDesignerAuth}>
          <AccountPage />
        </AuthContext.Provider>
      );
      expect(screen.getByText('Srinivasa Interiors')).toBeDefined();
      expect(screen.getByText('@srinivasa-interiors')).toBeDefined();
      expect(screen.getAllByText('ACTIVE').length).toBeGreaterThanOrEqual(2);
      expect(screen.getByText('UNPUBLISHED')).toBeDefined();
      expect(screen.getByText('Designer Workspace')).toBeDefined();
    });
  });

  describe('Professional Onboarding Wizard Flow', () => {
    beforeEach(() => {
      // Mock default onboarding status: new draft
      vi.spyOn(apiClient, 'apiFetch').mockImplementation(async (url: string) => {
        if (url.includes('/status')) {
          return {
            onboardingCompleted: false,
            currentStep: 1,
            draftPayload: null,
            studio: null,
          };
        }
        if (url.includes('/check-slug')) {
          return {
            slug: 'srinivasa-atelier',
            available: true,
          };
        }
        if (url.includes('/draft')) {
          return { success: true };
        }
        if (url.includes('/complete')) {
          return {
            studio: {
              id: 'new-studio-123',
              name: 'Srinivasa Atelier',
              slug: 'srinivasa-atelier',
              professionalType: 'INTERIOR_STUDIO',
              status: 'ACTIVE',
              publicationStatus: 'UNPUBLISHED',
              role: 'OWNER',
            },
            newCsrfToken: 'rotated-csrf-token',
            message: 'Professional onboarding completed successfully',
          };
        }
        return {};
      });
    });

    it('renders Step 1 with 7 professional categories', async () => {
      render(
        <AuthContext.Provider value={mockCustomerAuth}>
          <ProfessionalOnboardingPage />
        </AuthContext.Provider>
      );

      await waitFor(() => {
        expect(screen.getByText('Select Your Professional Category')).toBeDefined();
      });

      expect(screen.getByText('Individual Interior Designer')).toBeDefined();
      expect(screen.getByText('Interior Design Studio')).toBeDefined();
      expect(screen.getByText('Individual Architect')).toBeDefined();
      expect(screen.getByText('Architecture & Design Firm')).toBeDefined();
      expect(screen.getByText('Custom Furniture Studio')).toBeDefined();
      expect(screen.getByText('Woodwork & Cabinetry Specialist')).toBeDefined();
      expect(screen.getByText('Turnkey Execution Contractor')).toBeDefined();
    });

    it('advances from Step 1 to Step 2 when clicking Continue', async () => {
      render(
        <AuthContext.Provider value={mockCustomerAuth}>
          <ProfessionalOnboardingPage />
        </AuthContext.Provider>
      );

      await waitFor(() => {
        expect(screen.getByText('Select Your Professional Category')).toBeDefined();
      });

      const continueBtn = screen.getByText('Continue');
      fireEvent.click(continueBtn);

      await waitFor(() => {
        expect(screen.getByText('Studio Identity & Platform Handle')).toBeDefined();
      });
    });

    it('validates Step 2 requires studio name and slug', async () => {
      render(
        <AuthContext.Provider value={mockCustomerAuth}>
          <ProfessionalOnboardingPage />
        </AuthContext.Provider>
      );

      await waitFor(() => {
        expect(screen.getByText('Select Your Professional Category')).toBeDefined();
      });
      fireEvent.click(screen.getByText('Continue'));

      await waitFor(() => {
        expect(screen.getByText('Studio Identity & Platform Handle')).toBeDefined();
      });

      // Try to continue without filling studio name
      fireEvent.click(screen.getByText('Continue'));

      await waitFor(() => {
        expect(screen.getByText('Studio / Business Name must be at least 2 characters.')).toBeDefined();
      });
    });

    it('renders Step 8 success screen when user already completed onboarding', async () => {
      vi.spyOn(apiClient, 'apiFetch').mockResolvedValueOnce({
        onboardingCompleted: true,
        currentStep: 7,
        draftPayload: null,
        studio: {
          id: 'existing-studio-789',
          name: 'Deccan Studio',
          slug: 'deccan-studio',
          professionalType: 'ARCHITECTURE_STUDIO',
          status: 'ACTIVE',
          publicationStatus: 'UNPUBLISHED',
          role: 'OWNER',
        },
      });

      render(
        <AuthContext.Provider value={mockDesignerAuth}>
          <ProfessionalOnboardingPage />
        </AuthContext.Provider>
      );

      await waitFor(() => {
        expect(screen.getByText('Welcome to Elégance Professional')).toBeDefined();
        expect(screen.getByText('Deccan Studio')).toBeDefined();
        expect(screen.getByText('@deccan-studio')).toBeDefined();
        expect(screen.getByText('ACTIVE')).toBeDefined();
        expect(screen.getByText('UNPUBLISHED')).toBeDefined();
      });
    });
  });
});
