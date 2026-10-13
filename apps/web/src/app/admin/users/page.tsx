'use client';

import React, { useEffect, useState } from 'react';
import {
  Users,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  AlertOctagon,
  MoreVertical,
  RefreshCw,
  KeyRound,
  ShieldAlert,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { fetchAdminUsers, resetUserMfa, updateUserStatus } from '@/lib/admin/api';
import { AdminUserSummary } from '@/lib/admin/types';

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const isSuperAdmin = Boolean(currentUser?.roles?.includes('SUPER_ADMIN'));

  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');

  // Status Change Dialog State
  const [selectedUser, setSelectedUser] = useState<AdminUserSummary | null>(null);
  const [targetStatus, setTargetStatus] = useState<'ACTIVE' | 'SUSPENDED' | 'DELETED'>('ACTIVE');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // MFA Reset Dialog State
  const [mfaUser, setMfaUser] = useState<AdminUserSummary | null>(null);
  const [mfaReason, setMfaReason] = useState('');
  const [isResettingMfa, setIsResettingMfa] = useState(false);
  const [mfaError, setMfaError] = useState<string | null>(null);

  const loadUsers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminUsers(statusFilter || undefined);
      setUsers(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [statusFilter]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;

    setIsSubmitting(true);
    try {
      await updateUserStatus(selectedUser.id, {
        status: targetStatus,
        reason: reason.trim() || undefined,
      });
      setSelectedUser(null);
      setReason('');
      setSuccessMessage('User account status updated successfully.');
      await loadUsers();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update user status');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResetMfa = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!mfaUser) return;
    if (mfaReason.trim().length < 10) {
      setMfaError('A valid administrative reason (minimum 10 characters) is required.');
      return;
    }

    setIsResettingMfa(true);
    setMfaError(null);
    try {
      await resetUserMfa(mfaUser.id, { reason: mfaReason.trim() });
      setSuccessMessage(`Multi-factor authentication reset successfully for ${mfaUser.displayName}.`);
      setMfaUser(null);
      setMfaReason('');
      await loadUsers();
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Failed to reset MFA');
    } finally {
      setIsResettingMfa(false);
    }
  };

  const filteredUsers = users.filter((u) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      u.displayName.toLowerCase().includes(q) ||
      (u.email && u.email.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {successMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-center justify-between">
          <span>{successMessage}</span>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-700 hover:text-emerald-900 font-bold ml-2"
          >
            ✕
          </button>
        </div>
      )}
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">User Management</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Inspect platform user accounts, manage account states, and review memberships.
          </p>
        </div>
        <button
          onClick={loadUsers}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filters Bar */}
      <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="w-4 h-4 text-charcoal-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by name or email..."
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          />
        </div>
        <div className="flex items-center gap-2 text-xs">
          <span className="text-charcoal-500 font-medium">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 text-xs focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          >
            <option value="">All States</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="DELETED">Deleted</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Users Table */}
      <div className="bg-white border border-sand-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-sand-50/50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">User</th>
                <th className="py-3 px-4">Roles</th>
                <th className="py-3 px-4">Studio Affiliations</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Registered</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sand-100">
              {filteredUsers.length > 0 ? (
                filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-sand-50/30">
                    <td className="py-3 px-4">
                      <div className="font-medium text-charcoal-900">{u.displayName}</div>
                      <div className="text-[11px] text-charcoal-500">{u.email || 'No email'}</div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex flex-wrap gap-1">
                        {u.roles.map((r) => (
                          <span
                            key={r}
                            className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-sand-100 text-charcoal-700"
                          >
                            {r}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-charcoal-600">
                      {u.studioMemberships.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {u.studioMemberships.map((s, idx) => (
                            <span key={idx} className="text-[11px] bg-sand-50 px-1.5 py-0.5 rounded border border-sand-200">
                              {s}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-charcoal-400 italic">None</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold ${
                          u.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700'
                            : u.status === 'SUSPENDED'
                            ? 'bg-red-50 text-red-700'
                            : 'bg-sand-100 text-charcoal-600'
                        }`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-charcoal-500 whitespace-nowrap text-[11px]">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedUser(u);
                            setTargetStatus(u.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE');
                            setReason('');
                          }}
                          className="px-2.5 py-1 text-xs font-medium rounded border border-sand-300 hover:bg-sand-100 text-charcoal-700"
                        >
                          Change Status
                        </button>
                        {isSuperAdmin && (
                          <button
                            onClick={() => {
                              setMfaUser(u);
                              setMfaReason('');
                              setMfaError(null);
                            }}
                            disabled={currentUser?.id === u.id}
                            title={currentUser?.id === u.id ? 'Cannot reset own MFA administratively' : 'Reset Multi-Factor Authentication'}
                            className="px-2.5 py-1 text-xs font-medium rounded border border-sand-300 hover:bg-amber-50 text-amber-800 disabled:opacity-40 disabled:hover:bg-white"
                          >
                            Reset MFA
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-xs text-charcoal-500">
                    {isLoading ? 'Loading users...' : 'No users match the search/filter criteria.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Change Status Modal */}
      {selectedUser && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-lg border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              Update Account Status
            </h2>
            <p className="text-xs text-charcoal-600 mb-4">
              Modifying status for <strong>{selectedUser.displayName}</strong> ({selectedUser.email}).
            </p>

            <form onSubmit={handleUpdateStatus} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">New Status</label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                  <option value="DELETED">DELETED</option>
                </select>
              </div>

              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason for Audit Record
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Requested account closure, policy violation investigation..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setSelectedUser(null)}
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-3 py-1.5 rounded-lg bg-charcoal-900 text-white font-medium hover:bg-charcoal-800 disabled:opacity-50"
                >
                  {isSubmitting ? 'Updating...' : 'Confirm Status Change'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset MFA Modal */}
      {mfaUser && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 backdrop-blur-xs"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-sand-200">
            <div className="flex items-center gap-2 mb-2 text-amber-700">
              <ShieldAlert className="w-5 h-5" />
              <h2 className="text-lg font-serif text-charcoal-900">
                Reset Multi-Factor Authentication
              </h2>
            </div>

            <div className="bg-sand-50 p-3 rounded-lg border border-sand-200 mb-4 text-xs space-y-1">
              <div>
                <span className="text-charcoal-500">Target User: </span>
                <span className="font-semibold text-charcoal-900">{mfaUser.displayName}</span> ({mfaUser.email})
              </div>
              <div>
                <span className="text-charcoal-500">Roles: </span>
                <span className="font-mono text-[11px] text-charcoal-800">{mfaUser.roles.join(', ')}</span>
              </div>
            </div>

            <p className="text-xs text-charcoal-600 mb-4 leading-relaxed">
              This will remove the user&apos;s currently enrolled MFA method and revoke affected privileged sessions. The user will need to enroll MFA again upon next sign-in.
            </p>

            {mfaError && (
              <div className="p-3 mb-4 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
                {mfaError}
              </div>
            )}

            <form onSubmit={handleResetMfa} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason for Administrative Reset (Minimum 10 characters)
                </label>
                <textarea
                  value={mfaReason}
                  onChange={(e) => setMfaReason(e.target.value)}
                  placeholder="e.g. Device lost / replaced by employee, verified identity via secondary channel..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
                <span className="text-[10px] text-charcoal-400 mt-1 block">
                  {mfaReason.trim().length} / 10 characters minimum
                </span>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setMfaUser(null)}
                  disabled={isResettingMfa}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isResettingMfa || mfaReason.trim().length < 10}
                  className="px-3 py-1.5 rounded-lg bg-amber-700 text-white font-medium hover:bg-amber-800 disabled:opacity-50"
                >
                  {isResettingMfa ? 'Resetting...' : 'Confirm Reset MFA'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
