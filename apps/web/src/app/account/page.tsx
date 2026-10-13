'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  User,
  Shield,
  MessageSquare,
  Lock,
  Trash2,
  AlertTriangle,
  ExternalLink,
  Laptop,
  CheckCircle,
  Clock,
  Sparkles,
  Sliders,
  Send,
  Building2,
  Download,
  FileText,
  CheckCircle2,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/auth-context';
import {
  fetchActiveSessions,
  revokeActiveSession,
  revokeAllOtherSessions,
  fetchCustomerInquiries,
  updateAccountProfile,
  requestAccountErasure,
} from '@/lib/account/api';
import {
  fetchMyPrivacyRequests,
  triggerUserDataExport,
  closeAccountSelfService,
  deleteAccountSelfService,
  getExportDownloadUrl,
  PrivacyRequestDto,
  DataExportDto,
} from '@/lib/privacy/api';
import {
  fetchMfaStatus,
  startMfaEnrollment,
  confirmMfaEnrollment,
  regenerateRecoveryCodes,
  disableMfa,
  MfaStatusDto,
  MfaEnrollmentDto,
} from '@/lib/auth/mfa-api';
import {
  fetchNotificationPreferences,
  updateNotificationPreferences,
} from '@/lib/notifications/api';
import { formatStudioRoleLabel } from '@/lib/team/permissions';

interface ActiveSession {
  id: string;
  deviceLabel: string;
  authTime: string;
  lastSeenAt: string;
  idleExpiresAt: string;
  current: boolean;
}

interface CustomerInquiry {
  id: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  projectCategory?: string;
  budgetRange?: string;
  message: string;
  status: string;
  createdAt: string;
}

interface NotificationPreferences {
  inAppEnabled: boolean;
  emailEnabled: boolean;
  whatsappEnabled: boolean;
  leadNotifications: boolean;
  reviewNotifications: boolean;
  aiNotifications: boolean;
  systemNotifications: boolean;
}

