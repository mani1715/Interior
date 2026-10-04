'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Users2,
  Shield,
  User,
  AlertTriangle,
  Loader2,
  ArrowRight,
  LogOut,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { getInvitationSession, acceptInvitation } from '@/lib/team/api';
import { ValidateInvitationResponse } from '@/lib/team/types';
import { formatStudioRoleLabel } from '@/lib/team/permissions';

export default function InviteSessionAcceptancePage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading, logout } = useAuth();

  const [session, setSession] = useState<ValidateInvitationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    getInvitationSession()
      .then((res) => {
        setSession(res);
        if (!res.valid) {
          setError(res.error || 'Invitation session is invalid or has expired.');
        }
      })
      .catch((err) => {
        setError(err?.message || 'Failed to load invitation session.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const handleAccept = async () => {
    setAccepting(true);
    setError(null);
    try {
      await acceptInvitation();
      setSuccess(true);
      setTimeout(() => {
        router.push('/workspace');
      }, 1500);
    } catch (err: any) {
      setError(err?.message || 'Failed to accept invitation. Please ensure your signed-in email matches the invite.');
      setAccepting(false);
    }
  };

  const displayedEmail = session?.maskedEmail || session?.invitedEmail;

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex flex-col justify-center items-center p-4 sm:p-6 lg:p-8">
      <div className="max-w-md w-full bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl shadow-xl overflow-hidden">
        {/* Brand Header */}
        <div className="bg-gradient-to-r from-amber-600 to-amber-700 p-6 text-white text-center">
          <div className="w-12 h-12 bg-white/10 backdrop-blur-md rounded-xl flex items-center justify-center mx-auto mb-3 border border-white/20">
            <Users2 className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-xl font-bold tracking-tight">Studio Team Invitation</h1>
          <p className="text-amber-100 text-xs mt-1">
            Join a professional interior design workspace
          </p>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          {loading || authLoading ? (
            <div className="flex flex-col items-center justify-center py-12 text-zinc-500 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-amber-600" />
              <p className="text-sm">Validating invitation credentials...</p>
            </div>
          ) : success ? (
            <div className="text-center py-8 space-y-4">
              <div className="w-14 h-14 bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 rounded-full flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <div className="space-y-1">
                <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
                  Welcome to {session?.studioName}!
                </h3>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  Your membership has been verified. Redirecting to workspace...
                </p>
              </div>
            </div>
          ) : error || !session?.valid ? (
            <div className="text-center py-6 space-y-4">
              <div className="w-12 h-12 bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 rounded-full flex items-center justify-center mx-auto">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
                  Invitation Unavailable
                </h3>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  {error || 'This invitation has expired or has already been used.'}
                </p>
              </div>
              <div className="pt-2">
                <Link
                  href="/"
                  className="inline-flex items-center gap-1.5 text-xs font-medium text-amber-600 hover:text-amber-700"
                >
                  Return to Home
                </Link>
              </div>
            </div>
          ) : (
            <div className="space-y-5">
              {/* Studio & Invitation Details */}
              <div className="bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700 rounded-xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-zinc-500 dark:text-zinc-400">Studio</span>
                  <span className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
                    {session.studioName}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-xs text-zinc-500 dark:text-zinc-400">Invited Role</span>
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                    {session.role === 'DESIGNER_ADMIN' || session.role === 'ADMIN' ? (
                      <>
                        <Shield className="w-3 h-3" /> {formatStudioRoleLabel(session.role)}
                      </>
                    ) : (
                      <>
                        <User className="w-3 h-3" /> {formatStudioRoleLabel(session.role)}
                      </>
                    )}
                  </span>
                </div>
                {displayedEmail && (
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-zinc-500 dark:text-zinc-400">Recipient</span>
                    <span className="text-xs font-mono text-zinc-700 dark:text-zinc-300">
                      {displayedEmail}
                    </span>
                  </div>
                )}
              </div>

              {/* Authentication Conditions */}
              {!isAuthenticated ? (
                <div className="space-y-3 text-center">
                  <p className="text-xs text-zinc-600 dark:text-zinc-400">
                    To accept this invitation and access {session.studioName}, please sign in or register with your invited email address.
                  </p>
                  <Link
                    href="/sign-in?returnUrl=/invite/accept"
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 min-h-[44px] bg-amber-600 hover:bg-amber-700 text-white text-sm font-semibold rounded-xl shadow-sm transition"
                  >
                    Sign In to Accept
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-lg text-xs text-emerald-800 dark:text-emerald-300 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    <span>
                      Signed in as <span className="font-mono font-semibold">{user?.email}</span>
                    </span>
                  </div>

                  {error && (
                    <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-xs text-red-700 dark:text-red-400 flex items-start gap-2">
                      <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                      <span>{error}</span>
                    </div>
                  )}

                  <button
                    type="button"
                    disabled={accepting}
                    onClick={handleAccept}
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 min-h-[44px] bg-amber-600 hover:bg-amber-700 text-white text-sm font-semibold rounded-xl shadow-sm transition disabled:opacity-50"
                  >
                    {accepting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Joining Studio...
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        Accept & Join {session.studioName}
                      </>
                    )}
                  </button>

                  <div className="pt-1 text-center">
                    <button
                      type="button"
                      onClick={async () => {
                        await logout();
                        window.location.href = '/sign-in?returnUrl=/invite/accept';
                      }}
                      className="inline-flex items-center gap-1.5 text-xs text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      Sign in with a different account
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
