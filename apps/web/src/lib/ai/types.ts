export type AiJobStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';

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
}
