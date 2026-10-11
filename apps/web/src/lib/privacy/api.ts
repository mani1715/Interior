import { apiFetch } from '../api-client';

export interface PrivacyRequestDto {
  id: string;
  userId: string;
  studioId?: string | null;
  requestType: 'USER_DATA_EXPORT' | 'STUDIO_DATA_EXPORT' | 'ACCOUNT_CLOSURE' | 'STUDIO_CLOSURE' | 'ACCOUNT_DELETION';
  status: 'REQUESTED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  confirmedAt?: string | null;
  completedAt?: string | null;
  failureReason?: string | null;
  createdAt: string;
}

export interface DataExportDto {
  jobId: string;
  privacyRequestId: string;
  exportType: 'USER' | 'STUDIO';
  status: 'QUEUED' | 'PROCESSING' | 'READY' | 'EXPIRED' | 'FAILED';
  fileSizeBytes?: number | null;
  downloadToken?: string;
  downloadUrl?: string;
  expiresAt?: string;
  downloadCount: number;
  createdAt: string;
}

export interface LegalHoldDto {
  id: string;
  subjectType: 'USER' | 'STUDIO';
  subjectId: string;
  reason: string;
  placedBy: string;
  placedAt: string;
  releasedAt?: string | null;
  active: boolean;
}

export async function fetchMyPrivacyRequests(): Promise<PrivacyRequestDto[]> {
  return apiFetch<PrivacyRequestDto[]>('/privacy/requests');
}

export async function triggerUserDataExport(): Promise<DataExportDto> {
  return apiFetch<DataExportDto>('/privacy/export/user', {
    method: 'POST',
  });
}

export async function triggerStudioDataExport(studioId: string): Promise<DataExportDto> {
  return apiFetch<DataExportDto>(`/privacy/export/studio/${encodeURIComponent(studioId)}`, {
    method: 'POST',
  });
}

export async function closeAccountSelfService(): Promise<PrivacyRequestDto> {
  return apiFetch<PrivacyRequestDto>('/privacy/close-account', {
    method: 'POST',
  });
}

export async function deleteAccountSelfService(): Promise<PrivacyRequestDto> {
  return apiFetch<PrivacyRequestDto>('/privacy/request-deletion', {
    method: 'POST',
  });
}

export async function closeStudioSelfService(studioId: string): Promise<PrivacyRequestDto> {
  return apiFetch<PrivacyRequestDto>(`/privacy/studio/${encodeURIComponent(studioId)}/close`, {
    method: 'POST',
  });
}

export function getExportDownloadUrl(jobId: string, token: string): string {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  return `${baseUrl}/privacy/export/download/${encodeURIComponent(jobId)}?token=${encodeURIComponent(token)}`;
}
