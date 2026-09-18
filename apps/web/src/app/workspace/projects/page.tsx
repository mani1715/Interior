import React from 'react';
import Link from 'next/link';
import { FolderKanban, ArrowLeft, Grid, Image as ImageIcon, MapPin } from 'lucide-react';

export default function ProjectsWorkspacePage() {
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
            <FolderKanban className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Work Showcase
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
              Interior Projects
            </h1>
          </div>
        </div>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-8 sm:p-10 text-center shadow-sm">
        <div className="w-14 h-14 rounded-2xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto mb-4">
          <FolderKanban className="w-7 h-7" />
        </div>
        <h2 className="font-serif text-xl sm:text-2xl text-charcoal-900 mb-2">
          Project Workspace
        </h2>
        <p className="text-xs sm:text-sm text-charcoal-600 max-w-md mx-auto leading-relaxed mb-8">
          The comprehensive project publishing suite is currently in preparation. Once activated, you will be able to organize residential and commercial project case studies, room photos, and material specifications.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left max-w-2xl mx-auto pt-6 border-t border-sand-200">
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <Grid className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Room-by-Room Breakdown</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Tag living rooms, kitchens, bedrooms, bathrooms, and commercial zones.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <ImageIcon className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Before & After Galleries</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Showcase project transformations with interactive before/after split sliders.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-sand-50 border border-sand-200">
            <MapPin className="w-4 h-4 text-bronze-700 mb-2" />
            <h3 className="text-xs font-semibold text-charcoal-900">Location Tagging</h3>
            <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
              Connect completed projects to regional discovery search in your service areas.
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
