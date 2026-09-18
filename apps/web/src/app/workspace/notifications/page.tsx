import React from 'react';
import Link from 'next/link';
import { Bell, ArrowLeft, CheckCircle2, ShieldCheck } from 'lucide-react';

export default function NotificationsWorkspacePage() {
  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-4xl mx-auto space-y-8">
      <div>
        <Link
          href="/workspace"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Workspace Home</span>
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Communication
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
              Notifications & Alerts
            </h1>
          </div>
        </div>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
        <h2 className="font-serif text-base font-semibold text-charcoal-900 pb-3 border-b border-sand-200">
          System Notices
        </h2>

        <div className="space-y-3">
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-forest-600 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="text-xs sm:text-sm font-semibold text-charcoal-900">
                Professional Workspace Initialized
              </h3>
              <p className="text-xs text-charcoal-600 mt-0.5 leading-relaxed">
                Your studio identity, services catalog, and contact channels have been registered. Complete portfolio preparation to prepare for launch.
              </p>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-3">
            <CheckCircle2 className="w-5 h-5 text-bronze-700 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="text-xs sm:text-sm font-semibold text-charcoal-900">
                Session Rotation Successful
              </h3>
              <p className="text-xs text-charcoal-600 mt-0.5 leading-relaxed">
                Professional workspace enabled with active studio ownership credentials.
              </p>
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-sand-100 flex justify-end">
          <Link
            href="/workspace"
            className="text-xs font-medium text-bronze-800 hover:text-bronze-900"
          >
            ← Return to Workspace Overview
          </Link>
        </div>
      </div>
    </div>
  );
}
