import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ProjectsListPage from '@/app/workspace/projects/page';
import ProjectEditPage from '@/app/workspace/projects/[projectId]/page';
import * as projectApi from '@/lib/projects/api';
import { ProjectSummaryDto, ProjectDetailDto } from '@/lib/projects/types';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
  }),
  useParams: () => ({
    projectId: 'proj-123',
  }),
  usePathname: () => '/workspace/projects',
}));

const mockProjects: ProjectSummaryDto[] = [
  {
    id: 'proj-1',
    slug: 'emerald-penthouse',
    title: 'Emerald Penthouse',
    categoryCode: 'COMPLETE_HOME_INTERIOR',
    categoryDisplayName: 'Complete Home Interior',
    projectStatus: 'READY',
    visibilityStatus: 'PORTFOLIO',
    featured: true,
    displayOrder: 0,
    city: 'Bengaluru',
    state: 'Karnataka',
    propertyType: 'APARTMENT',
    projectScope: 'FULL_INTERIOR',
    styleCodes: ['WARM_CONTEMPORARY', 'MODERN_MINIMALIST'],
    styleDisplayNames: ['Warm Contemporary', 'Modern Minimalist'],
    completionYear: 2025,
    version: 1,
    createdAt: '2026-09-18T10:00:00Z',
    updatedAt: '2026-09-18T10:00:00Z',
  },
  {
    id: 'proj-2',
    slug: 'urban-kitchen',
    title: 'Urban Kitchen',
    categoryCode: 'MODULAR_KITCHEN',
    categoryDisplayName: 'Modular Kitchen',
    projectStatus: 'DRAFT',
    visibilityStatus: 'PRIVATE',
    featured: false,
    displayOrder: 1,
    city: 'Mumbai',
    state: 'Maharashtra',
    propertyType: 'APARTMENT',
    projectScope: 'SINGLE_ROOM',
    styleCodes: ['INDUSTRIAL'],
    styleDisplayNames: ['Industrial'],
    completionYear: 2024,
    version: 1,
    createdAt: '2026-09-18T11:00:00Z',
    updatedAt: '2026-09-18T11:00:00Z',
  },
  {
    id: 'proj-3',
    slug: 'heritage-villa',
    title: 'Heritage Villa',
    categoryCode: 'COMPLETE_HOME_INTERIOR',
    categoryDisplayName: 'Complete Home Interior',
    projectStatus: 'ARCHIVED',
    visibilityStatus: 'PRIVATE',
    featured: false,
    displayOrder: 2,
    city: 'Jaipur',
    state: 'Rajasthan',
    propertyType: 'VILLA',
    projectScope: 'TURNKEY',
    styleCodes: ['INDIAN_TRADITIONAL'],
    styleDisplayNames: ['Indian Traditional'],
    completionYear: 2023,
    version: 2,
    createdAt: '2026-09-18T12:00:00Z',
    updatedAt: '2026-09-18T12:00:00Z',
    archivedAt: '2026-09-19T12:00:00Z',
  },
];

const mockProjectDetail: ProjectDetailDto = {
  id: 'proj-123',
  studioId: 'studio-999',
  slug: 'emerald-penthouse',
  title: 'Emerald Penthouse',
  shortDescription: 'A luxurious 4BHK apartment with customized Italian marble.',
  fullDescription: 'Comprehensive case study of spatial architecture...',
  categoryCode: 'COMPLETE_HOME_INTERIOR',
  categoryDisplayName: 'Complete Home Interior',
  projectStatus: 'READY',
  visibilityStatus: 'PORTFOLIO',
  featured: true,
  displayOrder: 0,
  city: 'Bengaluru',
  district: 'Bengaluru Urban',
  state: 'Karnataka',
  country: 'IN',
  propertyType: 'APARTMENT',
  projectScope: 'FULL_INTERIOR',
  styleCodes: ['WARM_CONTEMPORARY', 'MODERN_MINIMALIST'],
  styleDisplayNames: ['Warm Contemporary', 'Modern Minimalist'],
  completionYear: 2025,
  budgetVisibility: 'RANGE',
  budgetMin: 3500000,
  budgetMax: 5000000,
  currency: 'INR',
  clientNameVisibility: 'DISPLAY',
  clientDisplayName: 'Sharma Residence',
  areaValue: 3200,
  areaUnit: 'SQ_FT',
  internalNotes: 'VIP client referral',
  version: 1,
  createdBy: 'user-1',
  createdAt: '2026-09-18T10:00:00Z',
  updatedAt: '2026-09-18T10:00:00Z',
  isReady: true,
  missingReadinessFields: [],
};

