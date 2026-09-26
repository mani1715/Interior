'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  CreditCard,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  RefreshCw,
  FolderKanban,
  HardDrive,
  Sparkles,
  Users,
  BarChart3,
  BadgeCheck,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { getStudioBillingSummary } from '@/lib/billing/api';
import { StudioBillingSummaryDto } from '@/lib/billing/types';

export default function BillingWorkspacePage() {
  const { user } = useAuth();
  const [data, setData] = useState<StudioBillingSummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const activeStudioId = user?.activeStudioId;

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const summary = await getStudioBillingSummary(activeStudioId || undefined);
      setData(summary);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load billing details';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeStudioId]);

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-5xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Workspace Home</span>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Subscription & Capacity
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Billing & Plans
              </h1>
            </div>
          </div>
        </div>

        <button
          onClick={loadData}
          disabled={loading}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-sand-200 text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center gap-3">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-amber-600" />
          <span>{error}</span>
        </div>
      )}

      {loading && !data && (
        <div className="p-12 text-center text-charcoal-500 text-xs">
          Loading billing and plan entitlement details...
        </div>
      )}

      {data && (
        <div className="space-y-8">
          {/* Current Plan Card */}
          <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sand-200 pb-6">
              <div>
                <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-800 text-[11px] font-semibold mb-2">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>Active Operational Access</span>
                </div>
                <h2 className="font-serif text-2xl text-charcoal-900">
                  {data.currentPlan.name}
                </h2>
                <p className="text-xs text-charcoal-500 mt-1 max-w-xl">
                  {data.currentPlan.description ||
                    'Standard studio configuration preserving all core platform features.'}
                </p>
              </div>

              <div className="text-left sm:text-right">
                <span className="text-2xl font-serif font-bold text-charcoal-900">
                  ₹0
                </span>
                <span className="text-xs text-charcoal-500 block">
                  Non-commercial operational plan
                </span>
              </div>
            </div>

            {/* Entitlements Breakdown */}
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-4">
                Effective Platform Entitlements
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <FolderKanban className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      Portfolio Projects
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      {data.effectiveEntitlements.PROJECT_LIMIT != null
                        ? `Limit: ${data.effectiveEntitlements.PROJECT_LIMIT}`
                        : 'Unlimited capacity'}
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <HardDrive className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      Cloud Storage
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      {data.effectiveEntitlements.STORAGE_LIMIT_BYTES != null
                        ? `${Math.round(Number(data.effectiveEntitlements.STORAGE_LIMIT_BYTES) / (1024 * 1024 * 1024))} GB`
                        : 'Unlimited asset storage'}
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <Sparkles className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      AI Visualizer Credits
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      {data.effectiveEntitlements.AI_MONTHLY_CREDITS != null
                        ? `${data.effectiveEntitlements.AI_MONTHLY_CREDITS} / month`
                        : 'Unlimited generations'}
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <Users className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      Client Leads CRM
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      Included with WhatsApp handoff
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <BarChart3 className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      Studio Analytics
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      Full funnel telemetry included
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
                  <BadgeCheck className="w-4 h-4 text-bronze-700 mt-0.5" />
                  <div>
                    <span className="text-xs font-semibold text-charcoal-900 block">
                      Evidence Verification
                    </span>
                    <span className="text-[11px] text-charcoal-500">
                      Independent review (not purchasable)
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Truthful Provider Status Banner */}
          <div className="p-6 rounded-2xl bg-sand-50 border border-sand-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-bronze-700 flex-shrink-0 mt-0.5" />
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">
                  Billing Provider: {data.billingProviderStatus}
                </span>
                <p className="text-[11px] text-charcoal-600 mt-0.5 leading-relaxed max-w-xl">
                  Commercial checkout is not enabled on this platform instance.
                  All features and operational entitlements remain completely active without billing requirement.
                </p>
              </div>
            </div>

            <div className="text-right flex-shrink-0">
              <span className="text-xs font-semibold text-charcoal-500 bg-sand-200 px-3 py-1.5 rounded-lg inline-block">
                No Payment Required
              </span>
            </div>
          </div>

          {/* Transactions History */}
          <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
            <h3 className="font-serif text-lg text-charcoal-900">Billing History</h3>
            {data.recentTransactions.length === 0 ? (
              <div className="p-6 rounded-xl bg-sand-50 border border-sand-200 text-center text-xs text-charcoal-500">
                No billing transactions recorded on your operational account.
              </div>
            ) : (
              <div className="divide-y divide-sand-200">
                {data.recentTransactions.map((tx) => (
                  <div key={tx.id} className="py-3 flex items-center justify-between text-xs">
                    <div>
                      <span className="font-medium text-charcoal-900 block">
                        {tx.description || 'Subscription charge'}
                      </span>
                      <span className="text-[11px] text-charcoal-500">
                        {new Date(tx.occurredAt).toLocaleDateString()} · {tx.status}
                      </span>
                    </div>
                    <div className="font-mono font-semibold text-charcoal-900">
                      ₹{(tx.amountMinor / 100).toFixed(2)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
