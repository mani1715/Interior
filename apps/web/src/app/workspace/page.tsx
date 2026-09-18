'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  CheckCircle2,
  Circle,
  ArrowUpRight,
  Sparkles,
  Layout,
  FolderKanban,
  Building2,
  AlertCircle,
  Clock,
  ShieldCheck,
  ChevronRight,
} from 'lucide-react';
import { fetchWorkspaceSummary } from '@/lib/workspace/api';
import { WorkspaceSummary } from '@/lib/workspace/types';

export default function WorkspaceHomePage() {
  const [summary, setSummary] = useState<WorkspaceSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchWorkspaceSummary()
      .then(setSummary)
      .catch((err) => setError(err.message || 'Failed to load workspace'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="p-6 md:p-10 max-w-6xl mx-auto space-y-6 animate-pulse">
        <div className="h-8 bg-sand-200 rounded-lg w-1/3" />
        <div className="h-28 bg-sand-200 rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-48 bg-sand-200 rounded-2xl" />
          <div className="h-48 bg-sand-200 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="p-6 md:p-10 max-w-4xl mx-auto">
        <div className="bg-white border border-sand-200 rounded-2xl p-8 text-center">
          <AlertCircle className="w-10 h-10 text-terracotta-600 mx-auto mb-3" />
          <h1 className="font-serif text-xl text-charcoal-900 mb-2">Unable to Load Workspace</h1>
          <p className="text-sm text-charcoal-600 mb-4">{error || 'Workspace data could not be retrieved.'}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-bronze-700 text-white text-xs font-medium rounded-xl hover:bg-bronze-800 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const { studio, user, completeness, setupChecklist, modules, activityFeed } = summary;

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-6xl mx-auto space-y-8">
      {/* 1. Greeting & Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
            Professional Workspace
          </span>
          <h1 className="font-serif text-2xl sm:text-3xl lg:text-4xl text-charcoal-900 tracking-tight">
            Welcome back, {user.displayName}
          </h1>
          <p className="text-xs sm:text-sm text-charcoal-600 mt-1">
            Operating as <strong className="text-charcoal-900">{studio.name}</strong> • {studio.professionalTitle || studio.professionalType.replace(/_/g, ' ')}
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-forest-50 text-forest-700 border border-forest-200">
            <span className="w-1.5 h-1.5 rounded-full bg-forest-600" />
            Operational: {studio.operationalStatus}
          </span>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-sand-100 text-charcoal-700 border border-sand-300">
            Public: {studio.publicationStatus}
          </span>
          <span className="px-2.5 py-1 rounded-full text-xs font-mono font-semibold bg-sand-200 text-charcoal-800">
            {studio.role}
          </span>
        </div>
      </div>

      {/* 2. Truthful Publication Status Notice */}
      <div className="bg-sand-50 border border-sand-200 rounded-2xl p-4 sm:p-5 flex items-start gap-3.5 shadow-2xs">
        <ShieldCheck className="w-5 h-5 text-bronze-700 flex-shrink-0 mt-0.5" />
        <div className="text-xs sm:text-sm text-charcoal-700 leading-relaxed">
          <strong className="font-semibold text-charcoal-900">Studio Status: Operational Internally</strong>.
          {' '}Your professional studio profile has been verified and registered. Public discovery remains unpublished until you configure your portfolio and publish your first interior project.
        </div>
      </div>

      {/* 3. Dual Completeness & Readiness Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Profile Completeness Card */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-serif text-base font-semibold text-charcoal-900">
              Professional Profile Completeness
            </h2>
            <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-forest-50 text-forest-700 border border-forest-200">
              {completeness.profileCompletenessPercentage}%
            </span>
          </div>
          <div className="w-full bg-sand-100 rounded-full h-2 mb-3 overflow-hidden">
            <div
              className="bg-forest-600 h-2 rounded-full transition-all duration-500"
              style={{ width: `${completeness.profileCompletenessPercentage}%` }}
              role="progressbar"
              aria-valuenow={completeness.profileCompletenessPercentage}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label="Professional Profile Completeness"
            />
          </div>
          <p className="text-xs text-charcoal-500 leading-relaxed">
            Measures your registered business identity, location, interior services catalog, aesthetic specialties, and contact channels.
          </p>
        </div>

        {/* Platform Launch Readiness Card */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-serif text-base font-semibold text-charcoal-900">
              Platform Launch Readiness
            </h2>
            <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-sand-100 text-charcoal-800 border border-sand-300">
              {completeness.platformReadinessPercentage}%
            </span>
          </div>
          <div className="w-full bg-sand-100 rounded-full h-2 mb-3 overflow-hidden">
            <div
              className="bg-bronze-600 h-2 rounded-full transition-all duration-500"
              style={{ width: `${completeness.platformReadinessPercentage}%` }}
              role="progressbar"
              aria-valuenow={completeness.platformReadinessPercentage}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label="Platform Launch Readiness"
            />
          </div>
          <p className="text-xs text-charcoal-500 leading-relaxed">
            Reflects overall public readiness. Portfolio configuration and adding your first interior project will unlock public launch.
          </p>
        </div>
      </div>

      {/* 4. Quick Actions Grid */}
      <div>
        <h2 className="font-serif text-lg font-semibold text-charcoal-900 mb-4">
          Quick Actions
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            {
              title: 'Prepare Portfolio',
              description: 'Configure your portfolio layout and aesthetic themes.',
              icon: Layout,
              href: '/workspace/portfolio',
              badge: 'Next Step',
            },
            {
              title: 'Project Workspace',
              description: 'Explore project case studies and showcase preparation.',
              icon: FolderKanban,
              href: '/workspace/projects',
              badge: 'Coming Soon',
            },
            {
              title: 'Explore AI Studio',
              description: 'Preview spatial rendering and design generation tools.',
              icon: Sparkles,
              href: '/workspace/ai',
              badge: 'Preview',
            },
            {
              title: 'Business Profile',
              description: 'Inspect registered studio details and contact channels.',
              icon: Building2,
              href: '/workspace/business',
              badge: 'Active',
            },
          ].map((action) => {
            const Icon = action.icon;
            return (
              <Link
                key={action.title}
                href={action.href}
                className="group bg-white border border-sand-200 rounded-2xl p-5 hover:border-bronze-300 hover:shadow-md transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="w-9 h-9 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center group-hover:bg-bronze-50 transition-colors">
                      <Icon className="w-4 h-4" />
                    </div>
                    <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-sand-100 text-charcoal-600">
                      {action.badge}
                    </span>
                  </div>
                  <h3 className="text-sm font-semibold text-charcoal-900 group-hover:text-bronze-800 transition-colors">
                    {action.title}
                  </h3>
                  <p className="text-xs text-charcoal-500 mt-1 leading-relaxed">
                    {action.description}
                  </p>
                </div>
                <div className="mt-4 pt-3 border-t border-sand-100 flex items-center justify-between text-xs font-medium text-bronze-700">
                  <span>Open module</span>
                  <ArrowUpRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                </div>
              </Link>
            );
          })}
        </div>
      </div>

      {/* 5. Setup Checklist */}
      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center justify-between pb-4 border-b border-sand-200 mb-6">
          <div>
            <h2 className="font-serif text-lg font-semibold text-charcoal-900">
              Studio Setup Checklist
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Authoritative setup roadmap derived from your active studio data.
            </p>
          </div>
          <span className="text-xs font-mono font-medium px-2.5 py-1 rounded bg-sand-100 text-charcoal-700">
            {setupChecklist.filter((i) => i.completed).length} / {setupChecklist.length} Complete
          </span>
        </div>

        <div className="divide-y divide-sand-100">
          {setupChecklist.map((item) => (
            <div key={item.id} className="py-3.5 flex items-center justify-between gap-4">
              <div className="flex items-start gap-3">
                {item.completed ? (
                  <CheckCircle2 className="w-5 h-5 text-forest-600 flex-shrink-0 mt-0.5" />
                ) : (
                  <Circle className="w-5 h-5 text-sand-400 flex-shrink-0 mt-0.5" />
                )}
                <div>
                  <h3 className={`text-xs sm:text-sm font-medium ${item.completed ? 'text-charcoal-900' : 'text-charcoal-700'}`}>
                    {item.title}
                  </h3>
                  <p className="text-[11px] text-charcoal-500 mt-0.5">
                    {item.description}
                  </p>
                </div>
              </div>

              <div>
                <Link
                  href={item.actionRoute}
                  className={`text-xs font-medium px-3 py-1.5 rounded-lg transition-colors whitespace-nowrap ${
                    item.completed
                      ? 'bg-sand-50 text-charcoal-600 hover:bg-sand-100'
                      : 'bg-bronze-50 text-bronze-800 hover:bg-bronze-100'
                  }`}
                >
                  {item.actionLabel}
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 6. Module Readiness Grid */}
      <div>
        <div className="mb-4">
          <h2 className="font-serif text-lg font-semibold text-charcoal-900">
            Workspace Modules
          </h2>
          <p className="text-xs text-charcoal-500 mt-0.5">
            Operational status and preparation state for all platform modules.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {modules.map((mod) => {
            const statusConfig: Record<string, { label: string; bg: string; text: string }> = {
              READY: { label: 'Ready', bg: 'bg-forest-50', text: 'text-forest-700' },
              NOT_CONFIGURED: { label: 'Not Configured', bg: 'bg-sand-100', text: 'text-charcoal-700' },
              NOT_STARTED: { label: 'Not Started', bg: 'bg-sand-100', text: 'text-charcoal-700' },
              COMING_SOON: { label: 'Coming Soon', bg: 'bg-sand-100', text: 'text-charcoal-500' },
              LOCKED: { label: 'Available Post-Launch', bg: 'bg-sand-100', text: 'text-charcoal-500' },
            };
            const badge = statusConfig[mod.status] || { label: mod.status, bg: 'bg-sand-100', text: 'text-charcoal-600' };

            return (
              <div
                key={mod.id}
                className="bg-white border border-sand-200 rounded-2xl p-5 flex flex-col justify-between hover:border-sand-300 transition-colors"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-semibold text-charcoal-900">
                      {mod.name}
                    </h3>
                    <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full border border-sand-200 ${badge.bg} ${badge.text}`}>
                      {badge.label}
                    </span>
                  </div>
                  <p className="text-xs text-charcoal-600 leading-relaxed mb-4">
                    {mod.description}
                  </p>
                </div>

                <div className="pt-3 border-t border-sand-100">
                  <Link
                    href={mod.route}
                    className="inline-flex items-center justify-between w-full text-xs font-medium text-bronze-800 hover:text-bronze-900"
                  >
                    <span>{mod.ctaLabel}</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 7. Truthful System Activity Feed */}
      {activityFeed && activityFeed.length > 0 && (
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
          <h2 className="font-serif text-base font-semibold text-charcoal-900 mb-4 pb-3 border-b border-sand-200">
            Recent Studio Activity
          </h2>
          <div className="space-y-4">
            {activityFeed.map((act) => (
              <div key={act.id} className="flex items-start gap-3 text-xs">
                <div className="w-7 h-7 rounded-lg bg-sand-100 text-charcoal-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Clock className="w-3.5 h-3.5" />
                </div>
                <div>
                  <p className="font-semibold text-charcoal-900">{act.title}</p>
                  <p className="text-charcoal-500 mt-0.5">{act.description}</p>
                  <p className="text-[10px] font-mono text-charcoal-400 mt-1">
                    {new Date(act.timestamp).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