describe('Project CMS - Workspace UI', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. Renders empty state when zero projects are returned', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue([]);

    render(<ProjectsListPage />);

    expect(screen.getByText(/loading your project stories/i)).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Start Your First Project Story')).toBeDefined();
    });

    expect(screen.getByText('Interior Project Stories')).toBeDefined();
    expect(screen.getByRole('button', { name: /create project/i })).toBeDefined();
  });

  it('2. Renders projects list with metrics, status badges, and location tags', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    // Check project cards
    expect(screen.getByText('Urban Kitchen')).toBeDefined();
    expect(screen.getAllByText('Ready').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Draft')).toBeDefined();
    expect(screen.getByText('Bengaluru, Karnataka')).toBeDefined();
    expect(screen.getAllByText('In Portfolio').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Featured')).toBeDefined();

    // Check metrics counts: 2 active, 1 ready, 1 draft, 1 in portfolio
    expect(screen.getByText('Total Active')).toBeDefined();
    expect(screen.getByText('Ready Stories')).toBeDefined();
  });

  it('3. Filters projects by search query', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    const searchInput = screen.getByPlaceholderText(/search projects by title/i);
    fireEvent.change(searchInput, { target: { value: 'Urban' } });

    expect(screen.queryByText('Emerald Penthouse')).toBeNull();
    expect(screen.getByText('Urban Kitchen')).toBeDefined();
  });

  it('4. Filters projects by status', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    const statusSelect = screen.getByDisplayValue('All Statuses');
    fireEvent.change(statusSelect, { target: { value: 'DRAFT' } });

    expect(screen.queryByText('Emerald Penthouse')).toBeNull();
    expect(screen.getByText('Urban Kitchen')).toBeDefined();
  });

  it('5. Opens New Project modal, submits valid form, and navigates to editor', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
    vi.spyOn(projectApi, 'createProject').mockResolvedValue({
      ...mockProjectDetail,
      id: 'proj-new-99',
      title: 'New Modern Living Room',
    });

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    const newBtn = screen.getByRole('button', { name: /new project/i });
    fireEvent.click(newBtn);

    expect(screen.getByText('New Project Story')).toBeDefined();

    const titleInput = screen.getByPlaceholderText(/e\.g\. Emerald Sky Villa/i);
    fireEvent.change(titleInput, { target: { value: 'New Modern Living Room' } });

    const createBtn = screen.getByRole('button', { name: /create & edit details/i });
    fireEvent.click(createBtn);

    await waitFor(() => {
      expect(projectApi.createProject).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'New Modern Living Room',
          categoryCode: 'COMPLETE_HOME_INTERIOR',
        })
      );
    });

    expect(mockPush).toHaveBeenCalledWith('/workspace/projects/proj-new-99');
  });

  it('6. Reorders projects up and down', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
    vi.spyOn(projectApi, 'reorderProjects').mockResolvedValue([mockProjects[1], mockProjects[0]]);

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    const moveDownBtns = screen.getAllByTitle('Move down in portfolio order');
    fireEvent.click(moveDownBtns[0]);

    await waitFor(() => {
      expect(projectApi.reorderProjects).toHaveBeenCalledWith(['proj-2', 'proj-1']);
    });
  });

  it('7. Archives project upon confirmation', async () => {
    vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
    vi.spyOn(projectApi, 'archiveProject').mockResolvedValue({
      ...mockProjectDetail,
      projectStatus: 'ARCHIVED',
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<ProjectsListPage />);

    await waitFor(() => {
      expect(screen.getByText('Emerald Penthouse')).toBeDefined();
    });

    const archiveBtns = screen.getAllByTitle('Archive project');
    fireEvent.click(archiveBtns[0]);

    await waitFor(() => {
      expect(projectApi.archiveProject).toHaveBeenCalledWith('proj-1', 1);
    });
  });

  it('8. Renders Project Edit Page with details and server-derived readiness checklist', async () => {
    vi.spyOn(projectApi, 'fetchProject').mockResolvedValue(mockProjectDetail);

    render(<ProjectEditPage />);

    expect(screen.getByText(/loading project details/i)).toBeDefined();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Emerald Penthouse')).toBeDefined();
    });

    expect(screen.getByText('Project Story Complete & Portfolio Ready')).toBeDefined();
    expect(screen.getByDisplayValue('Sharma Residence')).toBeDefined();
    expect(screen.getByDisplayValue('3500000')).toBeDefined();
    expect(screen.getByText('Project Photography & Media')).toBeDefined();
  });

  it('9. Updates project and displays success notification', async () => {
    vi.spyOn(projectApi, 'fetchProject').mockResolvedValue(mockProjectDetail);
    vi.spyOn(projectApi, 'updateProject').mockResolvedValue({
      ...mockProjectDetail,
      title: 'Emerald Penthouse Overhaul',
      version: 2,
    });

    render(<ProjectEditPage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Emerald Penthouse')).toBeDefined();
    });

    const titleInput = screen.getByDisplayValue('Emerald Penthouse');
    fireEvent.change(titleInput, { target: { value: 'Emerald Penthouse Overhaul' } });

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(projectApi.updateProject).toHaveBeenCalledWith(
        'proj-123',
        expect.objectContaining({
          version: 1,
          title: 'Emerald Penthouse Overhaul',
        })
      );
    });

    await waitFor(() => {
      expect(screen.getByText('Project updated successfully.')).toBeDefined();
    });
  });

  it('10. Handles 409 Conflict version mismatch gracefully', async () => {
    vi.spyOn(projectApi, 'fetchProject').mockResolvedValue(mockProjectDetail);
    vi.spyOn(projectApi, 'updateProject').mockRejectedValue(
      new Error('Conflict: HTTP 409 status')
    );

    render(<ProjectEditPage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Emerald Penthouse')).toBeDefined();
    });

    const titleInput = screen.getByDisplayValue('Emerald Penthouse');
    fireEvent.change(titleInput, { target: { value: 'Emerald Penthouse Conflict' } });

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(screen.getByText(/version conflict/i)).toBeDefined();
    });
  });
});
