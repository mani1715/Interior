export type SubscriptionStatus =
  | 'INACTIVE'
  | 'PENDING'
  | 'ACTIVE'
  | 'PAST_DUE'
  | 'CANCEL_AT_PERIOD_END'
  | 'CANCELLED'
  | 'EXPIRED';

export type BillingTransactionStatus =
  | 'PENDING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'REFUNDED';

export interface BillingPlanRecord {
  id: string;
  code: string;
  name: string;
  description?: string;
  billingPeriod?: string;
  currency: string;
  priceMinor: number;
  active: boolean;
  purchasable: boolean;
  displayOrder: number;
  providerPriceId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BillingPlanDto {
  id: string;
  code: string;
  name: string;
  description?: string;
  billingPeriod?: string;
  currency: string;
  priceMinor: number;
  active: boolean;
  purchasable: boolean;
  displayOrder: number;
  entitlements: Record<string, unknown>;
}

export interface StudioSubscriptionRecord {
  id: string;
  studioId: string;
  planId: string;
  status: SubscriptionStatus;
  provider: string;
  providerCustomerId?: string;
  providerSubscriptionId?: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd: boolean;
  cancelledAt?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface BillingTransactionRecord {
  id: string;
  studioId: string;
  subscriptionId?: string;
  provider: string;
  providerPaymentId?: string;
  providerOrderId?: string;
  amountMinor: number;
  currency: string;
  status: BillingTransactionStatus;
  description?: string;
  receiptUrl?: string;
  occurredAt: string;
  createdAt: string;
}

export interface StudioBillingSummaryDto {
  studioId: string;
  currentPlan: BillingPlanRecord;
  activeSubscription?: StudioSubscriptionRecord | null;
  effectiveEntitlements: Record<string, unknown>;
  billingProviderStatus: string;
  commercialCheckoutEnabled: boolean;
  availablePlans: BillingPlanDto[];
  recentTransactions: BillingTransactionRecord[];
}

export interface CreateCheckoutRequest {
  planCode: string;
  successUrl?: string;
  cancelUrl?: string;
}

export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
  provider: string;
  planCode: string;
  amountMinor: number;
  currency: string;
}
