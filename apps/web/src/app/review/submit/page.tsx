'use client';

import React, { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { submitReview } from '@/lib/reviews/api';
import { DisplayNameMode } from '@/lib/reviews/types';

function ReviewSubmitForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const studioName = searchParams.get('studio') || 'the studio';
  const clientName = searchParams.get('client') || 'Client';

  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [reviewText, setReviewText] = useState('');
  const [projectTitle, setProjectTitle] = useState('');
  const [completedYear, setCompletedYear] = useState<number>(new Date().getFullYear());
  const [displayNameMode, setDisplayNameMode] = useState<DisplayNameMode>('FIRST_NAME');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submittedSuccess, setSubmittedSuccess] = useState(false);

  // Compute preview of how the reviewer name appears based on displayNameMode
  const getDisplayNamePreview = () => {
    const parts = clientName.trim().split(/\s+/);
    if (displayNameMode === 'FIRST_NAME') {
      return parts[0];
    }
    if (displayNameMode === 'INITIALS') {
      return parts.map((p) => p.charAt(0).toUpperCase() + '.').join(' ');
    }
    return 'Anonymous Client';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reviewText.trim()) {
      setError('Please provide your review feedback.');
      return;
    }
    if (rating < 1 || rating > 5) {
      setError('Rating must be between 1 and 5.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      await submitReview({
        rating,
        reviewText: reviewText.trim(),
        projectTitle: projectTitle.trim() || null,
        completedYear: completedYear || null,
        displayNameMode,
      });
      setSubmittedSuccess(true);
    } catch {
      setError('Failed to submit review. Your session may have expired or was already used.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (submittedSuccess) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4 bg-zinc-50">
        <div className="max-w-md w-full bg-white rounded-2xl p-8 border border-zinc-200/80 shadow-sm text-center space-y-4">
          <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto text-xl">
            ✓
          </div>
          <h2 className="text-xl font-bold text-zinc-900">Thank You for Your Feedback!</h2>
          <p className="text-sm text-zinc-600 leading-relaxed">
            Your review for <span className="font-semibold text-zinc-900">{studioName}</span> has been submitted and verified. It is now published directly on their portfolio profile.
          </p>
          <div className="pt-4">
            <button
              type="button"
              onClick={() => router.push('/')}
              className="px-5 py-2 text-xs font-medium text-white bg-zinc-900 rounded-lg hover:bg-zinc-800 transition-colors"
            >
              Back to Home
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-50 py-12 px-4 flex justify-center items-center">
      <div className="max-w-xl w-full bg-white rounded-2xl p-8 border border-zinc-200/80 shadow-sm space-y-6">
        <div className="space-y-1 text-center">
          <span className="text-[11px] font-semibold tracking-wider uppercase text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200/60">
            Verified Client Review
          </span>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-900 pt-2">
            Share Your Experience
          </h1>
          <p className="text-xs text-zinc-500">
            You are reviewing <span className="font-semibold text-zinc-800">{studioName}</span>
          </p>
        </div>

        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-xs leading-relaxed">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Rating Stars Selector */}
          <div className="space-y-2 text-center">
            <label className="block text-xs font-semibold text-zinc-700">
              Overall Rating (1 to 5)
            </label>
            <div className="flex items-center justify-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  aria-label={`Rate ${star} star${star > 1 ? 's' : ''}`}
                  className="p-1 focus:outline-none transition-transform hover:scale-110"
                >
                  <svg
                    className={`w-8 h-8 transition-colors ${
                      star <= (hoverRating || rating)
                        ? 'text-amber-400 fill-amber-400'
                        : 'text-zinc-300'
                    }`}
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                </button>
              ))}
            </div>
            <p className="text-[11px] text-zinc-400 font-medium">
              {['Poor', 'Fair', 'Good', 'Very Good', 'Excellent'][rating - 1]}
            </p>
          </div>

          {/* Project Details (optional) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Project Title (optional)
              </label>
              <input
                type="text"
                value={projectTitle}
                onChange={(e) => setProjectTitle(e.target.value)}
                placeholder="e.g. 3BHK Modular Kitchen & Wardrobe"
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Completion Year (optional)
              </label>
              <input
                type="number"
                min={2000}
                max={new Date().getFullYear()}
                value={completedYear}
                onChange={(e) => setCompletedYear(parseInt(e.target.value) || new Date().getFullYear())}
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>
          </div>

          {/* Review Text */}
          <div>
            <label className="block text-xs font-medium text-zinc-700 mb-1">
              Your Detailed Review <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={5}
              required
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="Describe the design quality, adherence to timelines, communication, and overall outcome..."
              className="w-full text-xs p-3 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900 leading-relaxed"
            />
          </div>

          {/* Display Name Privacy Mode */}
          <div className="space-y-2 p-4 bg-zinc-50 rounded-xl border border-zinc-200">
            <label className="block text-xs font-semibold text-zinc-800">
              Reviewer Name Privacy
            </label>
            <p className="text-[11px] text-zinc-500">
              Choose how your name appears publicly on the review.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
              {[
                { mode: 'FIRST_NAME' as DisplayNameMode, label: 'First Name Only' },
                { mode: 'INITIALS' as DisplayNameMode, label: 'Initials Only' },
                { mode: 'ANONYMOUS' as DisplayNameMode, label: 'Anonymous' },
              ].map((opt) => (
                <button
                  key={opt.mode}
                  type="button"
                  onClick={() => setDisplayNameMode(opt.mode)}
                  className={`p-2.5 rounded-lg border text-xs text-center transition-all ${
                    displayNameMode === opt.mode
                      ? 'border-zinc-900 bg-white font-semibold text-zinc-900 shadow-xs'
                      : 'border-zinc-200 bg-zinc-50 text-zinc-600 hover:border-zinc-300'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <p className="text-[11px] text-zinc-500 pt-1">
              Appears as:{' '}
              <span className="font-semibold text-zinc-900">{getDisplayNamePreview()}</span>
            </p>
          </div>

          {/* Submit Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 px-4 text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 rounded-lg disabled:opacity-50 transition-colors shadow-sm"
            >
              {isSubmitting ? 'Publishing Review...' : 'Publish Verified Review'}
            </button>
            <p className="text-[10px] text-zinc-400 text-center mt-2">
              Reviews cannot be altered by designers or studios. They reflect honest client experiences.
            </p>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function ReviewSubmitPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-zinc-50 flex items-center justify-center">Loading...</div>}>
      <ReviewSubmitForm />
    </Suspense>
  );
}
