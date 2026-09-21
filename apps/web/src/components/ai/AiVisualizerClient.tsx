'use client';

import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import {
  Sparkles,
  ArrowLeft,
  Wand2,
  AlertCircle,
  CheckCircle2,
  Clock,
  XCircle,
  RotateCcw,
  Loader2,
  ImageIcon,
  Eye,
  ChevronRight,
  Shield,
  Layers,
} from 'lucide-react';
import {
  fetchAiStudioStatus,
  createAiJob,
  fetchAiJobDetail,
  cancelAiJob,
  listAiJobs,
} from '@/lib/ai/api';
import { AiJobDetail, AiStudioStatus } from '@/lib/ai/types';
import { fetchProjects } from '@/lib/projects/api';
import { ProjectSummaryDto } from '@/lib/projects/types';
import { fetchProjectMedia } from '@/lib/media/api';
import { MediaDetailResponse } from '@/lib/media/types';

const PROMPT_PRESETS = [
  'Modern Minimalist with warm oak flooring, flush baseboards, and indirect recessed ceiling cove lighting',
  'Warm Contemporary with fluted travertine feature wall, boucle seating, and brass architectural accents',
  'Scandinavian Japandi with pale timber cabinetry, wabi-sabi ceramic decor, and sheer linen drapery',
  'Industrial Loft with exposed concrete ceiling, raw brick accents, and matte black steel-framed glazing',
  'Neo-Classical Luxury with subtle wall mouldings, herringbone timber floor, and warm alabaster chandeliers',
];

