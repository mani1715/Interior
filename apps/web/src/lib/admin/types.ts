export interface AdminAuditLog {
  id: string;
  studioId: string | null;
  actorId: string | null;
  actorEmail: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  requestId: string;
  details: string | null;
  timestamp: string;
}

export interface AdminDashboardMetrics {
  totalStudios: number;
  activeStudios: number;
  suspendedStudios: number;
  publishedStudios: number;
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  totalProjects: number;
  publicProjects: number;
  totalLeads: number;
  pendingVerifications: number;
  pendingReviewReports: number;
  totalMediaAssets: number;
  totalAiGenerations: number;
  recentActivity: AdminAuditLog[];
}

export interface AdminUserSummary {
  id: string;
  displayName: string;
  email: string | null;
  phone: string | null;
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  createdAt: string;
  roles: string[];
  studioMemberships: string[];
}

export interface AdminStudioSummary {
  id: string;
  name: string;
  slug: string;
  ownerId: string;
  ownerEmail: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'DRAFT' | 'DELETED';
  publicationStatus: string;
  verificationStatus: string;
  projectCount: number;
  publicProjectCount: number;
  createdAt: string;
}

export interface AdminVerificationSummary {
  id: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  businessName: string;
  professionalType: string;
  status: 'NOT_SUBMITTED' | 'PENDING' | 'NEEDS_MORE_INFO' | 'VERIFIED' | 'REJECTED' | 'REVERIFY_REQUIRED' | 'EXPIRED';
  registrationNumber: string | null;
  gstNumber: string | null;
  websiteDomain: string | null;
  notes: string | null;
  decisionReason: string | null;
  verifiedAt: string | null;
  expiresAt: string | null;
  submittedAt: string | null;
  documentCount: number;
}

export interface AdminReviewSummary {
  id: string;
  studioId: string;
  studioName: string;
  leadId: string;
  rating: number;
  title: string | null;
  reviewText: string;
  reviewerDisplayName: string;
  displayNameMode: string;
  status: 'SUBMITTED' | 'PUBLISHED' | 'FLAGGED' | 'REMOVED';
  studioResponseText: string | null;
  studioResponseAt: string | null;
  submittedAt: string;
  publishedAt: string | null;
  reportCount: number;
}

export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  reason?: string;
}

export interface UpdateStudioStatusRequest {
  status: 'ACTIVE' | 'SUSPENDED';
  reason?: string;
}

export interface ModerateReviewRequest {
  status: 'PUBLISHED' | 'FLAGGED' | 'REMOVED';
  reason?: string;
}

export interface AdminVerificationDecisionRequest {
  status: 'NEEDS_MORE_INFO' | 'VERIFIED' | 'REJECTED' | 'REVERIFY_REQUIRED';
  reason?: string;
}
