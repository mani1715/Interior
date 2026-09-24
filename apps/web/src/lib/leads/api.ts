import { apiFetch } from '../api-client';
import {
  PublicLeadSubmissionRequest,
  PublicLeadSubmissionResponse,
  PublicWhatsAppHandoffRequest,
  PublicWhatsAppHandoffResponse,
  LeadListResponse,
  LeadCounts,
  LeadDetail,
  LeadNote,
  LeadQueryParams,
  LeadUpdateRequest,
  LeadAssignmentRequest,
  LeadNoteCreateRequest,
  SendWhatsAppMessageRequest,
  WhatsAppMessage,
  WhatsAppProviderStatus,
} from './types';

/**
 * Public, unauthenticated lead submission for a studio or specific project.
 */
export async function submitPublicLead(
  req: PublicLeadSubmissionRequest
): Promise<PublicLeadSubmissionResponse> {
  return apiFetch<PublicLeadSubmissionResponse>('/public/leads', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Public visitor-initiated WhatsApp handoff URL generation.
 */
export async function initiatePublicWhatsAppHandoff(
  req: PublicWhatsAppHandoffRequest
): Promise<PublicWhatsAppHandoffResponse> {
  return apiFetch<PublicWhatsAppHandoffResponse>('/public/contact/whatsapp-handoff', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Studio Workspace: List leads with status, search, and pagination.
 */
export async function fetchLeads(params: LeadQueryParams = {}): Promise<LeadListResponse> {
  const query = new URLSearchParams();
  if (params.status && params.status !== 'ALL') query.set('status', params.status);
  if (params.assignedUserId) query.set('assignedUserId', params.assignedUserId);
  if (params.search) query.set('search', params.search);
  if (params.sort) query.set('sort', params.sort);
  if (params.limit !== undefined) query.set('limit', params.limit.toString());
  if (params.offset !== undefined) query.set('offset', params.offset.toString());
  if (params.studioId) query.set('studioId', params.studioId);

  const qs = query.toString();
  return apiFetch<LeadListResponse>(`/leads${qs ? `?${qs}` : ''}`);
}

/**
 * Studio Workspace: Pipeline counts by status.
 */
export async function fetchLeadCounts(studioId?: string): Promise<LeadCounts> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<LeadCounts>(`/leads/counts${query}`);
}

/**
 * Studio Workspace: Full lead detail with activity audit and notes.
 */
export async function fetchLeadDetail(id: string, studioId?: string): Promise<LeadDetail> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<LeadDetail>(`/leads/${encodeURIComponent(id)}${query}`);
}

/**
 * Studio Workspace: Update lead status or follow-up date with optimistic concurrency.
 */
export async function updateLead(
  id: string,
  req: LeadUpdateRequest,
  studioId?: string
): Promise<LeadDetail> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<LeadDetail>(`/leads/${encodeURIComponent(id)}${query}`, {
    method: 'PATCH',
    body: JSON.stringify(req),
  });
}

/**
 * Studio Workspace: Assign lead to a studio team member.
 */
export async function assignLead(
  id: string,
  req: LeadAssignmentRequest,
  studioId?: string
): Promise<LeadDetail> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<LeadDetail>(`/leads/${encodeURIComponent(id)}/assign${query}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Studio Workspace: Add an internal note to the lead.
 */
export async function addLeadNote(
  id: string,
  req: LeadNoteCreateRequest,
  studioId?: string
): Promise<LeadNote> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<LeadNote>(`/leads/${encodeURIComponent(id)}/notes${query}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Studio Workspace: Archive a lead.
 */
export async function archiveLead(
  id: string,
  expectedVersion?: number,
  studioId?: string
): Promise<{ success: boolean; id: string; archived: boolean }> {
  const query = new URLSearchParams();
  if (expectedVersion !== undefined) query.set('expectedVersion', expectedVersion.toString());
  if (studioId) query.set('studioId', studioId);

  const qs = query.toString();
  return apiFetch<{ success: boolean; id: string; archived: boolean }>(
    `/leads/${encodeURIComponent(id)}/archive${qs ? `?${qs}` : ''}`,
    {
      method: 'POST',
    }
  );
}

/**
 * Studio Workspace: Get WhatsApp status for lead/studio.
 */
export async function fetchWhatsAppStatus(
  id: string,
  studioId?: string
): Promise<WhatsAppProviderStatus> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<WhatsAppProviderStatus>(
    `/leads/${encodeURIComponent(id)}/whatsapp/status${query}`
  );
}

/**
 * Studio Workspace: Send managed WhatsApp message.
 */
export async function sendWhatsAppMessage(
  id: string,
  req: SendWhatsAppMessageRequest,
  studioId?: string
): Promise<WhatsAppMessage> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<WhatsAppMessage>(
    `/leads/${encodeURIComponent(id)}/whatsapp/messages${query}`,
    {
      method: 'POST',
      body: JSON.stringify(req),
    }
  );
}
