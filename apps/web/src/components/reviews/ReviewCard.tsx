'use client';

import React, { useState } from 'react';
import { PublicStudioReviewDto, ReportReason } from '@/lib/reviews/types';
import { reportReview } from '@/lib/reviews/api';
import { ReviewStars } from './ReviewStars';

interface ReviewCardProps {
  review: PublicStudioReviewDto;
  onReported?: () => void;
}

export function ReviewCard({ review, onReported }: ReviewCardProps) {
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportReason, setReportReason] = useState<ReportReason>('INAPPROPRIATE');
  const [reportDetails, setReportDetails] = useState('');
  const [isSubmittingReport, setIsSubmittingReport] = useState(false);
  const [reportSuccess, setReportSuccess] = useState(false);
  const [reportError, setReportError] = useState<string | null>(null);

  const formattedDate = new Date(review.createdAt).toLocaleDateString('en-IN', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });

  const handleReportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingReport(true);
    setReportError(null);
    try {
      await reportReview(review.id, {
        reason: reportReason,
        details: reportDetails.trim() || null,
      });
      setReportSuccess(true);
      setTimeout(() => {
        setShowReportModal(false);
        setReportSuccess(false);
        setReportDetails('');
        onReported?.();
      }, 1500);
    } catch {
      setReportError('Failed to submit report. Please try again.');
    } finally {
      setIsSubmittingReport(false);
    }
  };

  return (
    <article className="p-6 bg-white rounded-xl border border-zinc-200/80 shadow-sm space-y-4">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h4 className="font-semibold text-zinc-900">{review.displayName}</h4>
            {review.verifiedClient && (
              <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200/60">
                <svg className="w-3 h-3 text-emerald-600" viewBox="0 0 20 20" fill="currentColor">
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clipRule="evenodd"
                  />
                </svg>
                Verified Client
              </span>
            )}
          </div>
          {(review.projectTitle || review.completedYear) && (
            <p className="text-xs text-zinc-500 mt-0.5">
              Project:{' '}
              <span className="text-zinc-700 font-medium">
                {review.projectTitle || 'Interior Project'}
              </span>
              {review.completedYear && ` • Completed ${review.completedYear}`}
            </p>
          )}
        </div>
        <div className="text-right shrink-0">
          <ReviewStars rating={review.rating} size="sm" />
          <time dateTime={review.createdAt} className="block text-[11px] text-zinc-400 mt-1">
            {formattedDate}
          </time>
        </div>
      </div>

      {/* Review Body */}
      <p className="text-sm text-zinc-700 leading-relaxed whitespace-pre-line">
        {review.reviewText}
      </p>

      {/* Studio Public Response */}
      {review.studioResponse && (
        <div className="mt-3 p-4 bg-zinc-50 rounded-lg border-l-4 border-zinc-400 space-y-1">
          <div className="flex items-center justify-between text-xs text-zinc-600">
            <span className="font-semibold text-zinc-800">Response from Studio</span>
            {review.studioRespondedAt && (
              <time dateTime={review.studioRespondedAt} className="text-[11px] text-zinc-400">
                {new Date(review.studioRespondedAt).toLocaleDateString('en-IN', {
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })}
              </time>
            )}
          </div>
          <p className="text-xs text-zinc-700 whitespace-pre-line leading-relaxed">
            {review.studioResponse}
          </p>
        </div>
      )}

      {/* Footer / Report Action */}
      <div className="pt-2 border-t border-zinc-100 flex items-center justify-end">
        <button
          type="button"
          onClick={() => setShowReportModal(true)}
          className="text-xs text-zinc-400 hover:text-zinc-600 transition-colors"
        >
          Report review
        </button>
      </div>

      {/* Report Modal */}
      {showReportModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-zinc-900">Report this Review</h3>
              <button
                type="button"
                onClick={() => setShowReportModal(false)}
                className="text-zinc-400 hover:text-zinc-600"
              >
                ✕
              </button>
            </div>
            {reportSuccess ? (
              <p className="text-sm text-emerald-600 bg-emerald-50 p-3 rounded-lg">
                Report submitted for administrative review. Thank you.
              </p>
            ) : (
              <form onSubmit={handleReportSubmit} className="space-y-4">
                {reportError && (
                  <p className="text-xs text-red-600 bg-red-50 p-2 rounded">{reportError}</p>
                )}
                <div>
                  <label className="block text-xs font-medium text-zinc-700 mb-1">
                    Reason for report
                  </label>
                  <select
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value as ReportReason)}
                    className="w-full text-xs p-2 border border-zinc-300 rounded-md focus:ring-1 focus:ring-zinc-900"
                  >
                    <option value="INAPPROPRIATE">Inappropriate or abusive language</option>
                    <option value="SPAM">Spam or advertisement</option>
                    <option value="CONFLICT_OF_INTEREST">Conflict of interest</option>
                    <option value="FALSE_INFORMATION">False or misleading claims</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-zinc-700 mb-1">
                    Additional details (optional)
                  </label>
                  <textarea
                    rows={3}
                    value={reportDetails}
                    onChange={(e) => setReportDetails(e.target.value)}
                    placeholder="Provide details to assist moderation team..."
                    className="w-full text-xs p-2 border border-zinc-300 rounded-md focus:ring-1 focus:ring-zinc-900"
                  />
                </div>
                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowReportModal(false)}
                    className="px-3 py-1.5 text-xs text-zinc-600 hover:text-zinc-800"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmittingReport}
                    className="px-4 py-1.5 text-xs font-medium text-white bg-red-600 hover:bg-red-700 rounded-md disabled:opacity-50"
                  >
                    {isSubmittingReport ? 'Submitting...' : 'Submit Report'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </article>
  );
}
