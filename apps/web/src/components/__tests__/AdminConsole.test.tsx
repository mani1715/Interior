import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import AdminLayout from '@/app/admin/layout';
import AdminDashboardPage from '@/app/admin/page';
import AdminUsersPage from '@/app/admin/users/page';
import AdminStudiosPage from '@/app/admin/studios/page';
import AdminVerificationPage from '@/app/admin/verification/page';
import AdminReviewsPage from '@/app/admin/reviews/page';
import AdminOperationsPage from '@/app/admin/operations/page';
import * as adminApi from '@/lib/admin/api';
import * as authContext from '@/lib/auth/auth-context';

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  usePathname: () => '/admin',
}));

vi.mock('@/lib/admin/api', () => ({
  fetchAdminDashboard: vi.fn(),
  fetchAdminUsers: vi.fn(),
  updateUserStatus: vi.fn(),
  fetchAdminStudios: vi.fn(),
  updateStudioStatus: vi.fn(),
  fetchAdminVerifications: vi.fn(),
  recordVerificationDecision: vi.fn(),
  fetchAdminReviews: vi.fn(),
  moderateReview: vi.fn(),
  fetchAdminAuditLogs: vi.fn(),
  fetchOperationalState: vi.fn(),
  updateOperationalState: vi.fn(),
  fetchProviderMatrix: vi.fn(),
  fetchOperationalJobs: vi.fn(),
  triggerOperationalJob: vi.fn(),
  resetUserMfa: vi.fn(),
}));

