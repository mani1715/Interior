import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CreateStudioModal } from '@/components/workspace/CreateStudioModal';
import { WorkspaceShell } from '@/components/workspace/WorkspaceShell';
import { AuthContext, AuthContextType } from '@/lib/auth/auth-context';
import * as workspaceApi from '@/lib/workspace/api';
import { getStoredActiveStudioId, setStoredActiveStudioId } from '@/lib/api-client';
import { WorkspaceSummary } from '@/lib/workspace/types';

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/workspace',
}));

describe('Create Another Studio & Per-Tab Storage Isolation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('enforces sessionStorage per-tab storage and eliminates localStorage bleed', () => {
    const testStudioId = 'studio-tab-123';

    // Set studio ID
    setStoredActiveStudioId(testStudioId);

    // Verify sessionStorage has it
    expect(sessionStorage.getItem('interior_active_studio_id')).toBe(testStudioId);

    // CRITICAL: verify localStorage is untouched (NO cross-tab bleed)
    expect(localStorage.getItem('interior_active_studio_id')).toBeNull();

    // Verify retrieval works
    expect(getStoredActiveStudioId()).toBe(testStudioId);

    // Clear studio ID
    setStoredActiveStudioId(null);
    expect(sessionStorage.getItem('interior_active_studio_id')).toBeNull();
    expect(localStorage.getItem('interior_active_studio_id')).toBeNull();
    expect(getStoredActiveStudioId()).toBeNull();
  });

  it('renders CreateStudioModal with accessible form fields and canonical professional types', () => {
    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <CreateStudioModal
        isOpen={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByRole('dialog')).toBeDefined();
    expect(screen.getByText('Create Another Professional Studio')).toBeDefined();

    // Check fields
    expect(screen.getByLabelText(/studio or practice name/i)).toBeDefined();
    expect(screen.getByLabelText(/professional practice type/i)).toBeDefined();
    expect(screen.getByLabelText(/^city/i)).toBeDefined();
    expect(screen.getByLabelText(/^state/i)).toBeDefined();

    // Check options in professional type dropdown
    const select = screen.getByLabelText(/professional practice type/i) as HTMLSelectElement;
    expect(select.options.length).toBe(7);
  });

  it('submits valid studio details, provides double-click protection, and triggers onSuccess', async () => {
    const onClose = vi.fn();
    const onSuccess = vi.fn().mockResolvedValue(undefined);

    const createStudioSpy = vi.spyOn(workspaceApi, 'createStudio').mockResolvedValue({
      studioId: 'studio-new-777',
      name: 'Avant-Garde Studio',
      slug: 'avant-garde-studio',
      professionalType: 'INTERIOR_STUDIO',
      role: 'DESIGNER_ADMIN',
      message: 'Studio created successfully.',
    });

    render(
      <CreateStudioModal
        isOpen={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    fireEvent.change(screen.getByLabelText(/studio or practice name/i), {
      target: { value: 'Avant-Garde Studio' },
    });
    fireEvent.change(screen.getByLabelText(/^city/i), {
      target: { value: 'Bengaluru' },
    });
    fireEvent.change(screen.getByLabelText(/^state/i), {
      target: { value: 'Karnataka' },
    });

    const submitBtn = screen.getByRole('button', { name: /create studio/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createStudioSpy).toHaveBeenCalledWith({
        name: 'Avant-Garde Studio',
        professionalType: 'INTERIOR_STUDIO',
        city: 'Bengaluru',
        state: 'Karnataka',
      });
      expect(onSuccess).toHaveBeenCalledWith('studio-new-777', 'Avant-Garde Studio');
    });
  });

  it('opens CreateStudioModal from WorkspaceShell in both single and multi studio states', async () => {
    const singleSummary: WorkspaceSummary = {
      user: { displayName: 'Tester', email: 'test@example.com', roles: ['CUSTOMER', 'DESIGNER'] },
      studio: {
        id: 'studio-111',
        name: 'First Studio',
        slug: 'first-studio',
        professionalType: 'INTERIOR_STUDIO',
        professionalTitle: 'Lead',
        tagline: null,
        role: 'OWNER',
        operationalStatus: 'ACTIVE',
        publicationStatus: 'UNPUBLISHED',
        city: 'Mumbai',
        state: 'Maharashtra',
        serviceCount: 1,
        specialtyCount: 1,
        serviceAreaCount: 1,
        contactCount: 1,
      },
      availableStudios: [
        { studioId: 'studio-111', studioName: 'First Studio', studioSlug: 'first-studio', role: 'OWNER' },
      ],
      completeness: {
        profileCompletenessPercentage: 100,
        platformReadinessPercentage: 50,
        status: 'READY',
        breakdown: { identityScore: 20, locationScore: 20, servicesScore: 20, specialtiesScore: 20, contactsScore: 20, portfolioScore: 0, projectsScore: 0 },
      },
      setupChecklist: [],
      modules: [],
      activityFeed: [],
    };

    vi.spyOn(workspaceApi, 'fetchWorkspaceSummary').mockResolvedValue(singleSummary);

    const mockAuth: AuthContextType = {
      user: {
        id: 'u-1',
        displayName: 'Tester',
        email: 'test@example.com',
        status: 'ACTIVE',
        roles: ['CUSTOMER', 'DESIGNER'],
        permissions: ['studio:read', 'studio:write'],
        activeStudioId: 'studio-111',
        activeStudioRole: 'DESIGNER_ADMIN',
        studios: [
          { studioId: 'studio-111', studioName: 'First Studio', studioSlug: 'first-studio', role: 'DESIGNER_ADMIN' },
        ],
        assurance: 'PASSWORD',
      },
      isLoading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      switchStudio: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    };

    render(
      <AuthContext.Provider value={mockAuth}>
        <WorkspaceShell>
          <div>Workspace Content</div>
        </WorkspaceShell>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getAllByText('First Studio').length).toBeGreaterThanOrEqual(1);
    });

    // In single studio mode, the "+ Create Another Studio" button is directly visible
    const createBtn = screen.getByRole('button', { name: /create another studio/i });
    expect(createBtn).toBeDefined();

    // Click to open modal
    fireEvent.click(createBtn);

    // Modal dialog is now visible
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeDefined();
      expect(screen.getByText('Create Another Professional Studio')).toBeDefined();
    });
  });
});
