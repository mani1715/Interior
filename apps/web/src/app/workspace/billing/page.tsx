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
  Film,
  Camera,
  Layers,
  ArrowUpRight,
  Info,
  X,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { getStudioBillingSummary } from '@/lib/billing/api';
import { StudioBillingSummaryDto, BillingPlanDto } from '@/lib/billing/types';

export default function BillingWorkspacePage() {
  const { user } = useAuth();
  const [data, setData] = useState<StudioBillingSummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlanForReview, setSelectedPlanForReview] = useState<BillingPlanDto | null>(null);

  const activeStudioId = user?.activeStudioId;

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const summary = await getStudioBillingSummary(activeStudioId || undefined);
      setData(summary);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load plan details';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeStudioId]);

  const isLegacyBase = data?.currentPlan?.code === 'BASE';

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
                Subscription & Quota
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Plan & Usage
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
          Loading plan entitlements and capacity usage...
        </div>
      )}

      {data && (
        <div className="space-y-8">
          {/* Current Active Plan Card */}
          <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sand-200 pb-6">
              <div>
                <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-800 text-[11px] font-semibold mb-2">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>
                    {isLegacyBase ? 'Existing Studio Access' : 'Active Plan'}
                  </span>
                </div>
                <h2 className="font-serif text-2xl sm:text-3xl text-charcoal-900">
                  {isLegacyBase ? 'Existing Studio Access' : data.currentPlan.name}
                </h2>
                <p className="text-xs text-charcoal-500 mt-1 max-w-xl">
                  {isLegacyBase
                    ? 'Grandfathered studio access preserving your existing portfolio and operational capacity.'
                    : (data.currentPlan.description || 'Active studio plan configuration.')}
                </p>
              </div>

              <div className="text-left sm:text-right">
                <span className="text-xs font-semibold px-2.5 py-1 rounded-md bg-sand-100 text-charcoal-700 inline-block uppercase tracking-wider">
                  Tier: {data.currentPlan.code}
                </span>
                <span className="text-xs text-charcoal-500 block mt-1">
                  Pricing structure locked · Checkout not required
                </span>
              </div>
            </div>

            {/* Quota & Usage Progress Breakdown */}
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-4">
                Capacity & Usage Breakdown
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {/* 1. Projects Quota */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <FolderKanban className="w-4 h-4 text-bronze-700" />
                      <span>Portfolio Projects</span>
                    </div>
                    <span className="text-xs font-semibold text-charcoal-900">
                      {data.usage?.projectCount ?? 0}
                      {data.effectiveEntitlements.PROJECT_LIMIT != null
                        ? ` / ${data.effectiveEntitlements.PROJECT_LIMIT}`
                        : ' (Unlimited)'}
                    </span>
                  </div>
                  {data.effectiveEntitlements.PROJECT_LIMIT != null && (
                    <div className="w-full bg-sand-200 h-1.5 rounded-full overflow-hidden">
                      <div
                        className="bg-bronze-600 h-full rounded-full transition-all"
                        style={{
                          width: `${Math.min(
                            100,
                            ((data.usage?.projectCount ?? 0) /
                              Number(data.effectiveEntitlements.PROJECT_LIMIT)) *
                              100
                          )}%`,
                        }}
                      />
                    </div>
                  )}
                  <span className="text-[11px] text-charcoal-500 block">
                    All retained projects (including drafts & archived)
                  </span>
                </div>

                {/* 2. Photo Quota per Project */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <Camera className="w-4 h-4 text-bronze-700" />
                      <span>Photos / Project</span>
                    </div>
                    <span className="text-xs font-semibold text-charcoal-900">
                      {data.effectiveEntitlements.PROJECT_PHOTO_LIMIT != null
                        ? `Max ${data.effectiveEntitlements.PROJECT_PHOTO_LIMIT} photos`
                        : 'Unlimited'}
                    </span>
                  </div>
                  <span className="text-[11px] text-charcoal-500 block pt-1">
                    Counts committed portfolio photographs and pending upload slots
                  </span>
                </div>

                {/* 3. Cinematic Allocations */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <Film className="w-4 h-4 text-bronze-700" />
                      <span>Cinematic Presentations</span>
                    </div>
                    <span className="text-xs font-semibold text-charcoal-900">
                      {Boolean(data.effectiveEntitlements.CINEMATIC_PORTFOLIO)
                        ? `${data.usage?.cinematicProjectCount ?? 0} / ${
                            data.effectiveEntitlements.CINEMATIC_PROJECT_LIMIT ?? 'Unlimited'
                          }`
                        : 'Not Included'}
                    </span>
                  </div>
                  {Boolean(data.effectiveEntitlements.CINEMATIC_PORTFOLIO) &&
                    data.effectiveEntitlements.CINEMATIC_PROJECT_LIMIT != null && (
                      <div className="w-full bg-sand-200 h-1.5 rounded-full overflow-hidden">
                        <div
                          className="bg-amber-600 h-full rounded-full transition-all"
                          style={{
                            width: `${Math.min(
                              100,
                              ((data.usage?.cinematicProjectCount ?? 0) /
                                Number(data.effectiveEntitlements.CINEMATIC_PROJECT_LIMIT)) *
                                100
                            )}%`,
                          }}
                        />
                      </div>
                    )}
                  <span className="text-[11px] text-charcoal-500 block">
                    {Boolean(data.effectiveEntitlements.CINEMATIC_PORTFOLIO)
                      ? 'Allocated within your total project limit'
                      : 'Available exclusively on Pro plan'}
                  </span>
                </div>

                {/* 4. Asset Storage Quota */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <HardDrive className="w-4 h-4 text-bronze-700" />
                      <span>Storage Quota</span>
                    </div>
                    <span className="text-xs font-semibold text-charcoal-900">
                      {data.effectiveEntitlements.STORAGE_LIMIT_BYTES != null
                        ? `${Math.round(
                            Number(data.effectiveEntitlements.STORAGE_LIMIT_BYTES) /
                              (1024 * 1024 * 1024)
                          )} GB`
                        : 'Unlimited'}
                    </span>
                  </div>
                  <span className="text-[11px] text-charcoal-500 block pt-1">
                    Independent quota for originals, room photos & derivatives
                  </span>
                </div>

                {/* 5. AI Concept Generations */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <Sparkles className="w-4 h-4 text-bronze-700" />
                      <span>AI Visualizer Monthly</span>
                    </div>
                    <span className="text-xs font-semibold text-charcoal-900">
                      {data.effectiveEntitlements.AI_MONTHLY_CREDITS != null
                        ? `${data.effectiveEntitlements.AI_MONTHLY_CREDITS} credits`
                        : 'Unlimited'}
                    </span>
                  </div>
                  <span className="text-[11px] text-charcoal-500 block pt-1">
                    Monthly quota separate from portfolio project counts
                  </span>
                </div>

                {/* 6. Lead CRM & Inquiries */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-charcoal-800 font-medium text-xs">
                      <Users className="w-4 h-4 text-bronze-700" />
                      <span>Inquiry Management</span>
                    </div>
                    <span className="text-xs font-semibold text-emerald-800">
                      Included
                    </span>
                  </div>
                  <span className="text-[11px] text-charcoal-500 block pt-1">
                    WhatsApp direct handoff and lead inbox included across plans
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Customer-Facing Plans Comparison */}
          <div className="space-y-4">
            <div>
              <h3 className="font-serif text-xl text-charcoal-900">
                Available Studio Plans
              </h3>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Review platform tiers and capacity envelopes designed for professional studios.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Plan 1: STANDARD */}
              <div
                className={`bg-white border rounded-2xl p-6 shadow-sm flex flex-col justify-between space-y-6 ${
                  data.currentPlan.code === 'STANDARD'
                    ? 'border-bronze-500 ring-1 ring-bronze-500'
                    : 'border-sand-200'
                }`}
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-500">
                      Tier 1
                    </span>
                    {data.currentPlan.code === 'STANDARD' && (
                      <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800">
                        Current
                      </span>
                    )}
                  </div>
                  <h4 className="font-serif text-2xl text-charcoal-900">Standard</h4>
                  <p className="text-xs text-charcoal-600 leading-relaxed">
                    Essential portfolio presence for independent interior designers and boutique studios.
                  </p>

                  <div className="border-t border-sand-200 pt-4 space-y-2.5 text-xs text-charcoal-700">
                    <div className="flex items-center gap-2">
                      <FolderKanban className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>10</strong> Projects total</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Camera className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>15</strong> Photos per project</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Layers className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span>Standard Presentation only</span>
                    </div>
                    <div className="flex items-center gap-2 text-charcoal-400">
                      <Film className="w-3.5 h-3.5 text-charcoal-300 flex-shrink-0" />
                      <span>0 Cinematic allocations</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => {
                    const plan = data.availablePlans.find((p) => p.code === 'STANDARD');
                    if (plan) setSelectedPlanForReview(plan);
                  }}
                  disabled={data.currentPlan.code === 'STANDARD'}
                  className={`w-full py-2.5 px-4 rounded-xl text-xs font-semibold transition-colors ${
                    data.currentPlan.code === 'STANDARD'
                      ? 'bg-sand-100 text-charcoal-400 cursor-default'
                      : 'bg-sand-100 text-charcoal-800 hover:bg-sand-200'
                  }`}
                >
                  {data.currentPlan.code === 'STANDARD' ? 'Current Plan' : 'Review Standard'}
                </button>
              </div>

              {/* Plan 2: PREMIUM */}
              <div
                className={`bg-white border rounded-2xl p-6 shadow-sm flex flex-col justify-between space-y-6 ${
                  data.currentPlan.code === 'PREMIUM'
                    ? 'border-bronze-500 ring-1 ring-bronze-500'
                    : 'border-sand-200'
                }`}
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-500">
                      Tier 2
                    </span>
                    {data.currentPlan.code === 'PREMIUM' && (
                      <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800">
                        Current
                      </span>
                    )}
                  </div>
                  <h4 className="font-serif text-2xl text-charcoal-900">Premium</h4>
                  <p className="text-xs text-charcoal-600 leading-relaxed">
                    Expanded project and media capacity for established and growing design practices.
                  </p>

                  <div className="border-t border-sand-200 pt-4 space-y-2.5 text-xs text-charcoal-700">
                    <div className="flex items-center gap-2">
                      <FolderKanban className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>20</strong> Projects total</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Camera className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>25</strong> Photos per project</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Layers className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span>Standard Presentation only</span>
                    </div>
                    <div className="flex items-center gap-2 text-charcoal-400">
                      <Film className="w-3.5 h-3.5 text-charcoal-300 flex-shrink-0" />
                      <span>0 Cinematic allocations</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => {
                    const plan = data.availablePlans.find((p) => p.code === 'PREMIUM');
                    if (plan) setSelectedPlanForReview(plan);
                  }}
                  disabled={data.currentPlan.code === 'PREMIUM'}
                  className={`w-full py-2.5 px-4 rounded-xl text-xs font-semibold transition-colors ${
                    data.currentPlan.code === 'PREMIUM'
                      ? 'bg-sand-100 text-charcoal-400 cursor-default'
                      : 'bg-sand-100 text-charcoal-800 hover:bg-sand-200'
                  }`}
                >
                  {data.currentPlan.code === 'PREMIUM' ? 'Current Plan' : 'Review Premium'}
                </button>
              </div>

              {/* Plan 3: PRO */}
              <div
                className={`bg-white border rounded-2xl p-6 shadow-sm flex flex-col justify-between space-y-6 relative overflow-hidden ${
                  data.currentPlan.code === 'PRO'
                    ? 'border-bronze-600 ring-2 ring-bronze-600'
                    : 'border-sand-300'
                }`}
              >
                <div className="absolute top-0 right-0 bg-bronze-700 text-sand-50 text-[10px] font-bold px-3 py-1 rounded-bl-lg uppercase tracking-wider">
                  Cinematic Enabled
                </div>

                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-500">
                      Tier 3
                    </span>
                    {data.currentPlan.code === 'PRO' && (
                      <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800">
                        Current
                      </span>
                    )}
                  </div>
                  <h4 className="font-serif text-2xl text-charcoal-900">Pro</h4>
                  <p className="text-xs text-charcoal-600 leading-relaxed">
                    Maximum capacity with cinematic project presentations for distinguished studios.
                  </p>

                  <div className="border-t border-sand-200 pt-4 space-y-2.5 text-xs text-charcoal-700">
                    <div className="flex items-center gap-2">
                      <FolderKanban className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>20</strong> Projects total</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Camera className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>30</strong> Photos per project</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Layers className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span>Standard + Cinematic Presentation</span>
                    </div>
                    <div className="flex items-center gap-2 text-bronze-800 font-medium">
                      <Film className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                      <span><strong>5</strong> Cinematic allocations (within 20 total)</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => {
                    const plan = data.availablePlans.find((p) => p.code === 'PRO');
                    if (plan) setSelectedPlanForReview(plan);
                  }}
                  disabled={data.currentPlan.code === 'PRO'}
                  className={`w-full py-2.5 px-4 rounded-xl text-xs font-semibold transition-colors ${
                    data.currentPlan.code === 'PRO'
                      ? 'bg-sand-100 text-charcoal-400 cursor-default'
                      : 'bg-bronze-700 text-white hover:bg-bronze-800'
                  }`}
                >
                  {data.currentPlan.code === 'PRO' ? 'Current Plan' : 'Review Pro'}
                </button>
              </div>
            </div>
          </div>

          {/* Truthful Provider Status Banner */}
          <div className="p-6 rounded-2xl bg-sand-50 border border-sand-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-bronze-700 flex-shrink-0 mt-0.5" />
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">
                  Commercial Checkout Gateway: Not Active
                </span>
                <p className="text-[11px] text-charcoal-600 mt-0.5 leading-relaxed max-w-xl">
                  Live payment checkout is currently disabled on this instance. Plan purchases and billing transitions are not yet available. All platform features and existing assets remain safe with zero data loss.
                </p>
              </div>
            </div>

            <div className="text-right flex-shrink-0">
              <span className="text-xs font-semibold text-charcoal-500 bg-sand-200 px-3 py-1.5 rounded-lg inline-block">
                No Payment Required
              </span>
            </div>
          </div>

          {/* Plan Change Review Modal */}
          {selectedPlanForReview && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-charcoal-950/60 backdrop-blur-sm">
              <div className="bg-white rounded-2xl max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl relative border border-sand-200">
                <button
                  onClick={() => setSelectedPlanForReview(null)}
                  className="absolute top-5 right-5 text-charcoal-400 hover:text-charcoal-700 p-1"
                >
                  <X className="w-5 h-5" />
                </button>

                <div className="space-y-2">
                  <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                    Plan Review
                  </span>
                  <h3 className="font-serif text-2xl text-charcoal-900">
                    Switch to {selectedPlanForReview.name}
                  </h3>
                  <p className="text-xs text-charcoal-600">
                    {selectedPlanForReview.description}
                  </p>
                </div>

                {/* Plan differences review */}
                <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-3 text-xs">
                  <div className="flex items-center justify-between py-1 border-b border-sand-200">
                    <span className="text-charcoal-600">Total Project Capacity</span>
                    <span className="font-semibold text-charcoal-900">
                      {String(selectedPlanForReview.entitlements.PROJECT_LIMIT ?? 'Unlimited')}
                    </span>
                  </div>
                  <div className="flex items-center justify-between py-1 border-b border-sand-200">
                    <span className="text-charcoal-600">Photos per Project</span>
                    <span className="font-semibold text-charcoal-900">
                      {String(selectedPlanForReview.entitlements.PROJECT_PHOTO_LIMIT ?? 'Unlimited')}
                    </span>
                  </div>
                  <div className="flex items-center justify-between py-1 border-b border-sand-200">
                    <span className="text-charcoal-600">Cinematic Portfolio</span>
                    <span className="font-semibold text-charcoal-900">
                      {Boolean(selectedPlanForReview.entitlements.CINEMATIC_PORTFOLIO)
                        ? `Included (Max ${selectedPlanForReview.entitlements.CINEMATIC_PROJECT_LIMIT})`
                        : 'Standard only'}
                    </span>
                  </div>
                </div>

                {/* Over-quota / Downgrade safety explanation */}
                <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-3">
                  <Info className="w-4 h-4 flex-shrink-0 text-amber-700 mt-0.5" />
                  <div className="space-y-1">
                    <span className="font-semibold block">Safe Capacity Policy</span>
                    <p className="leading-relaxed text-[11px] text-amber-800">
                      Downgrading or switching plans never deletes, unpublishes, or modifies your existing projects and photographs. Existing assets remain fully preserved and publicly viewable.
                    </p>
                  </div>
                </div>

                {/* Truthful Checkout Termination Message */}
                <div className="p-4 rounded-xl bg-sand-100 text-center space-y-1">
                  <span className="text-xs font-semibold text-charcoal-800 block">
                    Plan purchases are not available yet.
                  </span>
                  <span className="text-[11px] text-charcoal-500 block">
                    Pricing and commercial payment processing have not been activated on this platform.
                  </span>
                </div>

                <div className="flex justify-end">
                  <button
                    onClick={() => setSelectedPlanForReview(null)}
                    className="px-5 py-2.5 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors"
                  >
                    Close Review
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
