import { apiFetch } from '../api-client';
import {
  AiJobDetail,
  AiJobListResponse,
  AiStudioStatus,
  CreateAiJobPayload,
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
