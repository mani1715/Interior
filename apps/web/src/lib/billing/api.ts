import { apiFetch } from '../api-client';
import {
  CheckoutSessionResponse,
  CreateCheckoutRequest,
  StudioBillingSummaryDto,
} from './types';

/**
 * Retrieves studio billing summary, effective entitlements, and provider configuration status.
 */
export async function getStudioBillingSummary(studioId?: string): Promise<StudioBillingSummaryDto> {
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }
  return apiFetch<StudioBillingSummaryDto>('/studio/billing', {
    headers,
  });
}

/**
 * Initiates hosted checkout session for a purchasable plan.
 */
export async function initiateCheckout(
  data: CreateCheckoutRequest,
  studioId?: string
): Promise<CheckoutSessionResponse> {
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }
  return apiFetch<CheckoutSessionResponse>('/studio/billing/checkout', {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
  });
}

/**
 * Schedules subscription cancellation at current period end without data deletion.
 */
export async function cancelSubscription(studioId?: string): Promise<void> {
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }
  return apiFetch<void>('/studio/billing/cancel', {
    method: 'POST',
    headers,
  });
}
