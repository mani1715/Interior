import { apiFetch } from '../api-client';
import {
  CreateReviewInvitationRequest,
  CreateReviewInvitationResponse,
  ExchangeReviewTokenResponse,
  PublicStudioReviewDto,
  PublicStudioReviewsResponse,
  ReviewInvitationDto,
  ReviewReportRequest,
  StudioReviewResponseRequest,
  SubmitReviewRequest,
} from './types';

/**
 * Public review endpoints
 */
export async function getPublicReviews(studioSlug: string): Promise<PublicStudioReviewsResponse> {
  return apiFetch<PublicStudioReviewsResponse>(`/public/studios/${encodeURIComponent(studioSlug)}/reviews`);
}

export async function exchangeReviewToken(token: string): Promise<ExchangeReviewTokenResponse> {
  return apiFetch<ExchangeReviewTokenResponse>('/public/reviews/exchange-token', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function submitReview(data: SubmitReviewRequest): Promise<PublicStudioReviewDto> {
  return apiFetch<PublicStudioReviewDto>('/public/reviews/submit', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function reportReview(reviewId: string, data: ReviewReportRequest): Promise<void> {
  return apiFetch<void>(`/public/reviews/${encodeURIComponent(reviewId)}/report`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * Studio-scoped review management endpoints
 */
export async function listStudioInvitations(): Promise<ReviewInvitationDto[]> {
  return apiFetch<ReviewInvitationDto[]>('/studio/reviews/invitations');
}

export async function createReviewInvitation(
  data: CreateReviewInvitationRequest
): Promise<CreateReviewInvitationResponse> {
  return apiFetch<CreateReviewInvitationResponse>('/studio/reviews/invitations', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function respondToReview(
  reviewId: string,
  data: StudioReviewResponseRequest
): Promise<PublicStudioReviewDto> {
  return apiFetch<PublicStudioReviewDto>(`/studio/reviews/${encodeURIComponent(reviewId)}/response`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function deleteReviewResponse(reviewId: string): Promise<void> {
  return apiFetch<void>(`/studio/reviews/${encodeURIComponent(reviewId)}/response`, {
    method: 'DELETE',
  });
}
