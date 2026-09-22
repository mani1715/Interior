import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AiVisualizerClient } from '@/components/ai/AiVisualizerClient';
import * as aiApi from '@/lib/ai/api';
import * as projectsApi from '@/lib/projects/api';
import * as mediaApi from '@/lib/media/api';

vi.mock('@/lib/ai/api');
vi.mock('@/lib/projects/api');
vi.mock('@/lib/media/api');
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: (key: string) => (key === 'projectId' ? 'proj-1' : key === 'mediaId' ? 'media-1' : null),
  }),
}));

describe('AI Visualizer Component', () => {
  const mockStatus = {
    isConfigured: true,
    providerKey: 'stability',
    dailyQuota: 50,
    usedToday: 2,
    remainingToday: 48,
    supportsReferenceImages: true,
    maxReferenceImages: 4,
  };

  const mockProjects = [
    {
      id: 'proj-1',
      title: 'Luxury Villa Living',
      slug: 'luxury-villa',
      category: 'LIVING_ROOM' as const,
      status: 'READY' as const,
      visibility: 'PORTFOLIO' as const,
      featured: true,
      mediaCount: 3,
      sortOrder: 0,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
  ];

  const mockMedia = [
    {
      id: 'media-1',
      studioId: 'studio-1',
      projectId: 'proj-1',
      mediaType: 'BEFORE' as const,
      visibility: 'PORTFOLIO' as const,
      processingStatus: 'READY' as const,
      originalStorageKey: 'orig.jpg',
      contentType: 'image/jpeg',
      fileSize: 102400,
      width: 1920,
      height: 1080,
      sortOrder: 0,
      isCover: false,
      altText: 'Raw unfinished site photo',
      caption: 'Before renovation',
      watermarkEnabled: true,
      derivatives: [
        {
          id: 'drv-1',
          variantName: 'MEDIUM' as const,
          width: 800,
          height: 600,
          format: 'jpg',
          fileSize: 50000,
          publicUrl: 'https://cdn.example.com/site-before.jpg',
          isWatermarked: true,
        },
      ],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
  ];

  const mockJobs = {
    items: [
      {
        id: 'job-1',
        studioId: 'studio-1',
        projectId: 'proj-1',
        inputMediaId: 'media-1',
        inputPreviewUrl: 'https://cdn.example.com/site-before.jpg',
        outputMediaId: 'media-2',
        outputPreviewUrl: 'https://cdn.example.com/ai-concept.jpg',
        providerKey: 'stability',
        prompt: 'Modern Minimalist with warm oak flooring',
        status: 'SUCCEEDED' as const,
        createdAt: '2024-01-01T12:00:00Z',
        completedAt: '2024-01-01T12:00:20Z',
        preserveStructure: true,
        references: [
          {
            id: 'ref-snap-1',
            mediaId: 'ref-media-1',
            previewUrl: 'https://cdn.example.com/walnut.jpg',
            purpose: 'WOOD' as const,
            purposeDisplayName: 'Wood & Laminate',
            label: 'Dark Walnut',
            instruction: 'Apply to wardrobe shutters',
            displayOrder: 0,
            createdAt: '2024-01-01T12:00:00Z',
          },
        ],
      },
    ],
    total: 1,
    limit: 10,
    offset: 0,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(aiApi.fetchAiStudioStatus).mockResolvedValue(mockStatus);
    vi.mocked(projectsApi.fetchProjects).mockResolvedValue(mockProjects as unknown as any);
    vi.mocked(mediaApi.fetchProjectMedia).mockResolvedValue(mockMedia as unknown as any);
    vi.mocked(aiApi.listAiJobs).mockResolvedValue(mockJobs);
    vi.mocked(aiApi.fetchReferences).mockResolvedValue([]);
  });

  it('renders the AI Concept Visualizer header and quota', async () => {
    render(<AiVisualizerClient />);

    expect(screen.getByText('AI Concept Visualizer')).toBeDefined();
    await waitFor(() => {
      expect(screen.getByText(/48 \/ 50 generations left today/)).toBeDefined();
    });
  });

  it('displays provider unconfigured notice when isConfigured is false', async () => {
    vi.mocked(aiApi.fetchAiStudioStatus).mockResolvedValue({
      ...mockStatus,
      isConfigured: false,
    });

    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getAllByText(/AI Generation Provider Not Configured/i).length).toBeGreaterThan(0);
    });
  });

  it('renders comparison viewer with permanent AI Concept badge and truthful disclaimer', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Concept vs. Site Reality')).toBeDefined();
      expect(screen.getAllByText(/AI Concept Visualization/i).length).toBeGreaterThan(0);
      expect(
        screen.getByText(/final colors, materials, proportions, and execution may differ/i)
      ).toBeDefined();
    });
  });

  it('supports prompt presets and validates prompt length', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Modern Minimalist')).toBeDefined();
    });

    const presetBtn = screen.getByText('Modern Minimalist');
    fireEvent.click(presetBtn);

    const textarea = screen.getByPlaceholderText(/Transform to warm minimalist aesthetic/i) as HTMLTextAreaElement;
    expect(textarea.value).toContain('Modern Minimalist');
  });

  it('renders reference manager with structure preservation toggle and truthful disclaimer', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Structure Preservation')).toBeDefined();
      expect(screen.getByText(/AI will try to preserve the existing structure/i)).toBeDefined();
      expect(screen.getByText(/Visual References \(Materials & Finishes\)/i)).toBeDefined();
      expect(screen.getByText(/0\/4/)).toBeDefined();
    });

    // Toggle structure preservation
    const toggle = screen.getByLabelText('Toggle structure preservation');
    expect(toggle).toBeDefined();
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(screen.getByText(/Flexible mode gives the AI freedom to alter geometry and walls/i)).toBeDefined();
    });
  });

  it('displays attached visual references in concept viewer when job includes references', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText(/Visual References Used \(1\)/i)).toBeDefined();
      expect(screen.getByText('Wood & Laminate')).toBeDefined();
      expect(screen.getByText('Dark Walnut')).toBeDefined();
    });
  });

  it('submits generation job with preserveStructure and references payload', async () => {
    vi.mocked(aiApi.createAiJob).mockResolvedValue({
      id: 'job-new',
      studioId: 'studio-1',
      projectId: 'proj-1',
      inputMediaId: 'media-1',
      providerKey: 'stability',
      prompt: 'Contemporary warm aesthetic',
      status: 'QUEUED',
      createdAt: '2024-01-01T12:05:00Z',
      preserveStructure: true,
      references: [],
    });

    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Generate AI Concept')).toBeDefined();
    });

    const textarea = screen.getByPlaceholderText(/Transform to warm minimalist aesthetic/i);
    fireEvent.change(textarea, { target: { value: 'Contemporary warm aesthetic with bespoke cabinetry' } });

    const submitBtn = screen.getByText('Generate AI Concept');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(aiApi.createAiJob).toHaveBeenCalledWith(
        expect.objectContaining({
          projectId: 'proj-1',
          inputMediaId: 'media-1',
          prompt: 'Contemporary warm aesthetic with bespoke cabinetry',
          preserveStructure: true,
          references: [],
        })
      );
      expect(screen.getByText(/Queued in generation pipeline/i)).toBeDefined();
    });
  });
});
