import { apiFetch } from '../api-client';
import {
  AiJobDetail,
  AiJobListResponse,
  AiStudioStatus,
  CreateAiJobPayload,
  CreateReferencePayload,
  ReferenceDetail,
  ReferencePurpose,
  UpdateReferencePayload,
} from './types';

export async function fetchAiStudioStatus(studioId?: string): Promise<AiStudioStatus> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiStudioStatus>(`/ai/status${qs}`);
}

export async function createAiJob(
  data: CreateAiJobPayload,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function fetchAiJobDetail(
  jobId: string,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs/${jobId}${qs}`);
}

export async function cancelAiJob(
  jobId: string,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs/${jobId}/cancel${qs}`, {
    method: 'POST',
  });
}

export async function listAiJobs(
  filters?: {
    projectId?: string;
    limit?: number;
    offset?: number;
  },
  studioId?: string
): Promise<AiJobListResponse> {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (filters?.projectId) params.append('projectId', filters.projectId);
  if (filters?.limit !== undefined) params.append('limit', filters.limit.toString());
  if (filters?.offset !== undefined) params.append('offset', filters.offset.toString());
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<AiJobListResponse>(`/ai/jobs${qs}`);
}

// ============================================================================
// Reference Library API Functions
// ============================================================================

export async function fetchReferences(
  filters?: {
    projectId?: string;
    purpose?: ReferencePurpose;
    includeArchived?: boolean;
  },
  studioId?: string
): Promise<ReferenceDetail[]> {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (filters?.projectId) params.append('projectId', filters.projectId);
  if (filters?.purpose) params.append('purpose', filters.purpose);
  if (filters?.includeArchived) params.append('includeArchived', 'true');
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<ReferenceDetail[]>(`/ai/references${qs}`);
}

export async function createReference(
  data: CreateReferencePayload,
  studioId?: string
): Promise<ReferenceDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ReferenceDetail>(`/ai/references${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateReference(
  referenceId: string,
  data: UpdateReferencePayload,
  studioId?: string
): Promise<ReferenceDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ReferenceDetail>(`/ai/references/${referenceId}${qs}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export async function archiveReference(
  referenceId: string,
  studioId?: string
): Promise<void> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  await apiFetch<void>(`/ai/references/${referenceId}${qs}`, {
    method: 'DELETE',
  });
}

// ============================================================================
// Precision Mask Editing API Functions
// ============================================================================

export async function uploadPrecisionMask(
  inputMediaId: string,
  maskBlob: Blob,
  studioId?: string
): Promise<import('./types').UploadMaskResponse> {
  const formData = new FormData();
  formData.append('inputMediaId', inputMediaId);
  formData.append('file', maskBlob, 'mask.png');

  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  params.append('inputMediaId', inputMediaId);
  const qs = `?${params.toString()}`;

  return apiFetch<import('./types').UploadMaskResponse>(`/ai/masks${qs}`, {
    method: 'POST',
    body: formData,
  });
}

export function getJobMaskPreviewUrl(jobId: string, studioId?: string): string {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return `/api/ai/jobs/${encodeURIComponent(jobId)}/mask${qs}`;
}

// ============================================================================
// Phase 24: AI Variations & History API Functions
// ============================================================================

