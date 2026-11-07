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
vi.mock('@/components/ai/PrecisionMaskEditor', () => ({
  PrecisionMaskEditor: ({ onMaskChange, onExportReady }: {
    onMaskChange: (has: boolean, ratio: number) => void;
    onExportReady: (fn: () => Promise<Blob>) => void;
  }) => <button type="button" onClick={() => {
    onExportReady(async () => new Blob(['mask'], { type: 'image/png' }));
    onMaskChange(true, 0.1);
  }}>Paint test mask</button>,
}));
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

    expect(screen.getByText('AI design studio')).toBeDefined();
    await waitFor(() => {
      expect(screen.getByText(/48 \/ 50 generations left today/)).toBeDefined();
    });
  });

  it('explains disabled generation and enables it when the instruction is valid', async () => {
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByText('Describe the change you want (at least 5 characters).')).toBeDefined());
    const button = screen.getByRole('button', { name: 'Generate AI Concept' }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fireEvent.change(screen.getByRole('textbox', { name: 'AI Prompt Instruction' }), {
      target: { value: 'Give me white upper cupboards' },
    });
    expect(button.disabled).toBe(false);
    expect(screen.getByText('Photo and instruction ready. You can generate your concept.')).toBeDefined();
    expect(aiApi.createAiJob).not.toHaveBeenCalled();
  });

  it('routes yellow upper and black lower doors to separate painted edits before spending a generation', async () => {
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByText('Generate AI Concept')).toBeDefined());
    fireEvent.change(screen.getByRole('textbox', { name: 'AI Prompt Instruction' }), {
      target: { value: 'make the upper as the yellow and down black cupboards' },
    });
    expect((screen.getByRole('button', { name: 'Generate AI Concept' }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole('button', { name: 'Select upper doors · yellow' }));
    expect((screen.getByRole('textbox', { name: 'AI Prompt Instruction' }) as HTMLTextAreaElement).value).toContain('selected upper cupboard doors yellow');
    expect((screen.getByRole('button', { name: 'Generate Precision Edit' }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByText('Paint test mask'));
    expect((screen.getByRole('button', { name: 'Generate Precision Edit' }) as HTMLButtonElement).disabled).toBe(false);
    expect(aiApi.createAiJob).not.toHaveBeenCalled();
  });

  it('uses a completed result as the source for a second precise edit without generating automatically', async () => {
    vi.mocked(mediaApi.fetchMediaDetail).mockResolvedValue({ ...mockMedia[0], id: 'media-2', visibility: 'PRIVATE' } as any);
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Edit this result' })).toBeDefined());
    fireEvent.click(screen.getByRole('button', { name: 'Edit this result' }));
    await waitFor(() => expect(screen.getByText('Paint test mask')).toBeDefined());
    expect(mediaApi.fetchMediaDetail).toHaveBeenCalledWith('media-2', 'studio-1');
    fireEvent.change(screen.getByRole('textbox', { name: 'AI Prompt Instruction' }), { target: { value: 'Selected lower doors black' } });
    expect((screen.getByRole('button', { name: 'Generate Precision Edit' }) as HTMLButtonElement).disabled).toBe(true);
    expect(aiApi.createAiJob).not.toHaveBeenCalled();
  });

  it('uploads precision masks under the source studio, never the project ID', async () => {
    vi.mocked(aiApi.uploadPrecisionMask).mockResolvedValue({ maskStorageKey: 'private/mask.png' } as any);
    vi.mocked(aiApi.createAiJob).mockResolvedValue({ ...mockJobs.items[0], id: 'masked-job' });
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByText('Precise Edit')).toBeDefined());
    fireEvent.click(screen.getByText('Precise Edit'));
    fireEvent.click(screen.getByText('Paint test mask'));
    fireEvent.change(screen.getByRole('textbox', { name: 'AI Prompt Instruction' }), {
      target: { value: 'Paint selected upper cupboard doors black' },
    });
    fireEvent.click(screen.getByText('Generate Precision Edit'));
    await waitFor(() => expect(aiApi.uploadPrecisionMask).toHaveBeenCalledWith('media-1', expect.any(Blob), 'studio-1'));
    expect(aiApi.createAiJob).toHaveBeenCalledWith(expect.objectContaining({
      projectId: 'proj-1', editingMode: 'PRECISION_MASK', maskStorageKey: 'private/mask.png',
    }));
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

  it('does not describe an authentication or network failure as missing provider configuration', async () => {
    vi.mocked(aiApi.fetchAiStudioStatus).mockRejectedValue(new Error('Session expired'));
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByText('Unable to load AI Studio')).toBeDefined());
    expect(screen.queryByText('AI Generation Provider Not Configured')).toBeNull();
  });

  it('renders comparison viewer with permanent AI Concept badge and truthful disclaimer', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Review your concept')).toBeDefined();
      expect(screen.getAllByText(/AI Concept Visualization/i).length).toBeGreaterThan(0);
      expect(
        screen.getByText(/final colors, materials, proportions, and execution may differ/i)
      ).toBeDefined();
    });
  });

  it('supports prompt presets and validates prompt length', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Matte black')).toBeDefined();
    });

    const presetBtn = screen.getByText('Matte black');
    fireEvent.click(presetBtn);

    const textarea = screen.getByRole('textbox', { name: 'AI Prompt Instruction' }) as HTMLTextAreaElement;
    expect(textarea.value).toContain('cupboard doors matte black');
  });

  it('lets a user resolve a black and white combination without silently choosing a layout', async () => {
    render(<AiVisualizerClient />);
    await waitFor(() => expect(screen.getByText('Matte black')).toBeDefined());
    const input = screen.getByRole('textbox', { name: 'AI Prompt Instruction' }) as HTMLTextAreaElement;
    const original = 'give the cupboards in white and black combination';
    fireEvent.change(input, { target: { value: original } });
    expect(input.value).toBe(original);
    fireEvent.click(screen.getByRole('button', { name: 'White upper / black lower' }));
    expect(input.value).toBe(`${original}. Upper cupboard doors white; lower cupboard doors black.`);
    expect(screen.queryByText('Choose your black & white layout')).toBeNull();
  });

  it('renders reference manager with structure preservation toggle and truthful disclaimer', async () => {
    render(<AiVisualizerClient />);

    await waitFor(() => {
      expect(screen.getByText('Structure Preservation')).toBeDefined();
      expect(screen.getByText("AI will try to preserve the existing structure. Some geometry, colors, materials, and proportions may vary.", { exact: true })).toBeDefined();
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

    const textarea = screen.getByRole('textbox', { name: 'AI Prompt Instruction' });
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
