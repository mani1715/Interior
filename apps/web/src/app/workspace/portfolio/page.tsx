import React from 'react';
import Link from 'next/link';
import { Layout, ArrowLeft, Layers, Palette, Globe } from 'lucide-react';

export default function PortfolioWorkspacePage() {
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
            <Layout className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Website Builder
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
              Portfolio Builder
            </h1>
          </div>
        </div>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-8 sm:p-10 text-center shadow-sm">
        <div className="w-14 h-14 rounded-2xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto mb-4">
          <Layers className="w-7 h-7" />
        </div>
        <h2 className="font-serif text-xl sm:text-2xl text-charcoal-900 mb-2">
          Portfolio Engine in Preparation
        </h2>
        <p className="text-xs sm:text-sm text-charcoal-600 max-w-md mx-auto leading-relaxed mb-8">
          Your public portfolio website will showcase your interior design philosophy, curated project case studies, and client inquiry forms with custom aesthetic themes.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left max-w-2xl mx-auto pt-6 border-t border-sand-200">
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <Palette className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Six Aesthetic Themes</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Curated editorial, minimalist, and luxury layouts tailored for interior professionals.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <Layers className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Modular Sections</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Featured projects, hero statements, practice bio, service catalog, and client testimonials.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <Globe className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Custom Subdomains</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Automatic SEO-ready routing under your reserved studio handle.
            </p>
          </div>
        </div>

        <div className="mt-8">
          <Link
            href="/workspace"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors"
          >
            <span>Return to Workspace Overview</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
