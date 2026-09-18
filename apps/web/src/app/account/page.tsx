'use client';

import React, { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/auth-context';

export default function AccountPage() {
  const router = useRouter();
  const { user, isLoading, isAuthenticated, logout, revokeAllSessions } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/sign-in?returnUrl=/account');
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-bronze-200 border-t-bronze-600 rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10">
      <div className="mb-8">
        <span className="text-xs font-semibold tracking-widest uppercase text-bronze-700 block mb-1">
          Identity & Access
        </span>
        <h1 className="font-serif text-3xl sm:text-4xl text-charcoal-900 tracking-tight">
          Account Profile
        </h1>
        <p className="text-sm text-charcoal-600 mt-1">
          Verified platform identity, tenancy context, and cryptographic session security.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* User Card */}
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
            <h2 className="font-serif text-lg text-charcoal-900 mb-4 pb-3 border-b border-sand-200">
              Personal Information
            </h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-xs text-charcoal-500 font-medium">Display Name</dt>
                <dd className="font-semibold text-charcoal-900 mt-0.5">{user.displayName}</dd>
              </div>
              <div>
                <dt className="text-xs text-charcoal-500 font-medium">Email Address</dt>
                <dd className="text-charcoal-800 mt-0.5">{user.email || 'None associated'}</dd>
              </div>
              <div>
                <dt className="text-xs text-charcoal-500 font-medium">Account Status</dt>
                <dd className="mt-0.5">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-forest-50 text-forest-700">
                    {user.status}
                  </span>
                </dd>
              </div>
              <div>
                <dt className="text-xs text-charcoal-500 font-medium">User Identifier</dt>
                <dd className="font-mono text-xs text-charcoal-600 truncate mt-0.5">{user.id}</dd>
              </div>
            </dl>
          </div>

          {/* Tenancy / Studio Card */}
          <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
            <h2 className="font-serif text-lg text-charcoal-900 mb-4 pb-3 border-b border-sand-200">
              Studio Tenancy Context
            </h2>
            {user.studios.length > 0 ? (
              <div className="space-y-4">
                {user.studios.map((s) => (
                  <div key={s.studioId} className="p-4 bg-sand-50/70 border border-sand-200 rounded-xl space-y-3">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-sm font-serif font-bold text-charcoal-900">{s.studioName}</h3>
                        <p className="text-xs font-mono text-bronze-800 mt-0.5">@{s.studioSlug}</p>
                      </div>
                      <span className="text-xs font-semibold px-2.5 py-1 bg-bronze-50 text-bronze-800 border border-bronze-200 rounded-lg">
                        Role: {s.role}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-sand-200/60 text-[11px]">
                      <div>
                        <span className="text-charcoal-500">Operational Status:</span>
                        <span className="font-semibold text-forest-700 ml-1">ACTIVE</span>
                      </div>
                      <div>
                        <span className="text-charcoal-500">Publication Status:</span>
                        <span className="font-semibold text-charcoal-700 ml-1">UNPUBLISHED</span>
                      </div>
                    </div>
                  </div>
                ))}
                <div className="p-3 bg-bronze-50/50 rounded-xl border border-bronze-200 text-xs text-bronze-900">
                  <p className="font-medium">Designer Workspace</p>
                  <p className="text-[11px] text-bronze-800 mt-0.5">
                    Studio management, portfolio building, and publication workflows will activate in Phase 09.
                  </p>
                </div>
              </div>
            ) : (
              <div className="p-5 bg-sand-50/80 rounded-xl border border-sand-200 space-y-3">
                <div>
                  <p className="font-medium text-charcoal-900 text-sm">Customer Account Boundary</p>
                  <p className="text-xs text-charcoal-600 mt-1">
                    Are you an interior designer, architect, cabinetry specialist, or turnkey contractor?
                    Register your professional studio to unlock professional capabilities.
                  </p>
                </div>
                <Link href="/onboarding/professional">
                  <Button variant="primary" size="sm" className="mt-1">
                    Register Studio as Professional
                  </Button>
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* Security & Roles Card */}
        <div className="space-y-6">
          <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
            <h2 className="font-serif text-lg text-charcoal-900 pb-3 border-b border-sand-200">
              Security Context
            </h2>
            
            <div>
              <span className="text-xs text-charcoal-500 font-medium block mb-1.5">Platform Roles</span>
              <div className="flex flex-wrap gap-1.5">
                {user.roles.map((r) => (
                  <span key={r} className="px-2 py-0.5 bg-charcoal-100 text-charcoal-800 rounded font-mono text-xs">
                    {r}
                  </span>
                ))}
              </div>
            </div>

            <div>
              <span className="text-xs text-charcoal-500 font-medium block mb-1">Session Assurance</span>
              <span className="inline-block font-mono text-xs bg-muted-blue-50 text-muted-blue-700 px-2 py-0.5 rounded border border-muted-blue-200">
                {user.assurance}
              </span>
            </div>

            <div className="pt-4 border-t border-sand-200 space-y-2">
              <button
                onClick={logout}
                className="w-full py-2.5 px-4 bg-charcoal-900 hover:bg-charcoal-800 text-white rounded-xl text-sm font-medium transition-colors"
              >
                Sign Out
              </button>

              <button
                onClick={revokeAllSessions}
                className="w-full py-2.5 px-4 border border-terracotta-300 text-terracotta-700 hover:bg-terracotta-50 rounded-xl text-xs font-medium transition-colors"
              >
                Revoke All Active Sessions
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
