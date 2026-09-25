export type VerificationStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'NEEDS_MORE_INFO'
  | 'VERIFIED'
  | 'REJECTED'
  | 'REVERIFY_REQUIRED'
  | 'EXPIRED';

export type VerificationDocumentType =
  | 'BUSINESS_REGISTRATION'
  | 'GST_CERTIFICATE'
  | 'TRADE_LICENSE'
  | 'COA_ARCHITECT_REGISTRATION'
  | 'INCORPORATION_CERTIFICATE'
  | 'OTHER_GOVERNMENT_IDENTIFIER';

export type VerificationEventType =
  | 'SUBMITTED'
  | 'MORE_INFO_REQUESTED'
  | 'ADDITIONAL_INFO_SUBMITTED'
  | 'VERIFIED'
  | 'REJECTED'
  | 'INVALIDATED_REVERIFY_REQUIRED'
  | 'EXPIRED';

export interface VerificationDocumentDto {
  id: string;
  documentType: VerificationDocumentType;
  fileName: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadedAt: string;
}

export interface VerificationEventDto {
  id: string;
  eventType: VerificationEventType;
  previousStatus?: VerificationStatus | null;
  newStatus: VerificationStatus;
  actorUserId?: string | null;
  notes?: string | null;
  reason?: string | null;
  createdAt: string;
}

export interface StudioVerificationDto {
  id?: string | null;
  studioId: string;
  status: VerificationStatus;
  businessName?: string | null;
  registrationNumber?: string | null;
  taxIdentifier?: string | null;
  registeredAddress?: string | null;
  rejectionReason?: string | null;
  adminNotes?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
  verifiedAt?: string | null;
  expiresAt?: string | null;
  documents: VerificationDocumentDto[];
  events: VerificationEventDto[];
}

export interface SubmitVerificationRequest {
  businessName: string;
  registrationNumber?: string | null;
  taxIdentifier?: string | null;
  registeredAddress?: string | null;
  documentIds: string[];
}

export interface PublicVerificationBadgeDto {
  studioId: string;
  verified: boolean;
  status: VerificationStatus;
  verifiedAt?: string | null;
  businessName?: string | null;
  disclaimer: string;
}
