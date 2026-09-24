export type LeadStatus =
  | 'NEW'
  | 'CONTACTED'
  | 'QUALIFIED'
  | 'SITE_VISIT_PLANNED'
  | 'IN_DISCUSSION'
  | 'WON'
  | 'LOST'
  | 'ARCHIVED';

export type PreferredContactChannel = 'PHONE' | 'WHATSAPP' | 'EMAIL' | 'ANY';

export interface PublicLeadSubmissionRequest {
  targetStudioSlug: string;
  targetProjectSlug?: string | null;
  name: string;
  phone: string;
  email?: string | null;
  city?: string | null;
  projectCategory?: string | null;
  budgetRange?: string | null;
  message: string;
  preferredContactChannel?: PreferredContactChannel | null;
  consentContact: boolean;
  consentWhatsapp?: boolean;
  idempotencyKey?: string | null;
  website_hp?: string | null;
}

export interface PublicLeadSubmissionResponse {
  referenceNumber: string;
  message: string;
  studioName: string;
}

export interface PublicWhatsAppHandoffRequest {
  targetStudioSlug: string;
  targetProjectSlug?: string | null;
  visitorName?: string | null;
  visitorPhone?: string | null;
  customMessage?: string | null;
}

export interface PublicWhatsAppHandoffResponse {
  whatsappUrl: string;
  studioName: string;
  studioPhoneMasked: string;
  prefilledMessage: string;
}

export interface LeadSummary {
  id: string;
  studioId: string;
  projectId?: string | null;
  projectTitle?: string | null;
  projectSlug?: string | null;
  source: string;
  sourceDisplayName: string;
  status: LeadStatus;
  statusDisplayName: string;
  name: string;
  phoneMasked: string;
  phoneNormalized: string;
  emailNormalized?: string | null;
  city?: string | null;
  projectCategory?: string | null;
  budgetRange?: string | null;
  preferredContactChannel?: string | null;
  hasWhatsappConsent: boolean;
  assignedUserId?: string | null;
  assignedUserName?: string | null;
  nextFollowUpAt?: string | null;
  possibleDuplicate: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  archivedAt?: string | null;
}

export interface LeadNote {
  id: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface LeadActivity {
  id: string;
  actorId?: string | null;
  actorName?: string | null;
  activityType: string;
  activityTypeDisplayName: string;
  details?: string | null;
  createdAt: string;
}

export interface LeadWhatsAppMessage {
  id: string;
  direction: 'OUTBOUND' | 'INBOUND';
  provider: string;
  status: string;
  body: string;
  failureCode?: string | null;
  createdAt: string;
  statusUpdatedAt: string;
}

export type WhatsAppMessage = LeadWhatsAppMessage;

export interface LeadDetail extends LeadSummary {
  message: string;
  contactConsentAt: string;
  whatsappConsentAt?: string | null;
  lostReason?: string | null;
  notes: LeadNote[];
  activities: LeadActivity[];
  messages: LeadWhatsAppMessage[];
}

export interface LeadCounts {
  total: number;
  newLeads: number;
  active: number;
  won: number;
  lost: number;
  archived: number;
}

export interface LeadListResponse {
  items: LeadSummary[];
  total: number;
  limit: number;
  offset: number;
  hasMore: boolean;
}

export interface LeadQueryParams {
  status?: string;
  assignedUserId?: string;
  search?: string;
  sort?: string;
  limit?: number;
  offset?: number;
  studioId?: string;
}

export interface LeadUpdateRequest {
  status?: LeadStatus;
  nextFollowUpAt?: string | null;
  lostReason?: string | null;
  expectedVersion?: number;
}

export interface LeadAssignmentRequest {
  assignedUserId?: string | null;
  expectedVersion?: number;
}

export interface LeadNoteCreateRequest {
  content: string;
}

export interface SendWhatsAppMessageRequest {
  body: string;
  idempotencyKey?: string | null;
}

export interface WhatsAppProviderStatus {
  configured: boolean;
  providerName: string;
  status: string;
  handoffEnabled: boolean;
  publicPhone?: string | null;
}
