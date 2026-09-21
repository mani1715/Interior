export type MediaType =
  | 'REAL_PROJECT'
  | 'BEFORE'
  | 'AFTER'
  | 'AI_CONCEPT'
  | 'REFERENCE'
  | 'CLIENT_PRIVATE';

export type MediaVisibility = 'PRIVATE' | 'PORTFOLIO' | 'PUBLIC';

export type MediaProcessingStatus =
  | 'PENDING_UPLOAD'
  | 'UPLOADED'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED'
  | 'QUARANTINED'
  | 'DELETED';

export type DerivativeVariant = 'THUMBNAIL' | 'MEDIUM' | 'LARGE';

export type WatermarkPosition =
  | 'TOP_LEFT'
  | 'TOP_RIGHT'
  | 'BOTTOM_LEFT'
  | 'BOTTOM_RIGHT'
  | 'CENTER';

export interface MediaDerivativeDto {
  id: string;
  variantName: DerivativeVariant;
  width: number;
  height: number;
  format: string;
  fileSize: number;
  publicUrl: string;
  isWatermarked: boolean;
}

export interface MediaDetailResponse {
  id: string;
  studioId: string;
  projectId: string;
  mediaType: MediaType;
  mediaTypeDisplayName: string;
  visibility: MediaVisibility;
  processingStatus: MediaProcessingStatus;
  originalStorageKey: string;
  contentType: string;
  fileSize: number;
  width: number;
  height: number;
  sortOrder: number;
  isCover: boolean;
  altText?: string | null;
  caption?: string | null;
  watermarkEnabled: boolean;
  derivatives: MediaDerivativeDto[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateUploadIntentRequest {
  projectId: string;
  mediaType: MediaType;
  expectedContentType: string;
  expectedSizeBytes: number;
  clientFileName?: string;
}

export interface UploadIntentResponse {
  uploadIntentId: string;
  mediaAssetId: string;
  uploadUrl: string;
  quarantineKey: string;
  expiresAt: string;
}

export interface CommitUploadRequest {
  uploadIntentId: string;
  altText?: string;
  caption?: string;
  isCover?: boolean;
  visibility?: MediaVisibility;
  watermarkEnabled?: boolean;
}

export interface UpdateMediaRequest {
  altText?: string;
  caption?: string;
  isCover?: boolean;
  visibility?: MediaVisibility;
  watermarkEnabled?: boolean;
  sortOrder?: number;
}

export interface ReorderMediaRequest {
  orderedMediaIds: string[];
}

export interface WatermarkSettings {
  studioId?: string;
  enabled: boolean;
  position: WatermarkPosition;
  opacity: number;
  sizePercentage: number;
  useLogo: boolean;
  fallbackText?: string;
}

export const MEDIA_TYPE_LABELS: Record<MediaType, string> = {
  REAL_PROJECT: 'Real Project Photo',
  BEFORE: 'Before Renovation',
  AFTER: 'After Renovation',
  AI_CONCEPT: 'AI Concept Visualization',
  REFERENCE: 'Inspirational Reference (Private)',
  CLIENT_PRIVATE: 'Confidential Client Spec (Private)',
};

export const VISIBILITY_LABELS: Record<MediaVisibility, string> = {
  PORTFOLIO: 'Portfolio Showcase',
  PUBLIC: 'Public & Discoverable',
  PRIVATE: 'Private (Studio Only)',
};

export const CANONICAL_MEDIA_TYPES: { code: MediaType; label: string }[] = [
  { code: 'REAL_PROJECT', label: 'Real Project Photo' },
  { code: 'BEFORE', label: 'Before Renovation' },
  { code: 'AFTER', label: 'After Renovation' },
  { code: 'AI_CONCEPT', label: 'AI Concept Visualization' },
  { code: 'REFERENCE', label: 'Inspirational Reference (Private)' },
  { code: 'CLIENT_PRIVATE', label: 'Confidential Client Spec (Private)' },
];

export const WATERMARK_POSITIONS: { code: WatermarkPosition; label: string }[] = [
  { code: 'BOTTOM_RIGHT', label: 'Bottom Right (Standard)' },
  { code: 'BOTTOM_LEFT', label: 'Bottom Left' },
  { code: 'TOP_RIGHT', label: 'Top Right' },
  { code: 'TOP_LEFT', label: 'Top Left' },
  { code: 'CENTER', label: 'Center (Diagonal)' },
];

