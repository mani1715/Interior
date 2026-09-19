import { apiFetch } from '../api-client';
import {
  PortfolioDetailResponse,
  PortfolioPreviewResponse,
  InitializePortfolioRequest,
  UpdatePortfolioRequest,
  UpdateSectionRequest,
  ReorderSectionsRequest,
  SwitchTemplateRequest,
  CreateVersionSnapshotRequest,
  RestoreVersionRequest,
} from './types';

function buildQuery(studioId?: string): string {
  return studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
}

export async function fetchPortfolio(studioId?: string): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio${buildQuery(studioId)}`);
}

export async function initPortfolio(
  req: InitializePortfolioRequest = {},
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio${buildQuery(studioId)}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function updatePortfolio(
  req: UpdatePortfolioRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio${buildQuery(studioId)}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export async function updateSection(
  sectionId: string,
  req: UpdateSectionRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio/sections/${sectionId}${buildQuery(studioId)}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export async function reorderSections(
  req: ReorderSectionsRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio/sections/reorder${buildQuery(studioId)}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function switchTemplate(
  req: SwitchTemplateRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio/switch-template${buildQuery(studioId)}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function createVersionSnapshot(
  req: CreateVersionSnapshotRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio/versions${buildQuery(studioId)}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function restoreVersionSnapshot(
  versionNumber: number,
  req: RestoreVersionRequest,
  studioId?: string
): Promise<PortfolioDetailResponse> {
  return apiFetch<PortfolioDetailResponse>(`/portfolio/versions/${versionNumber}/restore${buildQuery(studioId)}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function fetchPortfolioPreview(studioId?: string): Promise<PortfolioPreviewResponse> {
  return apiFetch<PortfolioPreviewResponse>(`/portfolio/preview${buildQuery(studioId)}`);
}
