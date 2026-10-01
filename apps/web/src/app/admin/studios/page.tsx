'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Building2,
  Search,
  CheckCircle,
  AlertTriangle,
  Ban,
  ShieldCheck,
  RefreshCw,
  Eye,
} from 'lucide-react';
import { fetchAdminStudios, updateStudioStatus } from '@/lib/admin/api';
import { AdminStudioSummary } from '@/lib/admin/types';

export default function AdminStudiosPage() {
  const [studios, setStudios] = useState<AdminStudioSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Suspension Dialog State
  const [selectedStudio, setSelectedStudio] = useState<AdminStudioSummary | null>(null);
  const [targetStatus, setTargetStatus] = useState<'ACTIVE' | 'SUSPENDED'>('SUSPENDED');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadStudios = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminStudios(statusFilter || undefined);
      setStudios(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load studios');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStudios();
  }, [statusFilter]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStudio) return;

    setIsSubmitting(true);
    try {
      await updateStudioStatus(selectedStudio.id, {
        status: targetStatus,
        reason: reason.trim() || undefined,
      });
      setSelectedStudio(null);
      setReason('');
      await loadStudios();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update studio status');
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredStudios = studios.filter((s) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      s.name.toLowerCase().includes(q) ||
      s.slug.toLowerCase().includes(q) ||
      (s.ownerEmail && s.ownerEmail.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">Studio Management</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Inspect studio tenants, verify publications, and manage platform suspension states.
          </p>
        </div>
        <button
          onClick={loadStudios}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Info Notice */}
      <div className="bg-sand-100/70 border border-sand-200 rounded-lg p-3 text-xs text-charcoal-700 flex items-start gap-2.5">
        <Ban className="w-4 h-4 text-charcoal-500 shrink-0 mt-0.5" />
        <div>
          <strong className="font-semibold text-charcoal-900">Suspension Model:</strong> Suspending a studio immediately removes its profile, projects, and media from public discovery via PostgreSQL RLS policies. Tenant data and workspace access remain fully preserved for auditability.
        </div>
      </div>

      {/* Filters Bar */}
      <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="w-4 h-4 text-charcoal-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by name, slug, or owner..."
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          />
        </div>
        <div className="flex items-center gap-2 text-xs">
          <span className="text-charcoal-500 font-medium">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 text-xs focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          >
            <option value="">All States</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Studios Table */}
      <div className="bg-white border border-sand-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-sand-50/50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Studio</th>
                <th className="py-3 px-4">Owner</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Publication</th>
                <th className="py-3 px-4">Verification</th>
                <th className="py-3 px-4">Projects</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sand-100">
              {filteredStudios.length > 0 ? (
                filteredStudios.map((s) => (
                  <tr key={s.id} className="hover:bg-sand-50/30">
                    <td className="py-3 px-4">
                      <div className="font-medium text-charcoal-900">{s.name}</div>
                      <div className="text-[11px] text-charcoal-500 font-mono">/{s.slug}</div>
                    </td>
                    <td className="py-3 px-4 text-charcoal-600">
                      {s.ownerEmail || 'Unknown'}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold ${
                          s.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-red-50 text-red-700'
                        }`}
                      >
                        {s.status}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium ${
                          s.publicationStatus === 'PUBLISHED'
                            ? 'bg-sand-100 text-charcoal-800'
                            : 'bg-sand-50 text-charcoal-500'
                        }`}
                      >
                        {s.publicationStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium ${
                          s.verificationStatus === 'VERIFIED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : s.verificationStatus === 'PENDING'
                            ? 'bg-amber-50 text-amber-700 font-semibold'
                            : 'bg-sand-50 text-charcoal-500'
                        }`}
                      >
                        {s.verificationStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-charcoal-700">
                      {s.publicProjectCount} / {s.projectCount}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => {
                          setSelectedStudio(s);
                          setTargetStatus(s.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE');
                          setReason('');
                        }}
                        className={`px-2.5 py-1 text-xs font-medium rounded border ${
                          s.status === 'ACTIVE'
                            ? 'border-red-200 text-red-700 hover:bg-red-50'
                            : 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                        }`}
                      >
                        {s.status === 'ACTIVE' ? 'Suspend' : 'Unsuspend'}
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-xs text-charcoal-500">
                    {isLoading ? 'Loading studios...' : 'No studios match the criteria.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Suspension Confirmation Modal */}
      {selectedStudio && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-lg border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              {targetStatus === 'SUSPENDED' ? 'Suspend Studio Tenant' : 'Unsuspend Studio Tenant'}
            </h2>
            <p className="text-xs text-charcoal-600 mb-4">
              Updating operational state for <strong>{selectedStudio.name}</strong>.
            </p>

            <form onSubmit={handleUpdateStatus} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason for Audit Log
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Terms violation review, payment fraud check, operator clearance..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setSelectedStudio(null)}
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className={`px-3 py-1.5 rounded-lg text-white font-medium disabled:opacity-50 ${
                    targetStatus === 'SUSPENDED'
                      ? 'bg-red-600 hover:bg-red-700'
                      : 'bg-emerald-600 hover:bg-emerald-700'
                  }`}
                >
                  {isSubmitting
                    ? 'Submitting...'
                    : targetStatus === 'SUSPENDED'
                    ? 'Confirm Suspension'
                    : 'Confirm Unsuspension'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
