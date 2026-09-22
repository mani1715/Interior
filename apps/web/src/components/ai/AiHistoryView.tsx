'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  AiJobDetail,
  EditingMode,
  AiJobStatus,
  VariationStrategy,
  CreateVariationPayload,
  CreateClientReviewPayload,
  CreateClientReviewResponse,
} from '@/lib/ai/types';
import {
  fetchJobHistory,
  toggleShortlist,
  toggleStudioSelected,
  createVariation,
  createClientReview,
} from '@/lib/ai/api';
import {
  Star,
  CheckCircle2,
  GitBranch,
  Wand2,
  Share2,
  Copy,
  ExternalLink,
  Filter,
  Loader2,
  AlertCircle,
  Clock,
  Sparkles,
  Layers,
} from 'lucide-react';

interface AiHistoryViewProps {
  projectId?: string;
  studioId?: string;
  onSelectJobForCompare?: (job: AiJobDetail) => void;
  onVariationCreated?: (job: AiJobDetail) => void;
}

export function AiHistoryView({
  projectId,
  studioId,
  onSelectJobForCompare,
  onVariationCreated,
}: AiHistoryViewProps) {
  const [jobs, setJobs] = useState<AiJobDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [shortlistedOnly, setShortlistedOnly] = useState(false);
  const [modeFilter, setModeFilter] = useState<EditingMode | ''>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  // Selection for client review
  const [selectedJobIds, setSelectedJobIds] = useState<Set<string>>(new Set());

  // Variation Modal State
  const [variationModalJob, setVariationModalJob] = useState<AiJobDetail | null>(null);
  const [variationStrategy, setVariationStrategy] = useState<VariationStrategy>('REFINE_ORIGINAL');
  const [variationPrompt, setVariationPrompt] = useState('');
  const [reuseMask, setReuseMask] = useState(true);
  const [submittingVariation, setSubmittingVariation] = useState(false);
  const [variationError, setVariationError] = useState<string | null>(null);

  // Create Review Modal State
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewMessage, setReviewMessage] = useState('');
  const [reviewExpiryDays, setReviewExpiryDays] = useState(14);
  const [includeOriginal, setIncludeOriginal] = useState(true);
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewResult, setReviewResult] = useState<CreateClientReviewResponse | null>(null);
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  const loadHistory = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await fetchJobHistory(
        {
          projectId: projectId || undefined,
          editingMode: modeFilter || undefined,
          shortlistedOnly: shortlistedOnly || undefined,
          page,
          limit: 18,
        },
        studioId
      );
      setJobs(res.items);
      setTotalPages(res.totalPages);
      setTotalItems(res.totalItems);
    } catch (err: any) {
      setError(err?.envelope?.message || 'Failed to load generation history');
    } finally {
      setLoading(false);
    }
  }, [projectId, studioId, modeFilter, shortlistedOnly, page]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const handleToggleShortlist = async (jobId: string) => {
    try {
      const updated = await toggleShortlist(jobId, studioId);
      setJobs((prev) => prev.map((j) => (j.id === jobId ? updated : j)));
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to toggle shortlist');
    }
  };

  const handleToggleStudioSelected = async (jobId: string) => {
    try {
      const updated = await toggleStudioSelected(jobId, studioId);
      setJobs((prev) => prev.map((j) => (j.id === jobId ? updated : j)));
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to toggle studio selection');
    }
  };

  const handleToggleSelectForReview = (jobId: string) => {
    setSelectedJobIds((prev) => {
      const next = new Set(prev);
      if (next.has(jobId)) {
        next.delete(jobId);
      } else {
        if (next.size >= 10) {
          alert('Maximum of 10 concepts can be selected for a single client review.');
          return prev;
        }
        next.add(jobId);
      }
      return next;
    });
  };

  const openVariationModal = (job: AiJobDetail) => {
    setVariationModalJob(job);
    setVariationPrompt(job.prompt);
    setVariationStrategy('REFINE_ORIGINAL');
    setReuseMask(job.editingMode === 'PRECISION_MASK');
    setVariationError(null);
  };

  const handleCreateVariation = async () => {
    if (!variationModalJob) return;
    try {
      setSubmittingVariation(true);
      setVariationError(null);
      const payload: CreateVariationPayload = {
        variationStrategy,
        prompt: variationPrompt.trim(),
        preserveStructure: variationModalJob.preserveStructure ?? true,
        editingMode: variationModalJob.editingMode,
        reuseParentMask: variationModalJob.editingMode === 'PRECISION_MASK' ? reuseMask : false,
      };

      const newJob = await createVariation(variationModalJob.id, payload, studioId);
      setVariationModalJob(null);
      if (onVariationCreated) {
        onVariationCreated(newJob);
      }
      await loadHistory();
    } catch (err: any) {
      setVariationError(err?.envelope?.message || 'Failed to submit variation job');
    } finally {
      setSubmittingVariation(false);
    }
  };

  const openCreateReviewModal = () => {
    if (selectedJobIds.size === 0) {
      // Auto-select all shortlisted if none explicitly checked
      const shortlistedIds = jobs.filter((j) => j.isShortlisted && j.status === 'SUCCEEDED').map((j) => j.id);
      if (shortlistedIds.length > 0) {
        setSelectedJobIds(new Set(shortlistedIds.slice(0, 10)));
      } else {
        alert('Please select at least one succeeded concept to include in the review.');
        return;
      }
    }
    setReviewTitle('Concept Presentation');
    setReviewMessage('');
    setReviewResult(null);
    setReviewError(null);
    setReviewModalOpen(true);
  };

  const handleCreateClientReview = async () => {
    if (!projectId && jobs.length > 0) {
      // fallback to first job's projectId
    }
    const resolvedProjId = projectId || (jobs[0] ? jobs[0].projectId : '');
    if (!resolvedProjId) {
      setReviewError('Project ID is required to create a client review.');
      return;
    }
    if (selectedJobIds.size === 0) {
      setReviewError('Select at least one concept for the review.');
      return;
    }

    try {
      setSubmittingReview(true);
      setReviewError(null);
      const payload: CreateClientReviewPayload = {
        projectId: resolvedProjId,
        title: reviewTitle.trim() || 'Concept Options Review',
        customMessage: reviewMessage.trim() || undefined,
        expiryDays: reviewExpiryDays,
        includeOriginal,
        conceptJobIds: Array.from(selectedJobIds),
      };

      const res = await createClientReview(payload, studioId);
      setReviewResult(res);
      setSelectedJobIds(new Set());
    } catch (err: any) {
      setReviewError(err?.envelope?.message || 'Failed to create client review link');
    } finally {
      setSubmittingReview(false);
    }
  };

  const copyReviewLinkToClipboard = (url: string) => {
    const fullUrl = typeof window !== 'undefined' ? `${window.location.origin}${url}` : url;
    navigator.clipboard.writeText(fullUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Top Filter & Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-sand-200 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          {/* Shortlist Filter */}
          <button
            type="button"
            onClick={() => setShortlistedOnly(!shortlistedOnly)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1.5 border transition-all ${
              shortlistedOnly
                ? 'bg-amber-50 text-amber-800 border-amber-300'
                : 'bg-[#FAF8F5] text-charcoal-600 border-sand-300 hover:bg-sand-100'
            }`}
          >
            <Star className={`w-3.5 h-3.5 ${shortlistedOnly ? 'fill-amber-500 text-amber-500' : 'text-charcoal-400'}`} />
            <span>Shortlisted Only</span>
          </button>

          {/* Mode Filter */}
          <select
            value={modeFilter}
            onChange={(e) => setModeFilter(e.target.value as EditingMode | '')}
            className="rounded-full border border-sand-300 text-xs py-1.5 px-3 bg-[#FAF8F5] text-charcoal-700"
          >
            <option value="">All Modes</option>
            <option value="FULL_IMAGE">Full Concept</option>
            <option value="PRECISION_MASK">Precision Edit</option>
          </select>

          <span className="text-xs text-charcoal-400 ml-1">
            {totalItems} total generation{totalItems === 1 ? '' : 's'}
          </span>
        </div>

        {/* Action: Package into Client Review */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={openCreateReviewModal}
            className="px-4 py-2 bg-bronze-700 hover:bg-bronze-800 text-white rounded-xl text-xs font-semibold shadow-sm flex items-center gap-2 transition-colors"
          >
            <Share2 className="w-3.5 h-3.5" />
            <span>
              Create Client Review {selectedJobIds.size > 0 && `(${selectedJobIds.size})`}
            </span>
          </button>
        </div>
      </div>

      {/* Grid of Concept Jobs */}
      {loading ? (
        <div className="py-16 text-center text-charcoal-400 flex items-center justify-center gap-2">
          <Loader2 className="w-5 h-5 animate-spin text-bronze-700" />
          <span className="text-sm">Loading concept history...</span>
        </div>
      ) : error ? (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      ) : jobs.length === 0 ? (
        <div className="py-16 text-center text-charcoal-500 bg-white rounded-2xl border border-sand-200 p-8 space-y-2">
          <Sparkles className="w-8 h-8 text-sand-300 mx-auto" />
          <p className="font-serif text-lg text-charcoal-800">No generation history found</p>
          <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
            Generate concepts in the studio tab or adjust your filters above.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {jobs.map((job) => {
            const isSelected = selectedJobIds.has(job.id);
            const isPrecision = job.editingMode === 'PRECISION_MASK';
            const hasParent = Boolean(job.parentJobId);

            return (
              <div
                key={job.id}
                className={`bg-white rounded-2xl border transition-all shadow-sm overflow-hidden flex flex-col justify-between ${
                  job.isStudioSelected
                    ? 'border-bronze-700 ring-2 ring-bronze-700/20'
                    : job.isShortlisted
                    ? 'border-amber-300'
                    : 'border-sand-200'
                }`}
              >
                {/* Image & Overlay Header */}
                <div className="relative aspect-video bg-sand-100 overflow-hidden group">
                  {job.outputPreviewUrl ? (
                    <img
                      src={job.outputPreviewUrl}
                      alt={job.conceptLabel || 'Concept'}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                      {job.status === 'PROCESSING' || job.status === 'QUEUED' ? (
                        <div className="flex items-center gap-2">
                          <Loader2 className="w-4 h-4 animate-spin text-bronze-700" />
                          <span>Generating...</span>
                        </div>
                      ) : (
                        <span>Generation {job.status}</span>
                      )}
                    </div>
                  )}

                  {/* Selection Checkbox for Client Review */}
                  <div className="absolute top-3 left-3 z-10">
                    <button
                      type="button"
                      onClick={() => handleToggleSelectForReview(job.id)}
                      disabled={job.status !== 'SUCCEEDED'}
                      className={`w-6 h-6 rounded-md flex items-center justify-center border transition-all ${
                        isSelected
                          ? 'bg-bronze-700 border-bronze-700 text-white'
                          : 'bg-white/90 backdrop-blur-sm border-sand-300 text-transparent hover:border-bronze-700'
                      }`}
                      title="Select concept for client review presentation"
                    >
                      ✓
                    </button>
                  </div>

                  {/* Top Right Badges */}
                  <div className="absolute top-3 right-3 flex items-center gap-1.5 z-10">
                    {/* Shortlist Star */}
                    <button
                      type="button"
                      onClick={() => handleToggleShortlist(job.id)}
                      className={`p-1.5 rounded-full backdrop-blur-sm transition-all ${
                        job.isShortlisted
                          ? 'bg-amber-500 text-white shadow'
                          : 'bg-white/80 text-charcoal-400 hover:text-amber-600 hover:bg-white'
                      }`}
                      title={job.isShortlisted ? 'Remove from shortlist' : 'Add to shortlist'}
                    >
                      <Star className={`w-3.5 h-3.5 ${job.isShortlisted ? 'fill-white' : ''}`} />
                    </button>

                    {/* Studio Pick Checkmark */}
                    <button
                      type="button"
                      onClick={() => handleToggleStudioSelected(job.id)}
                      className={`p-1.5 rounded-full backdrop-blur-sm transition-all ${
                        job.isStudioSelected
                          ? 'bg-emerald-600 text-white shadow'
                          : 'bg-white/80 text-charcoal-400 hover:text-emerald-600 hover:bg-white'
                      }`}
                      title={job.isStudioSelected ? 'Studio Primary Selection' : 'Mark as Studio Selection'}
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Lineage & Mode Badges */}
                  <div className="absolute bottom-2 left-2 right-2 flex items-center justify-between text-[11px] pointer-events-none">
                    <span className="px-2 py-0.5 rounded-md bg-charcoal-900/80 backdrop-blur-sm text-white font-medium flex items-center gap-1">
                      {hasParent ? <GitBranch className="w-3 h-3 text-bronze-300" /> : <Sparkles className="w-3 h-3 text-amber-300" />}
                      <span>{job.conceptLabel || (hasParent ? 'Variation' : 'Base Concept')}</span>
                    </span>

                    {isPrecision && (
                      <span className="px-2 py-0.5 rounded-md bg-amber-950/80 backdrop-blur-sm text-amber-200 font-medium">
                        Precision Edit
                      </span>
                    )}
                  </div>
                </div>

                {/* Card Body */}
                <div className="p-4 space-y-3 flex-1 flex flex-col justify-between">
                  <div>
                    <p className="text-xs text-charcoal-700 line-clamp-2 leading-relaxed font-sans" title={job.prompt}>
                      {job.prompt}
                    </p>
                    <div className="flex items-center gap-2 mt-2 text-[10px] text-charcoal-400">
                      <Clock className="w-3 h-3" />
                      <span>{new Date(job.createdAt).toLocaleDateString()}</span>
                      <span>•</span>
                      <span className="uppercase">{job.status}</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="pt-3 border-t border-sand-100 flex items-center justify-between gap-2">
                    <button
                      type="button"
                      onClick={() => openVariationModal(job)}
                      disabled={job.status !== 'SUCCEEDED'}
                      className="px-3 py-1.5 rounded-lg border border-sand-300 text-xs font-semibold text-charcoal-700 hover:bg-sand-50 hover:border-bronze-700 flex items-center gap-1.5 transition-colors disabled:opacity-50"
                    >
                      <Wand2 className="w-3.5 h-3.5 text-bronze-700" />
                      <span>Create Variation</span>
                    </button>

                    {onSelectJobForCompare && job.status === 'SUCCEEDED' && (
                      <button
                        type="button"
                        onClick={() => onSelectJobForCompare(job)}
                        className="px-2.5 py-1.5 text-xs text-bronze-700 hover:text-bronze-900 font-medium underline"
                      >
                        Inspect
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 rounded-lg border border-sand-300 text-xs font-medium text-charcoal-700 disabled:opacity-40 hover:bg-sand-50"
          >
            Previous
          </button>
          <span className="text-xs text-charcoal-600 px-2">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 rounded-lg border border-sand-300 text-xs font-medium text-charcoal-700 disabled:opacity-40 hover:bg-sand-50"
          >
            Next
          </button>
        </div>
      )}

      {/* Variation Modal */}
      {variationModalJob && (
        <div className="fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-5 shadow-xl border border-sand-200">
            <div className="flex items-center justify-between border-b border-sand-100 pb-3">
              <h3 className="font-serif text-lg text-charcoal-900 font-medium flex items-center gap-2">
                <Wand2 className="w-4 h-4 text-bronze-700" />
                <span>Create Concept Variation</span>
              </h3>
              <button
                type="button"
                onClick={() => setVariationModalJob(null)}
                className="text-charcoal-400 hover:text-charcoal-700 text-sm"
              >
                ✕
              </button>
            </div>

            {variationError && (
              <div className="p-3 rounded-xl bg-red-50 text-red-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{variationError}</span>
              </div>
            )}

            {/* Parent Concept Preview */}
            <div className="flex items-center gap-3 bg-[#FAF8F5] p-3 rounded-xl border border-sand-200">
              {variationModalJob.outputPreviewUrl && (
                <img
                  src={variationModalJob.outputPreviewUrl}
                  alt="Parent"
                  className="w-16 h-12 rounded object-cover border border-sand-300"
                />
              )}
              <div className="text-xs space-y-0.5">
                <span className="font-semibold text-charcoal-800 block">
                  Parent: {variationModalJob.conceptLabel || 'Concept'}
                </span>
                <span className="text-charcoal-500 line-clamp-1">{variationModalJob.prompt}</span>
              </div>
            </div>

            {/* Strategy Selection */}
            <div className="space-y-2">
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                Variation Strategy
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setVariationStrategy('REFINE_ORIGINAL')}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    variationStrategy === 'REFINE_ORIGINAL'
                      ? 'border-bronze-700 bg-sand-50/80 ring-1 ring-bronze-700'
                      : 'border-sand-200 hover:border-sand-300'
                  }`}
                >
                  <span className="text-xs font-semibold text-charcoal-900 block mb-1">
                    Refine Original Room
                  </span>
                  <span className="text-[11px] text-charcoal-500 leading-relaxed block">
                    Uses the original unfinished photo with tweaked prompt and references.
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setVariationStrategy('EVOLVE_CONCEPT')}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    variationStrategy === 'EVOLVE_CONCEPT'
                      ? 'border-bronze-700 bg-sand-50/80 ring-1 ring-bronze-700'
                      : 'border-sand-200 hover:border-sand-300'
                  }`}
                >
                  <span className="text-xs font-semibold text-charcoal-900 block mb-1">
                    Evolve this AI Concept
                  </span>
                  <span className="text-[11px] text-charcoal-500 leading-relaxed block">
                    Feeds this concept image back in as the new structural base for layered changes.
                  </span>
                </button>
              </div>
            </div>

            {/* Prompt */}
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                Adjusted Prompt Instructions
              </label>
              <textarea
                rows={3}
                value={variationPrompt}
                onChange={(e) => setVariationPrompt(e.target.value)}
                placeholder="Describe modifications (e.g., warmer wood tones, darker hardware, linen curtains)..."
                className="w-full rounded-xl border border-sand-300 text-xs p-3 focus:border-bronze-700 focus:ring-bronze-700"
              />
            </div>

            {/* Precision Mask Reuse */}
            {variationModalJob.editingMode === 'PRECISION_MASK' && (
              <div className="flex items-center justify-between p-3 rounded-xl bg-sand-50 border border-sand-200">
                <div className="text-xs">
                  <span className="font-semibold text-charcoal-800 block">Reuse Precision Mask</span>
                  <span className="text-charcoal-500 text-[11px]">
                    Preserves the exact selected region from the parent generation.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={reuseMask}
                  onChange={(e) => setReuseMask(e.target.checked)}
                  className="w-4 h-4 text-bronze-700 rounded border-sand-300 focus:ring-bronze-700"
                />
              </div>
            )}

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setVariationModalJob(null)}
                className="px-4 py-2 text-xs font-medium text-charcoal-600 hover:text-charcoal-900"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleCreateVariation}
                disabled={submittingVariation}
                className="px-5 py-2.5 bg-bronze-700 hover:bg-bronze-800 text-white rounded-xl text-xs font-semibold shadow-sm flex items-center gap-2 disabled:opacity-60"
              >
                {submittingVariation && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Generate Variation</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Client Review Modal */}
      {reviewModalOpen && (
        <div className="fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-5 shadow-xl border border-sand-200">
            <div className="flex items-center justify-between border-b border-sand-100 pb-3">
              <h3 className="font-serif text-lg text-charcoal-900 font-medium flex items-center gap-2">
                <Share2 className="w-4 h-4 text-bronze-700" />
                <span>Create Client Review Link</span>
              </h3>
              <button
                type="button"
                onClick={() => setReviewModalOpen(false)}
                className="text-charcoal-400 hover:text-charcoal-700 text-sm"
              >
                ✕
              </button>
            </div>

            {reviewResult ? (
              <div className="space-y-4 py-2">
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs space-y-1">
                  <div className="font-semibold text-sm flex items-center gap-1.5">
                    <CheckCircle2 className="w-4 h-4 text-emerald-700" />
                    <span>Client Review Link Generated!</span>
                  </div>
                  <p className="text-emerald-800">
                    A secure, cryptographic review link has been created with {reviewResult.items.length} concept options.
                  </p>
                </div>

                <div className="space-y-2">
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                    Client Share URL
                  </label>
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      readOnly
                      value={typeof window !== 'undefined' ? `${window.location.origin}${reviewResult.reviewUrl}` : reviewResult.reviewUrl}
                      className="w-full bg-[#FAF8F5] border border-sand-300 rounded-xl px-3 py-2 text-xs font-mono text-charcoal-800"
                    />
                    <button
                      type="button"
                      onClick={() => copyReviewLinkToClipboard(reviewResult.reviewUrl)}
                      className="px-4 py-2 bg-bronze-700 hover:bg-bronze-800 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 flex-shrink-0"
                    >
                      <Copy className="w-3.5 h-3.5" />
                      <span>{copiedLink ? 'Copied!' : 'Copy'}</span>
                    </button>
                  </div>
                  <p className="text-[11px] text-charcoal-500">
                    When opened by the client, the link exchanges into an HttpOnly session without exposing private master files or references.
                  </p>
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    type="button"
                    onClick={() => setReviewModalOpen(false)}
                    className="px-5 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-semibold"
                  >
                    Done
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {reviewError && (
                  <div className="p-3 rounded-xl bg-red-50 text-red-800 text-xs flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 flex-shrink-0" />
                    <span>{reviewError}</span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                    Presentation Title *
                  </label>
                  <input
                    type="text"
                    value={reviewTitle}
                    onChange={(e) => setReviewTitle(e.target.value)}
                    placeholder="e.g., Master Bedroom Concept Options"
                    required
                    className="w-full rounded-xl border border-sand-300 text-xs p-2.5 focus:border-bronze-700 focus:ring-bronze-700"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                    Client Welcome Message (Optional)
                  </label>
                  <textarea
                    rows={2}
                    value={reviewMessage}
                    onChange={(e) => setReviewMessage(e.target.value)}
                    placeholder="Hi Sarah, please review these 3 finish options for your master bedroom wardrobe and feature wall..."
                    className="w-full rounded-xl border border-sand-300 text-xs p-2.5 focus:border-bronze-700 focus:ring-bronze-700"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Link Expiry
                    </label>
                    <select
                      value={reviewExpiryDays}
                      onChange={(e) => setReviewExpiryDays(Number(e.target.value))}
                      className="w-full rounded-xl border border-sand-300 text-xs py-2 px-3 bg-[#FAF8F5]"
                    >
                      <option value={7}>7 Days</option>
                      <option value={14}>14 Days (Standard)</option>
                      <option value={30}>30 Days</option>
                      <option value={60}>60 Days</option>
                    </select>
                  </div>

                  <div className="flex flex-col justify-end">
                    <label className="flex items-center gap-2 text-xs text-charcoal-700 p-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={includeOriginal}
                        onChange={(e) => setIncludeOriginal(e.target.checked)}
                        className="w-4 h-4 text-bronze-700 rounded border-sand-300"
                      />
                      <span>Include Original Photo for Comparison</span>
                    </label>
                  </div>
                </div>

                <div className="p-3 bg-sand-50 rounded-xl border border-sand-200 text-xs space-y-1">
                  <span className="font-semibold text-charcoal-800">
                    Selected Concepts: {selectedJobIds.size} option{selectedJobIds.size === 1 ? '' : 's'}
                  </span>
                  <p className="text-[11px] text-charcoal-500">
                    Client decisions will be securely recorded and will lock the concept package against unauthorized post-interaction modifications.
                  </p>
                </div>

                <div className="flex items-center justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setReviewModalOpen(false)}
                    className="px-4 py-2 text-xs font-medium text-charcoal-600 hover:text-charcoal-900"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleCreateClientReview}
                    disabled={submittingReview || selectedJobIds.size === 0}
                    className="px-5 py-2.5 bg-bronze-700 hover:bg-bronze-800 text-white rounded-xl text-xs font-semibold shadow-sm flex items-center gap-2 disabled:opacity-60"
                  >
                    {submittingReview && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Generate Review Link</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
