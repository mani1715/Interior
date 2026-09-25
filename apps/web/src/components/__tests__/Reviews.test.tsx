import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ReviewStars } from '../reviews/ReviewStars';
import { ReviewCard } from '../reviews/ReviewCard';
import { ReviewSection } from '../reviews/ReviewSection';
import * as reviewsApi from '@/lib/reviews/api';
import { PublicStudioReviewDto, PublicStudioReviewsResponse } from '@/lib/reviews/types';

vi.mock('@/lib/reviews/api', () => ({
  getPublicReviews: vi.fn(),
  reportReview: vi.fn(),
}));

const mockReview: PublicStudioReviewDto = {
  id: 'rev-001',
  studioId: 'studio-001',
  rating: 5,
  reviewText: 'They delivered the full penthouse interior within 8 weeks. Impeccable finish and transparent billing.',
  projectTitle: 'Penthouse Renovation',
  completedYear: 2025,
  displayName: 'Priya S.',
  verifiedClient: true,
  studioResponse: 'Thank you Priya, it was a pleasure designing your penthouse!',
  studioRespondedAt: '2025-06-01T12:00:00Z',
  createdAt: '2025-05-20T10:00:00Z',
};

const mockReviewsResponse: PublicStudioReviewsResponse = {
  studioId: 'studio-001',
  studioName: 'Zen Designs',
  aggregate: {
    totalReviews: 1,
    averageRating: 5.0,
    ratingBreakdown: {
      5: 1,
      4: 0,
      3: 0,
      2: 0,
      1: 0,
    },
  },
  reviews: [mockReview],
};

describe('Phase 27 — Reviews Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ReviewStars', () => {
    it('renders correct number of stars and rating number', () => {
      render(<ReviewStars rating={4.5} totalReviews={12} showNumber={true} />);
      expect(screen.getByText('4.5')).toBeDefined();
      expect(screen.getByText('(12 reviews)')).toBeDefined();
    });
  });

  describe('ReviewCard', () => {
    it('renders reviewer name, body, and verified client badge', () => {
      render(<ReviewCard review={mockReview} />);
      expect(screen.getByText('Priya S.')).toBeDefined();
      expect(screen.getByText('Verified Client')).toBeDefined();
      expect(
        screen.getByText(
          'They delivered the full penthouse interior within 8 weeks. Impeccable finish and transparent billing.'
        )
      ).toBeDefined();
    });

    it('renders studio response when present', () => {
      render(<ReviewCard review={mockReview} />);
      expect(screen.getByText(/Response from Studio/i)).toBeDefined();
      expect(
        screen.getByText('Thank you Priya, it was a pleasure designing your penthouse!')
      ).toBeDefined();
    });
  });

  describe('ReviewSection', () => {
    it('renders aggregate summary and review card list upon data fetch', async () => {
      vi.mocked(reviewsApi.getPublicReviews).mockResolvedValueOnce(mockReviewsResponse);

      render(<ReviewSection studioSlug="zen-designs" studioName="Zen Designs" />);

      await waitFor(() => {
        expect(screen.getByText('Client Reviews & Ratings')).toBeDefined();
        expect(screen.getByText('5.0')).toBeDefined();
        expect(screen.getByText(/Based on 1 verified review/i)).toBeDefined();
        expect(
          screen.getByText(
            'They delivered the full penthouse interior within 8 weeks. Impeccable finish and transparent billing.'
          )
        ).toBeDefined();
      });
    });

    it('handles empty review list gracefully with zero-count state', async () => {
      vi.mocked(reviewsApi.getPublicReviews).mockResolvedValueOnce({
        ...mockReviewsResponse,
        aggregate: {
          totalReviews: 0,
          averageRating: 0,
          ratingBreakdown: { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 },
        },
        reviews: [],
      });

      render(<ReviewSection studioSlug="zen-designs" studioName="Zen Designs" />);

      await waitFor(() => {
        expect(screen.getByText('No client reviews yet')).toBeDefined();
        expect(
          screen.getByText(
            /Reviews on this platform are submitted exclusively by verified clients upon project completion/i
          )
        ).toBeDefined();
      });
    });
  });
});
