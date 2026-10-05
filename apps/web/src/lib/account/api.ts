import { apiFetch } from '../api-client';

export interface ActiveSessionDto {
  id: string;
  deviceLabel: string;
  authTime: string;
  lastSeenAt: string;
  idleExpiresAt: string;
  current: boolean;
}

export interface CustomerInquiryDto {
  id: string;
  studioId: string;
  studioName: string;
  serviceType: string;
  status: string;
  budgetInr?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  displayName: string;
  phone?: string | null;
  avatarUrl?: string | null;
}

export interface PlatformFeedbackRequest {
  category: string;
  message: string;
  contactEmail?: string;
}

export async function fetchActiveSessions(): Promise<ActiveSessionDto[]> {
  return apiFetch<ActiveSessionDto[]>('/account/sessions');
}

export async function revokeActiveSession(sessionId: string): Promise<void> {
  await apiFetch<void>(`/account/sessions/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE',
  });
}

export async function fetchCustomerInquiries(limit: number = 30, offset: number = 0): Promise<CustomerInquiryDto[]> {
  return apiFetch<CustomerInquiryDto[]>(`/account/inquiries?limit=${limit}&offset=${offset}`);
}

export async function updateAccountProfile(req: UpdateProfileRequest): Promise<any> {
  return apiFetch('/account/profile', {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export async function requestAccountErasure(): Promise<void> {
  await apiFetch('/account/request-deletion', {
    method: 'POST',
  });
}

export async function submitPlatformFeedback(req: PlatformFeedbackRequest): Promise<{ success: boolean }> {
  return apiFetch<{ success: boolean }>('/account/feedback', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}
