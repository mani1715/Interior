'use client';

import React, { useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth/auth-context';
import { sanitizeRedirectUrl } from '@/lib/auth/redirect';
import { apiFetch } from '@/lib/api-client';

interface ProviderInfo {
  id: string;
  displayName: string;
  isConfigured: boolean;
}

interface DevPersona {
  key: string;
  displayName: string;
  email: string;
  role: string;
  assurance: string;
}

interface ProvidersResponse {
  providers: ProviderInfo[];
  devAuthEnabled: boolean;
  devPersonas: DevPersona[];
}

function SignInContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get('returnUrl') || '/account';
  const { user, isAuthenticated, loginDevPersona } = useAuth();

  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [devEnabled, setDevEnabled] = useState<boolean>(false);
  const [devPersonas, setDevPersonas] = useState<DevPersona[]>([]);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (isAuthenticated && user) {
      router.push(sanitizeRedirectUrl(returnUrl));
    }
  }, [isAuthenticated, user, returnUrl, router]);

  useEffect(() => {
    async function loadProviders() {
      try {
        const res = await apiFetch<ProvidersResponse>('/auth/providers');
        setProviders(res.providers || []);
        setDevEnabled(res.devAuthEnabled || false);
        setDevPersonas(res.devPersonas || []);
      } catch {
        // Network error / offline fallback
      }
    }
    loadProviders();
  }, []);

  const handleDevLogin = async (personaKey: string) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await loginDevPersona(personaKey);
      router.push(sanitizeRedirectUrl(returnUrl));
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Authentication failed');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-sand-50/50">
      <div className="w-full max-w-md bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="text-center mb-8">
          <span className="text-xs font-semibold tracking-widest uppercase text-bronze-700 mb-2 block">
            Secure Platform Access
          </span>
          <h1 className="font-serif text-3xl text-charcoal-900 tracking-tight">
            Sign In to Atelier
          </h1>
          <p className="text-sm text-charcoal-600 mt-2">
            Access your interior architecture projects, studio workspace, or client inquiries.
          </p>
        </div>

        {errorMessage && (
          <div className="mb-6 p-4 rounded-xl bg-terracotta-50 border border-terracotta-200 text-terracotta-800 text-sm">
            {errorMessage}
          </div>
        )}

        {/* Enterprise & Social Identity Providers */}
        <div className="space-y-3 mb-8">
          {providers.length > 0 ? (
            providers.map((p) => (
              <button
                key={p.id}
                disabled={!p.isConfigured || isSubmitting}
                onClick={() => {
                  if (p.isConfigured) {
                    window.location.href = `/api/v1/auth/login?provider=${p.id}&returnUrl=${encodeURIComponent(returnUrl)}`;
                  }
                }}
                className={`w-full flex items-center justify-center gap-3 px-4 py-3 rounded-xl border text-sm font-medium transition-all ${
                  p.isConfigured
                    ? 'border-charcoal-300 text-charcoal-800 hover:bg-sand-50 hover:border-charcoal-400'
                    : 'border-sand-200 text-charcoal-400 bg-sand-50 cursor-not-allowed opacity-75'
                }`}
              >
                <span>Continue with {p.displayName}</span>
                {!p.isConfigured && (
                  <span className="text-xs text-charcoal-500 bg-sand-200/60 px-2 py-0.5 rounded">
                    Configuration Pending
                  </span>
                )}
              </button>
            ))
          ) : (
            <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-xs text-charcoal-600 space-y-1">
              <p className="font-medium text-charcoal-800">Production Identity Federation</p>
              <p>
                Federated OpenID Connect (Google Workspace / Azure AD) is pending production provider provisioning.
              </p>
            </div>
          )}
        </div>

        {/* Development Auth Sandbox */}
        {devEnabled && devPersonas.length > 0 && (
          <div className="mt-8 pt-6 border-t border-sand-200">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-bronze-700 tracking-wider uppercase">
                Dev Persona Sandbox
              </span>
              <span className="text-[10px] bg-forest-50 text-forest-700 font-mono px-2 py-0.5 rounded-full">
                Active in Dev/Test
              </span>
            </div>
            <p className="text-xs text-charcoal-500 mb-4">
              Instant login with predetermined platform personas to test role-based permissions and tenant scoping.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {devPersonas.map((persona) => (
                <button
                  key={persona.key}
                  disabled={isSubmitting}
                  onClick={() => handleDevLogin(persona.key)}
                  className="flex flex-col items-start p-3 text-left border border-sand-200 rounded-xl hover:border-bronze-300 hover:bg-sand-50 transition-colors"
                >
                  <span className="text-xs font-medium text-charcoal-900">{persona.displayName}</span>
                  <span className="text-[10px] text-bronze-800 font-mono mt-0.5 uppercase tracking-wider">
                    {persona.role}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Footer Navigation & Security Assurance */}
        <div className="mt-8 pt-6 border-t border-sand-200 text-center space-y-4">
          <p className="text-sm text-charcoal-600">
            Don&apos;t have an account yet?{' '}
            <Link href="/sign-up" className="text-bronze-700 font-medium hover:underline">
              Create an account
            </Link>
          </p>
          <p className="text-[11px] text-charcoal-400">
            Protected by backend-owned opaque sessions, encrypted cookie transport, and CSRF synchronizer tokens.
          </p>
        </div>
      </div>
    </div>
  );
}

export default function SignInPage() {
  return (
    <Suspense fallback={<div className="min-h-[80vh] flex items-center justify-center">Loading...</div>}>
      <SignInContent />
    </Suspense>
  );
}