export function AiVisualizerClient() {
  const searchParams = useSearchParams();
  const initialProjectId = searchParams.get('projectId');
  const initialMediaId = searchParams.get('mediaId');

  // Status & Quota
  const [studioStatus, setStudioStatus] = useState<AiStudioStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(true);

  // Projects & Media
  const [projects, setProjects] = useState<ProjectSummaryDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>(initialProjectId || '');
  const [projectMedia, setProjectMedia] = useState<MediaDetailResponse[]>([]);
  const [loadingMedia, setLoadingMedia] = useState(false);
  const [selectedMediaId, setSelectedMediaId] = useState<string>(initialMediaId || '');

  // Generation Form
  const [prompt, setPrompt] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [activeJob, setActiveJob] = useState<AiJobDetail | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // History & Comparison
  const [recentJobs, setRecentJobs] = useState<AiJobDetail[]>([]);
  const [selectedComparisonJob, setSelectedComparisonJob] = useState<AiJobDetail | null>(null);
  const [viewMode, setViewMode] = useState<'split' | 'original' | 'concept'>('split');

  const pollingRef = useRef<NodeJS.Timeout | null>(null);

  // 1. Initial Load: Status, Projects, Recent Jobs
  useEffect(() => {
    async function init() {
      try {
        const [status, projList, jobsList] = await Promise.all([
          fetchAiStudioStatus().catch(() => null),
          fetchProjects().catch(() => []),
          listAiJobs({ limit: 10 }).catch(() => ({ items: [], total: 0, limit: 10, offset: 0 })),
        ]);
        setStudioStatus(status);
        setProjects(projList);
        setRecentJobs(jobsList.items);

        if (jobsList.items.length > 0) {
          const firstSuccess = jobsList.items.find((j) => j.status === 'SUCCEEDED');
          if (firstSuccess) {
            setSelectedComparisonJob(firstSuccess);
          }
        }
      } catch (err: any) {
        setFormError(err.message || 'Failed to initialize AI studio');
      } finally {
        setStatusLoading(false);
      }
    }
    init();
  }, []);

  // 2. Load Project Media when Project Changes
  useEffect(() => {
    if (!selectedProjectId) {
      setProjectMedia([]);
      return;
    }
    async function loadMedia() {
      setLoadingMedia(true);
      try {
        const media = await fetchProjectMedia(selectedProjectId);
        // Filter out confidential client docs and pure AI concepts from source selection
        const eligible = media.filter(
          (m) => m.mediaType !== 'CLIENT_PRIVATE' && m.visibility !== 'PRIVATE'
        );
        setProjectMedia(eligible);

        // Preselect media if query param matched
        if (initialMediaId && eligible.some((m) => m.id === initialMediaId)) {
          setSelectedMediaId(initialMediaId);
        } else if (!selectedMediaId && eligible.length > 0) {
          setSelectedMediaId(eligible[0].id);
        }
      } catch (err) {
        console.error('Failed to load project media:', err);
      } finally {
        setLoadingMedia(false);
      }
    }
    loadMedia();
  }, [selectedProjectId, initialMediaId]);

  // 3. Polling for Active Job
  useEffect(() => {
    if (!activeJob || activeJob.status === 'SUCCEEDED' || activeJob.status === 'FAILED' || activeJob.status === 'CANCELLED') {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
      return;
    }

    pollingRef.current = setInterval(async () => {
      try {
        const detail = await fetchAiJobDetail(activeJob.id);
        setActiveJob(detail);

        if (detail.status === 'SUCCEEDED') {
          setSelectedComparisonJob(detail);
          setSubmitting(false);
          // Refresh recent jobs
          listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
          // Refresh status/quota
          fetchAiStudioStatus().then(setStudioStatus).catch(() => {});
        } else if (detail.status === 'FAILED' || detail.status === 'CANCELLED') {
          setSubmitting(false);
          listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
        }
      } catch (err) {
        console.error('Error polling AI job:', err);
      }
    }, 2000);

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [activeJob]);

  // Handle Form Submit
  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!selectedProjectId) {
      setFormError('Please select a project');
      return;
    }
    if (!selectedMediaId) {
      setFormError('Please select a source image to visualize');
      return;
    }
    if (!prompt.trim() || prompt.trim().length < 5) {
      setFormError('Please enter a descriptive prompt (at least 5 characters)');
      return;
    }
    if (prompt.trim().length > 500) {
      setFormError('Prompt exceeds maximum length of 500 characters');
      return;
    }

    setSubmitting(true);
    try {
      const idempotencyKey = `ai-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const job = await createAiJob({
        projectId: selectedProjectId,
        inputMediaId: selectedMediaId,
        prompt: prompt.trim(),
        idempotencyKey,
      });

      setActiveJob(job);
    } catch (err: any) {
      setSubmitting(false);
      setFormError(err.message || 'Failed to submit AI generation request');
    }
  };

  // Handle Cancel Job
  const handleCancelJob = async () => {
    if (!activeJob) return;
    setCancelling(true);
    try {
      const updated = await cancelAiJob(activeJob.id);
      setActiveJob(updated);
      setSubmitting(false);
    } catch (err: any) {
      setFormError(err.message || 'Failed to cancel generation job');
    } finally {
      setCancelling(false);
    }
  };

  const selectedMediaAsset = projectMedia.find((m) => m.id === selectedMediaId);
  const selectedMediaThumbnail = selectedMediaAsset?.derivatives?.find(
    (d) => d.variantName === 'MEDIUM' || d.variantName === 'LARGE' || d.variantName === 'THUMBNAIL'
  )?.publicUrl;

  const isConfigured = studioStatus?.isConfigured ?? false;

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-7xl mx-auto space-y-8 bg-[#FAF8F5] text-[#1F1F1F] min-h-screen">
      {/* Top Header */}
      <div>
        <Link
          href="/workspace"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Workspace Home</span>
        </Link>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center border border-sand-200">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Generative Interior Design
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                AI Concept Visualizer
              </h1>
            </div>
          </div>

          {/* Quota & Status Pill */}
          {studioStatus && (
            <div className="flex items-center gap-2 text-xs">
              {isConfigured ? (
                <div className="flex items-center gap-2 bg-white px-3.5 py-1.5 rounded-full border border-sand-200 shadow-sm">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  <span className="font-medium text-charcoal-800">
                    {studioStatus.remainingToday} / {studioStatus.dailyQuota} generations left today
                  </span>
                  <span className="text-charcoal-400">({studioStatus.providerKey})</span>
                </div>
              ) : (
                <div className="flex items-center gap-1.5 bg-amber-50 text-amber-800 px-3 py-1 rounded-full border border-amber-200 font-medium">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>Provider Not Configured</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Provider Unconfigured Banner (Truthful & Explicit) */}
      {!statusLoading && !isConfigured && (
        <div className="p-4 sm:p-5 rounded-2xl bg-amber-50/80 border border-amber-200 text-amber-900 space-y-2">
          <div className="flex items-center gap-2 font-semibold text-sm">
            <AlertCircle className="w-4 h-4 text-amber-700 flex-shrink-0" />
            <span>AI Generation Provider Not Configured</span>
          </div>
          <p className="text-xs sm:text-sm text-amber-800 leading-relaxed max-w-3xl">
            AI concept generation requires an active provider key. To enable this feature in your deployment,
            set <code className="bg-amber-100/80 px-1.5 py-0.5 rounded text-xs font-mono font-bold">interior.ai.provider</code> and{' '}
            <code className="bg-amber-100/80 px-1.5 py-0.5 rounded text-xs font-mono font-bold">interior.ai.api-key</code> in your backend environment configuration.
          </p>
        </div>
      )}

      {/* Main Grid: Visualizer Studio & Comparison Viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Visualizer Controls & Inputs */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-white rounded-2xl border border-sand-200 p-5 sm:p-6 shadow-sm space-y-5">
            <h2 className="font-serif text-lg text-charcoal-900 font-medium flex items-center gap-2">
              <Wand2 className="w-4 h-4 text-bronze-700" />
              <span>Create AI Concept</span>
            </h2>

            {formError && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center justify-between">
                <span>{formError}</span>
                <button
                  type="button"
                  onClick={() => setFormError(null)}
                  className="text-red-600 hover:text-red-900 font-medium"
                >
                  Dismiss
                </button>
              </div>
            )}

            <form onSubmit={handleGenerate} className="space-y-4">
              {/* 1. Project Selection */}
              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  1. Select Project
                </label>
                <select
                  value={selectedProjectId}
                  onChange={(e) => {
                    setSelectedProjectId(e.target.value);
                    setSelectedMediaId('');
                  }}
                  disabled={submitting || !isConfigured}
                  className="w-full rounded-xl border-sand-300 text-xs focus:border-bronze-700 focus:ring-bronze-700 bg-[#FAF8F5] py-2 px-3 disabled:opacity-60"
                >
                  <option value="">-- Choose project --</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.categoryDisplayName || p.categoryCode})
                    </option>
                  ))}
                </select>
              </div>

              {/* 2. Source Image Selection */}
              {selectedProjectId && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                      2. Source Room Photo
                    </label>
                    <span className="text-[11px] text-charcoal-500">
                      {projectMedia.length} photo{projectMedia.length === 1 ? '' : 's'} available
                    </span>
                  </div>

                  {loadingMedia ? (
                    <div className="py-6 text-center text-xs text-charcoal-400 flex items-center justify-center gap-2">
                      <Loader2 className="w-4 h-4 animate-spin text-bronze-700" />
                      <span>Loading project photos...</span>
                    </div>
                  ) : projectMedia.length === 0 ? (
                    <div className="p-3 rounded-xl bg-sand-50 border border-sand-200 text-[11px] text-charcoal-600 text-center">
                      No photos uploaded for this project yet.{' '}
                      <Link
                        href={`/workspace/projects/${selectedProjectId}`}
                        className="text-bronze-700 font-semibold underline hover:text-bronze-800"
                      >
                        Upload site photo in Project Media
                      </Link>
                    </div>
                  ) : (
                    <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 max-h-48 overflow-y-auto p-1 bg-[#FAF8F5] rounded-xl border border-sand-200">
                      {projectMedia.map((m) => {
                        const thumb = m.derivatives?.find(
                          (d) => d.variantName === 'THUMBNAIL' || d.variantName === 'MEDIUM'
                        )?.publicUrl;
                        const isSelected = selectedMediaId === m.id;

                        return (
                          <button
                            key={m.id}
                            type="button"
                            onClick={() => setSelectedMediaId(m.id)}
                            disabled={submitting || !isConfigured}
                            className={`relative aspect-square rounded-lg overflow-hidden border-2 transition-all ${
                              isSelected
                                ? 'border-bronze-700 ring-2 ring-bronze-700/30'
                                : 'border-transparent hover:border-sand-300'
                            }`}
                          >
                            {thumb ? (
                              <img src={thumb} alt={m.altText || 'Source'} className="w-full h-full object-cover" />
                            ) : (
                              <div className="w-full h-full bg-sand-100 flex items-center justify-center">
                                <ImageIcon className="w-4 h-4 text-sand-400" />
                              </div>
                            )}
                            {isSelected && (
                              <div className="absolute inset-0 bg-bronze-900/20 flex items-center justify-center">
                                <CheckCircle2 className="w-4 h-4 text-white drop-shadow" />
                              </div>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  )}

                  {selectedMediaThumbnail && (
                    <div className="mt-2 flex items-center gap-2 p-2 rounded-lg bg-sand-50 border border-sand-200">
                      <img src={selectedMediaThumbnail} alt="Selected" className="w-10 h-10 rounded object-cover" />
                      <div className="flex-1 min-w-0">
                        <span className="text-[11px] font-medium text-charcoal-800 block truncate">
                          {selectedMediaAsset?.altText || 'Selected source photo'}
                        </span>
                        <span className="text-[10px] text-charcoal-400">
                          {selectedMediaAsset?.width} × {selectedMediaAsset?.height} px
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* 3. Prompt Input */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                    3. Desired Transformation
                  </label>
                  <span
                    className={`text-[11px] font-mono ${
                      prompt.length > 500 ? 'text-red-600 font-bold' : 'text-charcoal-400'
                    }`}
                  >
                    {prompt.length} / 500
                  </span>
                </div>
                <textarea
                  rows={3}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  disabled={submitting || !isConfigured}
                  placeholder="e.g. Transform to warm minimalist aesthetic with herringbone oak floors, concealed indirect lighting, and linen upholstery..."
                  className="w-full rounded-xl border-sand-300 text-xs focus:border-bronze-700 focus:ring-bronze-700 bg-[#FAF8F5] p-3 leading-relaxed disabled:opacity-60"
                />

                {/* Prompt Presets */}
                <div className="mt-2 space-y-1">
                  <span className="text-[10px] text-charcoal-400 uppercase tracking-wider block">
                    Design Starters:
                  </span>
                  <div className="flex flex-wrap gap-1">
                    {PROMPT_PRESETS.map((preset, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setPrompt(preset)}
                        disabled={submitting || !isConfigured}
                        className="text-[10px] px-2 py-1 rounded-md bg-sand-100 hover:bg-sand-200 text-charcoal-700 transition-colors text-left"
                      >
                        {preset.split(' with ')[0]}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Submit & Progress */}
              <div className="pt-2">
                {submitting || (activeJob && (activeJob.status === 'QUEUED' || activeJob.status === 'PROCESSING')) ? (
                  <div className="space-y-3 p-4 rounded-xl bg-sand-50 border border-sand-200">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Loader2 className="w-4 h-4 animate-spin text-bronze-700" />
                        <span className="text-xs font-medium text-charcoal-800">
                          {activeJob?.status === 'QUEUED'
                            ? 'Queued in generation pipeline...'
                            : 'Synthesizing interior concept with provider...'}
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={handleCancelJob}
                        disabled={cancelling}
                        className="text-xs text-charcoal-500 hover:text-red-700 font-medium transition-colors"
                      >
                        {cancelling ? 'Cancelling...' : 'Cancel'}
                      </button>
                    </div>
                    <p className="text-[11px] text-charcoal-500">
                      Processing may take 10-30 seconds depending on provider GPU availability.
                    </p>
                  </div>
                ) : (
                  <button
                    type="submit"
                    disabled={!isConfigured || !selectedProjectId || !selectedMediaId || prompt.trim().length < 5}
                    className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-semibold shadow-sm transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Wand2 className="w-4 h-4 text-bronze-300" />
                    <span>Generate AI Concept</span>
                  </button>
                )}
              </div>
            </form>
          </div>

          {/* Recent Generation History */}
          <div className="bg-white rounded-2xl border border-sand-200 p-5 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-serif text-sm font-medium text-charcoal-900 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-charcoal-500" />
                <span>Recent Concepts</span>
              </h3>
              <span className="text-[11px] text-charcoal-400 font-mono">
                {recentJobs.length} item{recentJobs.length === 1 ? '' : 's'}
              </span>
            </div>

            {recentJobs.length === 0 ? (
              <p className="text-xs text-charcoal-400 py-3 text-center italic">
                No generations recorded yet.
              </p>
            ) : (
              <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                {recentJobs.map((job) => {
                  const isSelected = selectedComparisonJob?.id === job.id;
                  return (
                    <div
                      key={job.id}
                      onClick={() => job.status === 'SUCCEEDED' && setSelectedComparisonJob(job)}
                      className={`p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                        isSelected
                          ? 'border-bronze-700 bg-sand-50/60'
                          : 'border-sand-200 hover:border-sand-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span
                          className={`inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full ${
                            job.status === 'SUCCEEDED'
                              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                              : job.status === 'FAILED'
                              ? 'bg-red-50 text-red-800 border border-red-200'
                              : job.status === 'CANCELLED'
                              ? 'bg-sand-100 text-charcoal-600 border border-sand-300'
                              : 'bg-amber-50 text-amber-800 border border-amber-200'
                          }`}
                        >
                          {job.status === 'SUCCEEDED' && <CheckCircle2 className="w-2.5 h-2.5" />}
                          {job.status === 'FAILED' && <XCircle className="w-2.5 h-2.5" />}
                          {job.status}
                        </span>
                        <span className="text-[10px] text-charcoal-400">
                          {new Date(job.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-[11px] text-charcoal-700 line-clamp-1 font-medium">
                        {job.prompt}
                      </p>
                      {job.errorMessageSafe && (
                        <p className="text-[10px] text-red-600 line-clamp-1 mt-0.5">
                          {job.errorMessageSafe}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Original vs Concept Comparison Viewer */}
        <div className="lg:col-span-7 space-y-4">
          <div className="bg-white rounded-2xl border border-sand-200 p-5 sm:p-6 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-sand-100">
              <div>
                <span className="text-[10px] font-semibold text-bronze-700 uppercase tracking-wider block">
                  Comparison Workspace
                </span>
                <div className="flex items-center gap-2">
                  <h3 className="font-serif text-lg text-charcoal-900 font-medium">
                    Concept vs. Site Reality
                  </h3>
                  <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-sand-100 text-charcoal-700 border border-sand-200">
                    <Shield className="w-2.5 h-2.5 text-bronze-700" /> Private Concept
                  </span>
                </div>
              </div>

              {/* View Mode Toggle */}
              <div className="flex items-center gap-1 bg-[#FAF8F5] p-1 rounded-xl border border-sand-200 text-xs self-start">
                <button
                  type="button"
                  onClick={() => setViewMode('split')}
                  className={`px-3 py-1 rounded-lg font-medium transition-all ${
                    viewMode === 'split' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500 hover:text-charcoal-900'
                  }`}
                >
                  Side by Side
                </button>
                <button
                  type="button"
                  onClick={() => setViewMode('original')}
                  className={`px-3 py-1 rounded-lg font-medium transition-all ${
                    viewMode === 'original' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500 hover:text-charcoal-900'
                  }`}
                >
                  Original
                </button>
                <button
                  type="button"
                  onClick={() => setViewMode('concept')}
                  className={`px-3 py-1 rounded-lg font-medium transition-all ${
                    viewMode === 'concept' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500 hover:text-charcoal-900'
                  }`}
                >
                  Concept
                </button>
              </div>
            </div>

            {/* Viewer Content */}
            {selectedComparisonJob && selectedComparisonJob.status === 'SUCCEEDED' ? (
              <div className="space-y-4">
                {/* Images View */}
                {viewMode === 'split' ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Original Site Photo */}
                    <div className="space-y-1.5">
                      <span className="text-[11px] font-semibold text-charcoal-700 uppercase tracking-wider block">
                        Original Site Photo
                      </span>
                      <div className="relative aspect-[4/3] rounded-xl overflow-hidden border border-sand-200 bg-sand-100">
                        {selectedComparisonJob.inputPreviewUrl ? (
                          <img
                            src={selectedComparisonJob.inputPreviewUrl}
                            alt="Original site room"
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                            Original Photo
                          </div>
                        )}
                        <span className="absolute top-2 left-2 text-[10px] font-medium px-2 py-0.5 rounded bg-charcoal-900/80 text-white backdrop-blur-sm">
                          Before
                        </span>
                      </div>
                    </div>

                    {/* AI Concept Visualization */}
                    <div className="space-y-1.5">
                      <span className="text-[11px] font-semibold text-bronze-700 uppercase tracking-wider block">
                        Generated AI Concept
                      </span>
                      <div className="relative aspect-[4/3] rounded-xl overflow-hidden border border-sand-200 bg-sand-100">
                        {selectedComparisonJob.outputPreviewUrl ? (
                          <img
                            src={selectedComparisonJob.outputPreviewUrl}
                            alt="AI Concept Visualization"
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                            Concept Render
                          </div>
                        )}
                        {/* Non-removable badge overlay */}
                        <div className="absolute top-2 left-2 inline-flex items-center gap-1.5 text-[10px] font-semibold px-2.5 py-1 rounded bg-[#1F1F1F]/90 text-white border border-[#B88A5A] shadow-md backdrop-blur-sm">
                          <span className="text-[#B88A5A]">✦</span>
                          <span>AI Concept Visualization</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : viewMode === 'original' ? (
                  <div className="relative aspect-[16/10] rounded-xl overflow-hidden border border-sand-200 bg-sand-100">
                    {selectedComparisonJob.inputPreviewUrl ? (
                      <img
                        src={selectedComparisonJob.inputPreviewUrl}
                        alt="Original site photo"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                        Original Photo
                      </div>
                    )}
                    <span className="absolute top-3 left-3 text-xs font-medium px-2.5 py-1 rounded bg-charcoal-900/80 text-white backdrop-blur-sm">
                      Original Site Photo
                    </span>
                  </div>
                ) : (
                  <div className="relative aspect-[16/10] rounded-xl overflow-hidden border border-sand-200 bg-sand-100">
                    {selectedComparisonJob.outputPreviewUrl ? (
                      <img
                        src={selectedComparisonJob.outputPreviewUrl}
                        alt="AI Concept Visualization"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                        Concept Render
                      </div>
                    )}
                    <div className="absolute top-3 left-3 inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded bg-[#1F1F1F]/95 text-white border border-[#B88A5A] shadow-lg backdrop-blur-sm">
                      <span className="text-[#B88A5A]">✦</span>
                      <span>AI Concept Visualization</span>
                    </div>
                  </div>
                )}

                {/* Concept Prompt Summary */}
                <div className="p-3.5 rounded-xl bg-[#FAF8F5] border border-sand-200 text-xs space-y-1">
                  <span className="text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider block">
                    Generation Prompt:
                  </span>
                  <p className="text-charcoal-800 leading-relaxed font-medium">
                    &ldquo;{selectedComparisonJob.prompt}&rdquo;
                  </p>
                </div>

                {/* Permanent Legal/Truthful Disclaimer */}
                <div className="p-3 rounded-xl bg-sand-50 border border-sand-200 flex items-start gap-2.5">
                  <Shield className="w-4 h-4 text-bronze-700 flex-shrink-0 mt-0.5" />
                  <p className="text-[11px] text-charcoal-600 leading-relaxed">
                    <strong className="text-charcoal-800">✦ AI Concept Visualization:</strong> AI Concept Visualization — final colors, materials, proportions, and execution may differ.
                  </p>
                </div>
              </div>
            ) : (
              <div className="py-16 text-center rounded-xl border border-dashed border-sand-300 p-8 space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto">
                  <Layers className="w-6 h-6" />
                </div>
                <h4 className="font-serif text-base text-charcoal-800 font-medium">
                  No Concept Selected
                </h4>
                <p className="text-xs text-charcoal-500 max-w-sm mx-auto leading-relaxed">
                  Select a room photo and enter a prompt to generate an AI concept, or click on a completed concept in the history list to compare.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
