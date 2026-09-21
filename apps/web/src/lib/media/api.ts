import { apiFetch, getCsrfToken } from '../api-client';
import { env } from '../env';
import {
  CommitUploadRequest,
  CreateUploadIntentRequest,
  MediaDetailResponse,
  MediaType,
  MediaVisibility,
  UpdateMediaRequest,
  UploadIntentResponse,
  WatermarkSettings,
} from './types';

export async function createUploadIntent(
  data: CreateUploadIntentRequest,
  studioId?: string
): Promise<UploadIntentResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<UploadIntentResponse>(`/media/upload-intent${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function uploadQuarantineData(
  uploadUrl: string,
  content: Blob | ArrayBuffer,
  contentType: string = 'image/jpeg'
): Promise<void> {
  let targetUrl = uploadUrl;
  if (!targetUrl.startsWith('http')) {
    const base = env.apiBaseUrl.replace(/\/api\/v1\/?$/, '');
    targetUrl = targetUrl.startsWith('/api/v1') ? `${base}${targetUrl}` : `${env.apiBaseUrl}${targetUrl}`;
  }

  const csrfToken = await getCsrfToken();
  const headers: Record<string, string> = {
    'Content-Type': contentType,
  };
  if (csrfToken) {
    headers['X-CSRF-Token'] = csrfToken;
  }

  const res = await fetch(targetUrl, {
    method: 'PUT',
    credentials: 'include',
    headers,
    body: content,
  });

  if (!res.ok) {
    throw new Error(`Direct upload failed with status ${res.status}`);
  }
}

export async function commitUpload(
  data: CommitUploadRequest,
  studioId?: string
): Promise<MediaDetailResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<MediaDetailResponse>(`/media/commit${qs}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function fetchProjectMedia(
  projectId: string,
  studioId?: string
): Promise<MediaDetailResponse[]> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<MediaDetailResponse[]>(`/projects/${projectId}/media${qs}`);
}

export async function fetchStudioMedia(
  filters?: {
    projectId?: string;
    mediaType?: MediaType;
    visibility?: MediaVisibility;
  },
  studioId?: string
): Promise<MediaDetailResponse[]> {
  const params = new URLSearchParams();
  if (studioId) params.append('studioId', studioId);
  if (filters?.projectId) params.append('projectId', filters.projectId);
  if (filters?.mediaType) params.append('mediaType', filters.mediaType);
  if (filters?.visibility) params.append('visibility', filters.visibility);
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<MediaDetailResponse[]>(`/media${qs}`);
}

export async function fetchMediaDetail(
  mediaId: string,
  studioId?: string
): Promise<MediaDetailResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<MediaDetailResponse>(`/media/${mediaId}${qs}`);
}

export async function updateMedia(
  mediaId: string,
  data: UpdateMediaRequest,
  studioId?: string
): Promise<MediaDetailResponse> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<MediaDetailResponse>(`/media/${mediaId}${qs}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export async function reorderProjectMedia(
  projectId: string,
  orderedMediaIds: string[],
  studioId?: string
): Promise<void> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  await apiFetch<void>(`/projects/${projectId}/media/reorder${qs}`, {
    method: 'POST',
    body: JSON.stringify({ orderedMediaIds }),
  });
}

export async function deleteMedia(
  mediaId: string,
  studioId?: string
): Promise<void> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  await apiFetch<void>(`/media/${mediaId}${qs}`, {
    method: 'DELETE',
  });
}

export async function fetchWatermarkSettings(
  studioId?: string
): Promise<WatermarkSettings> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<WatermarkSettings>(`/media/watermark-settings${qs}`);
}

export async function updateWatermarkSettings(
  data: WatermarkSettings,
  studioId?: string
): Promise<WatermarkSettings> {
  const qs = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<WatermarkSettings>(`/media/watermark-settings${qs}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function uploadMediaFile(
  file: File,
  projectId: string,
  options: {
    mediaType: MediaType;
    altText?: string;
    caption?: string;
    isCover?: boolean;
    visibility?: MediaVisibility;
    watermarkEnabled?: boolean;
    studioId?: string;
    onProgress?: (progress: number) => void;
  }
): Promise<MediaDetailResponse> {
  options.onProgress?.(10);
  const intent = await createUploadIntent(
    {
      projectId,
      mediaType: options.mediaType,
      expectedContentType: file.type || 'image/jpeg',
      expectedSizeBytes: file.size,
      clientFileName: file.name,
    },
    options.studioId
  );

  options.onProgress?.(40);
  await uploadQuarantineData(intent.uploadUrl, file, file.type || 'image/jpeg');

  options.onProgress?.(75);
  const result = await commitUpload(
    {
      uploadIntentId: intent.uploadIntentId,
      altText: options.altText,
      caption: options.caption,
      isCover: options.isCover,
      visibility: options.visibility,
      watermarkEnabled: options.watermarkEnabled,
    },
    options.studioId
  );

  options.onProgress?.(100);
  return result;
}
