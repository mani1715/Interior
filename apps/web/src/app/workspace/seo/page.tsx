'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  Globe,
  Share2,
  Search,
  Eye,
  AlertTriangle,
  ExternalLink,
  ShieldCheck,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { fetchSeoStatus, publishStudio, unpublishStudio, updateSeoSettings } from '@/lib/seo/api';
import { SeoStatusResponse } from '@/lib/seo/types';

export default function SeoWorkspacePage() {
  const [status, setStatus] = useState<SeoStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [togglingPub, setTogglingPub] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form state
  const [metaTitleOverride, setMetaTitleOverride] = useState('');
  const [metaDescriptionOverride, setMetaDescriptionOverride] = useState('');
  const [indexingEnabled, setIndexingEnabled] = useState(true);

  const loadStatus = async () => {
    try {
      setLoading(true);
      setErrorMessage(null);
      const data = await fetchSeoStatus();
      setStatus(data);
      setMetaTitleOverride(data.metaTitleOverride || '');
      setMetaDescriptionOverride(data.metaDescriptionOverride || '');
      setIndexingEnabled(data.indexingEnabled);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to load SEO configuration');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();
  }, []);

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      const updated = await updateSeoSettings({
        metaTitleOverride: metaTitleOverride.trim() || null,
        metaDescriptionOverride: metaDescriptionOverride.trim() || null,
        indexingEnabled,
      });
      setStatus(updated);
      setSuccessMessage('SEO settings saved successfully');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to save SEO overrides');
    } finally {
      setSaving(false);
    }
  };

  const handlePublishToggle = async () => {
    if (!status) return;
    try {
      setTogglingPub(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      if (status.publicationStatus === 'PUBLISHED') {
        const res = await unpublishStudio();
        setStatus(res);
        setSuccessMessage('Studio unpublished. Public routes now return 404 and search indexing is revoked.');
      } else {
        const res = await publishStudio();
        setStatus(res);
        setSuccessMessage('Studio successfully published! Public portfolio is now accessible and indexable.');
      }
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Publication state change failed');
    } finally {
      setTogglingPub(false);
    }
  };

  const effectiveTitle =
    metaTitleOverride.trim() || status?.metaTitle || 'Interior Architecture Studio';
  const effectiveDescription =
    metaDescriptionOverride.trim() ||
    status?.metaDescription ||
    'Bespoke architectural and residential interior design practice.';

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F] p-4 sm:p-6 lg:p-10">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header Navigation */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <Link
              href="/workspace"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-2 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Workspace</span>
            </Link>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center border border-sand-200">
                <Search className="w-5 h-5" />
              </div>
              <div>
                <span className="text-[11px] font-semibold uppercase tracking-widest text-bronze-700 block">
                  Search Engine Visibility
                </span>
                <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                  SEO Center
                </h1>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={loadStatus}
              disabled={loading}
              className="p-2.5 rounded-xl border border-sand-200 bg-white hover:bg-sand-50 text-charcoal-600 transition-colors"
              title="Refresh status"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            {status?.publicationStatus === 'PUBLISHED' && (
              <a
                href={status.canonicalUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-sand-200 bg-white hover:bg-sand-50 text-xs font-medium text-charcoal-800 transition-colors"
              >
                <span>View Public Studio</span>
                <ExternalLink className="w-3.5 h-3.5 text-bronze-600" />
              </a>
            )}
          </div>
        </div>

        {/* Notifications */}
        {errorMessage && (
          <div className="p-4 rounded-xl border border-red-200 bg-red-50 text-red-700 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}
        {successMessage && (
          <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-800 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Publication Control Banner */}
        <div className="p-6 rounded-2xl border border-sand-200 bg-white shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-1">
            <div className="flex items-center gap-2.5">
              <span
                className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider ${
                  status?.publicationStatus === 'PUBLISHED'
                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                    : 'bg-sand-100 text-charcoal-700 border border-sand-200'
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    status?.publicationStatus === 'PUBLISHED' ? 'bg-emerald-600' : 'bg-charcoal-400'
                  }`}
                />
                {status?.publicationStatus === 'PUBLISHED' ? 'Published & Indexed' : 'Unpublished (Draft)'}
              </span>
              {status?.publishedAt && (
                <span className="text-xs text-charcoal-500 font-mono">
                  Live since {new Date(status.publishedAt).toLocaleDateString()}
                </span>
              )}
            </div>
            <p className="text-xs sm:text-sm text-charcoal-600 max-w-2xl">
              {status?.publicationStatus === 'PUBLISHED'
                ? 'Your studio profile, project portfolio, and canonical schema are live on public search engines.'
                : 'Publishing activates your public URL and enables search indexation. Unpublishing reverts public access immediately to 404.'}
            </p>
          </div>

          <div>
            {status?.publicationStatus === 'PUBLISHED' ? (
              <button
                onClick={handlePublishToggle}
                disabled={togglingPub}
                className="px-5 py-2.5 rounded-xl border border-red-200 bg-red-50 text-red-700 hover:bg-red-100 text-xs font-semibold transition-colors disabled:opacity-50"
              >
                {togglingPub ? 'Updating...' : 'Unpublish Studio'}
              </button>
            ) : (
              <button
                onClick={handlePublishToggle}
                disabled={togglingPub || !status?.isPublishable}
                className={`px-5 py-2.5 rounded-xl text-xs font-semibold transition-colors shadow-sm ${
                  status?.isPublishable
                    ? 'bg-[#1F1F1F] text-white hover:bg-black'
                    : 'bg-sand-100 text-charcoal-400 cursor-not-allowed border border-sand-200'
                }`}
              >
                {togglingPub ? 'Publishing...' : 'Publish Studio'}
              </button>
            )}
          </div>
        </div>

        {/* Readiness Diagnostics Checklist */}
        <div className="p-6 rounded-2xl border border-sand-200 bg-white shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-sand-200">
            <div>
              <h2 className="font-serif text-lg font-semibold text-charcoal-900">
                Publication & SEO Readiness
              </h2>
              <p className="text-xs text-charcoal-500">
                Verified criteria required for clean Google indexation and high-intent local discovery.
              </p>
            </div>
            <span className="text-xs font-mono font-medium text-charcoal-600">
              {status?.checklist.filter((i) => i.passed).length || 0} /{' '}
              {status?.checklist.length || 0} Passed
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {status?.checklist.map((item) => (
              <div
                key={item.key}
                className={`p-3.5 rounded-xl border flex items-start gap-3 transition-colors ${
                  item.passed
                    ? 'bg-sand-50/50 border-sand-200'
                    : 'bg-amber-50/40 border-amber-200'
                }`}
              >
                {item.passed ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                ) : (
                  <XCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                )}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-charcoal-900">
                      {item.label}
                    </span>
                    {item.actionUrl && !item.passed && (
                      <Link
                        href={item.actionUrl}
                        className="text-[11px] font-medium text-bronze-700 hover:underline flex items-center gap-0.5"
                      >
                        Fix
                        <ArrowLeft className="w-3 h-3 rotate-180" />
                      </Link>
                    )}
                  </div>
                  <p className="text-[11px] text-charcoal-500 mt-0.5 leading-relaxed">
                    {item.message}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Live Search & Social Previews */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Google SERP Preview */}
          <div className="p-6 rounded-2xl border border-sand-200 bg-white shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-xs font-semibold text-charcoal-700 pb-2 border-b border-sand-200">
              <Globe className="w-4 h-4 text-bronze-600" />
              <span>Google Search Appearance (Desktop & Mobile)</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-sand-200 font-sans space-y-1.5">
              <div className="flex items-center gap-1.5 text-xs text-charcoal-500 font-mono">
                <span>interior.com</span>
                <span>›</span>
                <span>professionals</span>
                <span>›</span>
                <span className="text-charcoal-800 font-medium">
                  {status?.studioSlug || 'studio-slug'}
                </span>
              </div>
              <h3 className="text-base text-[#1a0dab] font-medium hover:underline cursor-pointer line-clamp-1">
                {effectiveTitle}
              </h3>
              <p className="text-xs text-[#4d5156] leading-relaxed line-clamp-2">
                {effectiveDescription}
              </p>
            </div>

            <div className="flex items-center justify-between text-[11px] text-charcoal-400 pt-1">
              <span>Title Length: {effectiveTitle.length} / 60 chars</span>
              <span>Description: {effectiveDescription.length} / 160 chars</span>
            </div>
          </div>

          {/* Social Share Preview (OpenGraph) */}
          <div className="p-6 rounded-2xl border border-sand-200 bg-white shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-xs font-semibold text-charcoal-700 pb-2 border-b border-sand-200">
              <Share2 className="w-4 h-4 text-bronze-600" />
              <span>Social Share Preview (WhatsApp / LinkedIn / Twitter)</span>
            </div>

            <div className="rounded-xl border border-sand-200 overflow-hidden bg-sand-50">
              <div className="aspect-[1.91/1] bg-sand-200 relative flex items-center justify-center text-charcoal-500 text-xs font-mono">
                <div className="text-center p-4">
                  <Sparkles className="w-6 h-6 text-bronze-600 mx-auto mb-1 opacity-70" />
                  <span>Cover Portfolio Visual Derivative</span>
                </div>
              </div>
              <div className="p-3 bg-white border-t border-sand-200 space-y-1">
                <span className="text-[10px] font-mono uppercase tracking-wider text-charcoal-400 block">
                  INTERIOR.COM
                </span>
                <h4 className="text-xs font-semibold text-charcoal-900 line-clamp-1">
                  {effectiveTitle}
                </h4>
                <p className="text-[11px] text-charcoal-500 line-clamp-2 leading-relaxed">
                  {effectiveDescription}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* SEO Overrides & Indexing Configuration Form */}
        <form
          onSubmit={handleSaveSettings}
          className="p-6 rounded-2xl border border-sand-200 bg-white shadow-sm space-y-6"
        >
          <div>
            <h2 className="font-serif text-lg font-semibold text-charcoal-900">
              Search Appearance Overrides
            </h2>
            <p className="text-xs text-charcoal-500">
              Fine-tune the exact title and description rendered in search results. Leave blank to use auto-generated defaults.
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-xs font-semibold text-charcoal-800">
                  Custom Meta Title
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    metaTitleOverride.length > 60 ? 'text-amber-600' : 'text-charcoal-400'
                  }`}
                >
                  {metaTitleOverride.length} / 60 characters
                </span>
              </div>
              <input
                type="text"
                value={metaTitleOverride}
                onChange={(e) => setMetaTitleOverride(e.target.value)}
                placeholder={status?.metaTitle || 'e.g. Apex Studio | Luxury Interior Architects in Bengaluru'}
                maxLength={100}
                className="w-full px-3.5 py-2.5 rounded-xl border border-sand-300 text-xs bg-white text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-bronze-600/30 focus:border-bronze-600"
              />
              <p className="text-[11px] text-charcoal-500 mt-1">
                Recommended format: [Studio Name] | [Specialty] in [City].
              </p>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-xs font-semibold text-charcoal-800">
                  Custom Meta Description
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    metaDescriptionOverride.length > 160 ? 'text-amber-600' : 'text-charcoal-400'
                  }`}
                >
                  {metaDescriptionOverride.length} / 160 characters
                </span>
              </div>
              <textarea
                value={metaDescriptionOverride}
                onChange={(e) => setMetaDescriptionOverride(e.target.value)}
                placeholder={
                  status?.metaDescription ||
                  'e.g. Award-winning residential interior architects crafting bespoke minimalist homes in Bengaluru.'
                }
                rows={3}
                maxLength={300}
                className="w-full px-3.5 py-2.5 rounded-xl border border-sand-300 text-xs bg-white text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-bronze-600/30 focus:border-bronze-600"
              />
              <p className="text-[11px] text-charcoal-500 mt-1">
                Describe your studio style, experience, and service areas truthfully without keyword stuffing.
              </p>
            </div>

            <div className="pt-2 border-t border-sand-200">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={indexingEnabled}
                  onChange={(e) => setIndexingEnabled(e.target.checked)}
                  className="mt-0.5 rounded border-sand-300 text-bronze-600 focus:ring-bronze-500"
                />
                <div>
                  <span className="text-xs font-semibold text-charcoal-900 block">
                    Allow Search Engine Indexing (robots: index, follow)
                  </span>
                  <span className="text-[11px] text-charcoal-500 leading-relaxed block">
                    When unchecked, pages include <code className="font-mono text-charcoal-700">noindex, nofollow</code> headers and are excluded from XML sitemaps.
                  </span>
                </div>
              </label>
            </div>
          </div>

          <div className="pt-4 border-t border-sand-200 flex items-center justify-end">
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 rounded-xl bg-[#1F1F1F] hover:bg-black text-white text-xs font-medium transition-colors disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Appearance Settings'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
