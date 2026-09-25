import { apiFetch } from '../api-client';
import {
  PublicVerificationBadgeDto,
  StudioVerificationDto,
  SubmitVerificationRequest,
  VerificationDocumentDto,
  VerificationDocumentType,
} from './types';

/**
 * Public verification badge
 */
export async function getPublicVerificationBadge(
  studioSlug: string
): Promise<PublicVerificationBadgeDto> {
  return apiFetch<PublicVerificationBadgeDto>(
    `/public/studios/${encodeURIComponent(studioSlug)}/verification`
  );
}

/**
 * Studio workspace verification endpoints
 */
export async function getStudioVerification(): Promise<StudioVerificationDto> {
  return apiFetch<StudioVerificationDto>('/studio/verification');
}

export async function submitVerification(
  data: SubmitVerificationRequest
): Promise<StudioVerificationDto> {
  return apiFetch<StudioVerificationDto>('/studio/verification/submit', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function registerVerificationDocument(
  file: File,
  documentType: VerificationDocumentType
): Promise<VerificationDocumentDto> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('documentType', documentType);

  return apiFetch<VerificationDocumentDto>('/studio/verification/documents', {
    method: 'POST',
    body: formData,
  });
}
