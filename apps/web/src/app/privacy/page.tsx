import React from 'react';
import Link from 'next/link';
import { Shield, ArrowLeft } from 'lucide-react';

export default function PrivacyPolicyPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 space-y-8">
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
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Legal & Trust
            </span>
            <h1 className="font-serif text-3xl sm:text-4xl text-charcoal-900 tracking-tight">
              Privacy Policy
            </h1>
          </div>
        </div>
        <p className="text-xs text-charcoal-500 mt-2">
          Last updated: October 2026 • Platform Draft Policy
        </p>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm prose prose-sm max-w-none text-charcoal-700 space-y-6">
        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">1. Information We Collect</h2>
          <p className="text-xs leading-relaxed">
            We collect personal identity data (display name, email address, optional contact telephone), studio business registration credentials (GST, business address, service areas), inquiry details submitted between clients and design professionals, and spatial photos uploaded for AI visualization.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">2. How Information Is Used</h2>
          <p className="text-xs leading-relaxed">
            Personal data is used solely to provide platform services: connecting homeowners with verified interior studios, powering client concept review sessions, and executing requested spatial transformations. We do not sell your personal data to data brokers.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">3. Private Collections & AI Disclosures</h2>
          <p className="text-xs leading-relaxed">
            Private mood boards, collections, and unapproved AI concepts remain strictly private to your authenticated account. Concept assets uploaded to the AI Studio are stored securely with multi-tenant row-level database isolation.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">4. Your Data Rights & Deletion</h2>
          <p className="text-xs leading-relaxed">
            You may review, update, or revoke active sessions at any time under your <Link href="/account" className="text-bronze-700 underline font-medium">Account Settings</Link>. You may also submit a formal account erasure request directly through the privacy management interface.
          </p>
        </section>
      </div>
    </div>
  );
}
