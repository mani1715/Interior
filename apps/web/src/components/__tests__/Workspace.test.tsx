import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { WorkspaceShell } from '@/components/workspace/WorkspaceShell';
import WorkspaceHomePage from '@/app/workspace/page';
import BusinessProfilePage from '@/app/workspace/business/page';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import ProfessionalOnboardingPage from '@/app/onboarding/professional/page';
import { AuthContext, AuthContextType } from '@/lib/auth/auth-context';
import * as workspaceApi from '@/lib/workspace/api';
import * as apiClient from '@/lib/api-client';
import { WorkspaceSummary, BusinessProfile } from '@/lib/workspace/types';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
  }),
  usePathname: () => '/workspace',
}));

beforeEach(() => {
  window.scrollTo = vi.fn();
  vi.clearAllMocks();
});

const mockSummaryData: WorkspaceSummary = {
  user: {
    displayName: 'Srinivasa Rao',
    email: 'srinivasa@interiors.com',
    roles: ['DESIGNER', 'CUSTOMER'],
  },
  studio: {
    id: 'studio-999',
    name: 'Srinivasa Interiors',
    slug: 'srinivasa-interiors',
    professionalType: 'INTERIOR_STUDIO',
    professionalTitle: 'Principal Architect',
    tagline: 'Soulful living spaces',
    role: 'OWNER',
    operationalStatus: 'ACTIVE',
    publicationStatus: 'UNPUBLISHED',
    city: 'Hyderabad',
    state: 'Telangana',
    serviceCount: 2,
    specialtyCount: 2,
    serviceAreaCount: 2,
    contactCount: 2,
  },
  availableStudios: [
    {
      studioId: 'studio-999',
      studioName: 'Srinivasa Interiors',
      studioSlug: 'srinivasa-interiors',
      role: 'OWNER',
    },
  ],
  completeness: {
    profileCompletenessPercentage: 100,
    platformReadinessPercentage: 50,
    status: 'PROFILE_COMPLETED',
    breakdown: {
      identityScore: 20,
      locationScore: 20,
      servicesScore: 20,
      specialtiesScore: 20,
      contactsScore: 20,
      portfolioScore: 0,
      projectsScore: 0,
    },
  },
  setupChecklist: [
    {
      id: 'registration',
      title: 'Professional Registration',
      description: 'Account verified and workspace activated.',
      completed: true,
      actionLabel: 'View',
      actionRoute: '/workspace',
    },
    {
      id: 'identity',
      title: 'Business Identity',
      description: 'Studio handle and classification defined.',
      completed: true,
      actionLabel: 'View Profile',
      actionRoute: '/workspace/business',
    },
    {
      id: 'portfolio',
      title: 'Professional Portfolio',
      description: 'Public portfolio website layout.',
      completed: false,
      actionLabel: 'Prepare Portfolio',
      actionRoute: '/workspace/portfolio',
    },
    {
      id: 'projects',
      title: 'Add First Project',
      description: 'Showcase interior work.',
      completed: false,
      actionLabel: 'View Projects',
      actionRoute: '/workspace/projects',
    },
  ],
  modules: [
    {
      id: 'portfolio',
      name: 'Portfolio',
      description: 'Build your professional portfolio website.',
      status: 'NOT_CONFIGURED',
      route: '/workspace/portfolio',
      ctaLabel: 'Prepare Portfolio',
    },
    {
      id: 'projects',
      name: 'Projects',
      description: 'Showcase completed projects.',
      status: 'NOT_STARTED',
      route: '/workspace/projects',
      ctaLabel: 'View Project Workspace',
    },
    {
      id: 'business',
      name: 'Business Profile',
      description: 'View and manage studio profile.',
      status: 'READY',
      route: '/workspace/business',
      ctaLabel: 'View Business Profile',
    },
  ],
  activityFeed: [
    {
      id: 'act-1',
      title: 'Professional Onboarding Completed',
      description: 'Studio profile registered.',
      timestamp: '2026-09-18T10:00:00Z',
      type: 'ONBOARDING',
    },
  ],
};

