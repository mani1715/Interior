'use client';

import React, { useEffect, useState } from 'react';
import { FileText, Search, RefreshCw, Filter } from 'lucide-react';
import { fetchAdminAuditLogs } from '@/lib/admin/api';
import { AdminAuditLog } from '@/lib/admin/types';

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState<AdminAuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionFilter, setActionFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const loadLogs = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchAdminAuditLogs(actionFilter || undefined, 100);
      setLogs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit logs');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [actionFilter]);

  const filtered = logs.filter((l) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      l.action.toLowerCase().includes(q) ||
      l.resourceType.toLowerCase().includes(q) ||
      (l.actorEmail && l.actorEmail.toLowerCase().includes(q)) ||
      (l.details && l.details.toLowerCase().includes(q)) ||
      (l.resourceId && l.resourceId.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif text-charcoal-900">Admin Audit Trail</h1>
          <p className="text-sm text-charcoal-600 mt-0.5">
            Immutable append-only record of security and administrative operations across the platform.
          </p>
        </div>
        <button
          onClick={loadLogs}
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
            placeholder="Search by action, actor, or details..."
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          />
        </div>
        <div className="flex items-center gap-2 text-xs">
          <span className="text-charcoal-500 font-medium">Action:</span>
          <select
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 text-xs focus:outline-none focus:ring-1 focus:ring-charcoal-900"
          >
            <option value="">All Actions</option>
            <option value="USER_STATUS_CHANGED">User Status Changed</option>
            <option value="STUDIO_STATUS_CHANGED">Studio Status Changed</option>
            <option value="REVIEW_MODERATION">Review Moderation</option>
            <option value="VERIFICATION_DECISION">Verification Decision</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Audit Logs Table */}
      <div className="bg-white border border-sand-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-sand-50/50 border-b border-sand-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Resource</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4">Request ID</th>
                <th className="py-3 px-4">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sand-100">
              {filtered.length > 0 ? (
                filtered.map((log) => (
                  <tr key={log.id} className="hover:bg-sand-50/30">
                    <td className="py-3 px-4 text-charcoal-500 whitespace-nowrap font-mono text-[11px]">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="inline-block px-2 py-0.5 rounded text-[10px] font-semibold bg-sand-100 text-charcoal-800 font-mono">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-charcoal-700 whitespace-nowrap">
                      <div>{log.resourceType}</div>
                      {log.resourceId && (
                        <div className="text-[10px] text-charcoal-400 font-mono truncate max-w-[120px]">
                          {log.resourceId}
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-4 text-charcoal-700 whitespace-nowrap">
                      <div>{log.actorEmail || 'System'}</div>
                      {log.actorId && (
                        <div className="text-[10px] text-charcoal-400 font-mono truncate max-w-[120px]">
                          {log.actorId}
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-4 text-charcoal-500 font-mono text-[11px] whitespace-nowrap">
                      {log.requestId || '—'}
                    </td>
                    <td className="py-3 px-4 text-charcoal-600 font-mono text-[11px] max-w-sm truncate">
                      {log.details || '—'}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-xs text-charcoal-500">
                    {isLoading ? 'Loading audit trail...' : 'No audit records found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
