'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { AlertCircle, RefreshCw, Home } from 'lucide-react';

export default function GlobalErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error securely without leaking sensitive backend attributes
    console.error('Unhandled application error:', error.message);
  }, [error]);

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F] flex items-center justify-center p-6 sm:p-8">
      <div className="max-w-md w-full bg-white border border-[#E7E1D8] rounded-2xl p-8 shadow-sm text-center space-y-6">
        <div className="w-12 h-12 rounded-full bg-amber-50 text-amber-700 flex items-center justify-center mx-auto">
          <AlertCircle className="w-6 h-6" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-semibold uppercase tracking-widest text-[#B88A5A] block">
            Unexpected Interruption
          </span>
          <h2 className="font-serif text-2xl font-semibold text-[#1F1F1F]">
            Something went wrong
          </h2>
          <p className="text-xs sm:text-sm text-[#6B6B6B] leading-relaxed">
            An unexpected error occurred while loading this view. You can retry the request or return to the homepage.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
          <button
            type="button"
            onClick={() => reset()}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-[#1F1F1F] text-white rounded-xl text-xs font-semibold hover:bg-black transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Try Again</span>
          </button>
          <Link
            href="/"
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 border border-[#D9D9D9] bg-white text-[#1F1F1F] rounded-xl text-xs font-semibold hover:bg-[#F4F4F4] transition-colors"
          >
            <Home className="w-3.5 h-3.5" />
            <span>Home</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
