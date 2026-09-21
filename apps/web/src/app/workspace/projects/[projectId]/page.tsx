'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import {
  ArrowLeft,
  Save,
  CheckCircle2,
  Clock,
  Archive,
  RotateCcw,
  AlertCircle,
  Eye,
  EyeOff,
  Star,
  MapPin,
  Building,
  DollarSign,
  User,
  FileText,
  Image as ImageIcon,
  Loader2,
  Sparkles,
  Info,
} from 'lucide-react';
import {
  ProjectDetailDto,
  ProjectCategory,
  ProjectStyle,
  PropertyType,
  ProjectScope,
  VisibilityStatus,
  BudgetVisibility,
  ClientNameVisibility,
  AreaUnit,
  CANONICAL_CATEGORIES,
  CANONICAL_STYLES,
  CANONICAL_PROPERTY_TYPES,
  CANONICAL_SCOPES,
  UpdateProjectRequest,
} from '@/lib/projects/types';
import { fetchProject, updateProject, archiveProject, restoreProject } from '@/lib/projects/api';

export default function ProjectEditPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.projectId as string;

  const [project, setProject] = useState<ProjectDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState(false);

  // Form State
  const [title, setTitle] = useState('');
  const [categoryCode, setCategoryCode] = useState<ProjectCategory>('COMPLETE_HOME_INTERIOR');
  const [shortDescription, setShortDescription] = useState('');
  const [fullDescription, setFullDescription] = useState('');
  const [propertyType, setPropertyType] = useState<PropertyType | ''>('');
  const [projectScope, setProjectScope] = useState<ProjectScope | ''>('');
  const [styleCodes, setStyleCodes] = useState<ProjectStyle[]>([]);
  const [city, setCity] = useState('');
  const [district, setDistrict] = useState('');
  const [state, setState] = useState('');
  const [country, setCountry] = useState('IN');
  const [completionYear, setCompletionYear] = useState<number | ''>('');
  const [budgetVisibility, setBudgetVisibility] = useState<BudgetVisibility>('HIDDEN');
  const [budgetMin, setBudgetMin] = useState<number | ''>('');
  const [budgetMax, setBudgetMax] = useState<number | ''>('');
  const [currency, setCurrency] = useState('INR');
  const [clientNameVisibility, setClientNameVisibility] = useState<ClientNameVisibility>('HIDDEN');
  const [clientDisplayName, setClientDisplayName] = useState('');
  const [areaValue, setAreaValue] = useState<number | ''>('');
  const [areaUnit, setAreaUnit] = useState<AreaUnit>('SQ_FT');
  const [visibilityStatus, setVisibilityStatus] = useState<VisibilityStatus>('PRIVATE');
  const [featured, setFeatured] = useState(false);
  const [internalNotes, setInternalNotes] = useState('');

  const loadProject = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchProject(projectId);
      setProject(data);

      // Populate form
      setTitle(data.title || '');
      setCategoryCode(data.categoryCode || 'COMPLETE_HOME_INTERIOR');
      setShortDescription(data.shortDescription || '');
      setFullDescription(data.fullDescription || '');
      setPropertyType(data.propertyType || '');
      setProjectScope(data.projectScope || '');
      setStyleCodes(data.styleCodes || []);
      setCity(data.city || '');
      setDistrict(data.district || '');
      setState(data.state || '');
      setCountry(data.country || 'IN');
      setCompletionYear(data.completionYear ?? '');
      setBudgetVisibility(data.budgetVisibility || 'HIDDEN');
      setBudgetMin(data.budgetMin ?? '');
      setBudgetMax(data.budgetMax ?? '');
      setCurrency(data.currency || 'INR');
      setClientNameVisibility(data.clientNameVisibility || 'HIDDEN');
      setClientDisplayName(data.clientDisplayName || '');
      setAreaValue(data.areaValue ?? '');
      setAreaUnit(data.areaUnit || 'SQ_FT');
      setVisibilityStatus(data.visibilityStatus || 'PRIVATE');
      setFeatured(Boolean(data.featured));
      setInternalNotes(data.internalNotes || '');
      setIsDirty(false);
    } catch (err: any) {
      setError(err?.message || 'Failed to load project');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadProject();
  }, [loadProject]);

  const handleStyleToggle = (style: ProjectStyle) => {
    setIsDirty(true);
    setStyleCodes((prev) =>
      prev.includes(style) ? prev.filter((s) => s !== style) : [...prev, style]
    );
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!project) return;

    if (!title.trim() || title.trim().length < 3) {
      setError('Title must be at least 3 characters');
      return;
    }

    if (budgetMin !== '' && budgetMax !== '' && Number(budgetMin) > Number(budgetMax)) {
      setError('Minimum budget cannot exceed maximum budget');
      return;
    }

    if (clientNameVisibility === 'DISPLAY' && !clientDisplayName.trim()) {
      setError('Client name is required when client name visibility is set to DISPLAY');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setSuccessMessage(null);

      const updatePayload: UpdateProjectRequest = {
        version: project.version,
        title: title.trim(),
        categoryCode,
        shortDescription: shortDescription.trim() || null,
        fullDescription: fullDescription.trim() || null,
        propertyType: (propertyType as PropertyType) || null,
        projectScope: (projectScope as ProjectScope) || null,
        styleCodes,
        city: city.trim() || null,
        district: district.trim() || null,
        state: state.trim() || null,
        country: country.trim() || 'IN',
        completionYear: completionYear !== '' ? Number(completionYear) : null,
        budgetVisibility,
        budgetMin: budgetMin !== '' ? Number(budgetMin) : null,
        budgetMax: budgetMax !== '' ? Number(budgetMax) : null,
        currency,
        clientNameVisibility,
        clientDisplayName: clientDisplayName.trim() || null,
        areaValue: areaValue !== '' ? Number(areaValue) : null,
        areaUnit,
        visibilityStatus,
        featured,
        internalNotes: internalNotes.trim() || null,
      };

      const updated = await updateProject(project.id, updatePayload);
      setProject(updated);
      setIsDirty(false);
      setSuccessMessage('Project updated successfully.');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      if (err?.message?.includes('409') || err?.status === 409) {
        setError('Version Conflict: Another user or window updated this project. Please reload the page to retrieve the latest version.');
      } else {
        setError(err?.message || 'Failed to save project');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleToggleArchive = async () => {
    if (!project) return;
    try {
      if (project.projectStatus === 'ARCHIVED') {
        const restored = await restoreProject(project.id, project.version);
        setProject(restored);
        setSuccessMessage('Project restored to draft/ready.');
      } else {
        if (!confirm(`Are you sure you want to archive "${project.title}"? It will be removed from your portfolio.`)) {
          return;
        }
        const archived = await archiveProject(project.id, project.version);
        setProject(archived);
        setSuccessMessage('Project archived and removed from portfolio.');
      }
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setError(err?.message || 'Action failed');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#FAF8F5] flex items-center justify-center">
        <div className="text-center space-y-3">
          <Loader2 className="w-8 h-8 text-bronze-700 animate-spin mx-auto" />
          <p className="text-xs text-charcoal-500">Loading project details...</p>
        </div>
      </div>
    );
  }

  if (!project) {
    return (
      <div className="min-h-screen bg-[#FAF8F5] p-10 text-center">
        <p className="text-xs text-red-600 mb-4">Project not found or access denied.</p>
        <Link
          href="/workspace/projects"
          className="inline-flex items-center gap-2 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Return to Projects List</span>
        </Link>
      </div>
    );
  }

  const isArchived = project.projectStatus === 'ARCHIVED';

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F]">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        {/* Navigation & Actions Topbar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sand-200/80 pb-4">
          <div className="space-y-1">
            <Link
              href="/workspace/projects"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Projects List</span>
            </Link>

            <div className="flex items-center gap-3">
              <h1 className="font-serif text-2xl sm:text-3xl font-normal text-charcoal-900">
                {title || 'Untitled Project'}
              </h1>

              {/* Status Pill */}
              {project.isReady ? (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-800 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Ready
                </span>
              ) : isArchived ? (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-charcoal-600 bg-sand-100 px-2.5 py-0.5 rounded-full border border-sand-300">
                  <Archive className="w-3.5 h-3.5" /> Archived
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-amber-800 bg-amber-50 px-2.5 py-0.5 rounded-full border border-amber-200">
                  <Clock className="w-3.5 h-3.5" /> Draft
                </span>
              )}
            </div>

            <div className="flex items-center gap-2 text-xs text-charcoal-500">
              <span>Slug:</span>
              <code className="bg-sand-100 px-1.5 py-0.5 rounded text-[11px] font-mono text-charcoal-700">
                {project.slug}
              </code>
              <span>•</span>
              <span>Version: {project.version}</span>
              {isDirty && (
                <>
                  <span>•</span>
                  <span className="text-amber-700 font-medium">Unsaved changes</span>
                </>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleToggleArchive}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-700 text-xs font-medium transition-colors"
            >
              {isArchived ? (
                <>
                  <RotateCcw className="w-3.5 h-3.5 text-emerald-700" />
                  <span>Restore Project</span>
                </>
              ) : (
                <>
                  <Archive className="w-3.5 h-3.5 text-charcoal-500" />
                  <span>Archive</span>
                </>
              )}
            </button>

            {!isArchived && (
              <button
                type="submit"
                form="project-form"
                disabled={saving}
                className="inline-flex items-center gap-2 px-5 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors disabled:opacity-50 shadow-sm"
              >
                {saving ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Save className="w-3.5 h-3.5 text-bronze-300" />
                )}
                <span>{saving ? 'Saving...' : 'Save Changes'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Feedback Banners */}
        {error && (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={() => setError(null)} className="text-red-600 hover:text-red-900 font-medium">
              Dismiss
            </button>
          </div>
        )}

        {successMessage && (
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Server-Derived Readiness Checklist Banner */}
        <div
          className={`p-4 rounded-xl border ${
            project.isReady
              ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900'
              : 'bg-amber-50/70 border-amber-200 text-amber-900'
          }`}
        >
          <div className="flex items-start gap-3">
            {project.isReady ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            ) : (
              <Clock className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            )}
            <div className="space-y-1">
              <h4 className="text-xs font-semibold">
                {project.isReady
                  ? 'Project Story Complete & Portfolio Ready'
                  : 'Project Incomplete (Draft Status)'}
              </h4>
              <p className="text-xs opacity-90">
                {project.isReady
                  ? 'All mandatory requirements (title, category, short summary, city, and state) are met. When visibility is set to Portfolio, this project appears in your live portfolio.'
                  : 'To transition this project from DRAFT to READY, complete the following missing items:'}
              </p>
              {!project.isReady && project.missingReadinessFields.length > 0 && (
                <ul className="list-disc list-inside text-[11px] pt-1 space-y-0.5 font-medium">
                  {project.missingReadinessFields.map((field) => (
                    <li key={field}>{field}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>

        {/* Main Editor Form */}
        <form id="project-form" onSubmit={handleSave} className="space-y-6">
          {/* Section 1: Core Story Information */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-4">
            <div className="border-b border-sand-100 pb-3">
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                1. Story & Narrative Details
              </h3>
              <p className="text-xs text-charcoal-500">
                Core naming and editorial description displayed on cards and project pages.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Project Title <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  maxLength={120}
                  value={title}
                  onChange={(e) => {
                    setTitle(e.target.value);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Primary Category <span className="text-red-500">*</span>
                </label>
                <select
                  value={categoryCode}
                  onChange={(e) => {
                    setCategoryCode(e.target.value as ProjectCategory);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white text-charcoal-900"
                >
                  {CANONICAL_CATEGORIES.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold text-charcoal-800">
                  Short Summary <span className="text-xs text-charcoal-400 font-normal">(Min 10 characters for Ready status)</span>
                </label>
                <span className="text-[11px] text-charcoal-400">{shortDescription.length}/300</span>
              </div>
              <textarea
                rows={2}
                maxLength={300}
                placeholder="Crisp 1-2 sentence overview for cards and summaries..."
                value={shortDescription}
                onChange={(e) => {
                  setShortDescription(e.target.value);
                  setIsDirty(true);
                }}
                className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold text-charcoal-800">
                  Full Case Study & Narrative
                </label>
                <span className="text-[11px] text-charcoal-400">{fullDescription.length}/4000</span>
              </div>
              <textarea
                rows={5}
                maxLength={4000}
                placeholder="Comprehensive design narrative: the client brief, spatial challenges, materials, woodwork craftsmanship, and finishing details..."
                value={fullDescription}
                onChange={(e) => {
                  setFullDescription(e.target.value);
                  setIsDirty(true);
                }}
                className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
              />
            </div>
          </div>

          {/* Section 2: Architectural Styles & Scope */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-4">
            <div className="border-b border-sand-100 pb-3">
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                2. Architectural Classification
              </h3>
              <p className="text-xs text-charcoal-500">
                Property typology, execution scope, and design aesthetics.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Property Type
                </label>
                <select
                  value={propertyType}
                  onChange={(e) => {
                    setPropertyType(e.target.value as PropertyType);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white text-charcoal-900"
                >
                  <option value="">-- Unspecified --</option>
                  {CANONICAL_PROPERTY_TYPES.map((t) => (
                    <option key={t.code} value={t.code}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Project Scope
                </label>
                <select
                  value={projectScope}
                  onChange={(e) => {
                    setProjectScope(e.target.value as ProjectScope);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white text-charcoal-900"
                >
                  <option value="">-- Unspecified --</option>
                  {CANONICAL_SCOPES.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Completion Year
                </label>
                <input
                  type="number"
                  min={1990}
                  max={2100}
                  placeholder="e.g. 2024"
                  value={completionYear}
                  onChange={(e) => {
                    setCompletionYear(e.target.value ? Number(e.target.value) : '');
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Carpet / Super Area
                </label>
                <div className="flex gap-1.5">
                  <input
                    type="number"
                    min={0}
                    placeholder="e.g. 2400"
                    value={areaValue}
                    onChange={(e) => {
                      setAreaValue(e.target.value ? Number(e.target.value) : '');
                      setIsDirty(true);
                    }}
                    className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                  />
                  <select
                    value={areaUnit}
                    onChange={(e) => {
                      setAreaUnit(e.target.value as AreaUnit);
                      setIsDirty(true);
                    }}
                    className="text-xs px-2 py-2 rounded-lg border border-sand-300 bg-white text-charcoal-900"
                  >
                    <option value="SQ_FT">sq ft</option>
                    <option value="SQ_M">sq m</option>
                  </select>
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-charcoal-800 mb-2">
                Aesthetic & Interior Styles (Select all that apply)
              </label>
              <div className="flex flex-wrap gap-2">
                {CANONICAL_STYLES.map((style) => {
                  const isSelected = styleCodes.includes(style.code);
                  return (
                    <button
                      type="button"
                      key={style.code}
                      onClick={() => handleStyleToggle(style.code)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                        isSelected
                          ? 'bg-charcoal-900 text-white shadow-sm'
                          : 'bg-sand-50 border border-sand-300 text-charcoal-700 hover:bg-sand-100'
                      }`}
                    >
                      {style.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Section 3: Geographic Location */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-4">
            <div className="border-b border-sand-100 pb-3">
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                3. Geographic Location
              </h3>
              <p className="text-xs text-charcoal-500">
                City and state are required for Ready status and public discovery.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  City <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. Mumbai, Bengaluru"
                  value={city}
                  onChange={(e) => {
                    setCity(e.target.value);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  District / Sub-region
                </label>
                <input
                  type="text"
                  placeholder="e.g. Mumbai Suburban, Bengaluru Urban"
                  value={district}
                  onChange={(e) => {
                    setDistrict(e.target.value);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  State <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. Maharashtra, Karnataka"
                  value={state}
                  onChange={(e) => {
                    setState(e.target.value);
                    setIsDirty(true);
                  }}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>
            </div>
          </div>

          {/* Section 4: Portfolio Visibility & Privacy */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-4">
            <div className="border-b border-sand-100 pb-3">
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                4. Portfolio & Client Privacy Controls
              </h3>
              <p className="text-xs text-charcoal-500">
                Determine visibility in your public website and protect confidential client financials.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Visibility Status */}
              <div className="p-4 rounded-xl bg-sand-50/60 border border-sand-200 space-y-2">
                <label className="block text-xs font-semibold text-charcoal-800">
                  Portfolio Website Visibility
                </label>
                <div className="space-y-1.5">
                  <label className="flex items-center gap-2 text-xs text-charcoal-700 cursor-pointer">
                    <input
                      type="radio"
                      name="visibilityStatus"
                      value="PORTFOLIO"
                      checked={visibilityStatus === 'PORTFOLIO'}
                      onChange={() => {
                        setVisibilityStatus('PORTFOLIO');
                        setIsDirty(true);
                      }}
                      className="text-bronze-700 focus:ring-bronze-700"
                    />
                    <span>Show in Portfolio Website</span>
                  </label>
                  <label className="flex items-center gap-2 text-xs text-charcoal-700 cursor-pointer">
                    <input
                      type="radio"
                      name="visibilityStatus"
                      value="PRIVATE"
                      checked={visibilityStatus === 'PRIVATE'}
                      onChange={() => {
                        setVisibilityStatus('PRIVATE');
                        setIsDirty(true);
                      }}
                      className="text-bronze-700 focus:ring-bronze-700"
                    />
                    <span>Private (Internal Studio Only)</span>
                  </label>
                </div>
              </div>

              {/* Featured Toggle */}
              <div className="p-4 rounded-xl bg-sand-50/60 border border-sand-200 flex items-start gap-3">
                <input
                  type="checkbox"
                  id="featured-toggle"
                  checked={featured}
                  onChange={(e) => {
                    setFeatured(e.target.checked);
                    setIsDirty(true);
                  }}
                  className="mt-0.5 rounded text-bronze-700 focus:ring-bronze-700"
                />
                <label htmlFor="featured-toggle" className="cursor-pointer space-y-0.5">
                  <span className="block text-xs font-semibold text-charcoal-800">
                    Feature on Portfolio Homepage
                  </span>
                  <span className="block text-[11px] text-charcoal-500 leading-relaxed">
                    Highlights this project in the Featured Projects carousel or hero split.
                  </span>
                </label>
              </div>
            </div>

            {/* Commercial Budget */}
            <div className="p-4 rounded-xl border border-sand-200 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-xs font-semibold text-charcoal-800">Commercial Budget Display</h4>
                  <p className="text-[11px] text-charcoal-500">
                    Controls whether estimated project budgets are shared with website visitors.
                  </p>
                </div>
                <select
                  value={budgetVisibility}
                  onChange={(e) => {
                    setBudgetVisibility(e.target.value as BudgetVisibility);
                    setIsDirty(true);
                  }}
                  className="text-xs py-1.5 px-3 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:ring-bronze-700"
                >
                  <option value="HIDDEN">Confidential (Hidden)</option>
                  <option value="RANGE">Price Range (Min - Max)</option>
                  <option value="STARTING_FROM">Starting Price (From Min)</option>
                </select>
              </div>

              {budgetVisibility !== 'HIDDEN' && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-sand-100">
                  <div>
                    <label className="block text-xs font-medium text-charcoal-700 mb-1">
                      Minimum Budget (INR)
                    </label>
                    <input
                      type="number"
                      min={0}
                      placeholder="e.g. 2500000"
                      value={budgetMin}
                      onChange={(e) => {
                        setBudgetMin(e.target.value ? Number(e.target.value) : '');
                        setIsDirty(true);
                      }}
                      className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40"
                    />
                  </div>
                  {budgetVisibility === 'RANGE' && (
                    <div>
                      <label className="block text-xs font-medium text-charcoal-700 mb-1">
                        Maximum Budget (INR)
                      </label>
                      <input
                        type="number"
                        min={0}
                        placeholder="e.g. 3500000"
                        value={budgetMax}
                        onChange={(e) => {
                          setBudgetMax(e.target.value ? Number(e.target.value) : '');
                          setIsDirty(true);
                        }}
                        className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40"
                      />
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Client Privacy */}
            <div className="p-4 rounded-xl border border-sand-200 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-xs font-semibold text-charcoal-800">Client Identity Privacy</h4>
                  <p className="text-[11px] text-charcoal-500">
                    Protect client confidentiality unless explicit consent to display their name was granted.
                  </p>
                </div>
                <select
                  value={clientNameVisibility}
                  onChange={(e) => {
                    setClientNameVisibility(e.target.value as ClientNameVisibility);
                    setIsDirty(true);
                  }}
                  className="text-xs py-1.5 px-3 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:ring-bronze-700"
                >
                  <option value="HIDDEN">Confidential (Anonymous)</option>
                  <option value="DISPLAY">Display Client Name</option>
                </select>
              </div>

              {clientNameVisibility === 'DISPLAY' && (
                <div className="pt-2 border-t border-sand-100">
                  <label className="block text-xs font-medium text-charcoal-700 mb-1">
                    Client Display Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Sharma Family, Dr. Roy Residence"
                    value={clientDisplayName}
                    onChange={(e) => {
                      setClientDisplayName(e.target.value);
                      setIsDirty(true);
                    }}
                    className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40"
                  />
                </div>
              )}
            </div>
          </div>

          {/* Section 5: Media Engine Truthfulness Banner */}
          <div className="bg-sand-50/70 border border-sand-200 rounded-xl p-5 shadow-sm space-y-2">
            <div className="flex items-center gap-2 text-charcoal-800">
              <ImageIcon className="w-4 h-4 text-bronze-700" />
              <h3 className="font-serif text-sm font-medium">Project Photography & Media</h3>
            </div>
            <p className="text-xs text-charcoal-600 leading-relaxed">
              High-resolution photography, before/after comparison sliders, and room-by-room media will be managed here once the Phase 19 Media Engine is deployed. No placeholder media uploads are active in Phase 18.
            </p>
          </div>

          {/* Section 6: Internal Studio Notes */}
          <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-3">
            <div>
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                5. Internal Studio Notes (Private)
              </h3>
              <p className="text-xs text-charcoal-500">
                Strictly confidential studio notes, contractor references, or execution milestones. Never rendered in public portfolios.
              </p>
            </div>
            <textarea
              rows={3}
              maxLength={2000}
              placeholder="Private contractor records, supplier codes, execution remarks..."
              value={internalNotes}
              onChange={(e) => {
                setInternalNotes(e.target.value);
                setIsDirty(true);
              }}
              className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
            />
          </div>

          {/* Bottom Save Bar */}
          {!isArchived && (
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-sand-200">
              <Link
                href="/workspace/projects"
                className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 text-xs font-medium hover:bg-sand-50 transition-colors"
              >
                Back to Projects
              </Link>
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors disabled:opacity-50 shadow-sm"
              >
                {saving ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Save className="w-3.5 h-3.5 text-bronze-300" />
                )}
                <span>{saving ? 'Saving...' : 'Save Project Changes'}</span>
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
