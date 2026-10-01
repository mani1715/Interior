'use client';

import React, { useEffect, useState } from 'react';
import {
  MessageSquare,
  Search,
  Star,
  AlertTriangle,
  EyeOff,
  Eye,
  RefreshCw,
  Flag,
} from 'lucide-react';
import { fetchAdminReviews, moderateReview } from '@/lib/admin/api';
import { AdminReviewSummary, ModerateReviewRequest } from '@/lib/admin/types';

export default function AdminReviewsPage() {
  const [reviews, setReviews] = useState<AdminReviewSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Moderation Modal State
  const [selectedReview, setSelectedReview] = useState<AdminReviewSummary | null>(null);
  const [targetStatus, setTargetStatus] = useState<ModerateReviewRequest['status']>('REMOVED');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadReviews = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminReviews(statusFilter || undefined);
      setReviews(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reviews');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReviews();
  }, [statusFilter]);

  const handleModerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReview) return;

    setIsSubmitting(true);
    try {
      await moderateReview(selectedReview.id, {
        status: targetStatus,
        reason: reason.trim() || undefined,
      });
      setSelectedReview(null);
      setReason('');
      await loadReviews();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to moderate review');
    } finally {
      setIsSubmitting(false);
    }
  };

  const filtered = reviews.filter((r) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      r.studioName.toLowerCase().includes(q) ||
      r.reviewerDisplayName.toLowerCase().includes(q) ||
      (r.title && r.title.toLowerCase().includes(q)) ||
      r.reviewText.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">Review Moderation</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Inspect reported reviews, manage visibility, and uphold review authenticity.
          </p>
        </div>
        <button
          onClick={loadReviews}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Notice */}
      <div className="bg-sand-100/70 border border-sand-200 rounded-lg p-3 text-xs text-charcoal-700 flex items-start gap-2.5">
        <AlertTriangle className="w-4 h-4 text-charcoal-500 shrink-0 mt-0.5" />
        <div>
          <strong className="font-semibold text-charcoal-900">Moderation Invariant:</strong> Administrators cannot alter customer star ratings or rewrite review text. Removing or hiding a review automatically updates studio aggregate ratings truthfully without data corruption.
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
            placeholder="Search by studio, client, or text..."
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
            <option value="FLAGGED">Flagged / Reported</option>
            <option value="PUBLISHED">Published</option>
            <option value="REMOVED">Removed</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Reviews Table */}
      <div className="bg-white border border-sand-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-sand-50/50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Studio / Reviewer</th>
                <th className="py-3 px-4">Rating</th>
                <th className="py-3 px-4 max-w-md">Content</th>
                <th className="py-3 px-4">Reports</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Submitted</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sand-100">
              {filtered.length > 0 ? (
                filtered.map((r) => (
                  <tr key={r.id} className="hover:bg-sand-50/30">
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="font-medium text-charcoal-900">{r.studioName}</div>
                      <div className="text-[11px] text-charcoal-500">
                        Client: {r.reviewerDisplayName}
                      </div>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="flex items-center gap-1 text-amber-500 font-semibold">
                        <Star className="w-3.5 h-3.5 fill-current" />
                        <span>{r.rating}.0</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 max-w-md">
                      {r.title && (
                        <div className="font-semibold text-charcoal-900 mb-0.5">{r.title}</div>
                      )}
                      <p className="text-charcoal-600 line-clamp-2">{r.reviewText}</p>
                    </td>
                    <td className="py-3 px-4">
                      {r.reportCount > 0 ? (
                        <span className="inline-flex items-center gap-1 text-red-700 bg-red-50 px-2 py-0.5 rounded text-[11px] font-semibold">
                          <Flag className="w-3 h-3" />
                          <span>{r.reportCount}</span>
                        </span>
                      ) : (
                        <span className="text-charcoal-400">0</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold ${
                          r.status === 'PUBLISHED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : r.status === 'FLAGGED'
                            ? 'bg-amber-50 text-amber-700'
                            : 'bg-red-50 text-red-700'
                        }`}
                      >
                        {r.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-charcoal-500 whitespace-nowrap text-[11px]">
                      {new Date(r.submittedAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {r.status === 'REMOVED' ? (
                        <button
                          onClick={() => {
                            setSelectedReview(r);
                            setTargetStatus('PUBLISHED');
                            setReason('');
                          }}
                          className="px-2.5 py-1 text-xs font-medium rounded border border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                        >
                          Restore
                        </button>
                      ) : (
                        <button
                          onClick={() => {
                            setSelectedReview(r);
                            setTargetStatus('REMOVED');
                            setReason('');
                          }}
                          className="px-2.5 py-1 text-xs font-medium rounded border border-red-300 text-red-700 hover:bg-red-50"
                        >
                          Remove
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-xs text-charcoal-500">
                    {isLoading ? 'Loading reviews...' : 'No reviews match this filter.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Moderation Modal */}
      {selectedReview && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-lg border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              Moderate Review
            </h2>
            <p className="text-xs text-charcoal-600 mb-4">
              Review for <strong>{selectedReview.studioName}</strong> by{' '}
              {selectedReview.reviewerDisplayName}.
            </p>

            <form onSubmit={handleModerate} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">Target Status</label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                >
                  <option value="REMOVED">REMOVED (Hide review from public profile)</option>
                  <option value="FLAGGED">FLAGGED (Keep marked for further investigation)</option>
                  <option value="PUBLISHED">PUBLISHED (Restore to public profile)</option>
                </select>
              </div>

              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason for Audit Record
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Terms violation, profanity, non-client inquiry..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setSelectedReview(null)}
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className={`px-3 py-1.5 rounded-lg text-white font-medium disabled:opacity-50 ${
                    targetStatus === 'REMOVED'
                      ? 'bg-red-600 hover:bg-red-700'
                      : 'bg-charcoal-900 hover:bg-charcoal-800'
                  }`}
                >
                  {isSubmitting ? 'Updating...' : 'Confirm Moderation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
