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
  return normalizeVerification(await apiFetch<VerificationWire>('/verification/studio'));
}

type VerificationWire = Omit<StudioVerificationDto, 'documents'> & {
  gstNumber?: string; notes?: string; decisionReason?: string;
  documents: (VerificationDocumentDto & { originalFilename?: string; createdAt?: string })[];
};
function normalizeVerification(data: VerificationWire): StudioVerificationDto {
  return { ...data, taxIdentifier: data.gstNumber, registeredAddress: data.notes,
    rejectionReason: data.decisionReason,
    documents: data.documents.map(doc => ({ ...doc, fileName: doc.originalFilename ?? doc.fileName, uploadedAt: doc.createdAt ?? doc.uploadedAt })) };
}

export async function submitVerification(
  data: SubmitVerificationRequest
): Promise<StudioVerificationDto> {
  return normalizeVerification(await apiFetch<VerificationWire>('/verification/studio/submit', {
    method: 'POST',
    body: JSON.stringify({ businessName: data.businessName, professionalType: data.professionalType,
      registrationNumber: data.registrationNumber, gstNumber: data.taxIdentifier, notes: data.registeredAddress }),
  }));
}

export async function registerVerificationDocument(
  file: File,
  documentType: VerificationDocumentType
): Promise<VerificationDocumentDto> {
  const formData = new FormData();
  formData.append('file', file);
  const canonicalType = documentType === 'COA_ARCHITECT_REGISTRATION' ? 'PROFESSIONAL_LICENSE'
    : ['TRADE_LICENSE', 'INCORPORATION_CERTIFICATE'].includes(documentType) ? 'BUSINESS_REGISTRATION'
    : documentType === 'OTHER_GOVERNMENT_IDENTIFIER' ? 'OTHER' : documentType;
  formData.append('documentType', canonicalType);

  return apiFetch<VerificationDocumentDto>('/verification/studio/documents', {
    method: 'POST',
    body: formData,
  });
}
