'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Users2,
  UserPlus,
  Shield,
  ShieldAlert,
  ShieldCheck,
  User,
  Copy,
  Check,
  Trash2,
  LogOut,
  AlertTriangle,
  Loader2,
  Mail,
  Calendar,
  ExternalLink,
  ChevronDown,
  Info,
} from 'lucide-react';
import {
  TeamOverview,
  TeamMember,
  PendingInvitation,
  StudioRole,
  CreateInvitationResponse,
} from '@/lib/team/types';
import {
  fetchTeamOverview,
  createInvitation,
  revokeInvitation,
  updateMemberRole,
  removeMember,
  leaveStudio,
} from '@/lib/team/api';
import { useRealtimeSubscription } from '@/lib/realtime/RealtimeProvider';
import { useAuth } from '@/lib/auth/auth-context';

export function TeamWorkspace() {
  const { user } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [teamData, setTeamData] = useState<TeamOverview | null>(null);

  // Modals & form state
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<'DESIGNER_ADMIN' | 'DESIGNER_MEMBER'>('DESIGNER_MEMBER');
  const [isInviting, setIsInviting] = useState(false);
  const [inviteSuccessData, setInviteSuccessData] = useState<CreateInvitationResponse | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  // Role change modal
  const [roleChangeTarget, setRoleChangeTarget] = useState<TeamMember | null>(null);
  const [newSelectedRole, setNewSelectedRole] = useState<'DESIGNER_ADMIN' | 'DESIGNER_MEMBER'>('DESIGNER_MEMBER');
  const [isChangingRole, setIsChangingRole] = useState(false);

  // Remove member modal
  const [memberToRemove, setMemberToRemove] = useState<TeamMember | null>(null);
  const [isRemoving, setIsRemoving] = useState(false);

  // Leave studio modal
  const [showLeaveModal, setShowLeaveModal] = useState(false);
  const [isLeaving, setIsLeaving] = useState(false);

  // Action status/toasts
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadTeamData = useCallback(async () => {
    try {
      const data = await fetchTeamOverview();
      setTeamData(data);
      setError(null);
    } catch (err: any) {
      setError(err?.message || 'Failed to load team data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTeamData();
  }, [loadTeamData]);

  // Realtime updates
  useRealtimeSubscription(
    [
      'STUDIO_MEMBER_ADDED',
      'STUDIO_MEMBER_ROLE_CHANGED',
      'STUDIO_MEMBER_REMOVED',
      'STUDIO_INVITATION_CREATED',
      'STUDIO_INVITATION_ACCEPTED',
      'RESYNC',
    ],
    () => {
      loadTeamData();
    }
  );

  // Helpers to inspect roles
  const isAdminOrOwner = useMemo(() => {
    if (!teamData) return false;
    const r = teamData.currentUserRole?.toUpperCase();
    return r === 'OWNER' || r === 'ADMIN' || r === 'DESIGNER_ADMIN';
  }, [teamData]);

  const adminCount = useMemo(() => {
    if (!teamData) return 0;
    return teamData.members.filter((m) => {
      const r = m.role?.toUpperCase();
      return r === 'OWNER' || r === 'ADMIN' || r === 'DESIGNER_ADMIN';
    }).length;
  }, [teamData]);

  const isSoleAdmin = useMemo(() => {
    if (!isAdminOrOwner) return false;
    return adminCount <= 1;
  }, [isAdminOrOwner, adminCount]);

  const showNotification = (msg: string, isErr = false) => {
    if (isErr) {
      setActionError(msg);
      setTimeout(() => setActionError(null), 5000);
    } else {
      setActionSuccess(msg);
      setTimeout(() => setActionSuccess(null), 4000);
    }
  };

  const handleCreateInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inviteEmail.trim()) return;

    setIsInviting(true);
    setActionError(null);
    try {
      const resp = await createInvitation({
        email: inviteEmail.trim(),
        role: inviteRole,
      });
      setInviteSuccessData(resp);
      setInviteEmail('');
      loadTeamData();
      showNotification('Invitation generated successfully');
    } catch (err: any) {
      setActionError(err?.message || 'Failed to create invitation');
    } finally {
      setIsInviting(false);
    }
  };

  const handleCopyLink = async (link: string) => {
    try {
      await navigator.clipboard.writeText(link);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
      showNotification('Invitation link copied to clipboard');
    } catch {
      showNotification('Failed to copy link', true);
    }
  };

  const handleRevokeInvite = async (invitationId: string) => {
    if (!confirm('Are you sure you want to revoke this invitation?')) return;
    try {
      await revokeInvitation(invitationId);
      loadTeamData();
      showNotification('Invitation revoked');
    } catch (err: any) {
      showNotification(err?.message || 'Failed to revoke invitation', true);
    }
  };

  const handleConfirmRoleChange = async () => {
    if (!roleChangeTarget) return;

    // Check last-admin safeguard
    const isTargetAdmin =
      roleChangeTarget.role === 'OWNER' ||
      roleChangeTarget.role === 'ADMIN' ||
      roleChangeTarget.role === 'DESIGNER_ADMIN';
    const isDemoting = isTargetAdmin && newSelectedRole === 'DESIGNER_MEMBER';
    if (isDemoting && adminCount <= 1) {
      showNotification(
        'Cannot demote administrator: A studio must have at least one administrator.',
        true
      );
      return;
    }

    setIsChangingRole(true);
    try {
      await updateMemberRole(roleChangeTarget.id, { role: newSelectedRole });
      setRoleChangeTarget(null);
      loadTeamData();
      showNotification('Member role updated successfully');
    } catch (err: any) {
      showNotification(err?.message || 'Failed to update member role', true);
    } finally {
      setIsChangingRole(false);
    }
  };

  const handleConfirmRemoveMember = async () => {
    if (!memberToRemove) return;

    const isTargetAdmin =
      memberToRemove.role === 'OWNER' ||
      memberToRemove.role === 'ADMIN' ||
      memberToRemove.role === 'DESIGNER_ADMIN';
    if (isTargetAdmin && adminCount <= 1) {
      showNotification(
        'Cannot remove member: You cannot remove the only administrator of this studio.',
        true
      );
      return;
    }

    setIsRemoving(true);
    try {
      await removeMember(memberToRemove.id);
      setMemberToRemove(null);
      loadTeamData();
      showNotification('Member removed from studio');
    } catch (err: any) {
      showNotification(err?.message || 'Failed to remove member', true);
    } finally {
      setIsRemoving(false);
    }
  };

  const handleConfirmLeaveStudio = async () => {
    if (isSoleAdmin) {
      showNotification(
        'Cannot leave studio: You are the sole administrator. Promote another member or assign ownership first.',
        true
      );
      return;
    }

    setIsLeaving(true);
    try {
      await leaveStudio();
      window.location.href = '/workspace';
    } catch (err: any) {
      showNotification(err?.message || 'Failed to leave studio', true);
      setIsLeaving(false);
    }
  };

  const getRoleBadge = (role: StudioRole) => {
    const r = role?.toUpperCase();
    if (r === 'OWNER') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 dark:bg-purple-950/50 dark:text-purple-300 border border-purple-200 dark:border-purple-800">
          <ShieldCheck className="w-3.5 h-3.5 text-purple-600 dark:text-purple-400" />
          Owner
        </span>
      );
    }
    if (r === 'ADMIN' || r === 'DESIGNER_ADMIN') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-950/50 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
          <Shield className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
          Admin
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700">
        <User className="w-3.5 h-3.5 text-zinc-500" />
        Member
      </span>
    );
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-zinc-500 gap-3">
        <Loader2 className="w-8 h-8 animate-spin text-amber-600" />
        <p className="text-sm">Loading studio team workspace...</p>
      </div>
    );
  }

  if (error || !teamData) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-red-700 dark:text-red-400 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-semibold text-sm">Unable to load team</h3>
            <p className="text-xs mt-1">{error || 'Studio team data is unavailable.'}</p>
            <button
              onClick={loadTeamData}
              className="mt-3 px-3 py-1.5 text-xs font-medium bg-red-100 dark:bg-red-900/60 rounded border border-red-300 dark:border-red-800 hover:bg-red-200 transition"
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-6">
      {/* Toast Notifications */}
      {actionSuccess && (
        <div
          role="status"
          className="p-3.5 bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-300 rounded-lg text-sm flex items-center justify-between shadow-sm animate-in fade-in slide-in-from-top-2"
        >
          <div className="flex items-center gap-2">
            <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <span>{actionSuccess}</span>
          </div>
          <button
            onClick={() => setActionSuccess(null)}
            className="text-emerald-600 hover:text-emerald-800 text-xs font-semibold px-2 py-1"
          >
            Dismiss
          </button>
        </div>
      )}

      {actionError && (
        <div
          role="alert"
          className="p-3.5 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-300 rounded-lg text-sm flex items-center justify-between shadow-sm animate-in fade-in slide-in-from-top-2"
        >
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-600 dark:text-red-400" />
            <span>{actionError}</span>
          </div>
          <button
            onClick={() => setActionError(null)}
            className="text-red-600 hover:text-red-800 text-xs font-semibold px-2 py-1"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Header Banner */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl p-5 sm:p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800/60 rounded-lg">
                <Users2 className="w-6 h-6 text-amber-600 dark:text-amber-500" />
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-bold text-zinc-900 dark:text-zinc-50">
                  {teamData.studioName}
                </h1>
                <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400">
                  Team Members & Collaborative Access
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 pt-2">
              <span className="text-xs font-medium text-zinc-600 dark:text-zinc-400">
                Your Role:
              </span>
              {getRoleBadge(teamData.currentUserRole)}
              <span className="text-zinc-300 dark:text-zinc-700">•</span>
              <span className="text-xs text-zinc-500 dark:text-zinc-400 font-medium">
                {teamData.totalActiveMembers}{' '}
                {teamData.totalActiveMembers === 1 ? 'Active Member' : 'Active Members'}
              </span>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2.5 pt-2 sm:pt-0">
            {isAdminOrOwner && (
              <button
                type="button"
                onClick={() => {
                  setInviteSuccessData(null);
                  setShowInviteModal(true);
                }}
                className="inline-flex items-center justify-center gap-2 px-4 py-2.5 min-h-[44px] rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2 dark:focus:ring-offset-zinc-900"
              >
                <UserPlus className="w-4 h-4" />
                Invite Member
              </button>
            )}

            <button
              type="button"
              onClick={() => setShowLeaveModal(true)}
              className="inline-flex items-center justify-center gap-1.5 px-3.5 py-2 min-h-[44px] rounded-lg border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-800/60 text-sm font-medium transition focus:outline-none focus:ring-2 focus:ring-zinc-400"
            >
              <LogOut className="w-4 h-4 text-zinc-500" />
              Leave Studio
            </button>
          </div>
        </div>
      </div>

      {/* Solo Member Empty State Guidance */}
      {teamData.members.length === 1 && (
        <div className="bg-amber-50/70 dark:bg-amber-950/20 border border-amber-200/80 dark:border-amber-900/50 rounded-xl p-4 sm:p-5 flex items-start gap-3.5">
          <Info className="w-5 h-5 text-amber-600 dark:text-amber-500 flex-shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-300">
              You are currently the only member of this studio
            </h4>
            <p className="text-xs text-amber-800/90 dark:text-amber-400 leading-relaxed">
              Invite design assistants, 3D visualizers, or partners to collaborate on client revision boards, AI visualizer prompts, and portfolio showcases together.
            </p>
          </div>
        </div>
      )}

      {/* Pending Invitations Section */}
      {teamData.pendingInvitations.length > 0 && (
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl shadow-sm overflow-hidden">
          <div className="p-4 sm:p-5 border-b border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Mail className="w-4 h-4 text-amber-600 dark:text-amber-500" />
              <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                Pending Invitations ({teamData.pendingInvitations.length})
              </h2>
            </div>
            <span className="text-xs text-zinc-500">Awaiting acceptance</span>
          </div>

          <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
            {teamData.pendingInvitations.map((inv) => (
              <div
                key={inv.id}
                className="p-4 sm:px-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-medium text-zinc-900 dark:text-zinc-100 font-mono">
                      {inv.invitedEmail}
                    </span>
                    {getRoleBadge(inv.role)}
                  </div>
                  <div className="text-xs text-zinc-500 flex items-center gap-2 flex-wrap">
                    <span>Invited by {inv.invitedByEmail}</span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3 text-zinc-400" />
                      Expires {new Date(inv.expiresAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                {isAdminOrOwner && (
                  <div className="flex items-center gap-2 self-end sm:self-center">
                    <button
                      type="button"
                      onClick={() => handleRevokeInvite(inv.id)}
                      className="inline-flex items-center gap-1 px-3 py-1.5 min-h-[44px] text-xs font-medium text-red-600 hover:text-red-700 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded border border-red-200 dark:border-red-900 transition"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Revoke
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Team Members List */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Users2 className="w-4 h-4 text-zinc-600 dark:text-zinc-400" />
            <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
              Active Studio Members ({teamData.members.length})
            </h2>
          </div>
        </div>

        {/* Desktop Table View */}
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-zinc-50/60 dark:bg-zinc-800/40 border-b border-zinc-100 dark:border-zinc-800 text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">
                <th className="py-3 px-5">Member</th>
                <th className="py-3 px-5">Role</th>
                <th className="py-3 px-5">Joined</th>
                {isAdminOrOwner && <th className="py-3 px-5 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800 text-sm">
              {teamData.members.map((member) => {
                const isTargetSelf = user?.email?.toLowerCase() === member.email?.toLowerCase();
                const isTargetAdmin =
                  member.role === 'OWNER' ||
                  member.role === 'ADMIN' ||
                  member.role === 'DESIGNER_ADMIN';
                const isSoleAdminTarget = isTargetAdmin && adminCount <= 1;

                return (
                  <tr
                    key={member.id}
                    className="hover:bg-zinc-50/50 dark:hover:bg-zinc-800/30 transition-colors"
                  >
                    <td className="py-3.5 px-5">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center text-white font-medium text-xs shadow-sm">
                          {member.fullName
                            ? member.fullName.slice(0, 2).toUpperCase()
                            : member.email.slice(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <div className="font-medium text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                            {member.fullName || 'Studio Member'}
                            {isTargetSelf && (
                              <span className="text-[10px] bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 px-1.5 py-0.5 rounded font-normal">
                                You
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-zinc-500 dark:text-zinc-400 font-mono">
                            {member.email}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-5">{getRoleBadge(member.role)}</td>

                    <td className="py-3.5 px-5 text-xs text-zinc-500 dark:text-zinc-400">
                      {new Date(member.joinedAt).toLocaleDateString()}
                    </td>

                    {isAdminOrOwner && (
                      <td className="py-3.5 px-5 text-right">
                        {isSoleAdminTarget ? (
                          <span
                            className="inline-flex items-center gap-1 text-xs text-zinc-400 dark:text-zinc-500 italic"
                            title="Cannot modify the sole administrator of this studio"
                          >
                            <ShieldAlert className="w-3.5 h-3.5 text-amber-500" />
                            Sole Admin
                          </span>
                        ) : (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => {
                                setRoleChangeTarget(member);
                                setNewSelectedRole(
                                  member.role === 'ADMIN' || member.role === 'DESIGNER_ADMIN'
                                    ? 'DESIGNER_MEMBER'
                                    : 'DESIGNER_ADMIN'
                                );
                              }}
                              className="px-2.5 py-1.5 text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded border border-zinc-200 dark:border-zinc-700 transition"
                            >
                              Change Role
                            </button>

                            <button
                              type="button"
                              onClick={() => setMemberToRemove(member)}
                              className="p-1.5 text-zinc-400 hover:text-red-600 dark:hover:text-red-400 rounded hover:bg-red-50 dark:hover:bg-red-950/30 transition"
                              title="Remove member"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        )}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Mobile Responsive Cards (Screens < 768px, optimized for 360px-430px) */}
        <div className="block md:hidden divide-y divide-zinc-100 dark:divide-zinc-800">
          {teamData.members.map((member) => {
            const isTargetSelf = user?.email?.toLowerCase() === member.email?.toLowerCase();
            const isTargetAdmin =
              member.role === 'OWNER' ||
              member.role === 'ADMIN' ||
              member.role === 'DESIGNER_ADMIN';
            const isSoleAdminTarget = isTargetAdmin && adminCount <= 1;

            return (
              <div key={member.id} className="p-4 space-y-3">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center text-white font-medium text-xs shadow-sm flex-shrink-0">
                      {member.fullName
                        ? member.fullName.slice(0, 2).toUpperCase()
                        : member.email.slice(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div className="font-medium text-sm text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                        {member.fullName || 'Studio Member'}
                        {isTargetSelf && (
                          <span className="text-[10px] bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 px-1.5 py-0.2 rounded font-normal">
                            You
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-zinc-500 dark:text-zinc-400 font-mono truncate max-w-[200px]">
                        {member.email}
                      </div>
                    </div>
                  </div>
                  <div>{getRoleBadge(member.role)}</div>
                </div>

                <div className="flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400 pt-1 border-t border-zinc-50 dark:border-zinc-800/60">
                  <span>Joined {new Date(member.joinedAt).toLocaleDateString()}</span>

                  {isAdminOrOwner && (
                    <div>
                      {isSoleAdminTarget ? (
                        <span className="text-[11px] text-zinc-400 flex items-center gap-1 italic">
                          <ShieldAlert className="w-3 h-3 text-amber-500" />
                          Sole Admin
                        </span>
                      ) : (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              setRoleChangeTarget(member);
                              setNewSelectedRole(
                                member.role === 'ADMIN' || member.role === 'DESIGNER_ADMIN'
                                  ? 'DESIGNER_MEMBER'
                                  : 'DESIGNER_ADMIN'
                              );
                            }}
                            className="px-3 py-2 min-h-[44px] text-xs font-medium text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                          >
                            Change Role
                          </button>
                          <button
                            type="button"
                            onClick={() => setMemberToRemove(member)}
                            className="p-2.5 min-h-[44px] min-w-[44px] flex items-center justify-center text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg border border-red-200 dark:border-red-900"
                            title="Remove member"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Invite Member Modal */}
      {showInviteModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
        >
          <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl max-w-md w-full p-6 shadow-xl space-y-4">
            {!inviteSuccessData ? (
              <>
                <div className="space-y-1">
                  <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                    <UserPlus className="w-5 h-5 text-amber-600" />
                    Invite Studio Member
                  </h3>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Generate a single-use secure link for your colleague to join {teamData.studioName}.
                  </p>
                </div>

                <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700 rounded-lg text-xs text-zinc-600 dark:text-zinc-400 flex items-start gap-2">
                  <Info className="w-4 h-4 text-zinc-400 flex-shrink-0 mt-0.5" />
                  <span>
                    Email delivery is not configured in this local environment. A secure invite link will be generated for you to copy and share directly.
                  </span>
                </div>

                <form onSubmit={handleCreateInvite} className="space-y-4 pt-1">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                      Colleague&apos;s Email Address
                    </label>
                    <input
                      type="email"
                      required
                      value={inviteEmail}
                      onChange={(e) => setInviteEmail(e.target.value)}
                      placeholder="designer@studio.com"
                      className="w-full px-3 py-2 text-sm bg-zinc-50 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700 rounded-lg text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                      Studio Role
                    </label>
                    <select
                      value={inviteRole}
                      onChange={(e) =>
                        setInviteRole(e.target.value as 'DESIGNER_ADMIN' | 'DESIGNER_MEMBER')
                      }
                      className="w-full px-3 py-2 text-sm bg-zinc-50 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700 rounded-lg text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-amber-500"
                    >
                      <option value="DESIGNER_MEMBER">Designer Member (Standard Access)</option>
                      <option value="DESIGNER_ADMIN">Designer Admin (Full Studio Management)</option>
                    </select>

                    {inviteRole === 'DESIGNER_ADMIN' && (
                      <p className="mt-2 text-xs text-amber-600 dark:text-amber-400 flex items-center gap-1.5">
                        <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                        Admins can invite members, change roles, and modify studio configuration.
                      </p>
                    )}
                  </div>

                  <div className="flex items-center justify-end gap-2.5 pt-3">
                    <button
                      type="button"
                      onClick={() => setShowInviteModal(false)}
                      disabled={isInviting}
                      className="px-4 py-2 min-h-[44px] text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg transition"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isInviting || !inviteEmail.trim()}
                      className="inline-flex items-center justify-center gap-2 px-4 py-2 min-h-[44px] text-xs font-medium text-white bg-amber-600 hover:bg-amber-700 rounded-lg transition disabled:opacity-50"
                    >
                      {isInviting ? (
                        <>
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          Generating...
                        </>
                      ) : (
                        'Generate Invite Link'
                      )}
                    </button>
                  </div>
                </form>
              </>
            ) : (
              <div className="space-y-4">
                <div className="text-center space-y-2">
                  <div className="w-12 h-12 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
                    <Check className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
                    Invitation Link Created!
                  </h3>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Copy and share this link directly with{' '}
                    <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                      {inviteSuccessData.invitedEmail}
                    </span>
                    . Valid for 7 days.
                  </p>
                </div>

                <div className="p-3 bg-zinc-50 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 rounded-lg space-y-2">
                  <label className="block text-[11px] font-semibold text-zinc-500 uppercase">
                    Invitation URL
                  </label>
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      readOnly
                      value={inviteSuccessData.inviteLink}
                      className="w-full text-xs bg-white dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded px-2.5 py-2 font-mono text-zinc-800 dark:text-zinc-200 select-all"
                    />
                    <button
                      type="button"
                      onClick={() => handleCopyLink(inviteSuccessData.inviteLink)}
                      className="px-3 py-2 min-h-[40px] text-xs font-medium text-white bg-amber-600 hover:bg-amber-700 rounded flex items-center gap-1.5 transition flex-shrink-0"
                    >
                      {copiedLink ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                      {copiedLink ? 'Copied' : 'Copy'}
                    </button>
                  </div>
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setShowInviteModal(false);
                      setInviteSuccessData(null);
                    }}
                    className="px-4 py-2 min-h-[44px] text-xs font-medium bg-zinc-100 dark:bg-zinc-800 hover:bg-zinc-200 text-zinc-800 dark:text-zinc-200 rounded-lg transition"
                  >
                    Done
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Role Change Modal */}
      {roleChangeTarget && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
        >
          <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
              Update Member Role
            </h3>
            <p className="text-xs text-zinc-500 dark:text-zinc-400">
              Change the studio permissions for{' '}
              <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                {roleChangeTarget.fullName || roleChangeTarget.email}
              </span>
              .
            </p>

            <div className="space-y-3 pt-1">
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                  Select Role
                </label>
                <select
                  value={newSelectedRole}
                  onChange={(e) =>
                    setNewSelectedRole(e.target.value as 'DESIGNER_ADMIN' | 'DESIGNER_MEMBER')
                  }
                  className="w-full px-3 py-2 text-sm bg-zinc-50 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700 rounded-lg text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-amber-500"
                >
                  <option value="DESIGNER_MEMBER">Designer Member</option>
                  <option value="DESIGNER_ADMIN">Designer Admin</option>
                </select>
              </div>

              {/* Sole admin warning if demoting */}
              {adminCount <= 1 &&
                (roleChangeTarget.role === 'OWNER' ||
                  roleChangeTarget.role === 'ADMIN' ||
                  roleChangeTarget.role === 'DESIGNER_ADMIN') &&
                newSelectedRole === 'DESIGNER_MEMBER' && (
                  <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-xs text-red-700 dark:text-red-400 flex items-start gap-2">
                    <ShieldAlert className="w-4 h-4 flex-shrink-0 mt-0.5" />
                    <span>
                      Cannot demote: This is the sole administrator in this studio. Assign another administrator before demoting.
                    </span>
                  </div>
                )}
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3">
              <button
                type="button"
                onClick={() => setRoleChangeTarget(null)}
                disabled={isChangingRole}
                className="px-4 py-2 min-h-[44px] text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmRoleChange}
                disabled={
                  isChangingRole ||
                  (adminCount <= 1 &&
                    (roleChangeTarget.role === 'OWNER' ||
                      roleChangeTarget.role === 'ADMIN' ||
                      roleChangeTarget.role === 'DESIGNER_ADMIN') &&
                    newSelectedRole === 'DESIGNER_MEMBER')
                }
                className="inline-flex items-center justify-center gap-2 px-4 py-2 min-h-[44px] text-xs font-medium text-white bg-amber-600 hover:bg-amber-700 rounded-lg transition disabled:opacity-50"
              >
                {isChangingRole ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : 'Save Role'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Remove Member Confirmation Modal */}
      {memberToRemove && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
        >
          <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 flex items-center justify-center">
              <Trash2 className="w-5 h-5" />
            </div>

            <div>
              <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
                Remove Studio Member?
              </h3>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                Are you sure you want to remove{' '}
                <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                  {memberToRemove.fullName || memberToRemove.email}
                </span>{' '}
                from {teamData.studioName}? They will immediately lose access to this workspace.
              </p>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3">
              <button
                type="button"
                onClick={() => setMemberToRemove(null)}
                disabled={isRemoving}
                className="px-4 py-2 min-h-[44px] text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmRemoveMember}
                disabled={isRemoving}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 min-h-[44px] text-xs font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition disabled:opacity-50"
              >
                {isRemoving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : 'Confirm Removal'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Leave Studio Confirmation Modal */}
      {showLeaveModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
        >
          <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <div className="w-10 h-10 rounded-full bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center">
              <LogOut className="w-5 h-5" />
            </div>

            <div>
              <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
                Leave {teamData.studioName}?
              </h3>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                You will forfeit access to projects, media, and client reviews associated with this studio.
              </p>

              {isSoleAdmin && (
                <div className="mt-3 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-xs text-red-700 dark:text-red-400 flex items-start gap-2">
                  <ShieldAlert className="w-4 h-4 flex-shrink-0 mt-0.5" />
                  <span>
                    Cannot leave studio: You are the sole administrator. Promote another member or assign ownership before leaving.
                  </span>
                </div>
              )}
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3">
              <button
                type="button"
                onClick={() => setShowLeaveModal(false)}
                disabled={isLeaving}
                className="px-4 py-2 min-h-[44px] text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmLeaveStudio}
                disabled={isLeaving || isSoleAdmin}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 min-h-[44px] text-xs font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition disabled:opacity-50"
              >
                {isLeaving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : 'Leave Studio'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
