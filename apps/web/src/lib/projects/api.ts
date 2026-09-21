import { apiFetch } from '../api-client';
import {
  CreateProjectRequest,
  ProjectDetailDto,
  ProjectFilterQuery,
  ProjectSummaryDto,
  UpdateProjectRequest,
} from './types';

function buildQuery(query?: ProjectFilterQuery, studioId?: string): string {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (query?.status) params.append('status', query.status);
  if (query?.category) params.append('category', query.category);
  if (query?.visibility) params.append('visibility', query.visibility);
  if (query?.featured !== undefined) params.append('featured', String(query.featured));
  if (query?.includeArchived) params.append('includeArchived', 'true');

  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

export async function fetchProjects(
  query?: ProjectFilterQuery,
  studioId?: string
): Promise<ProjectSummaryDto[]> {
  return apiFetch<ProjectSummaryDto[]>(`/projects${buildQuery(query, studioId)}`);
}

export async function fetchProject(
  projectId: string,
  studioId?: string
): Promise<ProjectDetailDto> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectDetailDto>(`/projects/${projectId}${qs}`);
}

export async function createProject(
  data: CreateProjectRequest,
  studioId?: string
): Promise<ProjectDetailDto> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectDetailDto>(`/projects${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateProject(
  projectId: string,
  data: UpdateProjectRequest,
  studioId?: string
): Promise<ProjectDetailDto> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectDetailDto>(`/projects/${projectId}${qs}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function reorderProjects(
  orderedProjectIds: string[],
  studioId?: string
): Promise<ProjectSummaryDto[]> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectSummaryDto[]>(`/projects/reorder${qs}`, {
    method: 'POST',
    body: JSON.stringify({ orderedProjectIds }),
  });
}

export async function archiveProject(
  projectId: string,
  version: number,
  studioId?: string
): Promise<ProjectDetailDto> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectDetailDto>(`/projects/${projectId}/archive${qs}`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  });
}

export async function restoreProject(
  projectId: string,
  version: number,
  studioId?: string
): Promise<ProjectDetailDto> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<ProjectDetailDto>(`/projects/${projectId}/restore${qs}`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  });
}
