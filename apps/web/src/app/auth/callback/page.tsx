'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth/auth-context';
import { sanitizeRedirectUrl } from '@/lib/auth/redirect';

function CallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get('returnUrl') || '/account';
  const { refreshUser, isAuthenticated } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function finalizeAuth() {
      try {
        await refreshUser();
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Failed to finalize session');
      }
    }
    finalizeAuth();
  }, [refreshUser]);

  useEffect(() => {
    if (isAuthenticated) {
      router.replace(sanitizeRedirectUrl(returnUrl));
    }
  }, [isAuthenticated, returnUrl, router]);

  if (error) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="max-w-md w-full p-6 bg-white border border-terracotta-200 rounded-2xl text-center shadow-sm">
          <h2 className="font-serif text-2xl text-charcoal-900 mb-2">Authentication Error</h2>
          <p className="text-sm text-charcoal-600 mb-6">{error}</p>
          <button
            onClick={() => router.push('/sign-in')}
            className="py-2.5 px-5 bg-charcoal-900 text-white rounded-xl text-sm font-medium hover:bg-charcoal-800 transition-colors"
          >
            Return to Sign In
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center px-4">
      <div className="w-10 h-10 border-4 border-bronze-200 border-t-bronze-600 rounded-full animate-spin mb-4" />
      <h2 className="font-serif text-xl text-charcoal-900 mb-1">Finalizing Authentication</h2>
      <p className="text-sm text-charcoal-500">Establishing secure session context...</p>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<div className="min-h-[70vh] flex items-center justify-center">Authenticating...</div>}>
      <CallbackContent />
    </Suspense>
  );
}
