import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ClientReviewView from '@/components/ai/ClientReviewView';
import * as aiApi from '@/lib/ai/api';
import { PublicClientReviewResponse } from '@/lib/ai/types';

vi.mock('@/lib/ai/api');

describe('ClientReviewView Component', () => {
  const mockReview: PublicClientReviewResponse = {
    id: 'rev-1',
    studioName: 'Studio Arcline',
    projectTitle: 'Penthouse Living Room',
    title: 'Living Room Concept Review',
    customMessage: 'Please review these 2 finish options for the feature wall and TV unit.',
    status: 'OPEN',
    includeOriginal: true,
    originalPreviewUrl: '/api/v1/client-review/media/media-before-1',
    expiresAt: '2026-10-01T00:00:00Z',
    isExpired: false,
    currentApprovedJobId: null,
    items: [
      {
        id: 'item-1',
        jobId: 'job-1',
        mediaId: 'media-concept-1',
        displayLabel: 'Option 1: Fluted Oak & Travertine',
        displayOrder: 0,
        previewUrl: '/api/v1/client-review/media/media-concept-1',
        currentDecision: null,
      },
      {
        id: 'item-2',
        jobId: 'job-2',
        mediaId: 'media-concept-2',
        displayLabel: 'Option 2: Smoked Walnut & Brass',
        displayOrder: 1,
        previewUrl: '/api/v1/client-review/media/media-concept-2',
        currentDecision: null,
      },
    ],
    decisions: [],
    comments: [
      {
        id: 'com-1',
        jobId: null,
        authorType: 'STUDIO',
        authorName: 'Arcline Team',
        commentText: 'Notice the custom fluted panel profiles in Option 1.',
        createdAt: '2026-09-22T10:00:00Z',
      },
    ],
    createdAt: '2026-09-22T08:00:00Z',
  };

  const mockRefresh = vi.fn().mockResolvedValue(undefined);
  const csrfToken = 'test-csrf-token-123';

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders studio name, project title, review title, and persistent AI disclosure badge', () => {
    render(<ClientReviewView review={mockReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    expect(screen.getByText('Studio Arcline')).toBeDefined();
    expect(screen.getByText('Penthouse Living Room')).toBeDefined();
    expect(screen.getByText('Living Room Concept Review')).toBeDefined();
    expect(screen.getByText(/Design Consultation Notice:/i)).toBeDefined();
    expect(screen.getByText(/AI visualizations are non-contractual aesthetic representations/i)).toBeDefined();
  });

  it('renders concept options and comparison controls', () => {
    render(<ClientReviewView review={mockReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    expect(screen.getAllByText('Option 1: Fluted Oak & Travertine').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Option 2: Smoked Walnut & Brass').length).toBeGreaterThan(0);
    expect(screen.getByText('Show Existing Room')).toBeDefined();
  });

  it('opens approval modal and submits decision with client name and CSRF token', async () => {
    vi.mocked(aiApi.submitClientDecision).mockResolvedValue(undefined);

    render(<ClientReviewView review={mockReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    const approveButtons = screen.getAllByRole('button', { name: /approve/i });
    fireEvent.click(approveButtons[0]);

    expect(screen.getByRole('button', { name: /Confirm Approval/i })).toBeDefined();
    expect(screen.getByText(/This visualization remains private and non-contractual/i)).toBeDefined();

    const nameInput = screen.getByPlaceholderText('e.g., Sarah Johnson');
    fireEvent.change(nameInput, { target: { value: 'Sarah Client' } });

    const confirmBtn = screen.getByRole('button', { name: /Confirm Approval/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(aiApi.submitClientDecision).toHaveBeenCalledWith(
        expect.objectContaining({
          jobId: 'job-1',
          decision: 'APPROVED',
          clientName: 'Sarah Client',
        }),
        csrfToken
      );
    });

    expect(mockRefresh).toHaveBeenCalled();
  });

  it('opens change request modal and submits feedback with CSRF token', async () => {
    vi.mocked(aiApi.submitClientDecision).mockResolvedValue(undefined);

    render(<ClientReviewView review={mockReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    const changeButtons = screen.getAllByRole('button', { name: /Request Changes/i });
    fireEvent.click(changeButtons[0]);

    expect(screen.getByText(/Request Changes on Option 1/i)).toBeDefined();

    const nameInput = screen.getByPlaceholderText('e.g., Sarah Johnson');
    fireEvent.change(nameInput, { target: { value: 'Sarah Client' } });

    const feedbackInput = screen.getByPlaceholderText(/Can we see a lighter wood tone/i);
    fireEvent.change(feedbackInput, { target: { value: 'We would prefer brushed brass instead of black hardware.' } });

    const submitBtn = screen.getByRole('button', { name: /Submit Request/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(aiApi.submitClientDecision).toHaveBeenCalledWith(
        expect.objectContaining({
          jobId: 'job-1',
          decision: 'CHANGES_REQUESTED',
          clientName: 'Sarah Client',
          feedback: 'We would prefer brushed brass instead of black hardware.',
        }),
        csrfToken
      );
    });

    expect(mockRefresh).toHaveBeenCalled();
  });

  it('submits a client comment and displays studio comments', async () => {
    vi.mocked(aiApi.submitClientComment).mockResolvedValue(undefined);

    render(<ClientReviewView review={mockReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    expect(screen.getByText('Notice the custom fluted panel profiles in Option 1.')).toBeDefined();

    const nameInput = screen.getByPlaceholderText('e.g., Jane Doe');
    fireEvent.change(nameInput, { target: { value: 'Sarah Client' } });

    const commentInput = screen.getByPlaceholderText(/Share your thoughts/i);
    fireEvent.change(commentInput, { target: { value: 'Could you confirm the travertine thickness?' } });

    const sendBtn = screen.getByRole('button', { name: /Send Feedback/i });
    fireEvent.click(sendBtn);

    await waitFor(() => {
      expect(aiApi.submitClientComment).toHaveBeenCalledWith(
        {
          authorName: 'Sarah Client',
          commentText: 'Could you confirm the travertine thickness?',
        },
        csrfToken
      );
    });
  });

  it('disables actions when review is expired or closed', () => {
    const expiredReview: PublicClientReviewResponse = {
      ...mockReview,
      status: 'CLOSED',
      isExpired: true,
    };

    render(<ClientReviewView review={expiredReview} csrfToken={csrfToken} onRefresh={mockRefresh} />);

    expect(screen.getByText(/Presentation Concluded:/i)).toBeDefined();
    expect(screen.queryByRole('button', { name: /Approve/i })).toBeNull();
  });
});
