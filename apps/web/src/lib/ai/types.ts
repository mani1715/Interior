export type AiJobStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';

export type EditingMode = 'FULL_IMAGE' | 'PRECISION_MASK';

export interface UploadMaskResponse {
  maskId: string;
  maskStorageKey: string;
  width: number;
  height: number;
  coverageRatio: number;
}

export type ReferencePurpose =
  | 'COLOR'
  | 'MATERIAL'
  | 'WOOD'
  | 'STONE'
  | 'TILE'
  | 'FABRIC'
  | 'HARDWARE'
  | 'FURNITURE_STYLE'
  | 'CABINET_STYLE'
  | 'ROOM_STYLE'
  | 'WALL_FINISH'
  | 'CEILING_STYLE'
  | 'GENERAL_STYLE';

export interface ReferencePurposeConfig {
  code: ReferencePurpose;
  label: string;
  description: string;
}

export const REFERENCE_PURPOSES: ReferencePurposeConfig[] = [
  { code: 'COLOR', label: 'Color Palette', description: 'Wall or accent colors, palettes, and tone' },
  { code: 'MATERIAL', label: 'Material', description: 'General material guidance' },
  { code: 'WOOD', label: 'Wood & Laminate', description: 'Veneer, timber species, grain, or laminate finish' },
  { code: 'STONE', label: 'Stone, Granite & Marble', description: 'Marble, granite, quartz, or natural stone' },
  { code: 'TILE', label: 'Tile & Backsplash', description: 'Floor, wall, or backsplash tiles and patterns' },
  { code: 'FABRIC', label: 'Fabric & Upholstery', description: 'Fabric texture, weave, curtains, and upholstery' },
  { code: 'HARDWARE', label: 'Hardware & Fixtures', description: 'Handles, knobs, faucets, and metal hardware' },
  { code: 'FURNITURE_STYLE', label: 'Furniture Style', description: 'Seating, tables, or freestanding furniture' },
  { code: 'CABINET_STYLE', label: 'Cabinetry & Wardrobes', description: 'Shutters, wardrobe doors, groove detailing' },
  { code: 'ROOM_STYLE', label: 'Room Style', description: 'Inspiration for spatial ambiance and theme' },
  { code: 'WALL_FINISH', label: 'Wall Finish', description: 'Wallpaper, texture paint, fluted panels' },
  { code: 'CEILING_STYLE', label: 'Ceiling & Lighting', description: 'False ceiling design, coves, lighting' },
  { code: 'GENERAL_STYLE', label: 'General Inspiration', description: 'Broad design inspiration or aesthetic mood' },
];

export interface AiStudioStatus {
  isConfigured: boolean;
  providerKey: string;
  dailyQuota: number;
  usedToday: number;
  remainingToday: number;
  supportsReferenceImages?: boolean;
  maxReferenceImages?: number;
  supportsMaskEditing?: boolean;
}

export interface AiJobReferenceInput {
  mediaId: string;
  purpose: ReferencePurpose;
  label?: string;
  instruction?: string;
  displayOrder?: number;
}

