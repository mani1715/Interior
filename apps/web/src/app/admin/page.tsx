'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Building2,
  Users,
  FolderKanban,
  Mail,
  ShieldCheck,
  AlertTriangle,
  Image as ImageIcon,
  Sparkles,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';
import { fetchAdminDashboard } from '@/lib/admin/api';
import { AdminDashboardMetrics } from '@/lib/admin/types';

export default function AdminDashboardPage() {
  const [metrics, setMetrics] = useState<AdminDashboardMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminDashboard();
      setMetrics(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load operational metrics');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-serif text-charcoal-900 tracking-tight">
            Platform Operations Dashboard
          </h1>
          <p className="text-sm text-charcoal-600 mt-1">
            Aggregated real-time metrics across tenants, verifications, and content moderation.
          </p>
        </div>
        <button
          onClick={loadData}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 transition-colors shrink-0"
          aria-label="Refresh operational metrics"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
          <span>{error}</span>
          <button onClick={loadData} className="font-medium underline hover:text-red-800">
            Retry
          </button>
        </div>
      )}

      {/* Metrics Grid */}
      <section aria-labelledby="platform-metrics-heading">
        <h2 id="platform-metrics-heading" className="sr-only">
          Platform Summary Metrics
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Studios */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between text-charcoal-500 mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Studios</span>
              <Building2 className="w-4 h-4 text-bronze-600" />
            </div>
            <div className="text-3xl font-serif text-charcoal-900 font-medium">
              {metrics ? metrics.activeStudios : '—'}
            </div>
            <div className="text-xs text-charcoal-500 mt-2 flex items-center gap-1.5">
              <span>{metrics?.totalStudios ?? 0} total</span>
              <span>•</span>
              <span className="text-emerald-700 font-medium">
                {metrics?.publishedStudios ?? 0} published
              </span>
              {Boolean(metrics?.suspendedStudios) && (
                <>
                  <span>•</span>
                  <span className="text-red-600">{metrics?.suspendedStudios} suspended</span>
                </>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-sand-100">
              <Link
                href="/admin/studios"
                className="text-xs text-bronze-700 font-medium hover:text-bronze-800 flex items-center gap-1"
              >
                <span>Manage Studios</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          </div>

          {/* Users */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between text-charcoal-500 mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Users</span>
              <Users className="w-4 h-4 text-bronze-600" />
            </div>
            <div className="text-3xl font-serif text-charcoal-900 font-medium">
              {metrics ? metrics.activeUsers : '—'}
            </div>
            <div className="text-xs text-charcoal-500 mt-2 flex items-center gap-1.5">
              <span>{metrics?.totalUsers ?? 0} registered</span>
              {Boolean(metrics?.suspendedUsers) && (
                <>
                  <span>•</span>
                  <span className="text-red-600">{metrics?.suspendedUsers} suspended</span>
                </>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-sand-100">
              <Link
                href="/admin/users"
                className="text-xs text-bronze-700 font-medium hover:text-bronze-800 flex items-center gap-1"
              >
                <span>Manage Users</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          </div>

          {/* Pending Verifications */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between text-charcoal-500 mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">
                Verifications
              </span>
              <ShieldCheck className="w-4 h-4 text-bronze-600" />
            </div>
            <div className="text-3xl font-serif text-charcoal-900 font-medium">
              {metrics ? metrics.pendingVerifications : '—'}
            </div>
            <div className="text-xs text-charcoal-500 mt-2">
              {metrics?.pendingVerifications ? (
                <span className="text-amber-700 font-medium">Requests awaiting review</span>
              ) : (
                <span>No pending reviews</span>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-sand-100">
              <Link
                href="/admin/verification"
                className="text-xs text-bronze-700 font-medium hover:text-bronze-800 flex items-center gap-1"
              >
                <span>Review Requests</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          </div>

          {/* Review Moderation */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between text-charcoal-500 mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Reports</span>
              <AlertTriangle className="w-4 h-4 text-amber-600" />
            </div>
            <div className="text-3xl font-serif text-charcoal-900 font-medium">
              {metrics ? metrics.pendingReviewReports : '—'}
            </div>
            <div className="text-xs text-charcoal-500 mt-2">
              {metrics?.pendingReviewReports ? (
                <span className="text-red-600 font-medium">Review flags requiring action</span>
              ) : (
                <span>Queue is clear</span>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-sand-100">
              <Link
                href="/admin/reviews"
                className="text-xs text-bronze-700 font-medium hover:text-bronze-800 flex items-center gap-1"
              >
                <span>Review Moderation</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          </div>
        </div>

        {/* Secondary Operational Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-4">
          <div className="bg-white/80 border border-sand-200 rounded-lg p-3.5">
            <div className="text-xs text-charcoal-500 flex items-center gap-1.5">
              <FolderKanban className="w-3.5 h-3.5 text-charcoal-400" />
              <span>Projects</span>
            </div>
            <div className="text-lg font-semibold text-charcoal-900 mt-1">
              {metrics ? `${metrics.publicProjects} / ${metrics.totalProjects}` : '—'}
            </div>
            <div className="text-[11px] text-charcoal-500">public / total</div>
          </div>

          <div className="bg-white/80 border border-sand-200 rounded-lg p-3.5">
            <div className="text-xs text-charcoal-500 flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5 text-charcoal-400" />
              <span>Leads</span>
            </div>
            <div className="text-lg font-semibold text-charcoal-900 mt-1">
              {metrics ? metrics.totalLeads : '—'}
            </div>
            <div className="text-[11px] text-charcoal-500">inquiries logged</div>
          </div>

          <div className="bg-white/80 border border-sand-200 rounded-lg p-3.5">
            <div className="text-xs text-charcoal-500 flex items-center gap-1.5">
              <ImageIcon className="w-3.5 h-3.5 text-charcoal-400" />
              <span>Media</span>
            </div>
            <div className="text-lg font-semibold text-charcoal-900 mt-1">
              {metrics ? metrics.totalMediaAssets : '—'}
            </div>
            <div className="text-[11px] text-charcoal-500">assets stored</div>
          </div>

          <div className="bg-white/80 border border-sand-200 rounded-lg p-3.5">
            <div className="text-xs text-charcoal-500 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-charcoal-400" />
              <span>AI Jobs</span>
            </div>
            <div className="text-lg font-semibold text-charcoal-900 mt-1">
              {metrics ? metrics.totalAiGenerations : '—'}
            </div>
            <div className="text-[11px] text-charcoal-500">generation requests</div>
          </div>
        </div>
      </section>

      {/* Recent Activity Table */}
      <section aria-labelledby="recent-activity-heading" className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 id="recent-activity-heading" className="text-base font-serif text-charcoal-900">
              Recent System Activity
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Latest administrative and security audit events
            </p>
          </div>
          <Link
            href="/admin/audit"
            className="text-xs text-bronze-700 hover:text-bronze-800 font-medium flex items-center gap-1"
          >
            <span>View All Logs</span>
            <ArrowRight className="w-3 h-3" />
          </Link>
        </div>

        {metrics?.recentActivity && metrics.recentActivity.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-sand-100 text-charcoal-500">
                  <th className="py-2.5 px-3 font-semibold uppercase tracking-wider">Timestamp</th>
                  <th className="py-2.5 px-3 font-semibold uppercase tracking-wider">Action</th>
                  <th className="py-2.5 px-3 font-semibold uppercase tracking-wider">Resource</th>
                  <th className="py-2.5 px-3 font-semibold uppercase tracking-wider">Actor</th>
                  <th className="py-2.5 px-3 font-semibold uppercase tracking-wider">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sand-50">
                {metrics.recentActivity.map((log) => (
                  <tr key={log.id} className="hover:bg-sand-50/50">
                    <td className="py-2.5 px-3 text-charcoal-500 whitespace-nowrap font-mono text-[11px]">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="py-2.5 px-3">
                      <span className="inline-block px-2 py-0.5 rounded text-[10px] font-semibold bg-sand-100 text-charcoal-800">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-charcoal-700 whitespace-nowrap">
                      {log.resourceType}
                    </td>
                    <td className="py-2.5 px-3 text-charcoal-600 truncate max-w-[150px]">
                      {log.actorEmail || log.actorId || 'System'}
                    </td>
                    <td className="py-2.5 px-3 text-charcoal-500 truncate max-w-xs font-mono text-[11px]">
                      {log.details || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-8 text-center text-xs text-charcoal-500 bg-sand-50/50 rounded-lg">
            No recent audit events recorded.
          </div>
        )}
      </section>
    </div>
  );
}
