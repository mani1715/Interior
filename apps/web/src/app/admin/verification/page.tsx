'use client';

import React, { useEffect, useState } from 'react';
import {
  ShieldCheck,
  Search,
  CheckCircle,
  XCircle,
  HelpCircle,
  RefreshCw,
  FileText,
  AlertCircle,
} from 'lucide-react';
import { fetchAdminVerifications, recordVerificationDecision } from '@/lib/admin/api';
import { AdminVerificationDecisionRequest, AdminVerificationSummary } from '@/lib/admin/types';

export default function AdminVerificationPage() {
  const [verifications, setVerifications] = useState<AdminVerificationSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [searchQuery, setSearchQuery] = useState('');

  // Decision Modal State
  const [selectedReq, setSelectedReq] = useState<AdminVerificationSummary | null>(null);
  const [decisionStatus, setDecisionStatus] =
    useState<AdminVerificationDecisionRequest['status']>('VERIFIED');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminVerifications(statusFilter || undefined);
      setVerifications(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load verification requests');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [statusFilter]);

  const handleSubmitDecision = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReq) return;

    setIsSubmitting(true);
    try {
      await recordVerificationDecision(selectedReq.studioId, {
        status: decisionStatus,
        reason: reason.trim() || undefined,
      });
      setSelectedReq(null);
      setReason('');
      await loadData();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to submit verification decision');
    } finally {
      setIsSubmitting(false);
    }
  };

  const filtered = verifications.filter((v) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      v.studioName.toLowerCase().includes(q) ||
      v.businessName.toLowerCase().includes(q) ||
      (v.registrationNumber && v.registrationNumber.toLowerCase().includes(q)) ||
      (v.gstNumber && v.gstNumber.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">Verification Reviews</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Review professional business credentials, inspect evidence metadata, and issue verification decisions.
          </p>
        </div>
        <button
          onClick={loadData}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filters Bar */}
      <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="w-4 h-4 text-charcoal-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by studio or business name..."
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
            <option value="">All Statuses</option>
            <option value="PENDING">Pending Review</option>
            <option value="NEEDS_MORE_INFO">Needs More Info</option>
            <option value="VERIFIED">Verified</option>
            <option value="REJECTED">Rejected</option>
            <option value="REVERIFY_REQUIRED">Re-verify Required</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Verification Table */}
      <div className="bg-white border border-sand-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-sand-50/50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Studio / Business</th>
                <th className="py-3 px-4">Professional Type</th>
                <th className="py-3 px-4">Registration / GST</th>
                <th className="py-3 px-4">Evidence</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Submitted</th>
                <th className="py-3 px-4 text-right">Decision</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sand-100">
              {filtered.length > 0 ? (
                filtered.map((v) => (
                  <tr key={v.id} className="hover:bg-sand-50/30">
                    <td className="py-3 px-4">
                      <div className="font-medium text-charcoal-900">{v.businessName}</div>
                      <div className="text-[11px] text-charcoal-500 font-mono">
                        Studio: {v.studioName}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-charcoal-700">{v.professionalType}</td>
                    <td className="py-3 px-4 text-charcoal-600 font-mono text-[11px]">
                      <div>Reg: {v.registrationNumber || '—'}</div>
                      <div>GST: {v.gstNumber || '—'}</div>
                    </td>
                    <td className="py-3 px-4 text-charcoal-700">
                      <span className="inline-flex items-center gap-1 bg-sand-100 text-charcoal-800 px-2 py-0.5 rounded text-[11px]">
                        <FileText className="w-3 h-3 text-charcoal-500" />
                        <span>{v.documentCount} docs</span>
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold ${
                          v.status === 'VERIFIED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : v.status === 'PENDING'
                            ? 'bg-amber-50 text-amber-700'
                            : v.status === 'NEEDS_MORE_INFO'
                            ? 'bg-blue-50 text-blue-700'
                            : 'bg-red-50 text-red-700'
                        }`}
                      >
                        {v.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-charcoal-500 whitespace-nowrap text-[11px]">
                      {v.submittedAt ? new Date(v.submittedAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => {
                          setSelectedReq(v);
                          setDecisionStatus('VERIFIED');
                          setReason('');
                        }}
                        className="px-2.5 py-1 text-xs font-medium rounded border border-bronze-300 bg-bronze-50/50 text-bronze-800 hover:bg-bronze-100/70"
                      >
                        Review
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-xs text-charcoal-500">
                    {isLoading
                      ? 'Loading requests...'
                      : 'No verification requests found for this filter.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Decision Modal */}
      {selectedReq && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-lg border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              Verification Decision
            </h2>
            <p className="text-xs text-charcoal-600 mb-4">
              Reviewing <strong>{selectedReq.businessName}</strong> ({selectedReq.studioName}).
            </p>

            {/* Credential summary */}
            <div className="bg-sand-50 rounded-lg p-3 text-xs space-y-1.5 mb-4 text-charcoal-700">
              <div>
                <span className="font-semibold text-charcoal-900">Professional Type:</span>{' '}
                {selectedReq.professionalType}
              </div>
              <div>
                <span className="font-semibold text-charcoal-900">Registration Number:</span>{' '}
                {selectedReq.registrationNumber || 'Not provided'}
              </div>
              <div>
                <span className="font-semibold text-charcoal-900">GST Number:</span>{' '}
                {selectedReq.gstNumber || 'Not provided'}
              </div>
              <div>
                <span className="font-semibold text-charcoal-900">Website:</span>{' '}
                {selectedReq.websiteDomain || 'Not provided'}
              </div>
              <div>
                <span className="font-semibold text-charcoal-900">Documents Submitted:</span>{' '}
                {selectedReq.documentCount} private evidence files
              </div>
              {selectedReq.notes && (
                <div>
                  <span className="font-semibold text-charcoal-900">Studio Notes:</span>{' '}
                  {selectedReq.notes}
                </div>
              )}
            </div>

            <form onSubmit={handleSubmitDecision} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">Decision</label>
                <select
                  value={decisionStatus}
                  onChange={(e) => setDecisionStatus(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                >
                  <option value="VERIFIED">VERIFIED (Approve & issue verified badge)</option>
                  <option value="NEEDS_MORE_INFO">
                    NEEDS_MORE_INFO (Request additional evidence)
                  </option>
                  <option value="REJECTED">REJECTED (Reject application)</option>
                  <option value="REVERIFY_REQUIRED">
                    REVERIFY_REQUIRED (Require re-verification)
                  </option>
                </select>
              </div>

              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Decision Notes / Reason (Shared with studio & audited)
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Validated business registration with registry records..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setSelectedReq(null)}
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg bg-charcoal-900 text-white font-medium hover:bg-charcoal-800 disabled:opacity-50"
                >
                  {isSubmitting ? 'Recording...' : 'Submit Decision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