export interface AiJobReference {
  id: string;
  mediaId: string;
  previewUrl?: string | null;
  purpose: ReferencePurpose;
  purposeDisplayName: string;
  label?: string | null;
  instruction?: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface ReferenceDetail {
  id: string;
  mediaId: string;
  studioId: string;
  projectId?: string | null;
  purpose: ReferencePurpose;
  purposeDisplayName: string;
  label?: string | null;
  defaultInstruction?: string | null;
  previewUrl?: string | null;
  createdAt: string;
  updatedAt: string;
  archivedAt?: string | null;
}

export interface CreateReferencePayload {
  mediaId: string;
  projectId?: string | null;
  purpose: ReferencePurpose;
  label?: string;
  defaultInstruction?: string;
}

export interface UpdateReferencePayload {
  purpose?: ReferencePurpose;
  label?: string;
  defaultInstruction?: string;
  projectId?: string | null;
}

export interface AiJobDetail {
  id: string;
  studioId: string;
  projectId: string;
  inputMediaId: string;
  inputPreviewUrl?: string | null;
  outputMediaId?: string | null;
  outputPreviewUrl?: string | null;
  providerKey: string;
  prompt: string;
  status: AiJobStatus;
  errorCode?: string | null;
  errorMessageSafe?: string | null;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  failedAt?: string | null;
  preserveStructure?: boolean;
  references?: AiJobReference[];
  editingMode?: EditingMode;
  maskPreviewUrl?: string | null;
  parentJobId?: string | null;
  rootJobId?: string | null;
  isShortlisted?: boolean;
  isStudioSelected?: boolean;
  conceptLabel?: string | null;
}

export interface AiJobListResponse {
  items: AiJobDetail[];
  total: number;
  limit: number;
  offset: number;
}

export interface CreateAiJobPayload {
  inputMediaId: string;
  projectId: string;
  prompt: string;
  idempotencyKey?: string;
  preserveStructure?: boolean;
  references?: AiJobReferenceInput[];
  editingMode?: EditingMode;
  maskStorageKey?: string;
}

export type VariationStrategy = 'REFINE_ORIGINAL' | 'EVOLVE_CONCEPT';

export interface CreateVariationPayload {
  variationStrategy: VariationStrategy;
  prompt?: string;
  preserveStructure?: boolean;
  editingMode?: EditingMode;
  reuseParentMask?: boolean;
  newMaskSourceJobId?: string;
  references?: AiJobReferenceInput[];
  idempotencyKey?: string;
}

export interface AiJobHistoryResponse {
  items: AiJobDetail[];
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
}

export type ClientReviewStatus = 'OPEN' | 'CLOSED' | 'REVOKED';
export type ClientReviewDecisionType = 'APPROVED' | 'CHANGES_REQUESTED';
export type CommentAuthorType = 'STUDIO' | 'CLIENT';

export interface CreateClientReviewPayload {
  projectId: string;
  title: string;
  customMessage?: string;
  expiryDays?: number;
  includeOriginal?: boolean;
  conceptJobIds: string[];
}

export interface ClientReviewItemDto {
  id: string;
  jobId: string;
  mediaId: string;
  displayLabel: string;
  displayOrder: number;
  previewUrl?: string | null;
}

export interface ClientReviewDecisionDto {
  id: string;
  jobId: string;
  decision: ClientReviewDecisionType;
  clientName?: string | null;
  feedback?: string | null;
  isCurrent: boolean;
  createdAt: string;
}

export interface ClientReviewCommentDto {
  id: string;
  jobId?: string | null;
  authorType: CommentAuthorType;
  authorName: string;
  commentText: string;
  createdAt: string;
}

export interface CreateClientReviewResponse {
  id: string;
  projectId: string;
  title: string;
  customMessage?: string | null;
  rawToken: string;
  reviewUrl: string;
  status: string;
  includeOriginal: boolean;
  expiresAt: string;
  items: ClientReviewItemDto[];
  createdAt: string;
}

export interface ClientReviewDetailResponse {
  id: string;
  projectId: string;
  projectTitle: string;
  title: string;
  customMessage?: string | null;
  status: string;
  includeOriginal: boolean;
  expiresAt: string;
  currentApprovedJobId?: string | null;
  items: ClientReviewItemDto[];
  decisions: ClientReviewDecisionDto[];
  comments: ClientReviewCommentDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ExchangeReviewTokenResponse {
  reviewPublicId: string;
  csrfToken: string;
  redirectUrl: string;
}

export interface PublicReviewItemDto {
  id: string;
  jobId: string;
  mediaId: string;
  displayLabel: string;
  displayOrder: number;
  previewUrl: string;
  currentDecision?: ClientReviewDecisionType | null;
}

export interface PublicReviewDecisionDto {
  id: string;
  jobId: string;
  decision: ClientReviewDecisionType;
  clientName?: string | null;
  feedback?: string | null;
  isCurrent: boolean;
  createdAt: string;
}

export interface PublicReviewCommentDto {
  id: string;
  jobId?: string | null;
  authorType: CommentAuthorType;
  authorName: string;
  commentText: string;
  createdAt: string;
}

export interface PublicClientReviewResponse {
  id: string;
  studioName: string;
  projectTitle: string;
  title: string;
  customMessage?: string | null;
  status: string;
  includeOriginal: boolean;
  originalPreviewUrl?: string | null;
  expiresAt: string;
  isExpired: boolean;
  currentApprovedJobId?: string | null;
  items: PublicReviewItemDto[];
  decisions: PublicReviewDecisionDto[];
  comments: PublicReviewCommentDto[];
  createdAt: string;
}

export interface SubmitClientDecisionRequest {
  jobId: string;
  decision: ClientReviewDecisionType;
  clientName?: string;
  feedback?: string;
}

export interface SubmitClientCommentRequest {
  jobId?: string;
  authorName: string;
  commentText: string;
}