const mockBusinessProfileData: BusinessProfile = {
  id: 'studio-999',
  name: 'Srinivasa Interiors',
  slug: 'srinivasa-interiors',
  professionalType: 'INTERIOR_STUDIO',
  professionalTitle: 'Principal Architect',
  tagline: 'Soulful living spaces',
  experienceSinceYear: 2018,
  teamSize: '5-10',
  budgetRange: '15L-35L',
  addressLine: '123 Lake View Rd',
  city: 'Hyderabad',
  district: 'Hyderabad',
  state: 'Telangana',
  postalCode: '500034',
  country: 'IN',
  travelAvailable: true,
  gstRegistered: true,
  gstNumber: '37AAAAA0000A1Z5',
  operationalStatus: 'ACTIVE',
  publicationStatus: 'UNPUBLISHED',
  roleInStudio: 'OWNER',
  onboardingCompletedAt: '2026-09-18T10:00:00Z',
  contacts: [
    {
      kind: 'PHONE',
      value: '+919876543210',
      publicConsent: true,
      visibilityLabel: 'Public when portfolio published',
      sortOrder: 1,
    },
    {
      kind: 'EMAIL',
      value: 'contact@srinivasa.com',
      publicConsent: false,
      visibilityLabel: 'Private / Internal only',
      sortOrder: 2,
    },
  ],
  services: [
    { code: 'MODULAR_KITCHEN', name: 'Modular Kitchen' },
    { code: 'FULL_HOME', name: 'Full Home Interior' },
  ],
  specialties: [
    { code: 'WARM_CONTEMPORARY', name: 'Warm Contemporary' },
  ],
  serviceAreas: [
    { city: 'Hyderabad', locality: 'Banjara Hills' },
  ],
};

