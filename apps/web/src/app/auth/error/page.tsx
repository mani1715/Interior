'use client';

import React, { Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';

function AuthErrorContent() {
  const searchParams = useSearchParams();
  const errorCode = searchParams.get('error') || 'authentication_failed';

  const errorMessages: Record<string, { title: string; description: string }> = {
    missing_parameters: {
      title: 'Missing Parameters',
      description: 'The authentication callback did not receive the required state or authorization code.',
    },
    AccessDeniedException: {
      title: 'Access Denied',
      description: 'The authentication request was rejected or your session transaction has expired.',
    },
    EMAIL_COLLISION: {
      title: 'Account Conflict',
      description: 'An existing account matches this email address. Automatic account merging without explicit password verification is forbidden.',
    },
    RateLimitExceededException: {
      title: 'Too Many Attempts',
      description: 'Too many authentication attempts were detected. Please wait a few moments before trying again.',
    },
  };

  const errorDetail = errorMessages[errorCode] || {
    title: 'Authentication Error',
    description: 'An unexpected authentication error occurred during provider communication.',
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-sand-50/50">
      <div className="w-full max-w-md bg-white border border-terracotta-200 rounded-2xl p-6 sm:p-8 shadow-sm text-center">
        <div className="w-12 h-12 rounded-full bg-terracotta-50 text-terracotta-700 flex items-center justify-center mx-auto mb-4">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>

        <h1 className="font-serif text-2xl text-charcoal-900 tracking-tight mb-2">
          {errorDetail.title}
        </h1>
        <p className="text-sm text-charcoal-600 mb-6">
          {errorDetail.description}
        </p>

        <div className="space-y-3">
          <Link
            href="/sign-in"
            className="w-full block py-2.5 px-4 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white font-medium text-sm transition-colors"
          >
            Try Signing In Again
          </Link>
          <Link
            href="/"
            className="w-full block py-2.5 px-4 rounded-xl border border-sand-300 text-charcoal-700 hover:bg-sand-50 font-medium text-sm transition-colors"
          >
            Return to Homepage
          </Link>
        </div>

        <div className="mt-6 pt-4 border-t border-sand-200">
          <span className="text-[11px] font-mono text-charcoal-400">
            Error Reference: {errorCode}
          </span>
        </div>
      </div>
    </div>
  );
}

export default function AuthErrorPage() {
  return (
    <Suspense fallback={<div className="min-h-[80vh] flex items-center justify-center">Loading error details...</div>}>
      <AuthErrorContent />
    </Suspense>
  );
}
