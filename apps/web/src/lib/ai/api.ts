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

