'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { exchangeReviewToken } from '@/lib/reviews/api';
import { ExchangeReviewTokenResponse } from '@/lib/reviews/types';

export default function ReviewInvitePage() {
  const params = useParams();
  const router = useRouter();
  const token = params?.token as string;

  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<ExchangeReviewTokenResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    const exchange = async () => {
      try {
        setLoading(true);
        const res = await exchangeReviewToken(token);
        setData(res);
        // Automatically redirect to review submission form after setting secure session cookie
        setTimeout(() => {
          router.push(`/review/submit?studio=${encodeURIComponent(res.studioName)}&client=${encodeURIComponent(res.clientName)}`);
        }, 1500);
      } catch {
        setError('This review invitation link is invalid or has expired.');
      } finally {
        setLoading(false);
      }
    };
    exchange();
  }, [token, router]);

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-zinc-50">
      <div className="max-w-md w-full bg-white rounded-2xl p-8 border border-zinc-200/80 shadow-sm text-center space-y-4">
        {loading && (
          <div className="space-y-4 py-8">
            <div className="w-8 h-8 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin mx-auto" />
            <h2 className="text-lg font-semibold text-zinc-900">Verifying Invitation...</h2>
            <p className="text-xs text-zinc-500">
              Validating your secure client review invitation.
            </p>
          </div>
        )}

        {error && (
          <div className="space-y-4 py-6">
            <div className="w-12 h-12 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto text-xl">
              ✕
            </div>
            <h2 className="text-lg font-semibold text-zinc-900">Invitation Expired or Invalid</h2>
            <p className="text-xs text-zinc-600 leading-relaxed">
              {error}
            </p>
            <p className="text-[11px] text-zinc-400">
              Review invitations are valid for 30 days and single-use only.
            </p>
          </div>
        )}

        {data && (
          <div className="space-y-4 py-6">
            <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto text-xl">
              ✓
            </div>
            <h2 className="text-lg font-semibold text-zinc-900">Welcome, {data.clientName}</h2>
            <p className="text-xs text-zinc-600">
              Your verified review session for <span className="font-semibold text-zinc-900">{data.studioName}</span> has been established. Redirecting to review submission...
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