export default function AccountPage() {
  const router = useRouter();
  const { user, isLoading, isAuthenticated, logout, revokeAllSessions } = useAuth();

  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'inquiries' | 'preferences' | 'privacy'>('profile');

  // Edit profile state
  const [displayName, setDisplayName] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);

  // Sessions state
  const [sessions, setSessions] = useState<ActiveSession[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(false);

  // Inquiries state
  const [inquiries, setInquiries] = useState<CustomerInquiry[]>([]);
  const [loadingInquiries, setLoadingInquiries] = useState(false);

  // Preferences state
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    inAppEnabled: true,
    emailEnabled: false,
    whatsappEnabled: false,
    leadNotifications: true,
    reviewNotifications: true,
    aiNotifications: true,
    systemNotifications: true,
  });
  const [isSavingPrefs, setIsSavingPrefs] = useState(false);
  const [prefsSuccess, setPrefsSuccess] = useState<string | null>(null);

  // MFA state
  const [mfaStatus, setMfaStatus] = useState<MfaStatusDto | null>(null);
  const [loadingMfa, setLoadingMfa] = useState(false);
  const [enrollmentData, setEnrollmentData] = useState<MfaEnrollmentDto | null>(null);
  const [enrollmentCode, setEnrollmentCode] = useState('');
  const [mfaError, setMfaError] = useState<string | null>(null);
  const [mfaSuccess, setMfaSuccess] = useState<string | null>(null);
  const [newRecoveryCodes, setNewRecoveryCodes] = useState<string[] | null>(null);
  const [isEnrolling, setIsEnrolling] = useState(false);
  const [isConfirmingMfa, setIsConfirmingMfa] = useState(false);
  const [isDisablingMfa, setIsDisablingMfa] = useState(false);
  const [isRegeneratingCodes, setIsRegeneratingCodes] = useState(false);
  const [revokingOtherSessions, setRevokingOtherSessions] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/sign-in?returnUrl=/account');
    }
  }, [isLoading, isAuthenticated, router]);

  useEffect(() => {
    if (user) {
      setDisplayName(user.displayName || '');
      setPhone('');
    }
  }, [user]);

  // Load sessions and MFA when switching to security
  useEffect(() => {
    if (activeTab === 'security' && isAuthenticated) {
      setLoadingSessions(true);
      fetchActiveSessions()
        .then(data => setSessions(data))
        .catch(() => {})
        .finally(() => setLoadingSessions(false));

      setLoadingMfa(true);
      fetchMfaStatus()
        .then(status => setMfaStatus(status))
        .catch(() => {})
        .finally(() => setLoadingMfa(false));
    }
  }, [activeTab, isAuthenticated]);

  const loadMfaStatus = async () => {
    try {
      const status = await fetchMfaStatus();
      setMfaStatus(status);
    } catch {
      // Ignore
    }
  };

  const handleStartEnrollment = async () => {
    setMfaError(null);
    setMfaSuccess(null);
    setIsEnrolling(true);
    try {
      const res = await startMfaEnrollment();
      setEnrollmentData(res);
      setEnrollmentCode('');
    } catch (err: any) {
      setMfaError(err.message || 'Failed to start MFA enrollment');
    } finally {
      setIsEnrolling(false);
    }
  };

  const handleConfirmEnrollment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!enrollmentCode.trim()) return;
    setMfaError(null);
    setIsConfirmingMfa(true);
    try {
      const res = await confirmMfaEnrollment(enrollmentCode.trim());
      setNewRecoveryCodes(res.recoveryCodes);
      setEnrollmentData(null);
      setEnrollmentCode('');
      setMfaSuccess('Multi-factor authentication enabled successfully.');
      await loadMfaStatus();
    } catch (err: any) {
      setMfaError(err.message || 'Invalid verification code. Please check your authenticator application.');
    } finally {
      setIsConfirmingMfa(false);
    }
  };

  const handleRegenerateCodes = async () => {
    if (!window.confirm('Regenerating recovery codes will invalidate all existing unused recovery codes. Continue?')) {
      return;
    }
    setMfaError(null);
    setIsRegeneratingCodes(true);
    try {
      const res = await regenerateRecoveryCodes();
      setNewRecoveryCodes(res.recoveryCodes);
      setMfaSuccess('Backup recovery codes regenerated successfully.');
      await loadMfaStatus();
    } catch (err: any) {
      setMfaError(err.message || 'Failed to regenerate recovery codes');
    } finally {
      setIsRegeneratingCodes(false);
    }
  };

  const handleDisableMfa = async () => {
    if (!window.confirm('Are you sure you want to disable multi-factor authentication? Elevated administrative capabilities will be blocked until re-enabled.')) {
      return;
    }
    setMfaError(null);
    setIsDisablingMfa(true);
    try {
      await disableMfa();
      setNewRecoveryCodes(null);
      setMfaSuccess('Multi-factor authentication disabled.');
      await loadMfaStatus();
    } catch (err: any) {
      setMfaError(err.message || 'Failed to disable multi-factor authentication');
    } finally {
      setIsDisablingMfa(false);
    }
  };

  const handleRevokeOtherSessions = async () => {
    if (!window.confirm('Terminate all other active sessions across devices?')) {
      return;
    }
    setRevokingOtherSessions(true);
    try {
      await revokeAllOtherSessions();
      const updated = await fetchActiveSessions();
      setSessions(updated);
    } catch {
      // Ignore
    } finally {
      setRevokingOtherSessions(false);
    }
  };

  // Load customer inquiries when switching to inquiries
  useEffect(() => {
    if (activeTab === 'inquiries' && isAuthenticated) {
      setLoadingInquiries(true);
      fetchCustomerInquiries()
        .then(data => setInquiries(data as any))
        .catch(() => {})
        .finally(() => setLoadingInquiries(false));
    }
  }, [activeTab, isAuthenticated]);

  // Load preferences when switching to preferences
  useEffect(() => {
    if (activeTab === 'preferences' && isAuthenticated) {
      fetchNotificationPreferences()
        .then(data => {
          if (data) setPreferences(data as any);
        })
        .catch(() => {});
    }
  }, [activeTab, isAuthenticated]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingProfile(true);
    setProfileSuccess(null);
    try {
      await updateAccountProfile({ displayName, phone, avatarUrl });
      setProfileSuccess('Profile updated successfully.');
    } catch {
      // Ignore
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleRevokeSession = async (sessionId: string) => {
    try {
      await revokeActiveSession(sessionId);
      setSessions(prev => prev.filter(s => s.id !== sessionId));
    } catch {
      // Ignore
    }
  };

  const handleSavePreferences = async () => {
    setIsSavingPrefs(true);
    setPrefsSuccess(null);
    try {
      await updateNotificationPreferences(preferences);
      setPrefsSuccess('Notification preferences saved.');
    } catch {
      // Ignore
    } finally {
      setIsSavingPrefs(false);
    }
  };

  // Privacy requests state
  const [privacyRequests, setPrivacyRequests] = useState<PrivacyRequestDto[]>([]);
  const [loadingPrivacy, setLoadingPrivacy] = useState(false);
  const [isExportingData, setIsExportingData] = useState(false);
  const [exportDataResult, setExportDataResult] = useState<DataExportDto | null>(null);
  const [privacyError, setPrivacyError] = useState<string | null>(null);
  const [isClosingAccount, setIsClosingAccount] = useState(false);
  const [accountClosed, setAccountClosed] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [deletionSuccess, setDeletionSuccess] = useState(false);

  // Load privacy requests when switching to privacy tab
  useEffect(() => {
    if (activeTab === 'privacy' && isAuthenticated) {
      setLoadingPrivacy(true);
      setPrivacyError(null);
      fetchMyPrivacyRequests()
        .then(data => setPrivacyRequests(data))
        .catch(() => {})
        .finally(() => setLoadingPrivacy(false));
    }
  }, [activeTab, isAuthenticated]);

  const handleTriggerUserDataExport = async () => {
    setIsExportingData(true);
    setPrivacyError(null);
    try {
      const res = await triggerUserDataExport();
      setExportDataResult(res);
      const updated = await fetchMyPrivacyRequests();
      setPrivacyRequests(updated);
    } catch (err: any) {
      setPrivacyError(err.message || 'Failed to generate data export.');
    } finally {
      setIsExportingData(false);
    }
  };

  const handleCloseAccount = async () => {
    if (!window.confirm('Are you sure you want to close your account? Your active sessions will be terminated and your account status will be set to suspended.')) {
      return;
    }
    setIsClosingAccount(true);
    setPrivacyError(null);
    try {
      await closeAccountSelfService();
      setAccountClosed(true);
      setTimeout(() => {
        logout();
        router.push('/');
      }, 3000);
    } catch (err: any) {
      setPrivacyError(err.message || 'Account closure blocked.');
    } finally {
      setIsClosingAccount(false);
    }
  };

  const handleRequestDeletion = async () => {
    if (!window.confirm('DANGER: Are you sure you want to permanently delete and anonymize your account? This action cannot be reversed.')) {
      return;
    }
    setIsDeletingAccount(true);
    setPrivacyError(null);
    try {
      await deleteAccountSelfService();
      setDeletionSuccess(true);
      setTimeout(() => {
        logout();
        router.push('/');
      }, 3000);
    } catch (err: any) {
      setPrivacyError(err.message || 'Account deletion failed.');
    } finally {
      setIsDeletingAccount(false);
    }
  };

  if (isLoading || !user) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-bronze-200 border-t-bronze-600 rounded-full animate-spin" />
      </div>
    );
  }

  const isProfessional = user.roles.includes('DESIGNER') || user.roles.includes('DESIGNER_TEAM');

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 space-y-8">
      {/* Page Header */}
      <div>
        <span className="text-xs font-semibold tracking-widest uppercase text-bronze-700 block mb-1">
          Identity & Preferences
        </span>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="font-serif text-3xl sm:text-4xl text-charcoal-900 tracking-tight">
              User Account Settings
            </h1>
            <p className="text-sm text-charcoal-600 mt-1">
              Manage personal identity, active sessions, privacy settings, and inquiries.
            </p>
          </div>

          {isProfessional && (
            <div className="flex items-center gap-2">
              <Link
                href="/workspace/business"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 border border-bronze-300 bg-bronze-50 text-bronze-900 rounded-xl text-xs font-medium hover:bg-bronze-100 transition-colors shadow-2xs"
              >
                <Building2 className="w-4 h-4 text-bronze-700" />
                <span>Go to Studio / Business Profile →</span>
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-sand-200 overflow-x-auto pb-1 text-xs font-medium">
        {[
          { id: 'profile', label: 'Personal Profile', icon: User },
          { id: 'security', label: 'Security & Sessions', icon: Shield },
          { id: 'inquiries', label: 'My Inquiries', icon: MessageSquare },
          { id: 'preferences', label: 'Notification Preferences', icon: Sliders },
          { id: 'privacy', label: 'Privacy & Data Rights', icon: Lock },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-t-xl transition-colors whitespace-nowrap ${
                isActive
                  ? 'border-b-2 border-bronze-700 font-semibold text-charcoal-900 bg-sand-100/60'
                  : 'text-charcoal-600 hover:text-charcoal-900 hover:bg-sand-50'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-bronze-700' : 'text-charcoal-500'}`} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* TAB CONTENT */}

      {/* 1. PERSONAL PROFILE */}
      {activeTab === 'profile' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between pb-3 border-b border-sand-200 mb-4">
                <div>
                  <h2 className="font-serif text-lg text-charcoal-900">
                    {user.displayName}
                  </h2>
                  <p className="text-xs text-charcoal-500 font-mono mt-0.5">
                    {user.email || 'None associated'}
                  </p>
                </div>
                <span className="text-xs font-semibold px-2.5 py-1 bg-forest-50 text-forest-700 border border-forest-200 rounded-lg">
                  {user.status}
                </span>
              </div>

              <form onSubmit={handleSaveProfile} className="space-y-4">
                {profileSuccess && (
                  <div className="p-3 bg-forest-50 border border-forest-200 rounded-xl text-forest-800 text-xs flex items-center gap-2">
                    <CheckCircle className="w-4 h-4 text-forest-600" />
                    <span>{profileSuccess}</span>
                  </div>
                )}

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Display Name
                  </label>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    required
                    className="w-full text-sm bg-sand-50/50 border border-sand-300 rounded-xl px-3.5 py-2.5 text-charcoal-900 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Email Address (Identity-bound)
                  </label>
                  <input
                    type="email"
                    value={user.email || 'None associated'}
                    disabled
                    className="w-full text-sm bg-sand-100 border border-sand-200 rounded-xl px-3.5 py-2.5 text-charcoal-500 cursor-not-allowed"
                  />
                  <p className="text-[11px] text-charcoal-500 mt-1">
                    Email verification and account ownership is tied to your identity provider.
                  </p>
                </div>

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Phone Number (Optional)
                  </label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 98765 43210"
                    className="w-full text-sm bg-sand-50/50 border border-sand-300 rounded-xl px-3.5 py-2.5 text-charcoal-900 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Profile Avatar URL (Optional)
                  </label>
                  <input
                    type="url"
                    value={avatarUrl}
                    onChange={(e) => setAvatarUrl(e.target.value)}
                    placeholder="https://..."
                    className="w-full text-sm bg-sand-50/50 border border-sand-300 rounded-xl px-3.5 py-2.5 text-charcoal-900 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                  />
                </div>

                <div className="pt-2">
                  <Button type="submit" variant="primary" size="sm" disabled={isSavingProfile}>
                    {isSavingProfile ? 'Saving...' : 'Save Profile Changes'}
                  </Button>
                </div>
              </form>
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
                          Role: {formatStudioRoleLabel(s.role)}
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
                  <div className="p-4 bg-bronze-50/60 rounded-xl border border-bronze-200 text-xs text-bronze-950 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <p className="font-semibold text-charcoal-900">Designer Workspace</p>
                      <p className="text-[11px] text-charcoal-600 mt-0.5">
                        Manage your published portfolio, projects, media library, reviews, and client leads.
                      </p>
                    </div>
                    <Link
                      href="/workspace"
                      className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-charcoal-900 text-white rounded-lg text-xs font-medium hover:bg-charcoal-800 transition-colors whitespace-nowrap self-start sm:self-auto"
                    >
                      <span>Open Workspace</span>
                      <span>↗</span>
                    </Link>
                  </div>

                  <div className="p-4 bg-sand-50 rounded-xl border border-sand-200 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <p className="font-semibold text-charcoal-900">Private Mood Boards</p>
                      <p className="text-[11px] text-charcoal-600 mt-0.5">
                        Curate design inspiration and saved projects. Completely private to you.
                      </p>
                    </div>
                    <Link
                      href="/account/collections"
                      className="inline-flex items-center gap-1.5 px-3.5 py-1.5 border border-sand-300 text-charcoal-800 bg-white rounded-lg text-xs font-medium hover:bg-sand-50 transition-colors whitespace-nowrap self-start sm:self-auto"
                    >
                      <span>View Collections</span>
                    </Link>
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

          <div className="space-y-6">
            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
              <h2 className="font-serif text-lg text-charcoal-900 pb-3 border-b border-sand-200">
                Security Context
              </h2>

              <div>
                <span className="text-xs text-charcoal-500 font-medium block mb-1">User Identifier</span>
                <span className="font-mono text-xs text-charcoal-700 break-all">{user.id}</span>
              </div>

              <div>
                <span className="text-xs text-charcoal-500 font-medium block mb-1">Account Status</span>
                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-forest-50 text-forest-700">
                  {user.status}
                </span>
              </div>

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
                  type="button"
                  onClick={logout}
                  className="w-full py-2.5 px-4 bg-charcoal-900 hover:bg-charcoal-800 text-white rounded-xl text-sm font-medium transition-colors"
                >
                  Sign Out
                </button>

                <button
                  type="button"
                  onClick={revokeAllSessions}
                  className="w-full py-2.5 px-4 border border-terracotta-300 text-terracotta-700 hover:bg-terracotta-50 rounded-xl text-xs font-medium transition-colors"
                >
                  Revoke All Active Sessions
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 2. SECURITY & SESSIONS */}
      {activeTab === 'security' && (
        <div className="space-y-6">
          {/* Notifications / Alerts */}
          {mfaError && (
            <div className="p-4 bg-terracotta-50 border border-terracotta-200 rounded-xl text-xs text-terracotta-800 flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 text-terracotta-600 shrink-0 mt-0.5" />
              <span>{mfaError}</span>
            </div>
          )}
          {mfaSuccess && (
            <div className="p-4 bg-forest-50 border border-forest-200 rounded-xl text-xs text-forest-800 flex items-start gap-2">
              <CheckCircle className="w-4 h-4 text-forest-600 shrink-0 mt-0.5" />
              <span>{mfaSuccess}</span>
            </div>
          )}

          {/* New Recovery Codes Modal / Banner */}
          {newRecoveryCodes && (
            <div className="p-5 bg-sand-50 border-2 border-bronze-400 rounded-2xl space-y-3">
              <div className="flex items-center gap-2">
                <Lock className="w-5 h-5 text-bronze-700" />
                <h3 className="font-serif text-base font-bold text-charcoal-900">
                  Save Your One-Time Backup Recovery Codes
                </h3>
              </div>
              <p className="text-xs text-charcoal-700">
                These recovery codes are your emergency key if you lose access to your authenticator app. Store them safely. Each code can be used exactly once.
              </p>
              <div className="p-4 bg-white rounded-xl border border-sand-300 font-mono text-xs grid grid-cols-2 sm:grid-cols-5 gap-2 text-center text-charcoal-900 select-all">
                {newRecoveryCodes.map((c, i) => (
                  <span key={i} className="p-1.5 bg-sand-100 rounded">
                    {c}
                  </span>
                ))}
              </div>
              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard?.writeText(newRecoveryCodes.join('\n'));
                    alert('Recovery codes copied to clipboard');
                  }}
                  className="px-3 py-1.5 bg-charcoal-900 text-white rounded-lg text-xs font-medium hover:bg-charcoal-800 transition-colors"
                >
                  Copy Codes
                </button>
                <button
                  type="button"
                  onClick={() => setNewRecoveryCodes(null)}
                  className="px-3 py-1.5 border border-sand-300 text-charcoal-700 rounded-lg text-xs font-medium hover:bg-sand-50 transition-colors"
                >
                  I Have Saved These Codes
                </button>
              </div>
            </div>
          )}

          {/* MFA Management Card */}
          <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-sand-200">
              <div>
                <h2 className="font-serif text-lg text-charcoal-900">
                  Multi-Factor Authentication (MFA)
                </h2>
                <p className="text-xs text-charcoal-600 mt-1">
                  Protect privileged actions with time-based one-time passwords (TOTP, RFC 6238).
                </p>
              </div>

              {mfaStatus?.mfaEnabled ? (
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-forest-100 text-forest-800">
                    <CheckCircle className="w-3.5 h-3.5 text-forest-700" />
                    <span>Active (Authenticator App)</span>
                  </span>
                  <button
                    type="button"
                    onClick={handleDisableMfa}
                    disabled={isDisablingMfa}
                    className="px-3 py-1 border border-sand-300 text-charcoal-700 hover:bg-terracotta-50 hover:text-terracotta-700 rounded-lg text-xs font-medium transition-colors"
                  >
                    {isDisablingMfa ? 'Disabling...' : 'Disable'}
                  </button>
                </div>
              ) : (
                <div>
                  <button
                    type="button"
                    onClick={handleStartEnrollment}
                    disabled={isEnrolling || !!enrollmentData}
                    className="px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors"
                  >
                    {isEnrolling ? 'Generating Secret...' : 'Set Up Two-Factor Auth'}
                  </button>
                </div>
              )}
            </div>

            {/* Pending Enrollment Setup Form */}
            {enrollmentData && (
              <div className="p-5 bg-sand-50 rounded-xl border border-sand-300 space-y-4">
                <div>
                  <h3 className="font-serif text-sm font-bold text-charcoal-900">
                    Step 1: Scan Authenticator QR / Enter Secret
                  </h3>
                  <p className="text-xs text-charcoal-600 mt-1">
                    Open Google Authenticator, 1Password, or any RFC 6238 compatible app and configure your token:
                  </p>
                </div>

                <div className="space-y-2">
                  <span className="text-[11px] font-medium text-charcoal-500 uppercase tracking-wider block">
                    Base32 Manual Entry Key
                  </span>
                  <div className="p-3 bg-white border border-sand-300 rounded-lg font-mono text-xs tracking-wider text-charcoal-900 select-all">
                    {enrollmentData.secretBase32}
                  </div>
                  <span className="text-[10px] text-charcoal-500 block">
                    Account: {user.email} | Issuer: Elégance
                  </span>
                </div>

                <form onSubmit={handleConfirmEnrollment} className="space-y-3 pt-2 border-t border-sand-200">
                  <label className="text-xs font-semibold text-charcoal-900 block">
                    Step 2: Enter 6-digit confirmation code from your app
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      maxLength={6}
                      placeholder="123456"
                      value={enrollmentCode}
                      onChange={(e) => setEnrollmentCode(e.target.value.replace(/\D/g, ''))}
                      className="px-3.5 py-2 border border-sand-300 rounded-lg font-mono text-base tracking-widest text-center w-36 focus:outline-none focus:ring-1 focus:ring-bronze-500"
                    />
                    <button
                      type="submit"
                      disabled={isConfirmingMfa || enrollmentCode.length !== 6}
                      className="px-4 py-2 bg-charcoal-900 text-white rounded-lg text-xs font-medium hover:bg-charcoal-800 disabled:opacity-50 transition-colors"
                    >
                      {isConfirmingMfa ? 'Verifying...' : 'Confirm & Activate'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setEnrollmentData(null)}
                      className="px-3 py-2 border border-sand-300 text-charcoal-600 rounded-lg text-xs font-medium hover:bg-white transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Active MFA Details */}
            {mfaStatus?.mfaEnabled && (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
                <div className="p-3 bg-sand-50 rounded-xl border border-sand-200">
                  <span className="text-[11px] text-charcoal-500 block">Unused Backup Codes</span>
                  <span className="font-mono text-sm font-bold text-charcoal-900">
                    {mfaStatus.unusedRecoveryCodesCount} remaining
                  </span>
                  <button
                    type="button"
                    onClick={handleRegenerateCodes}
                    disabled={isRegeneratingCodes}
                    className="text-[11px] font-medium text-bronze-700 hover:text-bronze-900 mt-2 block"
                  >
                    {isRegeneratingCodes ? 'Regenerating...' : 'Regenerate New Codes'}
                  </button>
                </div>

                <div className="p-3 bg-sand-50 rounded-xl border border-sand-200">
                  <span className="text-[11px] text-charcoal-500 block">Session Assurance</span>
                  <span className="font-mono text-sm font-bold text-forest-700">
                    {mfaStatus.sessionMfaVerified ? 'MFA_VERIFIED' : 'PRIMARY_AUTHENTICATED'}
                  </span>
                  <span className="text-[10px] text-charcoal-500 block mt-1">
                    {mfaStatus.stepUpValidUntil ? `Step-up active until ${new Date(mfaStatus.stepUpValidUntil).toLocaleTimeString()}` : 'Standard session'}
                  </span>
                </div>

                <div className="p-3 bg-sand-50 rounded-xl border border-sand-200">
                  <span className="text-[11px] text-charcoal-500 block">Role MFA Policy</span>
                  <span className="font-mono text-sm font-bold text-charcoal-900">
                    {mfaStatus.mfaRequiredForRole ? 'MANDATORY (ADMIN)' : 'OPTIONAL'}
                  </span>
                  <span className="text-[10px] text-charcoal-500 block mt-1">
                    Enforced on privileged operations
                  </span>
                </div>
              </div>
            )}
          </div>

          <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-6">
            <div>
              <h2 className="font-serif text-lg text-charcoal-900 pb-2 border-b border-sand-200">
                Password & Authentication Architecture
              </h2>
              <div className="p-4 bg-sand-50 rounded-xl border border-sand-200 mt-4 text-xs text-charcoal-700 leading-relaxed">
                <p className="font-semibold text-charcoal-900 mb-1">Managed via External Identity Provider</p>
                Platform primary authentication is handled securely through your configured Single Sign-On (Google / OIDC PKCE) identity provider. Multi-factor authentication adds an independent cryptographic layer with encrypted TOTP secrets and backup recovery codes.
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between pb-3 border-b border-sand-200">
                <div>
                  <h3 className="font-serif text-base text-charcoal-900">
                    Active Cryptographic Sessions
                  </h3>
                  <p className="text-[11px] text-charcoal-500">
                    30-minute idle sliding expiration / 12-hour maximum lifetime
                  </p>
                </div>
                <button
                  onClick={handleRevokeOtherSessions}
                  disabled={revokingOtherSessions}
                  className="px-3 py-1.5 border border-terracotta-300 text-terracotta-700 hover:bg-terracotta-50 rounded-xl text-xs font-medium transition-colors"
                >
                  {revokingOtherSessions ? 'Terminating...' : 'Revoke All Other Sessions'}
                </button>
              </div>

              <div className="mt-4 divide-y divide-sand-100">
                {loadingSessions ? (
                  <div className="p-6 text-center text-xs text-charcoal-500">
                    Loading session security data...
                  </div>
                ) : sessions.length === 0 ? (
                  <div className="p-6 text-center text-xs text-charcoal-500">
                    Current session active.
                  </div>
                ) : (
                  sessions.map((sess) => (
                    <div key={sess.id} className="py-3 flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-sand-100 text-charcoal-600 flex items-center justify-center">
                          <Laptop className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-semibold text-charcoal-900">{sess.deviceLabel}</span>
                            {sess.current && (
                              <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-forest-100 text-forest-800">
                                This device
                              </span>
                            )}
                          </div>
                          <span className="text-[11px] text-charcoal-500">
                            Last active: {new Date(sess.lastSeenAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                          </span>
                        </div>
                      </div>

                      {!sess.current && (
                        <button
                          onClick={() => handleRevokeSession(sess.id)}
                          className="text-xs font-medium text-terracotta-700 hover:text-terracotta-900"
                        >
                          Revoke
                        </button>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 3. MY INQUIRIES */}
      {activeTab === 'inquiries' && (
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="pb-3 border-b border-sand-200 flex items-center justify-between">
            <div>
              <h2 className="font-serif text-lg text-charcoal-900">
                My Submitted Inquiries
              </h2>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Inquiries and consultation requests you sent to verified interior professionals.
              </p>
            </div>
          </div>

          {loadingInquiries ? (
            <div className="p-8 text-center text-xs text-charcoal-500">
              Loading your inquiries...
            </div>
          ) : inquiries.length === 0 ? (
            <div className="p-10 text-center space-y-2">
              <MessageSquare className="w-8 h-8 text-charcoal-400 mx-auto" />
              <h3 className="font-serif text-sm font-semibold text-charcoal-900">No Inquiries Found</h3>
              <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
                You have not submitted any design consultations yet. Explore verified professionals to get in touch.
              </p>
              <div className="pt-2">
                <Link
                  href="/professionals"
                  className="inline-flex items-center gap-1.5 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors"
                >
                  <span>Explore Designers</span>
                  <span>↗</span>
                </Link>
              </div>
            </div>
          ) : (
            <div className="divide-y divide-sand-100">
              {inquiries.map((inq) => (
                <div key={inq.id} className="py-4 space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <Link
                        href={`/designers/${inq.studioSlug}`}
                        className="font-serif text-sm font-bold text-charcoal-900 hover:text-bronze-700"
                      >
                        {inq.studioName}
                      </Link>
                      <span className="text-[11px] text-charcoal-500 ml-2">
                        {inq.projectCategory || 'General Inquiry'}
                      </span>
                    </div>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-sand-100 text-charcoal-700">
                      {inq.status}
                    </span>
                  </div>
                  <p className="text-xs text-charcoal-600 bg-sand-50/70 p-3 rounded-xl border border-sand-200">
                    {inq.message}
                  </p>
                  <div className="flex items-center justify-between text-[11px] text-charcoal-400">
                    <span>Submitted {new Date(inq.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                    {inq.budgetRange && <span>Budget: {inq.budgetRange}</span>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 4. NOTIFICATION PREFERENCES */}
      {activeTab === 'preferences' && (
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-6">
          <div>
            <h2 className="font-serif text-lg text-charcoal-900 pb-2 border-b border-sand-200">
              Communication & Alert Channels
            </h2>
            <p className="text-xs text-charcoal-500 mt-1">
              Select how you want to be alerted for studio inquiries, reviews, and platform updates.
            </p>
          </div>

          {prefsSuccess && (
            <div className="p-3 bg-forest-50 border border-forest-200 rounded-xl text-forest-800 text-xs flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-forest-600" />
              <span>{prefsSuccess}</span>
            </div>
          )}

          <div className="space-y-4">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-charcoal-600">
              Delivery Channels
            </h3>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">In-App Notifications</span>
                <span className="text-[11px] text-charcoal-500">Show notification bell alerts and unread counters</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.inAppEnabled}
                onChange={(e) => setPreferences({ ...preferences, inAppEnabled: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">Email Notifications</span>
                <span className="text-[11px] text-charcoal-500">Receive lead summaries and review alerts via email (when configured)</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.emailEnabled}
                onChange={(e) => setPreferences({ ...preferences, emailEnabled: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">WhatsApp Direct Alerts</span>
                <span className="text-[11px] text-charcoal-500">Receive instant WhatsApp alerts for urgent customer leads</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.whatsappEnabled}
                onChange={(e) => setPreferences({ ...preferences, whatsappEnabled: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>
          </div>

          <div className="space-y-4 pt-4 border-t border-sand-200">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-charcoal-600">
              Notification Categories
            </h3>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">Client Leads & Inquiries</span>
                <span className="text-[11px] text-charcoal-500">Alerts when new customer inquiries are received</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.leadNotifications}
                onChange={(e) => setPreferences({ ...preferences, leadNotifications: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">Reviews & Testimonials</span>
                <span className="text-[11px] text-charcoal-500">Alerts when clients submit verified reviews</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.reviewNotifications}
                onChange={(e) => setPreferences({ ...preferences, reviewNotifications: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>

            <label className="flex items-center justify-between p-3.5 bg-sand-50/60 rounded-xl border border-sand-200 cursor-pointer">
              <div>
                <span className="text-xs font-semibold text-charcoal-900 block">AI Spatial Visualizer</span>
                <span className="text-[11px] text-charcoal-500">Alerts when render generation completes or client approves concepts</span>
              </div>
              <input
                type="checkbox"
                checked={preferences.aiNotifications}
                onChange={(e) => setPreferences({ ...preferences, aiNotifications: e.target.checked })}
                className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-500"
              />
            </label>
          </div>

          <div className="pt-2">
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={handleSavePreferences}
              disabled={isSavingPrefs}
            >
              {isSavingPrefs ? 'Saving...' : 'Save Preferences'}
            </Button>
          </div>
        </div>
      )}

      {/* 5. PRIVACY & DATA RIGHTS */}
      {activeTab === 'privacy' && (
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-8">
          <div>
            <h2 className="font-serif text-lg text-charcoal-900 pb-2 border-b border-sand-200">
              Privacy, Collections & Data Rights
            </h2>

            {privacyError && (
              <div className="mt-4 p-4 bg-terracotta-50 border border-terracotta-200 rounded-xl flex items-start gap-3 text-terracotta-800 text-xs">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-terracotta-600" />
                <div>
                  <span className="font-semibold block">Action Blocked</span>
                  <span>{privacyError}</span>
                </div>
              </div>
            )}

            <div className="space-y-4 mt-4 text-xs text-charcoal-700 leading-relaxed">
              <div className="p-4 bg-sand-50 rounded-xl border border-sand-200 space-y-2">
                <h3 className="font-semibold text-charcoal-900">Private Collections & Moodboards</h3>
                <p>
                  Any saved images and project collections created under your account are strictly private to you. They are never shared with third parties or indexed by search engines.
                </p>
                <Link
                  href="/account/collections"
                  className="inline-flex items-center gap-1 text-bronze-700 font-semibold hover:underline"
                >
                  <span>Manage saved collections</span>
                  <ExternalLink className="w-3 h-3" />
                </Link>
              </div>

              <div className="p-4 bg-sand-50 rounded-xl border border-sand-200 space-y-2">
                <h3 className="font-semibold text-charcoal-900">AI Privacy & Data Disclosure</h3>
                <p>
                  Images uploaded to the AI Studio are used strictly for spatial rendering generation. Uploaded images are stored with strict row-level security and are never used for public model training without explicit consent.
                </p>
              </div>
            </div>
          </div>

          {/* Data Portability / Export */}
          <div className="pt-4 border-t border-sand-200 space-y-4">
            <div>
              <h3 className="font-serif text-base text-charcoal-900">
                Data Portability & Export
              </h3>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Download a machine-readable archive (JSON & sanitized CSV) of your personal profile, collections, and inquiries. Authentication tokens, passwords, and encryption secrets are strictly excluded.
              </p>
            </div>

            {exportDataResult ? (
              <div className="p-4 bg-forest-50 border border-forest-200 rounded-xl space-y-3">
                <div className="flex items-center gap-2 text-forest-800 text-xs font-semibold">
                  <CheckCircle2 className="w-4 h-4 text-forest-600" />
                  <span>Data Export Package Ready</span>
                </div>
                <p className="text-[11px] text-forest-700">
                  Your archive is compiled ({exportDataResult.fileSizeBytes ? `${Math.round(exportDataResult.fileSizeBytes / 1024)} KB` : 'Archive generated'}). The secure one-time download link expires in 7 days.
                </p>
                {exportDataResult.downloadToken && (
                  <a
                    href={getExportDownloadUrl(exportDataResult.jobId, exportDataResult.downloadToken)}
                    download
                    className="inline-flex items-center gap-2 px-4 py-2 bg-forest-700 text-white rounded-xl text-xs font-medium hover:bg-forest-800 transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download Archive (.ZIP)</span>
                  </a>
                )}
              </div>
            ) : (
              <div>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleTriggerUserDataExport}
                  disabled={isExportingData}
                >
                  <Download className="w-3.5 h-3.5 mr-1.5" />
                  {isExportingData ? 'Generating Archive...' : 'Request Data Export'}
                </Button>
              </div>
            )}
          </div>

          {/* Privacy Requests History */}
          <div className="pt-4 border-t border-sand-200 space-y-4">
            <h3 className="font-serif text-base text-charcoal-900">
              Privacy Request History
            </h3>
            {loadingPrivacy ? (
              <p className="text-xs text-charcoal-400">Loading request status...</p>
            ) : privacyRequests.length === 0 ? (
              <p className="text-xs text-charcoal-400 italic">No formal data requests recorded.</p>
            ) : (
              <div className="divide-y divide-sand-100 border border-sand-200 rounded-xl overflow-hidden">
                {privacyRequests.map((req) => (
                  <div key={req.id} className="p-3 bg-sand-50/40 flex items-center justify-between text-xs">
                    <div className="space-y-0.5">
                      <span className="font-mono font-medium text-charcoal-900 block">
                        {req.requestType.replace(/_/g, ' ')}
                      </span>
                      <span className="text-[10px] text-charcoal-400">
                        {new Date(req.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    <div>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        req.status === 'COMPLETED' ? 'bg-forest-50 text-forest-700 border border-forest-200' :
                        req.status === 'FAILED' ? 'bg-terracotta-50 text-terracotta-700 border border-terracotta-200' :
                        'bg-sand-200 text-charcoal-700'
                      }`}>
                        {req.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Account Closure & Deletion Controls */}
          <div className="pt-4 border-t border-sand-200 space-y-6">
            <h3 className="font-serif text-base text-charcoal-900">
              Account Closure & Permanent Deletion
            </h3>

            {accountClosed ? (
              <div className="p-4 bg-forest-50 border border-forest-200 rounded-xl text-forest-800 text-xs">
                Your account has been closed and sessions terminated. Redirecting...
              </div>
            ) : deletionSuccess ? (
              <div className="p-4 bg-forest-50 border border-forest-200 rounded-xl text-forest-800 text-xs">
                Your account identity has been permanently deleted and personal data purged. Redirecting...
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Deactivation / Closure */}
                <div className="p-4 bg-sand-50 rounded-xl border border-sand-200 space-y-3">
                  <div>
                    <h4 className="text-xs font-semibold text-charcoal-900">Close Account</h4>
                    <p className="text-[11px] text-charcoal-500 mt-0.5">
                      Suspends your account, terminates all active sessions, and hides your profile.
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={handleCloseAccount}
                    disabled={isClosingAccount}
                  >
                    {isClosingAccount ? 'Closing...' : 'Close Account'}
                  </Button>
                </div>

                {/* Permanent Erasure / Anonymization */}
                <div className="p-4 bg-terracotta-50/60 border border-terracotta-200 rounded-xl space-y-3">
                  <div className="flex items-start gap-2">
                    <AlertTriangle className="w-4 h-4 text-terracotta-700 shrink-0 mt-0.5" />
                    <div>
                      <h4 className="text-xs font-semibold text-charcoal-900">Permanent Deletion</h4>
                      <p className="text-[11px] text-charcoal-600 mt-0.5">
                        Irreversibly anonymizes personal details, deletes credentials, MFA secrets, and private collections.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handleRequestDeletion}
                    disabled={isDeletingAccount}
                    className="px-3.5 py-1.5 bg-terracotta-700 text-white rounded-xl text-xs font-medium hover:bg-terracotta-800 transition-colors disabled:opacity-50"
                  >
                    {isDeletingAccount ? 'Deleting...' : 'Permanently Delete Account'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
