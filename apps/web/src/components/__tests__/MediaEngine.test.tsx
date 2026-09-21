import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ProjectMediaManager } from '@/components/media/ProjectMediaManager';
import MediaWorkspacePage from '@/app/workspace/media/page';
import * as mediaApi from '@/lib/media/api';
import * as projectApi from '@/lib/projects/api';
import { MediaDetailResponse, WatermarkSettings } from '@/lib/media/types';
import { ProjectSummaryDto } from '@/lib/projects/types';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  useParams: () => ({
    projectId: 'proj-123',
  }),
  usePathname: () => '/workspace/media',
}));

const mockMediaItems: MediaDetailResponse[] = [
  {
    id: 'media-1',
    studioId: 'studio-1',
    projectId: 'proj-123',
    mediaType: 'REAL_PROJECT',
    mediaTypeDisplayName: 'Real Project Photo',
    visibility: 'PORTFOLIO',
    processingStatus: 'READY',
    originalStorageKey: 'studio/studio-1/projects/proj-123/original/media-1.jpg',
    contentType: 'image/jpeg',
    fileSize: 1024000,
    width: 2400,
    height: 1600,
    sortOrder: 0,
    isCover: true,
    altText: 'Master bedroom with teak headboard',
    caption: 'Custom joinery handcrafted on site',
    watermarkEnabled: true,
    derivatives: [
      {
        id: 'deriv-1',
        variantName: 'THUMBNAIL',
        width: 400,
        height: 267,
        format: 'webp',
        fileSize: 32000,
        publicUrl: '/api/v1/media/public/studio-1/projects/proj-123/media-1_thumb.webp',
        isWatermarked: false,
      },
      {
        id: 'deriv-2',
        variantName: 'LARGE',
        width: 1920,
        height: 1280,
        format: 'webp',
        fileSize: 250000,
        publicUrl: '/api/v1/media/public/studio-1/projects/proj-123/media-1_large.webp',
        isWatermarked: true,
      },
    ],
    createdAt: '2026-09-20T10:00:00Z',
    updatedAt: '2026-09-20T10:00:00Z',
  },
  {
    id: 'media-2',
    studioId: 'studio-1',
    projectId: 'proj-123',
    mediaType: 'AI_CONCEPT',
    mediaTypeDisplayName: 'AI Concept Visualization',
    visibility: 'PORTFOLIO',
    processingStatus: 'READY',
    originalStorageKey: 'studio/studio-1/projects/proj-123/original/media-2.png',
    contentType: 'image/png',
    fileSize: 2048000,
    width: 1920,
    height: 1080,
    sortOrder: 1,
    isCover: false,
    altText: 'Futuristic living lounge concept',
    caption: 'Initial midjourney architectural exploration',
    watermarkEnabled: true,
    derivatives: [
      {
        id: 'deriv-3',
        variantName: 'THUMBNAIL',
        width: 400,
        height: 225,
        format: 'webp',
        fileSize: 45000,
        publicUrl: '/api/v1/media/public/studio-1/projects/proj-123/media-2_thumb.webp',
        isWatermarked: true,
      },
    ],
    createdAt: '2026-09-20T11:00:00Z',
    updatedAt: '2026-09-20T11:00:00Z',
  },
  {
    id: 'media-3',
    studioId: 'studio-1',
    projectId: 'proj-123',
    mediaType: 'CLIENT_PRIVATE',
    mediaTypeDisplayName: 'Confidential Client Spec (Private)',
    visibility: 'PRIVATE',
    processingStatus: 'READY',
    originalStorageKey: 'studio/studio-1/projects/proj-123/original/media-3.pdf',
    contentType: 'image/jpeg',
    fileSize: 512000,
    width: 1200,
    height: 900,
    sortOrder: 2,
    isCover: false,
    altText: 'Client budget ledger and structural wall markers',
    caption: 'Private internal notes',
    watermarkEnabled: false,
    derivatives: [],
    createdAt: '2026-09-20T12:00:00Z',
    updatedAt: '2026-09-20T12:00:00Z',
  },
];

const mockWatermarkSettings: WatermarkSettings = {
  studioId: 'studio-1',
  enabled: true,
  position: 'BOTTOM_RIGHT',
  opacity: 0.35,
  sizePercentage: 15,
  useLogo: false,
  fallbackText: 'Studio Arc Interiors',
};

const mockProjects: ProjectSummaryDto[] = [
  {
    id: 'proj-123',
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
    styleCodes: ['WARM_CONTEMPORARY'],
    styleDisplayNames: ['Warm Contemporary'],
    completionYear: 2025,
    version: 1,
    createdAt: '2026-09-18T10:00:00Z',
    updatedAt: '2026-09-18T10:00:00Z',
  },
];

