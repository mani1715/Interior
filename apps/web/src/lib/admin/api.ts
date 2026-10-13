import { apiFetch } from '../api-client';
import {
  AdminAuditLog,
  AdminDashboardMetrics,
  AdminResetMfaRequest,
  AdminReviewSummary,
  AdminStudioSummary,
  AdminUserSummary,
  AdminVerificationDecisionRequest,
  AdminVerificationSummary,
  ModerateReviewRequest,
  OperationalJobRun,
  PlatformOperationalState,
  ProviderMatrixResponse,
  UpdateOperationalStateRequest,
  UpdateStudioStatusRequest,
  UpdateUserRoleRequest,
  UpdateUserStatusRequest,
} from './types';

export async function fetchAdminDashboard(): Promise<AdminDashboardMetrics> {
  return apiFetch<AdminDashboardMetrics>('/admin/dashboard');
}

export async function fetchAdminUsers(
  status?: string,
  limit: number = 50,
  offset: number = 0
): Promise<AdminUserSummary[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (status) params.append('status', status);
  return apiFetch<AdminUserSummary[]>(`/admin/users?${params.toString()}`);
}

export async function updateUserStatus(
  userId: string,
  req: UpdateUserStatusRequest
): Promise<void> {
  return apiFetch<void>(`/admin/users/${userId}/status`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function fetchAdminStudios(
  status?: string,
  limit: number = 50,
  offset: number = 0
): Promise<AdminStudioSummary[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (status) params.append('status', status);
  return apiFetch<AdminStudioSummary[]>(`/admin/studios?${params.toString()}`);
}

export async function updateStudioStatus(
  studioId: string,
  req: UpdateStudioStatusRequest
): Promise<void> {
  return apiFetch<void>(`/admin/studios/${studioId}/status`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function fetchAdminVerifications(
  status?: string,
  limit: number = 50,
  offset: number = 0
): Promise<AdminVerificationSummary[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (status) params.append('status', status);
  return apiFetch<AdminVerificationSummary[]>(`/admin/verification?${params.toString()}`);
}

export async function recordVerificationDecision(
  studioId: string,
  req: AdminVerificationDecisionRequest
): Promise<void> {
  return apiFetch<void>(`/admin/verification/${studioId}/decision`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function fetchAdminReviews(
  status?: string,
  limit: number = 50,
  offset: number = 0
): Promise<AdminReviewSummary[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (status) params.append('status', status);
  return apiFetch<AdminReviewSummary[]>(`/admin/reviews?${params.toString()}`);
}

export async function moderateReview(
  reviewId: string,
  req: ModerateReviewRequest
): Promise<void> {
  return apiFetch<void>(`/admin/reviews/${reviewId}/status`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function fetchAdminAuditLogs(
  action?: string,
  limit: number = 50,
  offset: number = 0
): Promise<AdminAuditLog[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (action) params.append('action', action);
  return apiFetch<AdminAuditLog[]>(`/admin/audit-logs?${params.toString()}`);
}

export async function resetUserMfa(
  userId: string,
  req: AdminResetMfaRequest
): Promise<void> {
  return apiFetch<void>(`/admin/users/${userId}/mfa/reset`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function updateUserRole(
  userId: string,
  req: UpdateUserRoleRequest
): Promise<void> {
  return apiFetch<void>(`/admin/users/${userId}/role`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function revokeUserSessions(userId: string): Promise<void> {
  return apiFetch<void>(`/admin/users/${userId}/revoke-sessions`, {
    method: 'POST',
  });
}

export async function fetchOperationalState(): Promise<PlatformOperationalState> {
  return apiFetch<PlatformOperationalState>('/admin/operations/state');
}

export async function updateOperationalState(
  req: UpdateOperationalStateRequest
): Promise<PlatformOperationalState> {
  return apiFetch<PlatformOperationalState>('/admin/operations/state', {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export async function fetchProviderMatrix(): Promise<ProviderMatrixResponse> {
  return apiFetch<ProviderMatrixResponse>('/admin/operations/providers');
}

export async function fetchOperationalJobs(
  limit: number = 50,
  offset: number = 0,
  jobKey?: string
): Promise<OperationalJobRun[]> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  if (jobKey) params.append('jobKey', jobKey);
  return apiFetch<OperationalJobRun[]>(`/admin/operations/jobs?${params.toString()}`);
}

export async function triggerOperationalJob(
  jobKey: string
): Promise<OperationalJobRun> {
  return apiFetch<OperationalJobRun>(`/admin/operations/jobs/${jobKey}/run`, {
    method: 'POST',
  });
}

