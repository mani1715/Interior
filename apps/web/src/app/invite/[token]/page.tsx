'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
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
import { validateInvitation, acceptInvitation, exchangeInvitationToken } from '@/lib/team/api';
import { ValidateInvitationResponse } from '@/lib/team/types';
import { formatStudioRoleLabel } from '@/lib/team/permissions';

export default function InviteAcceptancePage() {
  const params = useParams();
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading, logout } = useAuth();

  const token = typeof params?.token === 'string' ? params.token : '';

  const [validation, setValidation] = useState<ValidateInvitationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      setError('Invalid or missing invitation token.');
      return;
    }

    let isSubscribed = true;

    // Secure token exchange: scrubs raw token from URL & transitions to /invite/accept
    if (typeof exchangeInvitationToken === 'function') {
      try {
        const exchangePromise = exchangeInvitationToken(token);
        if (exchangePromise && typeof exchangePromise.then === 'function') {
          exchangePromise
            .then((exchangeRes) => {
              if (isSubscribed && exchangeRes && exchangeRes.valid) {
                router.replace('/invite/accept');
                return;
              }
            })
            .catch(() => {
              // Proceed with direct validation fallback
            });
        }
      } catch {
        // Proceed with direct validation fallback
      }
    }

    validateInvitation(token)
      .then((res) => {
        if (!isSubscribed) return;
        setValidation(res);
        if (!res.valid) {
          setError(res.error || 'Invitation is invalid or has expired.');
        }
      })
      .catch((err) => {
        if (!isSubscribed) return;
        setError(err?.message || 'Failed to validate invitation.');
      })
      .finally(() => {
        if (isSubscribed) {
          setLoading(false);
        }
      });

    return () => {
      isSubscribed = false;
    };
  }, [token, router]);

  const handleAccept = async () => {
    if (!token) return;
    setAccepting(true);
    setError(null);
    try {
      await acceptInvitation({ token });
      setSuccess(true);
      setTimeout(() => {
        router.push('/workspace');
      }, 1500);
    } catch (err: any) {
      setError(err?.message || 'Failed to accept invitation. Please try again.');
      setAccepting(false);
    }
  };

  const displayedEmail = validation?.maskedEmail || validation?.invitedEmail;

  const isEmailMismatch =
    isAuthenticated &&
    user?.email &&
    validation?.invitedEmail &&
    user.email.toLowerCase() !== validation.invitedEmail.toLowerCase();

  return (
    <div className="min-h-screen bg-[#FAF8F5] flex flex-col justify-center items-center p-4 sm:p-6 lg:p-8">
      <div className="max-w-md w-full bg-white border border-sand-200 rounded-2xl shadow-xl overflow-hidden">
        {/* Brand Header */}
        <div className="bg-gradient-to-r from-bronze-700 to-bronze-800 p-6 text-white text-center">
          <div className="w-12 h-12 bg-white/10 backdrop-blur-md rounded-xl flex items-center justify-center mx-auto mb-3 border border-white/20">
            <Users2 className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-xl font-bold tracking-tight">Studio Team Invitation</h1>
          <p className="text-bronze-100 text-xs mt-1">
            Join a professional interior design workspace
          </p>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          {loading || authLoading ? (
            <div className="flex flex-col items-center justify-center py-12 text-charcoal-500 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-bronze-700" />
              <p className="text-sm">Validating invitation credentials...</p>
            </div>
          ) : success ? (
            <div className="text-center py-8 space-y-4">
              <div className="w-14 h-14 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <div className="space-y-1">
                <h3 className="text-lg font-bold text-charcoal-900">
                  Welcome to {validation?.studioName}!
                </h3>
                <p className="text-xs text-charcoal-500">
                  Your membership has been verified. Redirecting to workspace...
                </p>
              </div>
            </div>
          ) : error || !validation?.valid ? (
            <div className="text-center py-6 space-y-4">
              <div className="w-12 h-12 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-bold text-charcoal-900">
                  Invitation Unavailable
                </h3>
                <p className="text-xs text-charcoal-500">
                  {error || 'This invitation has expired or has already been used.'}
                </p>
              </div>
              <div className="pt-2">
                <Link
                  href="/"
                  className="inline-flex items-center gap-1.5 text-xs font-medium text-bronze-700 hover:text-bronze-800"
                >
                  Return to Home
                </Link>
              </div>
            </div>
          ) : (
            <div className="space-y-5">
              {/* Studio & Invitation Details */}
              <div className="bg-sand-50 border border-sand-200 rounded-xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-charcoal-500">Studio</span>
                  <span className="text-sm font-bold text-charcoal-900">
                    {validation.studioName}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-xs text-charcoal-500">Invited Role</span>
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full bg-bronze-100 text-bronze-900">
                    {validation.role === 'DESIGNER_ADMIN' || validation.role === 'ADMIN' ? (
                      <>
                        <Shield className="w-3 h-3" /> {formatStudioRoleLabel(validation.role)}
                      </>
                    ) : (
                      <>
                        <User className="w-3 h-3" /> {formatStudioRoleLabel(validation.role)}
                      </>
                    )}
                  </span>
                </div>
                {displayedEmail && (
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-charcoal-500">Recipient</span>
                    <span className="text-xs font-mono text-charcoal-700">
                      {displayedEmail}
                    </span>
                  </div>
                )}
              </div>

              {/* Authentication Conditions */}
              {!isAuthenticated ? (
                <div className="space-y-3 text-center">
                  <p className="text-xs text-charcoal-600">
                    To accept this invitation and access {validation.studioName}, please sign in or register with your invited email address.
                  </p>
                  <Link
                    href="/sign-in?returnUrl=/invite/accept"
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 min-h-[44px] bg-bronze-700 hover:bg-bronze-800 text-white text-sm font-semibold rounded-xl shadow-sm transition"
                  >
                    Sign In to Accept
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </div>
              ) : isEmailMismatch ? (
                <div className="space-y-4">
                  <div className="p-3.5 bg-bronze-50 border border-bronze-200 rounded-lg text-xs text-bronze-900 space-y-2">
                    <div className="flex items-center gap-2 font-semibold">
                      <AlertTriangle className="w-4 h-4 text-bronze-700 flex-shrink-0" />
                      Email Address Mismatch
                    </div>
                    <p>
                      You are currently signed in as{' '}
                      <span className="font-mono font-bold text-amber-950">
                        {user?.email}
                      </span>
                      , but this invitation was issued to{' '}
                      <span className="font-mono font-bold text-amber-950">
                        {displayedEmail}
                      </span>
                      .
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={async () => {
                      await logout();
                      window.location.href = '/sign-in?returnUrl=/invite/accept';
                    }}
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 min-h-[44px] border border-sand-300 hover:bg-sand-50 :bg-sand-100 text-charcoal-700 text-xs font-semibold rounded-lg transition"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    Switch Account to {displayedEmail}
                  </button>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    <span>
                      Authenticated as <span className="font-mono font-semibold">{user?.email}</span>.
                    </span>
                  </div>

                  <button
                    type="button"
                    disabled={accepting}
                    onClick={handleAccept}
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 min-h-[44px] bg-bronze-700 hover:bg-bronze-800 text-white text-sm font-semibold rounded-xl shadow-sm transition disabled:opacity-50"
                  >
                    {accepting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Joining Studio...
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        Accept & Join {validation.studioName}
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