export async function createVariation(
  parentJobId: string,
  data: import('./types').CreateVariationPayload,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs/${encodeURIComponent(parentJobId)}/variations${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function toggleShortlist(
  jobId: string,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs/${encodeURIComponent(jobId)}/shortlist${qs}`, {
    method: 'POST',
  });
}

export async function toggleStudioSelected(
  jobId: string,
  studioId?: string
): Promise<AiJobDetail> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<AiJobDetail>(`/ai/jobs/${encodeURIComponent(jobId)}/select${qs}`, {
    method: 'POST',
  });
}

export async function fetchJobHistory(
  filters?: {
    projectId?: string;
    editingMode?: import('./types').EditingMode;
    status?: import('./types').AiJobStatus;
    shortlistedOnly?: boolean;
    page?: number;
    limit?: number;
  },
  studioId?: string
): Promise<import('./types').AiJobHistoryResponse> {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (filters?.projectId) params.append('projectId', filters.projectId);
  if (filters?.editingMode) params.append('editingMode', filters.editingMode);
  if (filters?.status) params.append('status', filters.status);
  if (filters?.shortlistedOnly !== undefined) params.append('shortlistedOnly', filters.shortlistedOnly.toString());
  if (filters?.page !== undefined) params.append('page', filters.page.toString());
  if (filters?.limit !== undefined) params.append('limit', filters.limit.toString());
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<import('./types').AiJobHistoryResponse>(`/ai/history${qs}`);
}

// ============================================================================
// Phase 24: Studio Client Review API Functions
// ============================================================================

export async function createClientReview(
  data: import('./types').CreateClientReviewPayload,
  studioId?: string
): Promise<import('./types').CreateClientReviewResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<import('./types').CreateClientReviewResponse>(`/ai/client-reviews${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function listClientReviews(
  filters?: {
    projectId?: string;
    page?: number;
    limit?: number;
  },
  studioId?: string
): Promise<import('./types').ClientReviewDetailResponse[]> {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (filters?.projectId) params.append('projectId', filters.projectId);
  if (filters?.page !== undefined) params.append('page', filters.page.toString());
  if (filters?.limit !== undefined) params.append('limit', filters.limit.toString());
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<import('./types').ClientReviewDetailResponse[]>(`/ai/client-reviews${qs}`);
}

export async function getClientReviewDetail(
  reviewId: string,
  studioId?: string
): Promise<import('./types').ClientReviewDetailResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<import('./types').ClientReviewDetailResponse>(`/ai/client-reviews/${encodeURIComponent(reviewId)}${qs}`);
}

export async function closeClientReview(
  reviewId: string,
  studioId?: string
): Promise<void> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  await apiFetch<void>(`/ai/client-reviews/${encodeURIComponent(reviewId)}/close${qs}`, {
    method: 'POST',
  });
}

export async function revokeClientReview(
  reviewId: string,
  studioId?: string
): Promise<void> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  await apiFetch<void>(`/ai/client-reviews/${encodeURIComponent(reviewId)}/revoke${qs}`, {
    method: 'POST',
  });
}

export async function rotateClientReviewToken(
  reviewId: string,
  studioId?: string
): Promise<import('./types').CreateClientReviewResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<import('./types').CreateClientReviewResponse>(`/ai/client-reviews/${encodeURIComponent(reviewId)}/rotate-token${qs}`, {
    method: 'POST',
  });
}

// ============================================================================
// Phase 24: Public Client Review API Functions (Token-Free Session)
// ============================================================================

export async function exchangeReviewToken(token: string): Promise<import('./types').ExchangeReviewTokenResponse> {
  return apiFetch<import('./types').ExchangeReviewTokenResponse>('/client-review/exchange', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function fetchPublicReviewSession(): Promise<import('./types').PublicClientReviewResponse> {
  return apiFetch<import('./types').PublicClientReviewResponse>('/client-review/session');
}

export async function submitClientDecision(
  data: import('./types').SubmitClientDecisionRequest,
  csrfToken: string
): Promise<void> {
  await apiFetch<void>('/client-review/decisions', {
    method: 'POST',
    headers: {
      'X-CSRF-Token': csrfToken,
    },
    body: JSON.stringify(data),
  });
}

export async function submitClientComment(
  data: import('./types').SubmitClientCommentRequest,
  csrfToken: string
): Promise<void> {
  await apiFetch<void>('/client-review/comments', {
    method: 'POST',
    headers: {
      'X-CSRF-Token': csrfToken,
    },
    body: JSON.stringify(data),
  });
}