describe('Phase 29 — Admin Console & Operations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('blocks non-admin users from accessing the admin console', () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'usr-customer',
        displayName: 'Customer Jane',
        email: 'jane@example.com',
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
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    render(
      <AdminLayout>
        <div>Protected Content</div>
      </AdminLayout>
    );

    expect(screen.getByText('Administrator Access Required')).toBeDefined();
    expect(screen.queryByText('Protected Content')).toBeNull();
  });

  it('blocks studio owners who lack platform admin role', () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'usr-designer',
        displayName: 'Designer Bob',
        email: 'bob@example.com',
        status: 'ACTIVE',
        roles: ['DESIGNER'],
        permissions: [],
        activeStudioId: 'studio-123',
        activeStudioRole: 'OWNER',
        studios: [],
        assurance: 'MFA',
      },
      isLoading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    render(
      <AdminLayout>
        <div>Protected Content</div>
      </AdminLayout>
    );

    expect(screen.getByText('Administrator Access Required')).toBeDefined();
    expect(screen.queryByText('Protected Content')).toBeNull();
  });

  it('allows platform admin and renders dashboard metrics correctly', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'usr-admin',
        displayName: 'Platform Admin',
        email: 'admin@platform.local',
        status: 'ACTIVE',
        roles: ['ADMIN'],
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'MFA',
      },
      isLoading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.mocked(adminApi.fetchAdminDashboard).mockResolvedValue({
      totalStudios: 12,
      activeStudios: 11,
      suspendedStudios: 1,
      publishedStudios: 9,
      totalUsers: 45,
      activeUsers: 44,
      suspendedUsers: 1,
      totalProjects: 30,
      publicProjects: 24,
      totalLeads: 85,
      pendingVerifications: 2,
      pendingReviewReports: 3,
      totalMediaAssets: 150,
      totalAiGenerations: 90,
      recentActivity: [
        {
          id: 'log-001',
          studioId: null,
          actorId: 'usr-admin',
          actorEmail: 'admin@platform.local',
          action: 'USER_STATUS_CHANGED',
          resourceType: 'USER',
          resourceId: 'usr-test',
          requestId: 'req-123',
          details: '{"newStatus":"SUSPENDED"}',
          timestamp: '2026-10-01T10:00:00Z',
        },
      ],
    });

    render(
      <AdminLayout>
        <AdminDashboardPage />
      </AdminLayout>
    );

    await waitFor(() => {
      expect(screen.getByText('Platform Operations Dashboard')).toBeDefined();
      expect(screen.getByText('11')).toBeDefined(); // active studios
      expect(screen.getByText('44')).toBeDefined(); // active users
      expect(screen.getByText('Recent System Activity')).toBeDefined();
      expect(screen.getByText('USER_STATUS_CHANGED')).toBeDefined();
    });
  });

  it('renders user management and allows opening status dialog', async () => {
    vi.mocked(adminApi.fetchAdminUsers).mockResolvedValue([
      {
        id: 'usr-test-1',
        displayName: 'Spam User',
        email: 'spam@example.com',
        phone: null,
        status: 'ACTIVE',
        createdAt: '2026-09-01T00:00:00Z',
        roles: ['CUSTOMER'],
        studioMemberships: [],
      },
    ]);

    render(<AdminUsersPage />);

    await waitFor(() => {
      expect(screen.getByText('User Management')).toBeDefined();
      expect(screen.getByText('Spam User')).toBeDefined();
      expect(screen.getByText('Change Status')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Change Status'));
    expect(screen.getByText('Update Account Status')).toBeDefined();
  });

  it('renders studio management and allows opening suspension dialog', async () => {
    vi.mocked(adminApi.fetchAdminStudios).mockResolvedValue([
      {
        id: 'studio-test-1',
        name: 'Atelier Noir',
        slug: 'atelier-noir',
        ownerId: 'usr-owner-1',
        ownerEmail: 'owner@atelier.com',
        status: 'ACTIVE',
        publicationStatus: 'PUBLISHED',
        verificationStatus: 'VERIFIED',
        projectCount: 5,
        publicProjectCount: 4,
        createdAt: '2026-08-01T00:00:00Z',
      },
    ]);

    render(<AdminStudiosPage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Management')).toBeDefined();
      expect(screen.getByText('Atelier Noir')).toBeDefined();
      expect(screen.getByText('Suspend')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Suspend'));
    expect(screen.getByText('Suspend Studio Tenant')).toBeDefined();
  });

  it('renders verification review and allows reviewing pending requests', async () => {
    vi.mocked(adminApi.fetchAdminVerifications).mockResolvedValue([
      {
        id: 'ver-001',
        studioId: 'studio-test-1',
        studioName: 'Atelier Noir',
        studioSlug: 'atelier-noir',
        businessName: 'Atelier Noir Designs Pvt Ltd',
        professionalType: 'INTERIOR_DESIGNER',
        status: 'PENDING',
        registrationNumber: 'REG-12345',
        gstNumber: '29ABCDE1234F1Z5',
        websiteDomain: 'https://ateliernoir.com',
        notes: 'Ready for official verification.',
        decisionReason: null,
        verifiedAt: null,
        expiresAt: null,
        submittedAt: '2026-09-20T00:00:00Z',
        documentCount: 2,
      },
    ]);

    render(<AdminVerificationPage />);

    await waitFor(() => {
      expect(screen.getByText('Verification Reviews')).toBeDefined();
      expect(screen.getByText('Atelier Noir Designs Pvt Ltd')).toBeDefined();
      expect(screen.getByText('Review')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Review'));
    expect(screen.getByText('Verification Decision')).toBeDefined();
  });

  it('renders review moderation and allows moderating flagged reviews', async () => {
    vi.mocked(adminApi.fetchAdminReviews).mockResolvedValue([
      {
        id: 'rev-001',
        studioId: 'studio-test-1',
        studioName: 'Atelier Noir',
        leadId: 'lead-001',
        rating: 1,
        title: 'Spam content',
        reviewText: 'This is an abusive review with spam links.',
        reviewerDisplayName: 'Fake Reviewer',
        displayNameMode: 'FIRST_NAME',
        status: 'FLAGGED',
        studioResponseText: null,
        studioResponseAt: null,
        submittedAt: '2026-09-25T00:00:00Z',
        publishedAt: null,
        reportCount: 2,
      },
    ]);

    render(<AdminReviewsPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Review Moderation' })).toBeDefined();
      expect(screen.getByText(/Fake Reviewer/)).toBeDefined();
      expect(screen.getByText('Remove')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Remove'));
    expect(screen.getByRole('heading', { name: 'Moderate Review' })).toBeDefined();
  });

  it('renders operations page with platform state, providers and jobs', async () => {
    vi.mocked(adminApi.fetchOperationalState).mockResolvedValue({
      id: 'default',
      maintenanceMode: 'NORMAL',
      maintenanceMessage: null,
      uploadsEnabled: true,
      aiEnabled: true,
      registrationsEnabled: true,
      updatedBy: null,
      updatedAt: '2026-10-01T12:00:00Z',
    });

    vi.mocked(adminApi.fetchProviderMatrix).mockResolvedValue({
      providers: {
        DATABASE: {
          status: 'AVAILABLE',
          providerName: 'PostgreSQL Primary',
          details: 'Healthy',
        },
        STORAGE: {
          status: 'AVAILABLE',
          providerName: 'Object Storage S3',
          details: 'Healthy',
        },
      },
      timestamp: '2026-10-01T12:00:00Z',
    });

    vi.mocked(adminApi.fetchOperationalJobs).mockResolvedValue([
      {
        id: 'job-run-1',
        jobKey: 'SESSION_CLEANUP',
        instanceId: 'node-1',
        status: 'COMPLETED',
        startedAt: '2026-10-01T11:00:00Z',
        completedAt: '2026-10-01T11:01:00Z',
        processedCount: 14,
        failureCode: null,
        errorSummary: null,
      },
    ]);

    render(<AdminOperationsPage />);

    await waitFor(() => {
      expect(screen.getByText('Operations')).toBeDefined();
      expect(screen.getByText('Platform Status')).toBeDefined();
      expect(screen.getByText('NORMAL')).toBeDefined();
      expect(screen.getByText('PostgreSQL Primary')).toBeDefined();
      expect(screen.getByText('SESSION_CLEANUP')).toBeDefined();
    });
  });

  it('allows super admin to open Reset MFA dialog and submit reason', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'usr-super',
        displayName: 'Super Administrator',
        email: 'super@platform.local',
        status: 'ACTIVE',
        roles: ['ADMIN', 'SUPER_ADMIN'],
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'MFA',
      },
      isLoading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.mocked(adminApi.fetchAdminUsers).mockResolvedValue([
      {
        id: 'usr-target-1',
        displayName: 'Designer User',
        email: 'designer@studio.com',
        phone: null,
        status: 'ACTIVE',
        createdAt: '2026-09-01T00:00:00Z',
        roles: ['DESIGNER'],
        studioMemberships: ['Studio Alpha'],
      },
    ]);

    vi.mocked(adminApi.resetUserMfa).mockResolvedValue();

    render(<AdminUsersPage />);

    await waitFor(() => {
      expect(screen.getByText('Reset MFA')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Reset MFA'));

    expect(screen.getByText('Reset Multi-Factor Authentication')).toBeDefined();
    expect(screen.getByText(/This will remove the user's currently enrolled MFA method/)).toBeDefined();

    const textarea = screen.getByPlaceholderText(/Device lost \/ replaced by employee/);
    fireEvent.change(textarea, { target: { value: 'Verified caller identity via authenticated callback' } });

    fireEvent.click(screen.getByText('Confirm Reset MFA'));

    await waitFor(() => {
      expect(adminApi.resetUserMfa).toHaveBeenCalledWith('usr-target-1', {
        reason: 'Verified caller identity via authenticated callback',
      });
    });
  });
});
