import { apiFetch } from '../api-client';
import { env } from '../env';
import {
  PublicProjectDetailDto,
  PublicStudioDto,
  SeoStatusResponse,
  SitemapItemDto,
  UpdateSeoSettingsRequest,
} from './types';

export async function fetchSeoStatus(studioId?: string): Promise<SeoStatusResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<SeoStatusResponse>(`/seo/status${qs}`);
}

export async function publishStudio(studioId?: string): Promise<SeoStatusResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<SeoStatusResponse>(`/seo/publish${qs}`, {
    method: 'POST',
  });
}

export async function unpublishStudio(studioId?: string): Promise<SeoStatusResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<SeoStatusResponse>(`/seo/unpublish${qs}`, {
    method: 'POST',
  });
}

export async function updateSeoSettings(
  data: UpdateSeoSettingsRequest,
  studioId?: string
): Promise<SeoStatusResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<SeoStatusResponse>(`/seo/settings${qs}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function fetchPublicStudio(slug: string): Promise<PublicStudioDto | null> {
  try {
    const res = await fetch(`${env.apiBaseUrl}/public/studios/${encodeURIComponent(slug)}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      next: { revalidate: 60 },
    });
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      throw new Error(`Failed to fetch public studio: ${res.status}`);
    }
    return (await res.json()) as PublicStudioDto;
  } catch (e) {
    return null;
  }
}

export async function fetchPublicProject(
  studioSlug: string,
  projectSlug: string
): Promise<PublicProjectDetailDto | null> {
  try {
    const res = await fetch(
      `${env.apiBaseUrl}/public/studios/${encodeURIComponent(studioSlug)}/projects/${encodeURIComponent(projectSlug)}`,
      {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        next: { revalidate: 60 },
      }
    );
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      throw new Error(`Failed to fetch public project: ${res.status}`);
    }
    return (await res.json()) as PublicProjectDetailDto;
  } catch (e) {
    return null;
  }
}

export async function fetchSitemapEntries(): Promise<SitemapItemDto[]> {
  try {
    const res = await fetch(`${env.apiBaseUrl}/public/sitemap`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      next: { revalidate: 3600 },
    });
    if (!res.ok) {
      return [];
    }
    return (await res.json()) as SitemapItemDto[];
  } catch (e) {
    return [];
  }
}