const mockDesignerAuth: AuthContextType = {
  user: {
    id: 'user-des-123',
    displayName: 'Srinivasa Rao',
    email: 'srinivasa@interiors.com',
    status: 'ACTIVE',
    roles: ['DESIGNER'],
    permissions: [],
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

const mockCustomerAuth: AuthContextType = {
  user: {
    id: 'user-cust-456',
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

describe('Phase 09: Designer Dashboard & Professional Workspace', () => {
  it('renders WorkspaceShell with active studio and navigation items for authenticated DESIGNER', async () => {
    vi.spyOn(workspaceApi, 'fetchWorkspaceSummary').mockResolvedValue(mockSummaryData);

    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <WorkspaceShell>
          <div>Workspace Content Child</div>
        </WorkspaceShell>
      </AuthContext.Provider>
    );

    // Verify studio details in sidebar
    await waitFor(() => {
      expect(screen.getAllByText('Srinivasa Interiors').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('OWNER').length).toBeGreaterThanOrEqual(1);
    });

    // Primary nav items in sidebar and mobile nav
    expect(screen.getAllByRole('link', { name: /home/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('link', { name: /portfolio/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('link', { name: /projects/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('link', { name: /ai studio/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('link', { name: /business profile/i }).length).toBeGreaterThanOrEqual(1);

    // Content child rendered
    expect(screen.getByText('Workspace Content Child')).toBeDefined();
  });

  it('opens mobile bottom sheet More menu with secondary module links', async () => {
    vi.spyOn(workspaceApi, 'fetchWorkspaceSummary').mockResolvedValue(mockSummaryData);

    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <WorkspaceShell>
          <div>Workspace Content Child</div>
        </WorkspaceShell>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getAllByText('Srinivasa Interiors').length).toBeGreaterThanOrEqual(1);
    });

    // Click "More" button in mobile bottom nav
    const moreBtn = screen.getByRole('button', { name: /open more modules menu/i });
    fireEvent.click(moreBtn);

    // Check bottom sheet modal content
    expect(screen.getByText('Workspace Modules')).toBeDefined();
    expect(screen.getByRole('link', { name: /media library/i })).toBeDefined();
    expect(screen.getByRole('link', { name: /leads & clients/i })).toBeDefined();
    expect(screen.getAllByRole('link', { name: /seo center/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('link', { name: /analytics/i }).length).toBeGreaterThanOrEqual(1);
  });

  it('renders CUSTOMER guidance prompt redirecting to /onboarding/professional', async () => {
    render(
      <AuthContext.Provider value={mockCustomerAuth}>
        <WorkspaceShell>
          <div>Protected Content</div>
        </WorkspaceShell>
      </AuthContext.Provider>
    );

    expect(screen.getByText('Register Your Professional Studio')).toBeDefined();
    const ctaLink = screen.getByRole('link', { name: /complete professional onboarding/i });
    expect(ctaLink.getAttribute('href')).toBe('/onboarding/professional');
    expect(screen.queryByText('Protected Content')).toBeNull();
  });

  it('renders WorkspaceHomePage with operational/publication badges, dual completeness, and setup checklist', async () => {
    vi.spyOn(workspaceApi, 'fetchWorkspaceSummary').mockResolvedValue(mockSummaryData);

    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <WorkspaceHomePage />
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Welcome back, Srinivasa Rao')).toBeDefined();
    });

    // Operational and publication status badges
    expect(screen.getByText('Operational: ACTIVE')).toBeDefined();
    expect(screen.getByText('Public: UNPUBLISHED')).toBeDefined();

    // Dual completeness scores
    expect(screen.getByText('100%')).toBeDefined();
    expect(screen.getByText('50%')).toBeDefined();
    expect(screen.getByText('Professional Profile Completeness')).toBeDefined();
    expect(screen.getByText('Platform Launch Readiness')).toBeDefined();

    // Setup checklist
    expect(screen.getByText('Studio Setup Checklist')).toBeDefined();
    expect(screen.getByText('Professional Registration')).toBeDefined();
    expect(screen.getByText('Professional Portfolio')).toBeDefined();

    // Module readiness cards
    expect(screen.getByText('Not Configured')).toBeDefined();
    expect(screen.getByText('Not Started')).toBeDefined();
  });

  it('renders BusinessProfilePage with real studio attributes, GSTIN, and contact privacy labels', async () => {
    vi.spyOn(workspaceApi, 'fetchBusinessProfile').mockResolvedValue(mockBusinessProfileData);

    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <BusinessProfilePage />
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Srinivasa Interiors')).toBeDefined();
    });

    expect(screen.getByText('@srinivasa-interiors')).toBeDefined();
    expect(screen.getByText('37AAAAA0000A1Z5')).toBeDefined();

    // Privacy visibility labels
    expect(screen.getByText('Public when portfolio published')).toBeDefined();
    expect(screen.getByText('Private / Internal only')).toBeDefined();
  });

  it('PublicHeader displays Workspace link for DESIGNER user', () => {
    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <PublicHeader />
      </AuthContext.Provider>
    );

    const workspaceLinks = screen.getAllByRole('link', { name: /workspace/i });
    expect(workspaceLinks.length).toBeGreaterThanOrEqual(1);
    expect(workspaceLinks[0].getAttribute('href')).toBe('/workspace');
  });

  it('ProfessionalOnboardingPage success CTA links to /workspace with "Continue to Professional Workspace"', async () => {
    vi.spyOn(apiClient, 'apiFetch').mockImplementation(async (endpoint) => {
      if (endpoint === '/designers/onboarding/status') {
        return {
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
        };
      }
      return {};
    });

    render(
      <AuthContext.Provider value={mockDesignerAuth}>
        <ProfessionalOnboardingPage />
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Welcome to Elégance Professional')).toBeDefined();
    });

    const workspaceCta = screen.getByRole('link', { name: /continue to professional workspace/i });
    expect(workspaceCta.getAttribute('href')).toBe('/workspace');
  });
});
