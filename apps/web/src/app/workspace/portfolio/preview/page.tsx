'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Monitor, Smartphone, RefreshCw, AlertCircle, Shield } from 'lucide-react';
import { fetchPortfolioPreview } from '@/lib/portfolio/api';
import { PortfolioPreviewResponse } from '@/lib/portfolio/types';
import { TEMPLATE_REGISTRY } from '@/lib/portfolio/template-registry';

export default function PortfolioPreviewPage() {
  const [preview, setPreview] = useState<PortfolioPreviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        setErrorMessage(null);
        const data = await fetchPortfolioPreview();
        setPreview(data);
      } catch (err: any) {
        setErrorMessage(err.message || 'Failed to load portfolio preview.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-sand-50 flex flex-col items-center justify-center text-charcoal-500">
        <RefreshCw className="w-6 h-6 animate-spin text-bronze-700 mb-3" />
        <p className="text-sm font-medium">Generating Private Portfolio Preview...</p>
      </div>
    );
  }

  if (errorMessage || !preview) {
    return (
      <div className="min-h-screen bg-sand-50 flex flex-col items-center justify-center p-6 text-center">
        <AlertCircle className="w-10 h-10 text-rose-600 mb-3" />
        <h2 className="text-lg font-semibold text-charcoal-900 mb-1">Preview Generation Failed</h2>
        <p className="text-xs text-charcoal-600 max-w-sm mb-6">{errorMessage || 'Could not fetch portfolio preview.'}</p>
        <Link
          href="/workspace/portfolio"
          className="px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium"
        >
          Return to Portfolio Builder
        </Link>
      </div>
    );
  }

  const templateDef = TEMPLATE_REGISTRY[preview.templateKey] || TEMPLATE_REGISTRY.BASIC;
  const TemplateComponent = templateDef.component;

  return (
    <div className="min-h-screen bg-sand-100/50 flex flex-col">
      {/* Strict Search Engine Exclusion for Draft Portfolio Previews */}
      <head>
        <meta name="robots" content="noindex, nofollow" />
      </head>

      {/* Top Banner: Studio Preview Control Bar */}
      <div className="sticky top-0 z-50 bg-charcoal-950 text-white px-4 sm:px-6 py-2.5 flex items-center justify-between shadow-md">
        <div className="flex items-center gap-3">
          <Link
            href="/workspace/portfolio"
            className="inline-flex items-center gap-1.5 text-xs text-sand-300 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Editor</span>
          </Link>
          <span className="hidden sm:inline-block w-px h-3.5 bg-charcoal-700" />
          <div className="flex items-center gap-2">
            <Shield className="w-3.5 h-3.5 text-bronze-400" />
            <span className="text-xs font-medium text-sand-200">
              Private Studio Preview
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-charcoal-800 text-sand-400 font-mono">
              NOT PUBLISHED
            </span>
          </div>
        </div>

        {/* Viewport Switcher */}
        <div className="flex items-center gap-2">
          <div className="flex items-center rounded-lg bg-charcoal-900 p-0.5 text-xs">
            <button
              type="button"
              onClick={() => setIsMobile(false)}
              className={`p-1.5 rounded-md transition-all ${
                !isMobile ? 'bg-charcoal-800 text-white' : 'text-sand-400 hover:text-white'
              }`}
              title="Desktop Preview"
            >
              <Monitor className="w-3.5 h-3.5" />
            </button>
            <button
              type="button"
              onClick={() => setIsMobile(true)}
              className={`p-1.5 rounded-md transition-all ${
                isMobile ? 'bg-charcoal-800 text-white' : 'text-sand-400 hover:text-white'
              }`}
              title="Mobile Preview (375px)"
            >
              <Smartphone className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Preview Body Container */}
      <div className="flex-1 p-4 sm:p-8 flex justify-center items-start">
        <div className={`transition-all duration-300 ${isMobile ? 'w-[375px]' : 'w-full max-w-6xl'}`}>
          <TemplateComponent
            portfolioId={preview.portfolioId}
            studioId={preview.studioId}
            studioName={preview.studioName}
            studioSlug={preview.studioSlug}
            professionalType={preview.professionalType}
            professionalTitle={preview.professionalTitle}
            studioCity={preview.studioCity}
            studioState={preview.studioState}
            templateKey={preview.templateKey}
            headline={preview.headline}
            subheadline={preview.subheadline}
            bio={preview.bio}
            designPhilosophy={preview.designPhilosophy}
            yearsOfExperience={preview.yearsOfExperience}
            primaryColor={preview.primaryColor}
            secondaryColor={preview.secondaryColor}
            accentColor={preview.accentColor}
            fontPairing={preview.fontPairing}
            publicContacts={preview.publicContacts}
            canonicalServices={preview.canonicalServices}
            canonicalSpecialties={preview.canonicalSpecialties}
            canonicalServiceAreas={preview.canonicalServiceAreas}
            visibleSections={preview.visibleSections}
            isMobilePreview={isMobile}
          />
        </div>
      </div>
    </div>
  );
}
