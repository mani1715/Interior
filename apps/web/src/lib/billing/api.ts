import { apiFetch } from '../api-client';
import {
  BillingTransactionRecord,
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

/**
 * Schedules a plan downgrade at the end of the billing period without data deletion.
 */
export async function requestDowngrade(targetPlanCode: string, studioId?: string): Promise<void> {
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }
  return apiFetch<void>('/studio/billing/downgrade', {
    method: 'POST',
    headers,
    body: JSON.stringify({ targetPlanCode }),
  });
}

/**
 * Retrieves bounded, paginated billing transaction history for a studio.
 */
export async function listBillingTransactions(
  limit = 20,
  offset = 0,
  studioId?: string
): Promise<BillingTransactionRecord[]> {
  const headers: Record<string, string> = {};
  if (studioId) {
    headers['X-Studio-Id'] = studioId;
  }
  return apiFetch<BillingTransactionRecord[]>(
    `/studio/billing/transactions?limit=${limit}&offset=${offset}`,
    {
      headers,
    }
  );
}
