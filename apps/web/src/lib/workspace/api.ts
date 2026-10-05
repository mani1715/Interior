import { apiFetch } from '../api-client';
import { WorkspaceSummary, BusinessProfile, CreateStudioRequest, CreateStudioResponse } from './types';

export async function fetchWorkspaceSummary(studioId?: string): Promise<WorkspaceSummary> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<WorkspaceSummary>(`/workspace/summary${query}`);
}

export async function fetchBusinessProfile(studioId?: string): Promise<BusinessProfile> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<BusinessProfile>(`/workspace/business${query}`);
}

export async function createStudio(payload: CreateStudioRequest): Promise<CreateStudioResponse> {
  return apiFetch<CreateStudioResponse>('/workspace/studios', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

