export type AiJobStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';

export interface AiStudioStatus {
  isConfigured: boolean;
  providerKey: string;
  dailyQuota: number;
  usedToday: number;
  remainingToday: number;
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
}
