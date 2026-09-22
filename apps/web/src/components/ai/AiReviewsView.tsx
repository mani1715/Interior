'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { ClientReviewDetailResponse } from '@/lib/ai/types';
import {
  listClientReviews,
  closeClientReview,
  revokeClientReview,
  rotateClientReviewToken,
} from '@/lib/ai/api';
import {
  Share2,
  Copy,
  ExternalLink,
  CheckCircle2,
  AlertCircle,
  Clock,
  RotateCcw,
  Ban,
  Lock,
  MessageSquare,
  Loader2,
  ChevronRight,
} from 'lucide-react';

interface AiReviewsViewProps {
  projectId?: string;
  studioId?: string;
}

export function AiReviewsView({ projectId, studioId }: AiReviewsViewProps) {
  const [reviews, setReviews] = useState<ClientReviewDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedReview, setSelectedReview] = useState<ClientReviewDetailResponse | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const loadReviews = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await listClientReviews(
        {
          projectId: projectId || undefined,
          page: 0,
          limit: 30,
        },
        studioId
      );
      setReviews(data);
    } catch (err: any) {
      setError(err?.envelope?.message || 'Failed to load client review links');
    } finally {
      setLoading(false);
    }
  }, [projectId, studioId]);

  useEffect(() => {
    loadReviews();
  }, [loadReviews]);

  const handleCopy = (review: ClientReviewDetailResponse) => {
    // Note: Studio can share the public review link pattern
    // The initial exchange link is constructed by token; if review is open we can link to view or provide advice
    const url = `${window.location.origin}/review/view/${review.id}`;
    navigator.clipboard.writeText(url);
    setCopiedId(review.id);
    setTimeout(() => setCopiedId(null), 3000);
  };

  const handleClose = async (reviewId: string) => {
    if (!confirm('Are you sure you want to close this review? Clients will no longer be able to submit approvals or comments.')) return;
    try {
      setActionLoading(`close_${reviewId}`);
      await closeClientReview(reviewId, studioId);
      await loadReviews();
      if (selectedReview?.id === reviewId) {
        setSelectedReview((prev) => (prev ? { ...prev, status: 'CLOSED' } : null));
      }
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to close review');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRevoke = async (reviewId: string) => {
    if (!confirm('Revoking this review link immediately terminates all active client sessions and prevents further access. Proceed?')) return;
    try {
      setActionLoading(`revoke_${reviewId}`);
      await revokeClientReview(reviewId, studioId);
      await loadReviews();
      if (selectedReview?.id === reviewId) {
        setSelectedReview((prev) => (prev ? { ...prev, status: 'REVOKED' } : null));
      }
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to revoke review');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRotate = async (reviewId: string) => {
    if (!confirm('Rotating the link token generates a new private URL and invalidates all previously shared links. Proceed?')) return;
    try {
      setActionLoading(`rotate_${reviewId}`);
      const res = await rotateClientReviewToken(reviewId, studioId);
      const fullUrl = `${window.location.origin}${res.reviewUrl}`;
      navigator.clipboard.writeText(fullUrl);
      alert(`New review link generated and copied to clipboard!\n${fullUrl}`);
      await loadReviews();
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to rotate review link');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between bg-white p-4 rounded-2xl border border-sand-200 shadow-sm">
        <div>
          <h2 className="font-serif text-lg text-charcoal-900 font-medium">
            Client Review Presentations
          </h2>
          <p className="text-xs text-charcoal-500">
            Shareable, private concept presentations for client approvals and structured feedback.
          </p>
        </div>
        <button
          type="button"
          onClick={loadReviews}
          disabled={loading}
          className="px-3 py-1.5 rounded-lg border border-sand-300 text-xs font-medium text-charcoal-600 hover:bg-sand-50"
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {/* List */}
      {loading ? (
        <div className="py-16 text-center text-charcoal-400 flex items-center justify-center gap-2">
          <Loader2 className="w-5 h-5 animate-spin text-bronze-700" />
          <span className="text-sm">Loading client reviews...</span>
        </div>
      ) : error ? (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      ) : reviews.length === 0 ? (
        <div className="py-16 text-center text-charcoal-500 bg-white rounded-2xl border border-sand-200 p-8 space-y-2">
          <Share2 className="w-8 h-8 text-sand-300 mx-auto" />
          <p className="font-serif text-lg text-charcoal-800">No client reviews created yet</p>
          <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
            Select shortlisted concepts from the History tab and click "Create Client Review" to generate a private link for your client.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {reviews.map((review) => {
            const isExpired = new Date(review.expiresAt) < new Date();
            const isOpen = review.status === 'OPEN' && !isExpired;
            const currentApproval = review.decisions.find((d) => d.isCurrent && d.decision === 'APPROVED');
            const hasChangesRequested = review.decisions.some((d) => d.isCurrent && d.decision === 'CHANGES_REQUESTED');

            return (
              <div
                key={review.id}
                className="bg-white rounded-2xl border border-sand-200 p-5 shadow-sm space-y-4 hover:border-sand-300 transition-colors"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-sand-100 pb-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-serif text-base text-charcoal-900 font-semibold">
                        {review.title}
                      </h3>
                      {/* Status Badge */}
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold ${
                          isOpen
                            ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                            : review.status === 'REVOKED'
                            ? 'bg-red-50 text-red-800 border border-red-200'
                            : 'bg-sand-100 text-charcoal-600 border border-sand-200'
                        }`}
                      >
                        {isOpen ? 'Active' : isExpired ? 'Expired' : review.status}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 mt-1 text-xs text-charcoal-500">
                      <span className="font-medium text-bronze-800">{review.projectTitle}</span>
                      <span>•</span>
                      <span>Created {new Date(review.createdAt).toLocaleDateString()}</span>
                      <span>•</span>
                      <span>Expires {new Date(review.expiresAt).toLocaleDateString()}</span>
                    </div>
                  </div>

                  {/* Decision Summary Pill */}
                  <div>
                    {currentApproval ? (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-full text-xs font-semibold">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Concept Approved by {currentApproval.clientName || 'Client'}</span>
                      </span>
                    ) : hasChangesRequested ? (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-50 border border-amber-200 text-amber-800 rounded-full text-xs font-semibold">
                        <Clock className="w-3.5 h-3.5 text-amber-600" />
                        <span>Changes Requested</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-sand-50 border border-sand-200 text-charcoal-500 rounded-full text-xs">
                        <span>Awaiting Client Review</span>
                      </span>
                    )}
                  </div>
                </div>

                {/* Concept Thumbnails */}
                <div className="flex items-center gap-3 overflow-x-auto py-1">
                  {review.items.map((item) => (
                    <div key={item.id} className="flex-shrink-0 text-center">
                      <div className="w-20 h-16 rounded-lg overflow-hidden border border-sand-200 bg-sand-100">
                        {item.previewUrl ? (
                          <img src={item.previewUrl} alt={item.displayLabel} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-[10px] text-charcoal-400">Concept</div>
                        )}
                      </div>
                      <span className="text-[10px] text-charcoal-600 font-medium block mt-1">
                        {item.displayLabel}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Card Footer Actions */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-sand-100">
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setSelectedReview(review)}
                      className="px-3 py-1.5 bg-[#FAF8F5] hover:bg-sand-100 border border-sand-300 rounded-lg text-xs font-semibold text-charcoal-700 flex items-center gap-1.5 transition-colors"
                    >
                      <MessageSquare className="w-3.5 h-3.5 text-bronze-700" />
                      <span>Review Details ({review.decisions.length} decisions, {review.comments.length} notes)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleCopy(review)}
                      className="px-3 py-1.5 bg-[#FAF8F5] hover:bg-sand-100 border border-sand-300 rounded-lg text-xs font-semibold text-charcoal-700 flex items-center gap-1.5 transition-colors"
                    >
                      <Copy className="w-3.5 h-3.5" />
                      <span>{copiedId === review.id ? 'Copied View Link!' : 'Copy Link'}</span>
                    </button>
                  </div>

                  {isOpen && (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleRotate(review.id)}
                        disabled={actionLoading === `rotate_${review.id}`}
                        className="px-2.5 py-1 text-xs text-charcoal-600 hover:text-charcoal-900 border border-sand-300 rounded-lg hover:bg-sand-50"
                        title="Rotate security token"
                      >
                        Rotate Link
                      </button>

                      <button
                        type="button"
                        onClick={() => handleClose(review.id)}
                        disabled={actionLoading === `close_${review.id}`}
                        className="px-2.5 py-1 text-xs text-charcoal-600 hover:text-charcoal-900 border border-sand-300 rounded-lg hover:bg-sand-50"
                      >
                        Close Review
                      </button>

                      <button
                        type="button"
                        onClick={() => handleRevoke(review.id)}
                        disabled={actionLoading === `revoke_${review.id}`}
                        className="px-2.5 py-1 text-xs text-red-600 hover:text-red-800 border border-red-200 rounded-lg hover:bg-red-50"
                      >
                        Revoke Access
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Detail Modal */}
      {selectedReview && (
        <div className="fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 space-y-5 shadow-xl border border-sand-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-sand-100 pb-3">
              <div>
                <h3 className="font-serif text-lg text-charcoal-900 font-medium">
                  {selectedReview.title}
                </h3>
                <span className="text-xs text-bronze-800">{selectedReview.projectTitle}</span>
              </div>
              <button
                type="button"
                onClick={() => setSelectedReview(null)}
                className="text-charcoal-400 hover:text-charcoal-700 text-sm"
              >
                ✕
              </button>
            </div>

            {/* Decision Log */}
            <div className="space-y-3">
              <h4 className="text-xs font-semibold uppercase tracking-wider text-charcoal-700">
                Client Decisions & Approvals
              </h4>
              {selectedReview.decisions.length === 0 ? (
                <p className="text-xs text-charcoal-500 italic bg-sand-50 p-3 rounded-xl">
                  No decisions submitted yet by the client.
                </p>
              ) : (
                <div className="space-y-2">
                  {selectedReview.decisions.map((d) => (
                    <div
                      key={d.id}
                      className={`p-3 rounded-xl border text-xs space-y-1 ${
                        d.decision === 'APPROVED' ? 'bg-emerald-50/70 border-emerald-200' : 'bg-amber-50/70 border-amber-200'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-charcoal-900">
                          {d.decision === 'APPROVED' ? '✓ APPROVED' : '⟳ CHANGES REQUESTED'} by {d.clientName || 'Client'}
                        </span>
                        <span className="text-[10px] text-charcoal-500">
                          {new Date(d.createdAt).toLocaleString()} {d.isCurrent && '(Current)'}
                        </span>
                      </div>
                      {d.feedback && <p className="text-charcoal-700 italic">"{d.feedback}"</p>}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Comments Log */}
            <div className="space-y-3 pt-2">
              <h4 className="text-xs font-semibold uppercase tracking-wider text-charcoal-700">
                Feedback & Notes Log ({selectedReview.comments.length})
              </h4>
              {selectedReview.comments.length === 0 ? (
                <p className="text-xs text-charcoal-500 italic bg-sand-50 p-3 rounded-xl">
                  No comments logged yet.
                </p>
              ) : (
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {selectedReview.comments.map((c) => (
                    <div key={c.id} className="p-3 bg-[#FAF8F5] rounded-xl border border-sand-200 text-xs space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-charcoal-800">{c.authorName} ({c.authorType})</span>
                        <span className="text-[10px] text-charcoal-400">{new Date(c.createdAt).toLocaleString()}</span>
                      </div>
                      <p className="text-charcoal-700">{c.commentText}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex justify-end pt-3 border-t border-sand-100">
              <button
                type="button"
                onClick={() => setSelectedReview(null)}
                className="px-5 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
