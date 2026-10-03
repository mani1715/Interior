import React from 'react';
import Link from 'next/link';
import { FileText, ArrowLeft } from 'lucide-react';

export default function TermsOfServicePage() {
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
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Legal & Trust
            </span>
            <h1 className="font-serif text-3xl sm:text-4xl text-charcoal-900 tracking-tight">
              Terms of Service
            </h1>
          </div>
        </div>
        <p className="text-xs text-charcoal-500 mt-2">
          Last updated: October 2026 • Platform Terms Draft
        </p>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm text-charcoal-700 space-y-6">
        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">1. Platform Scope & Marketplace Independence</h2>
          <p className="text-xs leading-relaxed">
            Elégance is an architectural and interior design discovery and workspace software platform. Commercial agreements, project milestones, payments, and site work remain direct contracts between clients and independent design professionals.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">2. Professional Verification & Credentials</h2>
          <p className="text-xs leading-relaxed">
            While our team verifies official business documents (such as GST or CoA registration) submitted by studios, clients are advised to conduct their own due diligence prior to signing commercial interior execution contracts.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="font-serif text-base font-semibold text-charcoal-900">3. AI Visualizations Disclosure</h2>
          <p className="text-xs leading-relaxed">
            Renders generated in the AI Studio are conceptual design representations intended for inspiration and layout planning. Structural feasibility, electrical load calculations, and site measurements must be verified on-site by certified architects and structural engineers.
          </p>
        </section>
      </div>
    </div>
  );
}
