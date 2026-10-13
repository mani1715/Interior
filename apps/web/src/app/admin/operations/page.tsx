'use client';

import React, { useEffect, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Play,
  RefreshCw,
  Server,
  ShieldAlert,
  Sliders,
  XCircle,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import {
  fetchOperationalJobs,
  fetchOperationalState,
  fetchProviderMatrix,
  triggerOperationalJob,
  updateOperationalState,
} from '@/lib/admin/api';
import {
  OperationalJobRun,
  PlatformOperationalState,
  ProviderMatrixResponse,
  ProviderOperationalStatus,
  UpdateOperationalStateRequest,
} from '@/lib/admin/types';

const SUPPORTED_JOBS: { key: string; label: string; description: string }[] = [
  {
    key: 'SESSION_CLEANUP',
    label: 'Session Cleanup',
    description: 'Purges expired customer and administrator sessions from the authentication store.',
  },
  {
    key: 'UPLOAD_EXPIRY',
    label: 'Upload Expiry',
    description: 'Cancels unconfirmed direct-upload sessions that exceeded their upload TTL.',
  },
  {
    key: 'AI_STUCK_RECOVERY',
    label: 'AI Stuck Job Recovery',
    description: 'Recovers or fails generations that encountered unexpected timeouts or node disconnects.',
  },
  {
    key: 'PRIVACY_CLEANUP',
    label: 'Privacy Retention Cleanup',
    description: 'Executes verified erasure requests past cooling-off periods lacking active legal holds.',
  },
  {
    key: 'STORAGE_RECONCILIATION',
    label: 'Storage Reconciliation',
    description: 'Audits physical object storage blobs against registered platform media records.',
  },
];

export default function AdminOperationsPage() {
  const { user } = useAuth();
  const isSuperAdmin = Boolean(user?.roles?.includes('SUPER_ADMIN'));

  const [state, setState] = useState<PlatformOperationalState | null>(null);
  const [providers, setProviders] = useState<ProviderMatrixResponse | null>(null);
  const [jobs, setJobs] = useState<OperationalJobRun[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // State Change Modal
  const [stateModalOpen, setStateModalOpen] = useState(false);
  const [targetMaintenanceMode, setTargetMaintenanceMode] = useState<'NORMAL' | 'READ_ONLY'>('NORMAL');
  const [maintenanceReason, setMaintenanceReason] = useState('');
  const [isUpdatingState, setIsUpdatingState] = useState(false);

  // Feature Switch Modal
  const [switchModalOpen, setSwitchModalOpen] = useState(false);
  const [switchKey, setSwitchKey] = useState<'uploadsEnabled' | 'aiEnabled' | 'registrationsEnabled' | null>(null);
  const [switchTargetValue, setSwitchTargetValue] = useState<boolean>(true);
  const [switchReason, setSwitchReason] = useState('');
  const [isUpdatingSwitch, setIsUpdatingSwitch] = useState(false);

  // Job Run Modal
  const [selectedJobKey, setSelectedJobKey] = useState<string | null>(null);
  const [isRunningJob, setIsRunningJob] = useState(false);

  const loadAll = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [stateRes, provRes, jobsRes] = await Promise.all([
        fetchOperationalState(),
        fetchProviderMatrix().catch(() => null),
        fetchOperationalJobs(30).catch(() => []),
      ]);
      setState(stateRes);
      setProviders(provRes);
      setJobs(jobsRes);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load platform operations status');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleMaintenanceSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!state) return;

    setIsUpdatingState(true);
    setError(null);
    setActionSuccess(null);
    try {
      const updated = await updateOperationalState({
        maintenanceMode: targetMaintenanceMode,
        maintenanceMessage: maintenanceReason.trim() || undefined,
      });
      setState(updated);
      setStateModalOpen(false);
      setMaintenanceReason('');
      setActionSuccess(
        targetMaintenanceMode === 'READ_ONLY'
          ? 'Platform placed in Read-Only mode successfully.'
          : 'Platform restored to Normal operating state.'
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update platform state');
    } finally {
      setIsUpdatingState(false);
    }
  };

  const handleSwitchSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!state || !switchKey) return;

    setIsUpdatingSwitch(true);
    setError(null);
    setActionSuccess(null);
    try {
      const payload: UpdateOperationalStateRequest = {
        [switchKey]: switchTargetValue,
        maintenanceMessage: switchReason.trim() || undefined,
      };
      const updated = await updateOperationalState(payload);
      setState(updated);
      setSwitchModalOpen(false);
      setSwitchReason('');
      setActionSuccess('Feature availability updated successfully.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update feature state');
    } finally {
      setIsUpdatingSwitch(false);
    }
  };

  const handleTriggerJob = async () => {
    if (!selectedJobKey) return;

    setIsRunningJob(true);
    setError(null);
    setActionSuccess(null);
    try {
      const run = await triggerOperationalJob(selectedJobKey);
      setJobs((prev) => [run, ...prev]);
      setSelectedJobKey(null);
      setActionSuccess(`Job "${selectedJobKey}" completed successfully.`);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to trigger job ${selectedJobKey}`);
    } finally {
      setIsRunningJob(false);
    }
  };

  const getProviderStatusBadge = (status: ProviderOperationalStatus) => {
    switch (status) {
      case 'AVAILABLE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            Available
          </span>
        );
      case 'DEGRADED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="w-3 h-3 text-amber-600" />
            Degraded
          </span>
        );
      case 'DISABLED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-sand-100 text-charcoal-600 border border-sand-300">
            Disabled
          </span>
        );
      case 'NOT_CONFIGURED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-sand-50 text-charcoal-500 border border-sand-200">
            Not Configured
          </span>
        );
      case 'UNAVAILABLE':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-red-50 text-red-700 border border-red-200">
            <XCircle className="w-3 h-3 text-red-600" />
            Unavailable
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">Operations</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Platform health, availability, and controlled recovery actions.
          </p>
        </div>
        <button
          onClick={loadAll}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-sand-300 bg-white text-charcoal-700 hover:bg-sand-50 shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh Status</span>
        </button>
      </div>

      {actionSuccess && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-center justify-between">
          <span>{actionSuccess}</span>
          <button
            onClick={() => setActionSuccess(null)}
            className="text-emerald-700 hover:text-emerald-900 font-bold ml-2"
          >
            ✕
          </button>
        </div>
      )}

      {error && (
        <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="text-red-700 hover:text-red-900 font-bold ml-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Grid: Platform State + Feature Switches */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Platform Operational State Card */}
        <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-semibold tracking-wider text-charcoal-500">
              Platform Status
            </span>
            {state?.maintenanceMode === 'READ_ONLY' ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-300">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                READ_ONLY
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-300">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                NORMAL
              </span>
            )}
          </div>

          <div>
            <h3 className="text-base font-serif text-charcoal-900">
              {state?.maintenanceMode === 'READ_ONLY'
                ? 'Read-Only Mode Active'
                : 'Operational Normally'}
            </h3>
            <p className="text-xs text-charcoal-600 mt-1 leading-relaxed">
              {state?.maintenanceMode === 'READ_ONLY'
                ? 'Customer-facing mutations are temporarily restricted while safe reads remain available across the platform.'
                : 'Platform is operating normally. All customer, designer, and administrative capabilities are active.'}
            </p>
            {state?.maintenanceMessage && (
              <div className="mt-2.5 p-2.5 bg-sand-50 rounded-lg border border-sand-200 text-xs text-charcoal-700 italic">
                &ldquo;{state.maintenanceMessage}&rdquo;
              </div>
            )}
          </div>

          <div className="pt-2 border-t border-sand-100 flex items-center justify-between">
            {isSuperAdmin ? (
              <button
                type="button"
                onClick={() => {
                  setTargetMaintenanceMode(
                    state?.maintenanceMode === 'READ_ONLY' ? 'NORMAL' : 'READ_ONLY'
                  );
                  setMaintenanceReason('');
                  setStateModalOpen(true);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  state?.maintenanceMode === 'READ_ONLY'
                    ? 'bg-emerald-700 text-white hover:bg-emerald-800'
                    : 'bg-amber-700 text-white hover:bg-amber-800'
                }`}
              >
                {state?.maintenanceMode === 'READ_ONLY'
                  ? 'Restore Normal Mode'
                  : 'Enter Read-Only Mode'}
              </button>
            ) : (
              <span className="text-[11px] text-charcoal-400 italic">
                Requires Super Admin authority
              </span>
            )}
            {state?.updatedAt && (
              <span className="text-[10px] text-charcoal-400">
                Updated {new Date(state.updatedAt).toLocaleTimeString()}
              </span>
            )}
          </div>
        </div>

        {/* Feature Availability Switches */}
        <div className="lg:col-span-2 bg-white border border-sand-200 rounded-xl p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-semibold tracking-wider text-charcoal-500">
              Availability Controls
            </span>
            <span className="text-[11px] text-charcoal-400">Controlled operational restrictions</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Uploads Switch */}
            <div className="p-3.5 rounded-xl border border-sand-200 bg-sand-50/40 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-charcoal-900">Asset Uploads</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold ${
                    state?.uploadsEnabled
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {state?.uploadsEnabled ? 'ENABLED' : 'PAUSED'}
                </span>
              </div>
              <p className="text-[11px] text-charcoal-600 leading-normal">
                Direct media storage uploads for portfolio, avatars, and documents.
              </p>
              {isSuperAdmin && (
                <button
                  type="button"
                  onClick={() => {
                    setSwitchKey('uploadsEnabled');
                    setSwitchTargetValue(!state?.uploadsEnabled);
                    setSwitchReason('');
                    setSwitchModalOpen(true);
                  }}
                  className="w-full mt-1 px-2.5 py-1 text-[11px] font-medium rounded border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-700"
                >
                  {state?.uploadsEnabled ? 'Pause Uploads' : 'Enable Uploads'}
                </button>
              )}
            </div>

            {/* AI Generation Switch */}
            <div className="p-3.5 rounded-xl border border-sand-200 bg-sand-50/40 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-charcoal-900">AI Processing</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold ${
                    state?.aiEnabled
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {state?.aiEnabled ? 'ENABLED' : 'PAUSED'}
                </span>
              </div>
              <p className="text-[11px] text-charcoal-600 leading-normal">
                Generative AI room stylization, mood boards, and copy assistant tasks.
              </p>
              {isSuperAdmin && (
                <button
                  type="button"
                  onClick={() => {
                    setSwitchKey('aiEnabled');
                    setSwitchTargetValue(!state?.aiEnabled);
                    setSwitchReason('');
                    setSwitchModalOpen(true);
                  }}
                  className="w-full mt-1 px-2.5 py-1 text-[11px] font-medium rounded border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-700"
                >
                  {state?.aiEnabled ? 'Pause AI' : 'Enable AI'}
                </button>
              )}
            </div>

            {/* Registration Switch */}
            <div className="p-3.5 rounded-xl border border-sand-200 bg-sand-50/40 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-charcoal-900">Registrations</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold ${
                    state?.registrationsEnabled
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {state?.registrationsEnabled ? 'ENABLED' : 'PAUSED'}
                </span>
              </div>
              <p className="text-[11px] text-charcoal-600 leading-normal">
                New customer sign-ups and studio designer onboarding accounts.
              </p>
              {isSuperAdmin && (
                <button
                  type="button"
                  onClick={() => {
                    setSwitchKey('registrationsEnabled');
                    setSwitchTargetValue(!state?.registrationsEnabled);
                    setSwitchReason('');
                    setSwitchModalOpen(true);
                  }}
                  className="w-full mt-1 px-2.5 py-1 text-[11px] font-medium rounded border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-700"
                >
                  {state?.registrationsEnabled ? 'Pause Registrations' : 'Enable Registrations'}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Provider Status Matrix */}
      <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Server className="w-4 h-4 text-bronze-700" />
            <h2 className="text-base font-serif text-charcoal-900">Infrastructure Providers</h2>
          </div>
          {providers?.timestamp && (
            <span className="text-[11px] text-charcoal-500">
              Checked {new Date(providers.timestamp).toLocaleTimeString()}
            </span>
          )}
        </div>

        {providers && Object.keys(providers.providers).length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {Object.entries(providers.providers).map(([key, detail]) => (
              <div
                key={key}
                className="p-3 rounded-lg border border-sand-200 bg-white space-y-1.5 shadow-2xs"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-charcoal-800">{detail.providerName}</span>
                  {getProviderStatusBadge(detail.status)}
                </div>
                {detail.details && (
                  <p className="text-[11px] text-charcoal-500 truncate" title={detail.details}>
                    {detail.details}
                  </p>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="p-6 text-center text-xs text-charcoal-500 bg-sand-50/50 rounded-lg border border-dashed border-sand-200">
            {isLoading ? 'Checking infrastructure provider matrix...' : 'Provider health matrix unavailable.'}
          </div>
        )}
      </div>

      {/* Operational Job Coordinator */}
      <div className="bg-white border border-sand-200 rounded-xl p-5 shadow-sm space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-bronze-700" />
            <h2 className="text-base font-serif text-charcoal-900">Operational Background Jobs</h2>
          </div>
          <span className="text-[11px] text-charcoal-500">
            Fixed backend recovery tasks available for manual execution
          </span>
        </div>

        {/* Triggerable Jobs Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {SUPPORTED_JOBS.map((j) => (
            <div
              key={j.key}
              className="p-3.5 rounded-xl border border-sand-200 bg-sand-50/30 flex flex-col justify-between gap-3"
            >
              <div>
                <div className="font-semibold text-xs text-charcoal-900">{j.label}</div>
                <p className="text-[11px] text-charcoal-600 mt-1 leading-relaxed">
                  {j.description}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedJobKey(j.key)}
                className="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg border border-sand-300 bg-white hover:bg-sand-50 text-xs font-medium text-charcoal-800 self-start transition-colors"
              >
                <Play className="w-3 h-3 text-bronze-700" />
                <span>Run Job</span>
              </button>
            </div>
          ))}
        </div>

        {/* Recent Job Run History */}
        <div className="space-y-2 pt-3 border-t border-sand-100">
          <h3 className="text-xs uppercase font-semibold tracking-wider text-charcoal-500">
            Recent Job Executions
          </h3>
          <div className="overflow-x-auto border border-sand-200 rounded-lg">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-sand-50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider text-[10px]">
                  <th className="py-2.5 px-3">Job Key</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Processed</th>
                  <th className="py-2.5 px-3">Started</th>
                  <th className="py-2.5 px-3">Completed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sand-100">
                {jobs.length > 0 ? (
                  jobs.map((job) => (
                    <tr key={job.id} className="hover:bg-sand-50/40">
                      <td className="py-2.5 px-3 font-mono text-[11px] text-charcoal-800">
                        {job.jobKey}
                      </td>
                      <td className="py-2.5 px-3">
                        <span
                          className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                            job.status === 'COMPLETED' || job.status === 'SUCCESS'
                              ? 'bg-emerald-50 text-emerald-700'
                              : job.status === 'RUNNING'
                              ? 'bg-blue-50 text-blue-700'
                              : 'bg-red-50 text-red-700'
                          }`}
                        >
                          {job.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 text-charcoal-600 font-mono">
                        {job.processedCount}
                      </td>
                      <td className="py-2.5 px-3 text-charcoal-500 text-[11px] whitespace-nowrap">
                        {new Date(job.startedAt).toLocaleTimeString()}
                      </td>
                      <td className="py-2.5 px-3 text-charcoal-500 text-[11px] whitespace-nowrap">
                        {job.completedAt ? new Date(job.completedAt).toLocaleTimeString() : 'In progress'}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="py-6 text-center text-xs text-charcoal-400">
                      No operational job runs recorded recently.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Platform State Modal */}
      {stateModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 backdrop-blur-xs"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              {targetMaintenanceMode === 'READ_ONLY'
                ? 'Confirm Read-Only Platform Mode'
                : 'Confirm Restore to Normal Operation'}
            </h2>
            <p className="text-xs text-charcoal-600 mb-4 leading-relaxed">
              {targetMaintenanceMode === 'READ_ONLY'
                ? 'Placing the platform in Read-Only mode restricts all customer-facing mutations (project saves, inquiries, uploads, payments). Safe browse and reading will remain available.'
                : 'Restoring to Normal operation will re-enable all customer-facing mutations and background processing across the platform.'}
            </p>

            <form onSubmit={handleMaintenanceSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason / Operational Notice (Required for Audit)
                </label>
                <textarea
                  value={maintenanceReason}
                  onChange={(e) => setMaintenanceReason(e.target.value)}
                  placeholder="e.g. Scheduled database maintenance, incident mitigation..."
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setStateModalOpen(false)}
                  disabled={isUpdatingState}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isUpdatingState || !maintenanceReason.trim()}
                  className={`px-3 py-1.5 rounded-lg text-white font-medium disabled:opacity-50 ${
                    targetMaintenanceMode === 'READ_ONLY'
                      ? 'bg-amber-700 hover:bg-amber-800'
                      : 'bg-emerald-700 hover:bg-emerald-800'
                  }`}
                >
                  {isUpdatingState
                    ? 'Updating...'
                    : targetMaintenanceMode === 'READ_ONLY'
                    ? 'Confirm Read-Only Mode'
                    : 'Confirm Normal State'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Feature Switch Modal */}
      {switchModalOpen && switchKey && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 backdrop-blur-xs"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              Confirm Feature Availability Change
            </h2>
            <p className="text-xs text-charcoal-600 mb-4 leading-relaxed">
              You are about to {switchTargetValue ? 'ENABLE' : 'PAUSE'} <strong>{switchKey}</strong> for all platform tenants.
            </p>

            <form onSubmit={handleSwitchSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-charcoal-700 mb-1">
                  Reason for Audit Record
                </label>
                <textarea
                  value={switchReason}
                  onChange={(e) => setSwitchReason(e.target.value)}
                  placeholder="e.g. Mitigating third-party rate limiting, investigation..."
                  rows={2}
                  className="w-full px-3 py-2 rounded-lg border border-sand-300 text-xs focus:ring-1 focus:ring-charcoal-900"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setSwitchModalOpen(false)}
                  disabled={isUpdatingSwitch}
                  className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isUpdatingSwitch}
                  className="px-3 py-1.5 rounded-lg bg-charcoal-900 text-white font-medium hover:bg-charcoal-800 disabled:opacity-50"
                >
                  {isUpdatingSwitch ? 'Updating...' : 'Confirm Change'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Job Trigger Modal */}
      {selectedJobKey && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 backdrop-blur-xs"
        >
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-sand-200">
            <h2 className="text-lg font-serif text-charcoal-900 mb-1">
              Execute Operational Job
            </h2>
            <p className="text-xs text-charcoal-600 mb-4 leading-relaxed">
              Triggering manual run for <strong>{selectedJobKey}</strong>. The job coordinator will execute the task synchronously.
            </p>

            <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
              <button
                type="button"
                onClick={() => setSelectedJobKey(null)}
                disabled={isRunningJob}
                className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50 text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleTriggerJob}
                disabled={isRunningJob}
                className="px-3 py-1.5 rounded-lg bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 disabled:opacity-50"
              >
                {isRunningJob ? 'Running Job...' : 'Confirm & Run'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
