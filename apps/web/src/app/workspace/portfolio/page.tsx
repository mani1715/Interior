'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Eye,
  EyeOff,
  ArrowUp,
  ArrowDown,
  Palette,
  Layers,
  History,
  Type,
  ExternalLink,
  Save,
  CheckCircle2,
  AlertCircle,
  Smartphone,
  Monitor,
  RefreshCw,
} from 'lucide-react';
import {
  fetchPortfolio,
  initPortfolio,
  updatePortfolio,
  updateSection,
  reorderSections,
  switchTemplate,
  createVersionSnapshot,
  restoreVersionSnapshot,
} from '@/lib/portfolio/api';
import {
  PortfolioDetailResponse,
  PortfolioSectionDto,
  PortfolioTemplateKey,
  FontPairing,
} from '@/lib/portfolio/types';
import { TEMPLATE_REGISTRY, getAllTemplates } from '@/lib/portfolio/template-registry';
import { ReferenceTemplate } from '@/components/portfolio/templates/ReferenceTemplate';

export default function PortfolioBuilderPage() {
  const [portfolio, setPortfolio] = useState<PortfolioDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Active tab in builder
  const [activeTab, setActiveTab] = useState<'content' | 'sections' | 'design' | 'versions'>('content');

  // Preview mode in live panel
  const [isMobilePreview, setIsMobilePreview] = useState(false);

  // Form state for Content tab
  const [headline, setHeadline] = useState('');
  const [subheadline, setSubheadline] = useState('');
  const [bio, setBio] = useState('');
  const [designPhilosophy, setDesignPhilosophy] = useState('');
  const [yearsOfExperience, setYearsOfExperience] = useState<number | ''>('');
  const [primaryColor, setPrimaryColor] = useState('#1F2937');
  const [secondaryColor, setSecondaryColor] = useState('#F3F4F6');
  const [accentColor, setAccentColor] = useState('#C5A880');
  const [fontPairing, setFontPairing] = useState<FontPairing>('PLAYFAIR_INTER');
  const [isDirty, setIsDirty] = useState(false);

  // Version snapshot input state
  const [snapshotLabel, setSnapshotLabel] = useState('');
  const [creatingSnapshot, setCreatingSnapshot] = useState(false);

  // Load portfolio data
  const loadPortfolio = useCallback(async () => {
    try {
      setLoading(true);
      setErrorMessage(null);
      let data: PortfolioDetailResponse;
      try {
        data = await fetchPortfolio();
      } catch (err: any) {
        // If 404, automatically initialize
        if (err.status === 404) {
          data = await initPortfolio({ templateKey: 'BASIC' });
        } else {
          throw err;
        }
      }

      setPortfolio(data);
      setHeadline(data.headline || '');
      setSubheadline(data.subheadline || '');
      setBio(data.bio || '');
      setDesignPhilosophy(data.designPhilosophy || '');
      setYearsOfExperience(data.yearsOfExperience ?? '');
      setPrimaryColor(data.primaryColor || '#1F2937');
      setSecondaryColor(data.secondaryColor || '#F3F4F6');
      setAccentColor(data.accentColor || '#C5A880');
      setFontPairing(data.fontPairing || 'PLAYFAIR_INTER');
      setIsDirty(false);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to load portfolio.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPortfolio();
  }, [loadPortfolio]);

  // Save content & styles
  const handleSavePortfolio = async () => {
    if (!portfolio) return;
    try {
      setSaving(true);
      setErrorMessage(null);
      const updated = await updatePortfolio({
        headline,
        subheadline,
        bio,
        designPhilosophy,
        yearsOfExperience: yearsOfExperience === '' ? null : Number(yearsOfExperience),
        primaryColor,
        secondaryColor,
        accentColor,
        fontPairing,
        version: portfolio.version,
      });
      setPortfolio(updated);
      setIsDirty(false);
      setSuccessMessage('Portfolio settings saved successfully.');
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      if (err.status === 409) {
        setErrorMessage('Another session modified this portfolio. Please refresh to load the latest state.');
      } else {
        setErrorMessage(err.message || 'Failed to save portfolio.');
      }
    } finally {
      setSaving(false);
    }
  };

  // Section visibility toggle
  const handleToggleSectionVisibility = async (section: PortfolioSectionDto) => {
    if (!portfolio) return;
    try {
      setErrorMessage(null);
      const updated = await updateSection(section.id, {
        isVisible: !section.isVisible,
        content: section.content,
      });
      setPortfolio(updated);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to update section visibility.');
    }
  };

  // Reorder sections (Move up / down)
  const handleMoveSection = async (index: number, direction: 'up' | 'down') => {
    if (!portfolio) return;
    const sections = [...portfolio.sections].sort((a, b) => a.displayOrder - b.displayOrder);
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= sections.length) return;

    // Swap
    const temp = sections[index];
    sections[index] = sections[targetIndex];
    sections[targetIndex] = temp;

    const orderedSectionIds = sections.map((s) => s.id);
    try {
      setErrorMessage(null);
      const updated = await reorderSections({ orderedSectionIds });
      setPortfolio(updated);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to reorder sections.');
    }
  };

  // Switch template
  const handleSwitchTemplate = async (templateKey: PortfolioTemplateKey) => {
    if (!portfolio || portfolio.templateKey === templateKey) return;
    try {
      setSaving(true);
      setErrorMessage(null);
      const updated = await switchTemplate({
        templateKey,
        version: portfolio.version,
      });
      setPortfolio(updated);
      setSuccessMessage(`Switched template to ${templateKey}. Content preserved.`);
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to switch template.');
    } finally {
      setSaving(false);
    }
  };

  // Create version snapshot
  const handleCreateSnapshot = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!portfolio || !snapshotLabel.trim()) return;
    try {
      setCreatingSnapshot(true);
      setErrorMessage(null);
      const updated = await createVersionSnapshot({
        label: snapshotLabel.trim(),
      });
      setPortfolio(updated);
      setSnapshotLabel('');
      setSuccessMessage('Version snapshot created successfully.');
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to create version snapshot.');
    } finally {
      setCreatingSnapshot(false);
    }
  };

  // Restore version snapshot
  const handleRestoreSnapshot = async (versionNumber: number) => {
    if (!portfolio) return;
    if (!confirm(`Restore portfolio to version ${versionNumber}? Current un-snapshotted changes will be overwritten.`)) {
      return;
    }
    try {
      setSaving(true);
      setErrorMessage(null);
      const updated = await restoreVersionSnapshot(versionNumber, {
        version: portfolio.version,
      });
      setPortfolio(updated);
      setHeadline(updated.headline || '');
      setSubheadline(updated.subheadline || '');
      setBio(updated.bio || '');
      setDesignPhilosophy(updated.designPhilosophy || '');
      setYearsOfExperience(updated.yearsOfExperience ?? '');
      setPrimaryColor(updated.primaryColor || '#1F2937');
      setSecondaryColor(updated.secondaryColor || '#F3F4F6');
      setAccentColor(updated.accentColor || '#C5A880');
      setFontPairing(updated.fontPairing || 'PLAYFAIR_INTER');
      setIsDirty(false);
      setSuccessMessage(`Restored version ${versionNumber} successfully.`);
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to restore version snapshot.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-12 text-center text-charcoal-500 flex flex-col items-center justify-center min-h-[400px]">
        <RefreshCw className="w-6 h-6 animate-spin text-bronze-700 mb-3" />
        <p className="text-sm font-medium">Loading Portfolio Engine...</p>
      </div>
    );
  }

  if (!portfolio) {
    return (
      <div className="p-8 max-w-xl mx-auto text-center space-y-4">
        <AlertCircle className="w-8 h-8 text-amber-600 mx-auto" />
        <h2 className="text-lg font-semibold text-charcoal-900">Portfolio Not Found</h2>
        <p className="text-xs text-charcoal-600">Could not initialize or retrieve studio portfolio.</p>
        <button
          onClick={loadPortfolio}
          className="px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium"
        >
          Retry
        </button>
      </div>
    );
  }

  const sortedSections = [...portfolio.sections].sort((a, b) => a.displayOrder - b.displayOrder);
  const activeTemplateDef = TEMPLATE_REGISTRY[portfolio.templateKey] || TEMPLATE_REGISTRY.BASIC;
  const ActiveTemplateComponent = activeTemplateDef.component;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-6">
      {/* Top Header & Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sand-200 pb-4">
        <div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-1 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Workspace</span>
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="font-serif text-2xl text-charcoal-900 font-normal">
              Portfolio Builder
            </h1>
            <span
              className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold uppercase tracking-wider ${
                portfolio.status === 'READY'
                  ? 'bg-emerald-100 text-emerald-800'
                  : 'bg-sand-200 text-charcoal-700'
              }`}
            >
              {portfolio.status}
            </span>
            <span className="text-xs text-charcoal-400 font-mono">v{portfolio.version}</span>
          </div>
        </div>

        {/* Global Actions */}
        <div className="flex items-center gap-2.5">
          <Link
            href="/workspace/portfolio/preview"
            target="_blank"
            className="px-3 py-2 rounded-xl border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-800 text-xs font-medium transition-colors inline-flex items-center gap-1.5"
          >
            <ExternalLink className="w-3.5 h-3.5 text-bronze-700" />
            <span>Private Preview</span>
          </Link>

          <button
            type="button"
            onClick={handleSavePortfolio}
            disabled={saving || !isDirty}
            className={`px-4 py-2 rounded-xl text-xs font-semibold uppercase tracking-wider transition-colors inline-flex items-center gap-1.5 ${
              isDirty
                ? 'bg-charcoal-900 text-white hover:bg-charcoal-800 shadow-sm'
                : 'bg-sand-200 text-charcoal-400 cursor-not-allowed'
            }`}
          >
            {saving ? (
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Save className="w-3.5 h-3.5" />
            )}
            <span>{saving ? 'Saving...' : isDirty ? 'Save Changes' : 'Saved'}</span>
          </button>
        </div>
      </div>

      {/* Notifications */}
      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="font-bold ml-4">✕</button>
        </div>
      )}
      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="font-bold ml-4">✕</button>
        </div>
      )}

      {/* Readiness Check Notification */}
      {!portfolio.isReadyForPublish && portfolio.readinessMissingRequirements.length > 0 && (
        <div className="p-3.5 rounded-xl bg-sand-100 border border-sand-200 text-xs text-charcoal-700 flex items-start gap-2">
          <AlertCircle className="w-4 h-4 text-bronze-700 mt-0.5 shrink-0" />
          <div>
            <span className="font-semibold text-charcoal-900">Portfolio in Draft: </span>
            <span>To complete portfolio readiness, fulfill remaining items: {portfolio.readinessMissingRequirements.join(', ')}.</span>
          </div>
        </div>
      )}

      {/* Main Builder Grid: Left Editor & Right Live Preview */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Editor Column */}
        <div className="lg:col-span-6 space-y-4">
          {/* Editor Tabs Navigation */}
          <div className="flex rounded-xl bg-sand-100 p-1 text-xs font-medium">
            <button
              onClick={() => setActiveTab('content')}
              className={`flex-1 py-2 rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                activeTab === 'content' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-600 hover:text-charcoal-900'
              }`}
            >
              <Type className="w-3.5 h-3.5" />
              <span>Content</span>
            </button>
            <button
              onClick={() => setActiveTab('sections')}
              className={`flex-1 py-2 rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                activeTab === 'sections' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-600 hover:text-charcoal-900'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Sections ({sortedSections.filter((s) => s.isVisible).length})</span>
            </button>
            <button
              onClick={() => setActiveTab('design')}
              className={`flex-1 py-2 rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                activeTab === 'design' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-600 hover:text-charcoal-900'
              }`}
            >
              <Palette className="w-3.5 h-3.5" />
              <span>Design & Themes</span>
            </button>
            <button
              onClick={() => setActiveTab('versions')}
              className={`flex-1 py-2 rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                activeTab === 'versions' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-600 hover:text-charcoal-900'
              }`}
            >
              <History className="w-3.5 h-3.5" />
              <span>Snapshots ({portfolio.recentVersions?.length || 0})</span>
            </button>
          </div>

          {/* TAB 1: CONTENT */}
          {activeTab === 'content' && (
            <div className="bg-white border border-sand-200 rounded-2xl p-6 space-y-4 shadow-sm">
              <div className="border-b border-sand-100 pb-3">
                <h3 className="text-sm font-semibold text-charcoal-900">Studio Editorial Content</h3>
                <p className="text-xs text-charcoal-500">
                  Headline, bio, and design narrative shared across all themes.
                </p>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal-800 block mb-1">
                  Hero Headline
                </label>
                <input
                  type="text"
                  maxLength={200}
                  value={headline}
                  onChange={(e) => {
                    setHeadline(e.target.value);
                    setIsDirty(true);
                  }}
                  placeholder="e.g. Bespoke Interior Architecture & Spatial Curation"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                />
                <span className="text-[10px] text-charcoal-400 block text-right mt-1">
                  {headline.length}/200
                </span>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal-800 block mb-1">
                  Hero Subheadline
                </label>
                <input
                  type="text"
                  maxLength={300}
                  value={subheadline}
                  onChange={(e) => {
                    setSubheadline(e.target.value);
                    setIsDirty(true);
                  }}
                  placeholder="e.g. Designing timeless residences shaped by light, materiality, and enduring form."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                />
                <span className="text-[10px] text-charcoal-400 block text-right mt-1">
                  {subheadline.length}/300
                </span>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal-800 block mb-1">
                  Studio Narrative / Bio
                </label>
                <textarea
                  rows={4}
                  maxLength={4000}
                  value={bio}
                  onChange={(e) => {
                    setBio(e.target.value);
                    setIsDirty(true);
                  }}
                  placeholder="Describe your studio history, approach, and architectural sensibilities..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600 leading-relaxed"
                />
                <span className="text-[10px] text-charcoal-400 block text-right mt-1">
                  {bio.length}/4000
                </span>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal-800 block mb-1">
                  Design Philosophy Statement
                </label>
                <textarea
                  rows={3}
                  maxLength={2000}
                  value={designPhilosophy}
                  onChange={(e) => {
                    setDesignPhilosophy(e.target.value);
                    setIsDirty(true);
                  }}
                  placeholder="Your foundational philosophy (e.g. Quiet luxury rooted in regional craft...)"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600 leading-relaxed"
                />
                <span className="text-[10px] text-charcoal-400 block text-right mt-1">
                  {designPhilosophy.length}/2000
                </span>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal-800 block mb-1">
                  Years in Practice
                </label>
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={yearsOfExperience}
                  onChange={(e) => {
                    setYearsOfExperience(e.target.value === '' ? '' : Number(e.target.value));
                    setIsDirty(true);
                  }}
                  placeholder="e.g. 12"
                  className="w-32 px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                />
              </div>
            </div>
          )}

          {/* TAB 2: SECTIONS */}
          {activeTab === 'sections' && (
            <div className="bg-white border border-sand-200 rounded-2xl p-6 space-y-4 shadow-sm">
              <div className="border-b border-sand-100 pb-3">
                <h3 className="text-sm font-semibold text-charcoal-900">Portfolio Structure</h3>
                <p className="text-xs text-charcoal-500">
                  Toggle visibility and reorder modular page sections.
                </p>
              </div>

              <div className="space-y-2">
                {sortedSections.map((section, idx) => (
                  <div
                    key={section.id}
                    className={`p-3.5 rounded-xl border flex items-center justify-between transition-colors ${
                      section.isVisible
                        ? 'bg-sand-50/70 border-sand-200'
                        : 'bg-gray-50/70 border-dashed border-sand-200 opacity-60'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-[11px] font-bold text-charcoal-400 w-5">
                        {idx + 1}
                      </span>
                      <div>
                        <span className="text-xs font-semibold text-charcoal-900 block">
                          {section.sectionType.replace(/_/g, ' ')}
                        </span>
                        <span className="text-[10px] text-charcoal-500">
                          {section.isVisible ? 'Visible on portfolio' : 'Hidden from portfolio'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      {/* Move Up */}
                      <button
                        type="button"
                        onClick={() => handleMoveSection(idx, 'up')}
                        disabled={idx === 0}
                        aria-label={`Move ${section.sectionType} up`}
                        className={`p-1.5 rounded-lg border border-sand-200 hover:bg-white text-charcoal-700 transition-colors ${
                          idx === 0 ? 'opacity-30 cursor-not-allowed' : ''
                        }`}
                      >
                        <ArrowUp className="w-3.5 h-3.5" />
                      </button>

                      {/* Move Down */}
                      <button
                        type="button"
                        onClick={() => handleMoveSection(idx, 'down')}
                        disabled={idx === sortedSections.length - 1}
                        aria-label={`Move ${section.sectionType} down`}
                        className={`p-1.5 rounded-lg border border-sand-200 hover:bg-white text-charcoal-700 transition-colors ${
                          idx === sortedSections.length - 1 ? 'opacity-30 cursor-not-allowed' : ''
                        }`}
                      >
                        <ArrowDown className="w-3.5 h-3.5" />
                      </button>

                      {/* Visibility Toggle */}
                      <button
                        type="button"
                        onClick={() => handleToggleSectionVisibility(section)}
                        aria-label={`Toggle visibility for ${section.sectionType}`}
                        className={`p-1.5 rounded-lg border transition-colors ${
                          section.isVisible
                            ? 'border-sand-200 bg-white text-charcoal-800 hover:bg-sand-100'
                            : 'border-sand-200 bg-gray-100 text-charcoal-400 hover:bg-sand-100'
                        }`}
                      >
                        {section.isVisible ? (
                          <Eye className="w-3.5 h-3.5 text-bronze-700" />
                        ) : (
                          <EyeOff className="w-3.5 h-3.5" />
                        )}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: DESIGN & THEMES */}
          {activeTab === 'design' && (
            <div className="bg-white border border-sand-200 rounded-2xl p-6 space-y-6 shadow-sm">
              <div>
                <h3 className="text-sm font-semibold text-charcoal-900 mb-1">Aesthetic Themes</h3>
                <p className="text-xs text-charcoal-500">
                  Switch themes seamlessly. All content, sections, and ordering are strictly preserved.
                </p>
              </div>

              {/* Theme Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {getAllTemplates().map((tpl) => {
                  const isSelected = portfolio.templateKey === tpl.key;
                  return (
                    <div
                      key={tpl.key}
                      onClick={() => handleSwitchTemplate(tpl.key)}
                      className={`p-4 rounded-xl border-2 text-left cursor-pointer transition-all ${
                        isSelected
                          ? 'border-bronze-700 bg-sand-50/50 shadow-sm'
                          : 'border-sand-200 hover:border-sand-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-bold text-charcoal-900">{tpl.name}</span>
                        {isSelected && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-bronze-700 text-white">
                            Active
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-charcoal-500 leading-relaxed mb-3">
                        {tpl.tagline}
                      </p>
                      <div className="flex items-center justify-between text-[10px] text-charcoal-400 font-mono">
                        <span>{tpl.phase}</span>
                        <span className="text-bronze-700 font-sans font-medium">{tpl.status}</span>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Typography Pairing Selector */}
              <div className="pt-4 border-t border-sand-100 space-y-3">
                <div>
                  <h4 className="text-xs font-semibold text-charcoal-900">Typography Pairing</h4>
                  <p className="text-[11px] text-charcoal-500">
                    Carefully curated font combinations for luxury interior editorial layouts.
                  </p>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {[
                    { key: 'PLAYFAIR_INTER', name: 'Playfair Display + Inter', style: 'font-serif' },
                    { key: 'CORMORANT_PLUS_JAKARTA', name: 'Cormorant + Plus Jakarta', style: 'font-serif' },
                    { key: 'CINZEL_MANROPE', name: 'Cinzel + Manrope', style: 'font-serif tracking-wide' },
                    { key: 'SYNE_SPACE_GROTESK', name: 'Syne + Space Grotesk', style: 'font-sans font-bold' },
                    { key: 'FRAUNCES_OUTFIT', name: 'Fraunces + Outfit', style: 'font-serif' },
                    { key: 'BODONI_INTER', name: 'Bodoni Moda + Inter', style: 'font-serif italic' },
                  ].map((fp) => (
                    <button
                      key={fp.key}
                      type="button"
                      onClick={() => {
                        setFontPairing(fp.key as FontPairing);
                        setIsDirty(true);
                      }}
                      className={`p-3 rounded-xl border text-left text-xs transition-colors ${
                        fontPairing === fp.key
                          ? 'border-bronze-700 bg-sand-50 font-semibold text-charcoal-900'
                          : 'border-sand-200 hover:border-sand-300 text-charcoal-700'
                      }`}
                    >
                      <span className={`block text-sm mb-0.5 ${fp.style}`}>{fp.name}</span>
                      <span className="text-[10px] text-charcoal-400 font-mono">{fp.key}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Color Accents */}
              <div className="pt-4 border-t border-sand-100 space-y-3">
                <h4 className="text-xs font-semibold text-charcoal-900">Color Tokens</h4>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Primary</label>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={primaryColor}
                        onChange={(e) => {
                          setPrimaryColor(e.target.value);
                          setIsDirty(true);
                        }}
                        className="w-8 h-8 rounded-lg cursor-pointer border border-sand-200"
                      />
                      <span className="text-xs font-mono text-charcoal-700">{primaryColor}</span>
                    </div>
                  </div>
                  <div>
                    <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Secondary</label>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={secondaryColor}
                        onChange={(e) => {
                          setSecondaryColor(e.target.value);
                          setIsDirty(true);
                        }}
                        className="w-8 h-8 rounded-lg cursor-pointer border border-sand-200"
                      />
                      <span className="text-xs font-mono text-charcoal-700">{secondaryColor}</span>
                    </div>
                  </div>
                  <div>
                    <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Accent</label>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={accentColor}
                        onChange={(e) => {
                          setAccentColor(e.target.value);
                          setIsDirty(true);
                        }}
                        className="w-8 h-8 rounded-lg cursor-pointer border border-sand-200"
                      />
                      <span className="text-xs font-mono text-charcoal-700">{accentColor}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: SNAPSHOTS & HISTORY */}
          {activeTab === 'versions' && (
            <div className="bg-white border border-sand-200 rounded-2xl p-6 space-y-6 shadow-sm">
              <div>
                <h3 className="text-sm font-semibold text-charcoal-900 mb-1">Version History & Snapshots</h3>
                <p className="text-xs text-charcoal-500">
                  Save immutable milestones. You can restore your portfolio back to any snapshot at any time.
                </p>
              </div>

              {/* Create Snapshot Form */}
              <form onSubmit={handleCreateSnapshot} className="space-y-3 p-4 bg-sand-50 rounded-xl border border-sand-200">
                <label className="text-xs font-semibold text-charcoal-800 block">
                  Create New Snapshot Milestone
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={100}
                    value={snapshotLabel}
                    onChange={(e) => setSnapshotLabel(e.target.value)}
                    placeholder="e.g. Before spring editorial redesign"
                    className="flex-1 px-3 py-2 text-xs rounded-xl border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-600"
                  />
                  <button
                    type="submit"
                    disabled={creatingSnapshot || !snapshotLabel.trim()}
                    className="px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors disabled:opacity-50"
                  >
                    {creatingSnapshot ? 'Saving...' : 'Snapshot'}
                  </button>
                </div>
              </form>

              {/* Snapshots List */}
              <div className="space-y-2">
                {portfolio.recentVersions && portfolio.recentVersions.length > 0 ? (
                  portfolio.recentVersions.map((ver) => (
                    <div
                      key={ver.id}
                      className="p-3.5 rounded-xl border border-sand-200 flex items-center justify-between bg-white"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded-md bg-sand-100 text-charcoal-800 text-[10px] font-mono font-bold">
                            v{ver.versionNumber}
                          </span>
                          <span className="text-xs font-semibold text-charcoal-900">{ver.label}</span>
                        </div>
                        <span className="text-[10px] text-charcoal-400 block mt-1">
                          {new Date(ver.createdAt).toLocaleString()}
                        </span>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRestoreSnapshot(ver.versionNumber)}
                        className="px-3 py-1.5 rounded-lg border border-sand-300 text-charcoal-700 hover:bg-sand-50 text-xs font-medium transition-colors"
                      >
                        Restore
                      </button>
                    </div>
                  ))
                ) : (
                  <div className="p-8 text-center text-xs text-charcoal-400 border border-dashed border-sand-200 rounded-xl">
                    No version snapshots created yet. Create a snapshot above to preserve milestones.
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Live Preview Column */}
        <div className="lg:col-span-6 space-y-3">
          <div className="flex items-center justify-between px-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-600">
              Live Preview Frame
            </span>

            {/* Desktop / Mobile Switcher */}
            <div className="flex items-center rounded-lg bg-sand-100 p-0.5 text-xs">
              <button
                type="button"
                onClick={() => setIsMobilePreview(false)}
                className={`p-1.5 rounded-md transition-all ${
                  !isMobilePreview ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500'
                }`}
                title="Desktop View"
              >
                <Monitor className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => setIsMobilePreview(true)}
                className={`p-1.5 rounded-md transition-all ${
                  isMobilePreview ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500'
                }`}
                title="Mobile View (375px)"
              >
                <Smartphone className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Preview Container */}
          <div className="border border-sand-200 rounded-2xl bg-sand-50/40 p-4 max-h-[850px] overflow-y-auto">
            <ActiveTemplateComponent
              portfolioId={portfolio.id}
              studioId={portfolio.studioId}
              studioName="Aarav Design Atelier"
              studioSlug="aarav-atelier"
              professionalTitle="Lead Architect & Interior Designer"
              studioCity="Bengaluru"
              studioState="Karnataka"
              templateKey={portfolio.templateKey}
              headline={headline || portfolio.headline}
              subheadline={subheadline || portfolio.subheadline}
              bio={bio || portfolio.bio}
              designPhilosophy={designPhilosophy || portfolio.designPhilosophy}
              yearsOfExperience={yearsOfExperience === '' ? null : Number(yearsOfExperience)}
              primaryColor={primaryColor}
              secondaryColor={secondaryColor}
              accentColor={accentColor}
              fontPairing={fontPairing}
              publicContacts={[
                { kind: 'EMAIL', contactValue: 'studio@aarav.in' },
                { kind: 'PHONE', contactValue: '+91 98765 43210' },
                { kind: 'INSTAGRAM', contactValue: '@aarav.atelier' },
              ]}
              canonicalServices={[
                { serviceCode: 'RESIDENTIAL', serviceName: 'Residential Architecture & Interiors' },
                { serviceCode: 'KITCHEN', serviceName: 'Modular Kitchens & Bespoke Millwork' },
                { serviceCode: 'TURNKEY', serviceName: 'Turnkey Execution & Site Management' },
              ]}
              canonicalSpecialties={[
                { specialtyCode: 'WARM_CONTEMPORARY', specialtyName: 'Warm Contemporary' },
                { specialtyCode: 'INDIAN_TRADITIONAL', specialtyName: 'Indian Traditional Modern' },
              ]}
              canonicalServiceAreas={[
                { cityName: 'Bengaluru', locality: 'Indiranagar' },
                { cityName: 'Bengaluru', locality: 'Koramangala' },
              ]}
              visibleSections={sortedSections
                .filter((s) => s.isVisible)
                .map((s) => ({
                  sectionId: s.id,
                  sectionType: s.sectionType,
                  displayOrder: s.displayOrder,
                  schemaVersion: s.schemaVersion,
                  content: s.content,
                }))}
              isMobilePreview={isMobilePreview}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
