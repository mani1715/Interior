'use client';

import React, { useEffect, useState } from 'react';
import { PublicStudioReviewsResponse } from '@/lib/reviews/types';
import { getPublicReviews } from '@/lib/reviews/api';
import { ReviewStars } from './ReviewStars';
import { ReviewCard } from './ReviewCard';

interface ReviewSectionProps {
  studioSlug: string;
  studioName?: string;
}

export function ReviewSection({ studioSlug, studioName }: ReviewSectionProps) {
  const [data, setData] = useState<PublicStudioReviewsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      const res = await getPublicReviews(studioSlug);
      setData(res);
      setError(null);
    } catch {
      setError('Unable to load client reviews at this time.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews();
  }, [studioSlug]);

  if (loading) {
    return (
      <div className="py-12 flex justify-center items-center">
        <div className="w-6 h-6 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !data) {
    return null;
  }

  const { aggregate, reviews } = data;
  const total = aggregate.totalReviews;

  return (
    <section className="py-12 border-t border-zinc-200/80 space-y-8" id="client-reviews">
      <div className="space-y-1">
        <h3 className="text-xl font-bold tracking-tight text-zinc-900">
          Client Reviews & Ratings
        </h3>
        <p className="text-sm text-zinc-500">
          Genuine verified feedback from clients who completed projects with {studioName || data.studioName}.
        </p>
      </div>

      {total === 0 ? (
        <div className="text-center py-10 bg-zinc-50 rounded-xl border border-dashed border-zinc-300">
          <p className="text-sm font-medium text-zinc-700">No client reviews yet</p>
          <p className="text-xs text-zinc-500 mt-1 max-w-sm mx-auto">
            Reviews on this platform are submitted exclusively by verified clients upon project completion.
          </p>
        </div>
      ) : (
        <div className="space-y-8">
          {/* Summary Breakdown Card */}
          <div className="p-6 bg-zinc-50 rounded-2xl border border-zinc-200/80 grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
            {/* Average Score */}
            <div className="text-center md:text-left space-y-1">
              <div className="text-4xl font-extrabold text-zinc-900 tracking-tight">
                {aggregate.averageRating.toFixed(1)}
              </div>
              <ReviewStars rating={aggregate.averageRating} size="md" />
              <p className="text-xs text-zinc-500 pt-1">
                Based on {total} verified {total === 1 ? 'review' : 'reviews'}
              </p>
            </div>

            {/* Distribution Bars */}
            <div className="md:col-span-2 space-y-2">
              {[5, 4, 3, 2, 1].map((stars) => {
                const count = aggregate.ratingBreakdown?.[stars] || 0;
                const percentage = total > 0 ? (count / total) * 100 : 0;
                return (
                  <div key={stars} className="flex items-center gap-3 text-xs">
                    <span className="w-12 text-zinc-600 font-medium shrink-0">
                      {stars} star
                    </span>
                    <div className="flex-1 h-2 bg-zinc-200 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-amber-400 rounded-full transition-all duration-300"
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                    <span className="w-8 text-right text-zinc-500 text-[11px] shrink-0">
                      {count}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* List of Reviews */}
          <div className="space-y-4">
            {reviews.map((rev) => (
              <ReviewCard key={rev.id} review={rev} onReported={fetchReviews} />
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
