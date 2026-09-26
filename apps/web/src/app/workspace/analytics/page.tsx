'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  BarChart3,
  ArrowLeft,
  Eye,
  TrendingUp,
  FolderKanban,
  MessageSquare,
  Sparkles,
  PhoneCall,
  Calendar,
  AlertCircle,
  RefreshCw,
  Award,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { getStudioAnalytics } from '@/lib/analytics/api';
import { StudioAnalyticsSummaryDto } from '@/lib/analytics/types';

export default function AnalyticsWorkspacePage() {
  const { user } = useAuth();
  const [rangeDays, setRangeDays] = useState<number>(30);
  const [data, setData] = useState<StudioAnalyticsSummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const activeStudioId = user?.activeStudioId;

  const loadData = async (days: number) => {
    setLoading(true);
    setError(null);
    try {
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - days);

      const startIso = start.toISOString().split('T')[0];
      const endIso = end.toISOString().split('T')[0];

      const res = await getStudioAnalytics(activeStudioId || undefined, startIso, endIso);
      setData(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load analytics data';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData(rangeDays);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rangeDays, activeStudioId]);

  const hasData =
    data &&
    (data.totalProfileViews > 0 ||
      data.totalProjectViews > 0 ||
      data.totalInquiriesOpened > 0 ||
      data.totalLeadsCreated > 0 ||
      data.totalDiscoveryImpressions > 0);

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-6xl mx-auto space-y-8">
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
              <BarChart3 className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Performance Insights
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Studio Analytics
              </h1>
            </div>
          </div>
        </div>

        {/* Date Range Selector */}
        <div className="flex items-center gap-2 bg-sand-100 p-1.5 rounded-xl border border-sand-200">
          <Calendar className="w-4 h-4 text-charcoal-500 ml-2" />
          {[7, 30, 90].map((days) => (
            <button
              key={days}
              onClick={() => setRangeDays(days)}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all ${
                rangeDays === days
                  ? 'bg-white text-charcoal-900 shadow-sm'
                  : 'text-charcoal-600 hover:text-charcoal-900'
              }`}
            >
              {days === 7 ? 'Last 7 Days' : days === 30 ? 'Last 30 Days' : 'Last 90 Days'}
            </button>
          ))}
          <button
            onClick={() => loadData(rangeDays)}
            disabled={loading}
            className="p-1.5 text-charcoal-500 hover:text-charcoal-900 transition-colors"
            title="Refresh analytics"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center gap-3">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-amber-600" />
          <span>{error}</span>
        </div>
      )}

      {loading && !data && (
        <div className="p-12 text-center text-charcoal-500 text-xs">
          Loading privacy-safe analytics telemetry...
        </div>
      )}

      {data && !hasData && (
        <div className="bg-white border border-sand-200 rounded-2xl p-8 sm:p-12 text-center shadow-sm">
          <div className="w-14 h-14 rounded-2xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto mb-4">
            <BarChart3 className="w-7 h-7" />
          </div>
          <h2 className="font-serif text-xl sm:text-2xl text-charcoal-900 mb-2">
            No Visitor Traffic Recorded Yet
          </h2>
          <p className="text-xs sm:text-sm text-charcoal-600 max-w-md mx-auto leading-relaxed mb-6">
            Live views, discovery impressions, and inquiry funnel conversions will appear here once
            your published portfolio and projects start receiving traffic.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/workspace/portfolio"
              className="px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors"
            >
              Manage Portfolio
            </Link>
            <Link
              href="/workspace/projects"
              className="px-4 py-2 rounded-xl bg-sand-100 text-charcoal-900 text-xs font-medium hover:bg-sand-200 transition-colors"
            >
              Manage Projects
            </Link>
          </div>
        </div>
      )}

      {data && hasData && (
        <div className="space-y-8">
          {/* Key Stat Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl bg-white border border-sand-200 shadow-sm">
              <div className="flex items-center justify-between text-charcoal-500 mb-2">
                <span className="text-xs font-medium">Public Views</span>
                <Eye className="w-4 h-4 text-bronze-700" />
              </div>
              <div className="text-2xl font-serif font-bold text-charcoal-900">
                {data.funnel.publicViews.toLocaleString()}
              </div>
              <div className="text-[11px] text-charcoal-500 mt-1">
                {data.totalProfileViews} profile · {data.totalProjectViews} project
              </div>
            </div>

            <div className="p-5 rounded-2xl bg-white border border-sand-200 shadow-sm">
              <div className="flex items-center justify-between text-charcoal-500 mb-2">
                <span className="text-xs font-medium">Inquiries Opened</span>
                <MessageSquare className="w-4 h-4 text-bronze-700" />
              </div>
              <div className="text-2xl font-serif font-bold text-charcoal-900">
                {data.totalInquiriesOpened.toLocaleString()}
              </div>
              <div className="text-[11px] text-emerald-700 font-medium mt-1">
                {data.funnel.inquiryOpenRatePercent}% open rate
              </div>
            </div>

            <div className="p-5 rounded-2xl bg-white border border-sand-200 shadow-sm">
              <div className="flex items-center justify-between text-charcoal-500 mb-2">
                <span className="text-xs font-medium">Leads Converted</span>
                <TrendingUp className="w-4 h-4 text-bronze-700" />
              </div>
              <div className="text-2xl font-serif font-bold text-charcoal-900">
                {data.totalLeadsCreated.toLocaleString()}
              </div>
              <div className="text-[11px] text-emerald-700 font-medium mt-1">
                {data.funnel.leadConversionRatePercent}% lead conversion
              </div>
            </div>

            <div className="p-5 rounded-2xl bg-white border border-sand-200 shadow-sm">
              <div className="flex items-center justify-between text-charcoal-500 mb-2">
                <span className="text-xs font-medium">Projects Won</span>
                <Award className="w-4 h-4 text-bronze-700" />
              </div>
              <div className="text-2xl font-serif font-bold text-charcoal-900">
                {data.totalLeadsWon.toLocaleString()}
              </div>
              <div className="text-[11px] text-emerald-700 font-medium mt-1">
                {data.funnel.leadWinRatePercent}% win rate
              </div>
            </div>
          </div>

          {/* Secondary Telemetry Strip */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] text-charcoal-500 block">Discovery Impressions</span>
                <span className="text-base font-semibold text-charcoal-900">
                  {data.totalDiscoveryImpressions.toLocaleString()}
                </span>
              </div>
            </div>
            <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] text-charcoal-500 block">Discovery Clicks</span>
                <span className="text-base font-semibold text-charcoal-900">
                  {data.totalDiscoveryClicks.toLocaleString()}
                </span>
              </div>
            </div>
            <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] text-charcoal-500 block">AI Visualizations</span>
                <span className="text-base font-semibold text-charcoal-900">
                  {data.totalAiGenerations.toLocaleString()}
                </span>
              </div>
              <Sparkles className="w-4 h-4 text-bronze-600" />
            </div>
            <div className="p-3.5 rounded-xl bg-sand-50 border border-sand-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] text-charcoal-500 block">WhatsApp Handoffs</span>
                <span className="text-base font-semibold text-charcoal-900">
                  {data.totalWhatsappHandoffs.toLocaleString()}
                </span>
              </div>
              <PhoneCall className="w-4 h-4 text-emerald-600" />
            </div>
          </div>

          {/* Conversion Funnel */}
          <div className="p-6 rounded-2xl bg-white border border-sand-200 shadow-sm space-y-4">
            <h3 className="font-serif text-lg text-charcoal-900">Client Acquisition Funnel</h3>
            <p className="text-xs text-charcoal-500">
              Deterministic, non-duplicating conversion pipeline from initial visitor view to signed project agreement.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 pt-2">
              <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-center">
                <span className="text-xs text-charcoal-500 uppercase tracking-wider block mb-1">
                  1. Public Views
                </span>
                <div className="text-xl font-serif font-bold text-charcoal-900">
                  {data.funnel.publicViews}
                </div>
              </div>
              <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-center">
                <span className="text-xs text-charcoal-500 uppercase tracking-wider block mb-1">
                  2. Inquiries Opened
                </span>
                <div className="text-xl font-serif font-bold text-charcoal-900">
                  {data.funnel.inquiriesOpened}
                </div>
                <span className="text-[11px] text-emerald-700 font-medium">
                  {data.funnel.inquiryOpenRatePercent}% of views
                </span>
              </div>
              <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-center">
                <span className="text-xs text-charcoal-500 uppercase tracking-wider block mb-1">
                  3. Leads Created
                </span>
                <div className="text-xl font-serif font-bold text-charcoal-900">
                  {data.funnel.leadsCreated}
                </div>
                <span className="text-[11px] text-emerald-700 font-medium">
                  {data.funnel.leadConversionRatePercent}% of inquiries
                </span>
              </div>
              <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-center">
                <span className="text-xs text-charcoal-500 uppercase tracking-wider block mb-1">
                  4. Projects Won
                </span>
                <div className="text-xl font-serif font-bold text-charcoal-900">
                  {data.funnel.leadsWon}
                </div>
                <span className="text-[11px] text-emerald-700 font-medium">
                  {data.funnel.leadWinRatePercent}% win rate
                </span>
              </div>
            </div>
          </div>

          {/* Top Performing Projects */}
          {data.topProjects && data.topProjects.length > 0 && (
            <div className="p-6 rounded-2xl bg-white border border-sand-200 shadow-sm space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-serif text-lg text-charcoal-900">Top Performing Projects</h3>
                <span className="text-xs text-charcoal-500">Ordered by public project views</span>
              </div>
              <div className="divide-y divide-sand-200">
                {data.topProjects.map((p) => (
                  <div key={p.projectId} className="py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-lg bg-sand-100 text-charcoal-600 flex items-center justify-center">
                        <FolderKanban className="w-4 h-4 text-bronze-700" />
                      </div>
                      <div>
                        <span className="text-xs font-semibold text-charcoal-900 block">
                          {p.title}
                        </span>
                        <span className="text-[11px] text-charcoal-500">/{p.slug}</span>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-bold text-charcoal-900 block">
                        {p.viewCount} views
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
