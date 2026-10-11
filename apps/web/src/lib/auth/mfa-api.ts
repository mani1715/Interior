import { apiFetch } from '../api-client';

export interface MfaStatusDto {
  mfaEnabled: boolean;
  methodType: string | null;
  confirmedAt: string | null;
  unusedRecoveryCodesCount: number;
  sessionMfaVerified: boolean;
  stepUpValidUntil: string | null;
  mfaRequiredForRole: boolean;
}

export interface MfaEnrollmentDto {
  secretBase32: string;
  otpauthUri: string;
  expiresAt: string;
}

export interface MfaConfirmResponseDto {
  success: boolean;
  recoveryCodes: string[];
  sessionElevated: boolean;
}

export async function fetchMfaStatus(): Promise<MfaStatusDto> {
  return apiFetch<MfaStatusDto>('/auth/mfa/status');
}

export async function startMfaEnrollment(): Promise<MfaEnrollmentDto> {
  return apiFetch<MfaEnrollmentDto>('/auth/mfa/totp/enroll', {
    method: 'POST',
  });
}

export async function confirmMfaEnrollment(code: string): Promise<MfaConfirmResponseDto> {
  return apiFetch<MfaConfirmResponseDto>('/auth/mfa/totp/confirm', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function verifyMfaChallenge(code: string): Promise<MfaStatusDto> {
  return apiFetch<MfaStatusDto>('/auth/mfa/challenge', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function verifyRecoveryCode(recoveryCode: string): Promise<MfaStatusDto> {
  return apiFetch<MfaStatusDto>('/auth/mfa/recovery-code', {
    method: 'POST',
    body: JSON.stringify({ recoveryCode }),
  });
}

export async function regenerateRecoveryCodes(): Promise<{ recoveryCodes: string[] }> {
  return apiFetch<{ recoveryCodes: string[] }>('/auth/mfa/recovery-codes/regenerate', {
    method: 'POST',
  });
}

export async function disableMfa(): Promise<void> {
  await apiFetch<void>('/auth/mfa/disable', {
    method: 'POST',
  });
}
