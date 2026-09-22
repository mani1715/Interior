import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AiHistoryView } from '@/components/ai/AiHistoryView';
import { AiReviewsView } from '@/components/ai/AiReviewsView';
import * as aiApi from '@/lib/ai/api';
import { AiJobDetail, ClientReviewDetailResponse } from '@/lib/ai/types';

vi.mock('@/lib/ai/api');

describe('AI Variations & History View Components', () => {
  const mockJobs: AiJobDetail[] = [
    {
      id: 'job-1',
      studioId: 'studio-1',
      projectId: 'proj-1',
      inputMediaId: 'media-1',
      outputMediaId: 'out-1',
      outputPreviewUrl: 'https://example.com/out1.jpg',
      providerKey: 'stability',
      prompt: 'Minimalist warm timber kitchen',
      status: 'SUCCEEDED',
      createdAt: '2026-09-22T08:00:00Z',
      isShortlisted: true,
      isStudioSelected: false,
      conceptLabel: 'Base Concept',
      parentJobId: null,
      rootJobId: null,
      editingMode: 'FULL_IMAGE',
    },
    {
      id: 'job-2',
      studioId: 'studio-1',
      projectId: 'proj-1',
      inputMediaId: 'media-1',
      outputMediaId: 'out-2',
      outputPreviewUrl: 'https://example.com/out2.jpg',
      providerKey: 'stability',
      prompt: 'Minimalist warm timber kitchen with brass details',
      status: 'SUCCEEDED',
      createdAt: '2026-09-22T09:00:00Z',
      isShortlisted: false,
      isStudioSelected: true,
      conceptLabel: 'Variation 1',
      parentJobId: 'job-1',
      rootJobId: 'job-1',
      editingMode: 'FULL_IMAGE',
    },
  ];

  const mockReviews: ClientReviewDetailResponse[] = [
    {
      id: 'rev-1',
      projectId: 'proj-1',
      projectTitle: 'Penthouse Living Room',
      title: 'Kitchen Finish Review',
      customMessage: 'Please review',
      status: 'OPEN',
      includeOriginal: true,
      expiresAt: '2026-10-01T00:00:00Z',
      currentApprovedJobId: 'job-1',
      items: [
        {
          id: 'item-1',
          jobId: 'job-1',
          mediaId: 'out-1',
          displayLabel: 'Option 1',
          displayOrder: 0,
          previewUrl: 'https://example.com/out1.jpg',
        },
      ],
      decisions: [
        {
          id: 'dec-1',
          jobId: 'job-1',
          decision: 'APPROVED',
          clientName: 'Sarah Client',
          feedback: 'Love this direction!',
          isCurrent: true,
          createdAt: '2026-09-22T10:00:00Z',
        },
      ],
      comments: [],
      createdAt: '2026-09-22T08:30:00Z',
      updatedAt: '2026-09-22T10:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('AiHistoryView', () => {
    it('renders generation history cards with badges and actions', async () => {
      vi.mocked(aiApi.fetchJobHistory).mockResolvedValue({
        items: mockJobs,
        page: 0,
        limit: 18,
        totalItems: 2,
        totalPages: 1,
      });

      render(<AiHistoryView projectId="proj-1" />);

      await waitFor(() => {
        expect(screen.getByText('Base Concept')).toBeDefined();
        expect(screen.getByText('Variation 1')).toBeDefined();
      });

      expect(screen.getByText(/2 total generation/i)).toBeDefined();
    });

    it('toggles shortlist status when shortlist button is clicked', async () => {
      vi.mocked(aiApi.fetchJobHistory).mockResolvedValue({
        items: mockJobs,
        page: 0,
        limit: 18,
        totalItems: 2,
        totalPages: 1,
      });
      vi.mocked(aiApi.toggleShortlist).mockResolvedValue({
        ...mockJobs[0],
        isShortlisted: false,
      });

      render(<AiHistoryView projectId="proj-1" />);

      await waitFor(() => {
        expect(screen.getByText('Base Concept')).toBeDefined();
      });

      const starButtons = screen.getAllByTitle(/shortlist/i);
      fireEvent.click(starButtons[0]);

      await waitFor(() => {
        expect(aiApi.toggleShortlist).toHaveBeenCalledWith('job-1', undefined);
      });
    });

    it('opens variation modal and submits variation with REFINE_ORIGINAL strategy', async () => {
      vi.mocked(aiApi.fetchJobHistory).mockResolvedValue({
        items: mockJobs,
        page: 0,
        limit: 18,
        totalItems: 2,
        totalPages: 1,
      });
      vi.mocked(aiApi.createVariation).mockResolvedValue({
        ...mockJobs[0],
        id: 'job-3',
        conceptLabel: 'Variation 2',
      });

      render(<AiHistoryView projectId="proj-1" />);

      await waitFor(() => {
        expect(screen.getByText('Base Concept')).toBeDefined();
      });

      const createVarButtons = screen.getAllByRole('button', { name: /Create Variation/i });
      fireEvent.click(createVarButtons[0]);

      expect(screen.getByText('Create Concept Variation')).toBeDefined();
      expect(screen.getByText('Refine Original Room')).toBeDefined();
      expect(screen.getByText('Evolve this AI Concept')).toBeDefined();

      const promptInput = screen.getByPlaceholderText(/Describe modifications/i);
      fireEvent.change(promptInput, { target: { value: 'Add fluted marble backsplash' } });

      const submitBtn = screen.getByRole('button', { name: /Generate Variation/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(aiApi.createVariation).toHaveBeenCalledWith(
          'job-1',
          expect.objectContaining({
            variationStrategy: 'REFINE_ORIGINAL',
            prompt: 'Add fluted marble backsplash',
          }),
          undefined
        );
      });
    });
  });

  describe('AiReviewsView', () => {
    it('renders client reviews list with decision pill and details', async () => {
      vi.mocked(aiApi.listClientReviews).mockResolvedValue(mockReviews);

      render(<AiReviewsView projectId="proj-1" />);

      await waitFor(() => {
        expect(screen.getByText('Kitchen Finish Review')).toBeDefined();
        expect(screen.getByText(/Concept Approved by Sarah Client/i)).toBeDefined();
      });
    });

    it('opens review details modal with decision history and comments', async () => {
      vi.mocked(aiApi.listClientReviews).mockResolvedValue(mockReviews);

      render(<AiReviewsView projectId="proj-1" />);

      await waitFor(() => {
        expect(screen.getByText('Kitchen Finish Review')).toBeDefined();
      });

      const detailsBtn = screen.getByRole('button', { name: /Review Details/i });
      fireEvent.click(detailsBtn);

      expect(screen.getByText(/Client Decisions & Approvals/i)).toBeDefined();
      expect(screen.getByText(/Love this direction!/i)).toBeDefined();
    });
  });
});
