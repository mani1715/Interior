export type ReviewInvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED';
export type ReviewStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REMOVED';
export type DisplayNameMode = 'FIRST_NAME' | 'INITIALS' | 'ANONYMOUS';
export type ReportReason = 'INAPPROPRIATE' | 'SPAM' | 'CONFLICT_OF_INTEREST' | 'FALSE_INFORMATION' | 'OTHER';

export interface PublicStudioReviewDto {
  id: string;
  studioId: string;
  rating: number;
  reviewText: string;
  projectTitle?: string | null;
  completedYear?: number | null;
  displayName: string;
  verifiedClient: boolean;
  studioResponse?: string | null;
  studioRespondedAt?: string | null;
  createdAt: string;
}

export interface PublicReviewAggregateDto {
  totalReviews: number;
  averageRating: number;
  ratingBreakdown: Record<number, number>;
}

export interface PublicStudioReviewsResponse {
  studioId: string;
  studioName: string;
  aggregate: PublicReviewAggregateDto;
  reviews: PublicStudioReviewDto[];
}

export interface ReviewInvitationDto {
  id: string;
  studioId: string;
  leadId: string;
  clientName: string;
  clientEmail?: string | null;
  clientPhone?: string | null;
  status: ReviewInvitationStatus;
  invitationUrl?: string | null;
  sentAt?: string | null;
  expiresAt: string;
  createdAt: string;
}

export interface CreateReviewInvitationRequest {
  leadId: string;
  clientName: string;
  clientEmail?: string | null;
  clientPhone?: string | null;
}

export interface CreateReviewInvitationResponse {
  invitationId: string;
  invitationToken: string;
  invitationUrl: string;
  expiresAt: string;
}

export interface ExchangeReviewTokenResponse {
  valid: boolean;
  studioId: string;
  studioName: string;
  clientName: string;
  expiresAt: string;
}

export interface SubmitReviewRequest {
  rating: number;
  reviewText: string;
  projectTitle?: string | null;
  completedYear?: number | null;
  displayNameMode: DisplayNameMode;
}

export interface StudioReviewResponseRequest {
  responseText: string;
}

export interface ReviewReportRequest {
  reason: ReportReason;
  details?: string | null;
}
