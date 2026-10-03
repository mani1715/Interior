import React from 'react';
import Link from 'next/link';
import { HelpCircle, ArrowLeft, MessageSquare, ShieldCheck, Sparkles, User, Building2 } from 'lucide-react';

export default function HelpCenterPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-12 space-y-10">
      <div>
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Home</span>
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <HelpCircle className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Support & Documentation
            </span>
            <h1 className="font-serif text-3xl sm:text-4xl text-charcoal-900 tracking-tight">
              Help Center & Guidance
            </h1>
          </div>
        </div>
        <p className="text-sm text-charcoal-600 mt-2">
          Find answers on hiring designers, managing your studio workspace, and generating AI spatial concepts.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* For Homeowners & Clients */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
            <User className="w-4 h-4" />
          </div>
          <h2 className="font-serif text-lg font-semibold text-charcoal-900">
            For Homeowners
          </h2>
          <ul className="space-y-2.5 text-xs text-charcoal-600">
            <li>
              <strong className="text-charcoal-900 block">Submitting an inquiry:</strong>
              Send your room details, location, and budget directly to verified studios through their portfolio page.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Direct WhatsApp chat:</strong>
              When enabled by the professional, click &ldquo;Chat on WhatsApp&rdquo; for an instant prefilled handoff.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Reviewing design concepts:</strong>
              Studios can share interactive concept review links allowing you to approve options or request revisions.
            </li>
          </ul>
        </div>

        {/* For Design Professionals */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
            <Building2 className="w-4 h-4" />
          </div>
          <h2 className="font-serif text-lg font-semibold text-charcoal-900">
            For Professionals
          </h2>
          <ul className="space-y-2.5 text-xs text-charcoal-600">
            <li>
              <strong className="text-charcoal-900 block">Studio workspace:</strong>
              Manage case studies, project photography, inquiries, and verified reviews in one central dashboard.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Lead management:</strong>
              Track incoming client briefs, assign follow-up tasks, and log consultation notes.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Studio verification:</strong>
              Submit business GST, CoA, or registration credentials to earn the Verified Professional badge.
            </li>
          </ul>
        </div>

        {/* AI Studio & Renders */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
            <Sparkles className="w-4 h-4" />
          </div>
          <h2 className="font-serif text-lg font-semibold text-charcoal-900">
            AI Spatial Visualizer
          </h2>
          <ul className="space-y-2.5 text-xs text-charcoal-600">
            <li>
              <strong className="text-charcoal-900 block">Structural preservation:</strong>
              When structure lock is enabled, room layout, window positions, and perspective are strictly maintained.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Precision inpainting:</strong>
              Use the mask tool to brush specific cabinetry or wall finishes without affecting the rest of the room.
            </li>
            <li>
              <strong className="text-charcoal-900 block">Voice dictation:</strong>
              Click the microphone button to dictate design ideas naturally in plain language.
            </li>
          </ul>
        </div>
      </div>

      {/* Need More Assistance Banner */}
      <div className="p-6 bg-sand-50 rounded-2xl border border-sand-200 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <h3 className="font-serif text-base font-semibold text-charcoal-900">Have feedback or questions?</h3>
          <p className="text-xs text-charcoal-600 mt-0.5">
            Submit platform suggestions, report issues, or contact our support team.
          </p>
        </div>
        <Link
          href="/feedback"
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors whitespace-nowrap"
        >
          <MessageSquare className="w-3.5 h-3.5" />
          <span>Send Feedback</span>
        </Link>
      </div>
    </div>
  );
}