describe('Phase 19: Media Engine & Watermarks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ProjectMediaManager Component', () => {
    it('1. Renders empty media state when no assets exist', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue([]);

      render(<ProjectMediaManager projectId="proj-123" studioId="studio-1" />);

      await waitFor(() => {
        expect(screen.getByText('No project media uploaded yet')).toBeDefined();
      });

      expect(screen.getByText('Project Photography & Media')).toBeDefined();
      expect(screen.getByText('0 Assets')).toBeDefined();
      expect(screen.getByText('Upload Photography')).toBeDefined();
    });

    it('2. Renders media gallery with cover badge, AI Concept badge, and privacy indicators', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue(mockMediaItems);

      render(<ProjectMediaManager projectId="proj-123" studioId="studio-1" />);

      await waitFor(() => {
        expect(screen.getByText('Master bedroom with teak headboard')).toBeDefined();
      });

      // Check header count
      expect(screen.getByText('3 Assets')).toBeDefined();

      // Check Cover Badge
      expect(screen.getByText('Cover')).toBeDefined();

      // Check AI Concept badge (Truth in advertising invariant)
      expect(screen.getAllByText(/AI Concept/).length).toBeGreaterThanOrEqual(1);

      // Check Private indicator (Private media invariant)
      expect(screen.getByText('Private')).toBeDefined();
    });

    it('3. Reorders media items left and right', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(mediaApi, 'reorderProjectMedia').mockResolvedValue();

      render(<ProjectMediaManager projectId="proj-123" studioId="studio-1" />);

      await waitFor(() => {
        expect(screen.getByText('Master bedroom with teak headboard')).toBeDefined();
      });

      const moveRightBtns = screen.getAllByTitle('Move Right');
      expect(moveRightBtns.length).toBeGreaterThan(0);
      fireEvent.click(moveRightBtns[0]);

      await waitFor(() => {
        expect(mediaApi.reorderProjectMedia).toHaveBeenCalledWith(
          'proj-123',
          ['media-2', 'media-1', 'media-3'],
          'studio-1'
        );
      });
    });

    it('4. Sets cover image on a non-cover item', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(mediaApi, 'updateMedia').mockResolvedValue({
        ...mockMediaItems[1],
        isCover: true,
      });

      const coverChangeSpy = vi.fn();
      render(
        <ProjectMediaManager
          projectId="proj-123"
          studioId="studio-1"
          onCoverChanged={coverChangeSpy}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('Futuristic living lounge concept')).toBeDefined();
      });

      const setCoverBtn = screen.getByTitle('Set as Project Cover');
      fireEvent.click(setCoverBtn);

      await waitFor(() => {
        expect(mediaApi.updateMedia).toHaveBeenCalledWith(
          'media-2',
          { isCover: true },
          'studio-1'
        );
      });
    });

    it('5. Deletes media asset after user confirmation', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(mediaApi, 'deleteMedia').mockResolvedValue();
      vi.spyOn(window, 'confirm').mockReturnValue(true);

      render(<ProjectMediaManager projectId="proj-123" studioId="studio-1" />);

      await waitFor(() => {
        expect(screen.getByText('Master bedroom with teak headboard')).toBeDefined();
      });

      const deleteBtns = screen.getAllByTitle('Delete Asset');
      fireEvent.click(deleteBtns[0]);

      await waitFor(() => {
        expect(mediaApi.deleteMedia).toHaveBeenCalledWith('media-1', 'studio-1');
      });
    });
  });

  describe('MediaWorkspacePage (/workspace/media)', () => {
    it('6. Renders studio media library with metrics banner and project links', async () => {
      vi.spyOn(mediaApi, 'fetchStudioMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
      vi.spyOn(mediaApi, 'fetchWatermarkSettings').mockResolvedValue(mockWatermarkSettings);

      render(<MediaWorkspacePage />);

      await waitFor(() => {
        expect(screen.getByText('Studio Media Library')).toBeDefined();
      });

      // Metrics
      expect(screen.getByText('Total Assets')).toBeDefined();
      expect(screen.getByText('Portfolio Media')).toBeDefined();
      expect(screen.getByText('AI Visualizations')).toBeDefined();
      expect(screen.getByText('Private Assets')).toBeDefined();

      // Media cards
      expect(screen.getByText('Master bedroom with teak headboard')).toBeDefined();
      expect(screen.getByText('Futuristic living lounge concept')).toBeDefined();
    });

    it('7. Filters media by search query', async () => {
      vi.spyOn(mediaApi, 'fetchStudioMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
      vi.spyOn(mediaApi, 'fetchWatermarkSettings').mockResolvedValue(mockWatermarkSettings);

      render(<MediaWorkspacePage />);

      await waitFor(() => {
        expect(screen.getByText('Master bedroom with teak headboard')).toBeDefined();
      });

      const searchInput = screen.getByPlaceholderText(/search media by alt text/i);
      fireEvent.change(searchInput, { target: { value: 'futuristic' } });

      // Only media-2 should remain visible
      expect(screen.queryByText('Master bedroom with teak headboard')).toBeNull();
      expect(screen.getByText('Futuristic living lounge concept')).toBeDefined();
    });

    it('8. Opens and updates watermark settings modal', async () => {
      vi.spyOn(mediaApi, 'fetchStudioMedia').mockResolvedValue(mockMediaItems);
      vi.spyOn(projectApi, 'fetchProjects').mockResolvedValue(mockProjects);
      vi.spyOn(mediaApi, 'fetchWatermarkSettings').mockResolvedValue(mockWatermarkSettings);
      vi.spyOn(mediaApi, 'updateWatermarkSettings').mockResolvedValue({
        ...mockWatermarkSettings,
        fallbackText: 'Updated Studio Name',
      });

      render(<MediaWorkspacePage />);

      await waitFor(() => {
        expect(screen.getByText('Studio Media Library')).toBeDefined();
      });

      const wmSettingsBtn = screen.getByRole('button', { name: /watermark settings/i });
      fireEvent.click(wmSettingsBtn);

      expect(screen.getByText('Studio Watermark Settings')).toBeDefined();

      const labelInput = screen.getByDisplayValue('Studio Arc Interiors');
      fireEvent.change(labelInput, { target: { value: 'Updated Studio Name' } });

      const saveBtn = screen.getByRole('button', { name: /save watermark settings/i });
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(mediaApi.updateWatermarkSettings).toHaveBeenCalledWith(
          expect.objectContaining({
            fallbackText: 'Updated Studio Name',
          })
        );
      });
    });
  });
});
