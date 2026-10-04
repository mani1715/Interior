import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ClientReviewView from '@/components/ai/ClientReviewView';
import * as aiApi from '@/lib/ai/api';
import { PublicClientReviewResponse } from '@/lib/ai/types';

vi.mock('@/lib/ai/api');
vi.mock('@/lib/realtime/RealtimeProvider', () => ({
  useRealtimeSubscription: vi.fn(),
}));

describe('Client Collaboration & Annotations Features', () => {
  const mockReviewWithAnnotations: PublicClientReviewResponse = {
    id: 'rev-collab-1',
    studioName: 'Studio Arcline',
    projectTitle: 'Penthouse Living Room',
    title: 'Round 2 Concept Collaboration',
    customMessage: 'Revision round incorporating your brass handle feedback.',
    status: 'OPEN',
    includeOriginal: true,
    originalPreviewUrl: '/api/v1/client-review/media/before-1',
    expiresAt: '2026-10-15T00:00:00Z',
    isExpired: false,
    currentApprovedJobId: null,
    preferredJobId: null,
    revisionRound: 2,
    items: [
      {
        id: 'item-1',
        jobId: 'job-1',
        mediaId: 'media-1',
        displayLabel: 'Option 1: Fluted Travertine',
        displayOrder: 0,
        previewUrl: '/api/v1/client-review/media/opt-1',
        currentDecision: null,
      },
    ],
    decisions: [],
    comments: [],
    annotations: [
      {
        id: 'pin-1',
        reviewId: 'rev-collab-1',
        jobId: 'job-1',
        mediaId: 'media-1',
        pinNumber: 1,
        coordX: 0.45,
        coordY: 0.55,
        authorType: 'CLIENT',
        authorName: 'Alice Client',
        commentText: 'Make this handle satin brass please',
        isChangeRequest: true,
        resolvedAt: null,
        resolvedBy: null,
        parentAnnotationId: null,
        revisionRound: 2,
        createdAt: '2026-10-02T10:00:00Z',
      },
      {
        id: 'reply-1',
        reviewId: 'rev-collab-1',
        jobId: 'job-1',
        mediaId: 'media-1',
        pinNumber: 1,
        coordX: 0.45,
        coordY: 0.55,
        authorType: 'STUDIO',
        authorName: 'Lead Designer',
        commentText: 'Added in Revision 2 specifications.',
        isChangeRequest: false,
        resolvedAt: null,
        resolvedBy: null,
        parentAnnotationId: 'pin-1',
        revisionRound: 2,
        createdAt: '2026-10-02T11:00:00Z',
      },
    ],
    createdAt: '2026-10-01T08:00:00Z',
  };

  const mockRefresh = vi.fn().mockResolvedValue(undefined);
  const csrfToken = 'test-collab-csrf';

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders revision round badge and pinpoint annotations list with designer reply', () => {
    render(<ClientReviewView review={mockReviewWithAnnotations} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    expect(screen.getByText('Round 2')).toBeDefined();
    expect(screen.getByText(/Alice Client/i)).toBeDefined();
    expect(screen.getByText('Make this handle satin brass please')).toBeDefined();
    expect(screen.getByText(/Lead Designer \(Designer\):/i)).toBeDefined();
    expect(screen.getByText('Added in Revision 2 specifications.')).toBeDefined();
    expect(screen.getByText('Revision Request')).toBeDefined();
  });

  it('allows client to select Mark as Preferred concept', async () => {
    vi.mocked(aiApi.setPreferredConcept).mockResolvedValue(undefined);

    render(<ClientReviewView review={mockReviewWithAnnotations} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    const preferredBtn = screen.getByRole('button', { name: /Mark as Preferred/i });
    fireEvent.click(preferredBtn);

    await waitFor(() => {
      expect(aiApi.setPreferredConcept).toHaveBeenCalledWith(
        { jobId: 'job-1' },
        csrfToken
      );
    });
    expect(mockRefresh).toHaveBeenCalled();
  });

  it('allows client to enter pin mode and place pin annotation', async () => {
    vi.mocked(aiApi.submitClientAnnotation).mockResolvedValue({
      id: 'pin-new',
      reviewId: 'rev-collab-1',
      jobId: 'job-1',
      mediaId: 'media-1',
      pinNumber: 2,
      coordX: 0.25,
      coordY: 0.35,
      authorType: 'CLIENT',
      authorName: 'Alice',
      commentText: 'Lighter grout lines',
      isChangeRequest: false,
      revisionRound: 2,
      createdAt: new Date().toISOString(),
    });

    render(<ClientReviewView review={mockReviewWithAnnotations} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    const pinBtn = screen.getByRole('button', { name: /Add Pin Note/i });
    fireEvent.click(pinBtn);

    expect(screen.getByText(/Click Image to Place Pin/i)).toBeDefined();
  });
});
