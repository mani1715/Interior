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
  Palette,
  Sliders,
  Crosshair,
  History,
  Share2,
  UploadCloud,
  ImagePlus,
} from 'lucide-react';
import {
  fetchAiStudioStatus,
  createAiJob,
  fetchAiJobDetail,
  cancelAiJob,
  listAiJobs,
  fetchReferences,
  uploadPrecisionMask,
} from '@/lib/ai/api';
import { AiJobDetail, AiStudioStatus, ReferenceDetail, EditingMode } from '@/lib/ai/types';
import { fetchProjects } from '@/lib/projects/api';
import { ProjectSummaryDto } from '@/lib/projects/types';
import { fetchProjectMedia, fetchMediaDetail, uploadMediaFile } from '@/lib/media/api';
import { regionalColours } from '@/lib/ai/regional-colours';
import { MediaDetailResponse } from '@/lib/media/types';
import { ReferenceManager, SelectedReferenceItem } from './ReferenceManager';
import { PrecisionMaskEditor } from './PrecisionMaskEditor';
import { AiHistoryView } from './AiHistoryView';
import { AiReviewsView } from './AiReviewsView';
import { AiPromptComposer } from './AiPromptComposer';
import styles from './AiStudio.module.css';
import { useRealtimeSubscription } from '@/lib/realtime/RealtimeProvider';

const PROMPT_PRESETS = [
  'Modern Minimalist with warm oak flooring, flush baseboards, and indirect recessed ceiling cove lighting',
  'Warm Contemporary with fluted travertine feature wall, boucle seating, and brass architectural accents',
  'Scandinavian Japandi with pale timber cabinetry, wabi-sabi ceramic decor, and sheer linen drapery',
  'Industrial Loft with exposed concrete ceiling, raw brick accents, and matte black steel-framed glazing',
  'Neo-Classical Luxury with subtle wall mouldings, herringbone timber floor, and warm alabaster chandeliers',
  'Change Colours: Warm alabaster walls, muted sage accents, and warm oak flooring',
  'Change Material: Fluted Italian Calacatta marble surfaces with natural walnut wood veneer',
  'Change Wardrobe Finish: Fluted oak shutters with vertical grain and sleek recessed J-pull profiles',
  'Change Kitchen Cabinets: Soft-touch ultra-matte cashmere cabinetry with brushed champagne gold hardware',
  'Change Handles / Hardware: Minimalist knurled brushed brass pull handles and concealed hinges',
  'Change Wall Finish: Lime-washed warm terracotta textured plaster feature wall',
  'Change TV Unit Design: Backlit charcoal slatted acoustic timber panels with floating quartz shelf',
  'Change Pooja Unit Finish: Rich teakwood carved jaali pattern with warm ambient cove illumination',
  'Try Another Style: Scandinavian Japandi with wabi-sabi ceramic decor and raw linen drapery',
  'Preserve Layout: Maintain structural boundaries and window positions with refined contemporary joinery',
];

const PRECISION_PRESETS = [
  { label: 'Wardrobe Shutters', prompt: 'Change only these wardrobe shutters to walnut wood veneer with vertical grain and integrated black J-pulls' },
  { label: 'Kitchen Cabinets', prompt: 'Update kitchen cabinet frontages to ultra-matte sage finish with brushed champagne brass handles' },
  { label: 'Countertop & Backsplash', prompt: 'Apply honed Italian Calacatta marble slab to this countertop and seamless backsplash' },
  { label: 'TV Unit Design', prompt: 'Transform this TV wall unit with fluted oak panelling and floating low-height console' },
  { label: 'Pooja Unit Finish', prompt: 'Finish this pooja niche in warm teakwood with CNC-cut brass jaali screen accents' },
  { label: 'Wall Paint / Plaster', prompt: 'Repaint this wall in warm earthy terracotta matte lime-wash texture' },
  { label: 'Handles & Hardware', prompt: 'Replace all handles in this section with minimal knurled cylinder pulls in satin brass' },
];

export function AiVisualizerClient() {
  const searchParams = useSearchParams();
  const initialProjectId = searchParams.get('projectId');
  const initialMediaId = searchParams.get('mediaId');

  // Active Tab
  const [activeTab, setActiveTab] = useState<'create' | 'history' | 'reviews'>('create');

  // Status & Quota
  const [studioStatus, setStudioStatus] = useState<AiStudioStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(true);

  // Projects & Media
  const [projects, setProjects] = useState<ProjectSummaryDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>(initialProjectId || '');
  const [projectMedia, setProjectMedia] = useState<MediaDetailResponse[]>([]);
  const [conceptSource, setConceptSource] = useState<MediaDetailResponse | null>(null);
  const [loadingConceptSource, setLoadingConceptSource] = useState(false);
  const [loadingMedia, setLoadingMedia] = useState(false);
  const [selectedMediaId, setSelectedMediaId] = useState<string>(initialMediaId || '');

  // Direct Photo Upload State
  const [photoSourceMode, setPhotoSourceMode] = useState<'upload' | 'gallery'>('upload');
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handlePhotoUpload = async (file: File) => {
    if (!selectedProjectId) {
      setFormError('Please select a project first before uploading a room photo.');
      return;
    }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setUploadError('Only JPEG, PNG, or WebP images are supported.');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      setUploadError('File size exceeds 20MB limit.');
      return;
    }

    try {
      setUploadingPhoto(true);
      setUploadProgress(0);
      setUploadError(null);

      const media = await uploadMediaFile(file, selectedProjectId, {
        mediaType: 'BEFORE',
        altText: file.name.replace(/\.[^/.]+$/, ''),
        onProgress: (p) => setUploadProgress(p),
      });

      setProjectMedia((prev) => [media, ...prev]);
      setSelectedMediaId(media.id);
      setUploadProgress(100);
    } catch (err: any) {
      setUploadError(err?.message || 'Failed to upload room photo');
    } finally {
      setUploadingPhoto(false);
    }
  };

  // References & Structure Preservation
  const [preserveStructure, setPreserveStructure] = useState<boolean>(true);
  const [selectedReferences, setSelectedReferences] = useState<SelectedReferenceItem[]>([]);
  const [libraryReferences, setLibraryReferences] = useState<ReferenceDetail[]>([]);

  // Generation Form
  const [prompt, setPrompt] = useState('');
  const regionPlan = studioStatus?.providerKey === 'stability' ? regionalColours(prompt) : [];
  const [submitting, setSubmitting] = useState(false);
  const [activeJob, setActiveJob] = useState<AiJobDetail | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // History & Comparison
  const [recentJobs, setRecentJobs] = useState<AiJobDetail[]>([]);
  const [selectedComparisonJob, setSelectedComparisonJob] = useState<AiJobDetail | null>(null);
  const [viewMode, setViewMode] = useState<'split' | 'original' | 'concept'>('split');

  // Editing Mode & Precision Mask State
  const [editingMode, setEditingMode] = useState<EditingMode>('FULL_IMAGE');
  const [hasMask, setHasMask] = useState(false);
  const [maskSession, setMaskSession] = useState(0);
  const [maskCoverageRatio, setMaskCoverageRatio] = useState(0);
  const [showMaskOverlay, setShowMaskOverlay] = useState(true);
  const exportMaskRef = useRef<(() => Promise<Blob | null>) | null>(null);

  const pollingRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    setHasMask(false);
    setMaskCoverageRatio(0);
    setSelectedComparisonJob(null);
  }, [selectedProjectId, selectedMediaId]);

  // 1. Initial Load: Status, Projects, Recent Jobs, References Library
  useEffect(() => {
    async function init() {
      try {
        const [status, projList, jobsList, refList] = await Promise.all([
          fetchAiStudioStatus(),
          fetchProjects(),
          listAiJobs({ limit: 10 }).catch(() => ({ items: [], total: 0, limit: 10, offset: 0 })),
          (fetchReferences ? fetchReferences() : Promise.resolve([])).catch(() => []),
        ]);
        setStudioStatus(status);
        setProjects(projList);
        setRecentJobs(jobsList.items);
        setLibraryReferences(refList);

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

  // Real-time AI Job Progression via SSE (Instant transition without waiting for polling tick)
  useRealtimeSubscription(
    ['AI_JOB_PROCESSING', 'AI_JOB_COMPLETED', 'AI_JOB_FAILED', 'CLIENT_CONCEPT_APPROVED', 'CLIENT_CONCEPT_CHANGES_REQUESTED', 'RESYNC'],
    async (event) => {
      if (!activeJob) return;

      const isCurrentJob = event.resourceId === activeJob.id || (event.metadata && event.metadata.jobId === activeJob.id);
      if (isCurrentJob || event.type === 'RESYNC') {
        try {
          const detail = await fetchAiJobDetail(activeJob.id);
          setActiveJob(detail);

          if (detail.status === 'SUCCEEDED') {
            setSelectedComparisonJob(detail);
            setSubmitting(false);
            listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
            fetchAiStudioStatus().then(setStudioStatus).catch(() => {});
          } else if (detail.status === 'FAILED' || detail.status === 'CANCELLED' || detail.status === 'TIMED_OUT') {
            setSubmitting(false);
            listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
          }
        } catch (err) {
          console.error('Error fetching AI job on realtime event:', err);
        }
      }
    }
  );

  // 3. Low-frequency safety poll (10s fallback) in case SSE connection drops
  useEffect(() => {
    if (!activeJob || activeJob.status === 'SUCCEEDED' || activeJob.status === 'FAILED' || activeJob.status === 'CANCELLED' || activeJob.status === 'TIMED_OUT') {
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
          listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
          fetchAiStudioStatus().then(setStudioStatus).catch(() => {});
        } else if (detail.status === 'FAILED' || detail.status === 'CANCELLED' || detail.status === 'TIMED_OUT') {
          setSubmitting(false);
          listAiJobs({ limit: 10 }).then((res) => setRecentJobs(res.items)).catch(() => {});
        }
      } catch (err) {
        console.error('Error polling AI job:', err);
      }
    }, 10000);

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
    if (regionPlan.length) {
      setFormError('Select one colour region below and paint its doors on the photo before generating.');
      return;
    }

    if (!selectedProjectId) {
      setFormError('Please select a project');
      return;
    }
    if (!selectedMediaId) {
      setFormError('Please select a source room image to visualize');
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

    if (editingMode === 'PRECISION_MASK') {
      if (!hasMask || maskCoverageRatio <= 0) {
        setFormError('Please paint a target area on the room photo before submitting a precision edit.');
        return;
      }
      if (maskCoverageRatio > 0.98) {
        setFormError('The selected area covers almost the entire image (>98%). Please switch to Full Concept mode.');
        return;
      }
    }

    setSubmitting(true);
    try {
      const idempotencyKey = `ai-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const formattedReferences = selectedReferences.map((r, idx) => ({
        mediaId: r.mediaId,
        purpose: r.purpose,
        label: r.label,
        instruction: r.instruction,
        displayOrder: idx,
      }));

      let maskStorageKey: string | undefined = undefined;
      if (editingMode === 'PRECISION_MASK') {
        const maskBlob = await exportMaskRef.current?.();
        if (!maskBlob) {
          throw new Error('Failed to export mask drawing. Please try painting the selection again.');
        }
        const uploadRes = await uploadPrecisionMask(selectedMediaId, maskBlob, selectedMediaAsset?.studioId);
        maskStorageKey = uploadRes.maskStorageKey;
      }

      const job = await createAiJob({
        projectId: selectedProjectId,
        inputMediaId: selectedMediaId,
        prompt: prompt.trim(),
        idempotencyKey,
        preserveStructure,
        references: formattedReferences,
        editingMode,
        maskStorageKey,
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

  const selectedMediaAsset = conceptSource?.id === selectedMediaId && conceptSource.projectId === selectedProjectId
    ? conceptSource : projectMedia.find((m) => m.id === selectedMediaId);
  // Private projects intentionally cannot use public derivative URLs.
  const previewUrl = (media: MediaDetailResponse) =>
    `/api/v1/media/${encodeURIComponent(media.id)}/preview?studioId=${encodeURIComponent(media.studioId)}`;
  const selectedMediaThumbnail = selectedMediaAsset ? previewUrl(selectedMediaAsset) : undefined;
  const selectedMediaFullUrl = selectedMediaThumbnail;

  const isConfigured = studioStatus?.isConfigured ?? false;
  const generationBlocker = submitting ? 'Your current image is still processing.'
    : loadingConceptSource ? 'Loading the generated image for your next edit.'
    : uploadingPhoto ? 'Uploading your photo. Please wait.'
    : !isConfigured ? 'The AI service is unavailable. Refresh the page to reconnect.'
    : !selectedProjectId ? 'Choose a project in step 1.'
    : !selectedMediaId ? 'Upload or select a source photo in step 2.'
    : prompt.trim().length < 5 ? 'Describe the change you want (at least 5 characters).'
    : prompt.trim().length > 500 ? `Shorten your instruction by ${prompt.trim().length - 500} characters.`
    : regionPlan.length ? 'Choose one colour region below, then paint its doors on the photo.'
    : editingMode === 'PRECISION_MASK' && !hasMask ? 'Paint the area to edit on your photo.'
    : null;

  return (
    <div className={`${styles.studio} p-4 sm:p-6 lg:p-8 max-w-[1600px] mx-auto space-y-6 text-[#1F1F1F] min-h-screen`}>
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
                ELÉGANCE / DESIGN LAB
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                AI design studio
              </h1>
              <p className={styles.subtitle}>Your space. A new finish. Explore it before you build.</p>
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

      {/* Provider Unconfigured Banner */}
      {!statusLoading && studioStatus && !isConfigured && (
        <div className="p-4 sm:p-5 rounded-2xl bg-amber-50/80 border border-amber-200 text-amber-900 space-y-2">
          <div className="flex items-center gap-2 font-semibold text-sm">
            <AlertCircle className="w-4 h-4 text-amber-700 flex-shrink-0" />
            <span>AI Generation Provider Not Configured</span>
          </div>
          <p className="text-xs sm:text-sm text-amber-800 leading-relaxed max-w-3xl">
            Generative interior design features are currently unavailable on this platform instance.
            Style visualization and concept generations will activate once provider configuration is completed by the administrator.
          </p>
        </div>
      )}

      {!statusLoading && !studioStatus && (
        <div role="alert" className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900">
          <p className="font-semibold">Unable to load AI Studio</p>
          <p className="text-sm">Check your connection and sign-in session, then reload this page. Provider availability could not be verified.</p>
        </div>
      )}

      {/* Tab Navigation */}
      <div className={`${styles.tabs} flex items-center gap-2 border-b border-sand-200 pb-3`}>
        <button
          type="button"
          onClick={() => setActiveTab('create')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'create'
              ? 'bg-charcoal-900 text-white shadow-sm'
              : 'bg-white text-charcoal-600 hover:text-charcoal-900 border border-sand-200 hover:bg-sand-50'
          }`}
        >
          <Wand2 className="w-3.5 h-3.5" />
          <span>Studio & Generate</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('history')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'history'
              ? 'bg-charcoal-900 text-white shadow-sm'
              : 'bg-white text-charcoal-600 hover:text-charcoal-900 border border-sand-200 hover:bg-sand-50'
          }`}
        >
          <History className="w-3.5 h-3.5" />
          <span>History & Lineage</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('reviews')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'reviews'
              ? 'bg-charcoal-900 text-white shadow-sm'
              : 'bg-white text-charcoal-600 hover:text-charcoal-900 border border-sand-200 hover:bg-sand-50'
          }`}
        >
          <Share2 className="w-3.5 h-3.5" />
          <span>Client Reviews</span>
        </button>
      </div>

      {activeTab === 'history' && (
        <AiHistoryView
          projectId={selectedProjectId || undefined}
          studioId={undefined}
          onSelectJobForCompare={(job) => {
            setSelectedComparisonJob(job);
            setActiveTab('create');
          }}
          onVariationCreated={(job) => {
            setActiveJob(job);
            setSelectedComparisonJob(job);
            setActiveTab('create');
          }}
        />
      )}

      {activeTab === 'reviews' && (
        <AiReviewsView
          projectId={selectedProjectId || undefined}
          studioId={undefined}
          onUseInNextRevision={(annotationText, jobId) => {
            setPrompt((prev) => (prev ? `${prev}. Client Revision: ${annotationText}` : `Client Revision: ${annotationText}`));
            setActiveTab('create');
          }}
        />
      )}

      {activeTab === 'create' && (
        <div className={styles.workspace}>
        {/* Left Column: Visualizer Controls & Inputs */}
        <div className={styles.controls}>
          <div className="bg-white rounded-2xl border border-sand-200 p-5 sm:p-6 shadow-sm space-y-5">
            <h2 className="font-serif text-lg text-charcoal-900 font-medium flex items-center gap-2">
              <Wand2 className="w-4 h-4 text-bronze-700" />
              <span>Design direction</span>
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
                  01 / Project
                </label>
                <select
                  aria-label="Project"
                  value={selectedProjectId}
                  onChange={(e) => {
                    setSelectedProjectId(e.target.value);
                    setSelectedMediaId('');
                  }}
                  disabled={submitting}
                  className="w-full rounded-xl border border-sand-300 text-xs focus:border-bronze-700 focus:ring-bronze-700 bg-[#FAF8F5] py-2 px-3 disabled:opacity-60"
                >
                  <option value="">-- Choose project --</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.categoryDisplayName || p.categoryCode})
                    </option>
                  ))}
                </select>
              </div>

              {/* 2. Source Image Selection (Authoritative Spatial Geometry) */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                    02 / Your photo
                  </label>
                  {selectedProjectId && (
                    <div className="flex items-center rounded-lg border border-sand-200 bg-[#FAF8F5] p-0.5 text-[11px]">
                      <button
                        type="button"
                        onClick={() => setPhotoSourceMode('upload')}
                        className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                          photoSourceMode === 'upload'
                            ? 'bg-white text-charcoal-900 shadow-2xs font-semibold'
                            : 'text-charcoal-500 hover:text-charcoal-800'
                        }`}
                      >
                        Upload Photo
                      </button>
                      <button
                        type="button"
                        onClick={() => setPhotoSourceMode('gallery')}
                        className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                          photoSourceMode === 'gallery'
                            ? 'bg-white text-charcoal-900 shadow-2xs font-semibold'
                            : 'text-charcoal-500 hover:text-charcoal-800'
                        }`}
                      >
                        Project Photos ({projectMedia.length})
                      </button>
                    </div>
                  )}
                </div>

                {!selectedProjectId ? (
                  <div className="p-4 rounded-xl border border-dashed border-sand-300 bg-sand-50/50 text-center text-xs text-charcoal-500">
                    <ImageIcon className="w-5 h-5 mx-auto mb-1 text-sand-400" />
                    <span>Select a project in Step 1 above to upload or choose a room photo.</span>
                  </div>
                ) : photoSourceMode === 'upload' ? (
                  <div className="space-y-2">
                    {/* Drag and drop upload zone */}
                    <div
                      onDragOver={(e) => {
                        e.preventDefault();
                        setIsDragging(true);
                      }}
                      onDragLeave={() => setIsDragging(false)}
                      onDrop={(e) => {
                        e.preventDefault();
                        setIsDragging(false);
                        const file = e.dataTransfer.files?.[0];
                        if (file) handlePhotoUpload(file);
                      }}
                      onClick={() => fileInputRef.current?.click()}
                      role="button"
                      tabIndex={0}
                      aria-label="Upload source photo"
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInputRef.current?.click(); } }}
                      className={`p-5 rounded-xl border-2 border-dashed cursor-pointer text-center transition-all ${
                        isDragging
                          ? 'border-bronze-700 bg-bronze-50/40 ring-2 ring-bronze-700/20'
                          : 'border-sand-300 hover:border-bronze-600 bg-sand-50/40 hover:bg-sand-50'
                      }`}
                    >
                      <input
                        type="file"
                        ref={fileInputRef}
                        accept="image/jpeg,image/png,image/webp"
                        className="hidden"
                        onChange={(e) => {
                          const f = e.target.files?.[0];
                          if (f) handlePhotoUpload(f);
                        }}
                      />
                      {uploadingPhoto ? (
                        <div className="space-y-2 py-2">
                          <Loader2 className="w-6 h-6 animate-spin text-bronze-700 mx-auto" />
                          <span className="text-xs font-medium text-charcoal-800 block">
                            Uploading photo... {uploadProgress > 0 ? `${uploadProgress}%` : ''}
                          </span>
                        </div>
                      ) : (
                        <div className="space-y-1.5">
                          <UploadCloud className="w-7 h-7 text-bronze-700 mx-auto" />
                          <div>
                            <span className="text-xs font-semibold text-charcoal-800 block">
                              Click or drop room photo here
                            </span>
                            <span className="text-[11px] text-charcoal-400 block">
                              Supports high-resolution JPEG, PNG, or WebP (max 20MB)
                            </span>
                          </div>
                        </div>
                      )}
                    </div>

                    {uploadError && (
                      <p className="text-xs text-red-600 bg-red-50 p-2 rounded-lg border border-red-200">
                        {uploadError}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="space-y-2">
                    {loadingMedia ? (
                      <div className="py-6 text-center text-xs text-charcoal-400 flex items-center justify-center gap-2">
                        <Loader2 className="w-4 h-4 animate-spin text-bronze-700" />
                        <span>Loading project photos...</span>
                      </div>
                    ) : projectMedia.length === 0 ? (
                      <div className="p-3 rounded-xl bg-sand-50 border border-sand-200 text-[11px] text-charcoal-600 text-center">
                        No photos in this project gallery yet.{' '}
                        <button
                          type="button"
                          onClick={() => setPhotoSourceMode('upload')}
                          className="text-bronze-700 font-semibold underline hover:text-bronze-800"
                        >
                          Upload one now
                        </button>
                      </div>
                    ) : (
                      <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 max-h-48 overflow-y-auto p-1 bg-[#FAF8F5] rounded-xl border border-sand-200">
                        {projectMedia.map((m) => {
                          const thumb = previewUrl(m);
                          const isSelected = selectedMediaId === m.id;

                          return (
                            <button
                              key={m.id}
                              type="button"
                              onClick={() => setSelectedMediaId(m.id)}
                              disabled={submitting}
                              className={`relative aspect-square rounded-lg overflow-hidden border-2 transition-all ${
                                isSelected
                                  ? 'border-bronze-700 ring-2 ring-bronze-700/30'
                                  : 'border-transparent hover:border-sand-300'
                              }`}
                            >
                              {thumb ? (
                                /* eslint-disable-next-line @next/next/no-img-element */
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
                  </div>
                )}

                {/* Selected Photo Indicator */}
                {selectedMediaThumbnail && (
                  <div className="flex items-center gap-2.5 p-2 rounded-xl bg-sand-50 border border-sand-200">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={selectedMediaThumbnail} alt="Selected" className="w-12 h-12 rounded-lg object-cover border border-sand-300" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-[11px] font-semibold text-charcoal-800 truncate">
                          {selectedMediaAsset?.altText || 'Selected room photo'}
                        </span>
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 font-medium">
                          Source photo
                        </span>
                      </div>
                      <span className="text-[10px] text-charcoal-400 block">
                        {selectedMediaAsset?.width} × {selectedMediaAsset?.height} px · Original resolution
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* 3. Transformation Mode Selector */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider">
                    03 / Edit approach
                  </label>
                  <span className="text-[10px] text-charcoal-500">
                    {editingMode === 'PRECISION_MASK' ? 'Precise Edit (Manual Mask)' : 'Quick Recolor (Auto-Detect)'}
                  </span>
                </div>

                {!selectedMediaId ? (
                  <div className="p-3 rounded-xl border border-sand-200 bg-sand-50/40 text-[11px] text-charcoal-500">
                    Select or upload a source room photo in Step 2 to enable Quick Recolor or Precise Mask brush.
                  </div>
                ) : (
                  <>
                    <div className="grid grid-cols-2 gap-2 p-1 bg-[#FAF8F5] rounded-xl border border-sand-200">
                      <button
                        type="button"
                        onClick={() => setEditingMode('FULL_IMAGE')}
                        disabled={submitting}
                        className={`min-h-[44px] px-3 py-2 rounded-lg text-xs font-medium flex items-center justify-center gap-2 transition-all ${
                          editingMode === 'FULL_IMAGE'
                            ? 'bg-white text-charcoal-900 shadow-sm border border-sand-200'
                            : 'text-charcoal-500 hover:text-charcoal-900'
                        }`}
                      >
                        <Sparkles className="w-3.5 h-3.5 text-bronze-700" />
                        <span>Quick Recolor</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => setEditingMode('PRECISION_MASK')}
                        disabled={submitting || studioStatus?.supportsMaskEditing === false}
                        className={`min-h-[44px] px-3 py-2 rounded-lg text-xs font-medium flex items-center justify-center gap-2 transition-all ${
                          editingMode === 'PRECISION_MASK'
                            ? 'bg-white text-charcoal-900 shadow-sm border border-sand-200'
                            : 'text-charcoal-500 hover:text-charcoal-900 disabled:opacity-40 disabled:cursor-not-allowed'
                        }`}
                      >
                        <Crosshair className="w-3.5 h-3.5 text-bronze-700" />
                        <span>Precise Edit</span>
                      </button>
                    </div>

                    <p className="text-[11px] text-charcoal-500 mt-1 leading-relaxed">
                      {editingMode === 'PRECISION_MASK'
                        ? 'Paint one area on the canvas, then describe only its new finish. Use a separate edit for each colour.'
                        : 'Best for one object and one finish. For different upper and lower colours, use Precise Edit with separate selections.'}
                    </p>
                  </>
                )}
              </div>

              {/* Reference Images & Structure Preservation Controls */}
              {selectedProjectId && studioStatus?.supportsReferenceImages && (
                <ReferenceManager
                  selectedReferences={selectedReferences}
                  onChangeReferences={setSelectedReferences}
                  preserveStructure={preserveStructure}
                  onTogglePreserveStructure={setPreserveStructure}
                  availableMedia={projectMedia}
                  libraryReferences={libraryReferences}
                  disabled={submitting}
                />
              )}

              {/* 4. Prompt Composer with Voice Dictation & Architectural Prompt Enhancement */}
              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-2">
                  {editingMode === 'PRECISION_MASK' ? '4. Target Edit Instruction' : '4. Desired Transformation'}
                </label>
                <AiPromptComposer
                  prompt={prompt}
                  onPromptChange={setPrompt}
                  editingMode={editingMode}
                  preserveStructure={preserveStructure}
                  roomType={projects.find((p) => p.id === selectedProjectId)?.categoryCode?.replace(/_/g, ' ')}
                  referenceCount={selectedReferences.length}
                  referencePurposes={selectedReferences.map((r: SelectedReferenceItem) => r.purpose)}
                  disabled={submitting}
                  maxLength={500}
                  presets={
                    studioStatus?.providerKey === 'stability'
                      ? [
                          { label: 'Matte black', prompt: editingMode === 'PRECISION_MASK' ? 'Matte black cupboard doors.' : 'Recolour the cupboard doors matte black.' },
                          { label: 'Sage kitchen cabinets', prompt: 'Recolor the kitchen cabinet fronts sage green. Preserve handles, geometry and the surrounding room.' },
                          { label: 'Warm white wall', prompt: 'Recolor the selected wall warm white. Preserve the cabinets, floor, ceiling and furnishings.' },
                        ]
                    : editingMode === 'PRECISION_MASK'
                      ? PRECISION_PRESETS
                      : PROMPT_PRESETS.map((p) => ({ label: p.split(' with ')[0], prompt: p }))
                  }
                />
                {regionPlan.length > 0 && (
                  <section aria-label="Separate colour regions" className="mt-3 rounded-lg border border-sand-300 bg-sand-50 p-3 space-y-3">
                    <h3 className="text-sm font-semibold">Two colours · two selected areas</h3>
                    <p className="text-sm">Automatic selection can colour the wrong doors. Choose the first area, paint only those doors on the photo, then generate. Use “Edit this result” to apply the second colour. Each edit uses a generation.</p>
                    <div className="flex flex-wrap gap-2">
                      {regionPlan.map(({ region, colour }) => (
                        <button key={region} type="button" disabled={submitting} className="rounded-lg border border-sand-300 bg-white px-3 py-2 text-sm"
                          onClick={() => {
                            setEditingMode('PRECISION_MASK');
                            setSelectedComparisonJob(null);
                            setHasMask(false);
                            setMaskSession((value) => value + 1);
                            setMaskCoverageRatio(0);
                            setPrompt(`Recolour only the selected ${region} cupboard doors ${colour}. Preserve the handles and panel seams.`);
                          }}>Select {region} doors · {colour}</button>
                      ))}
                    </div>
                  </section>
                )}
                {/\bblack\b/i.test(prompt) && /\bwhite\b/i.test(prompt) && /\b(combination|two[- ]tone)\b/i.test(prompt) && !/\b(upper|lower|top|bottom|left|right|alternating)\b/i.test(prompt) && (
                  <fieldset className="mt-3 rounded-lg border border-sand-200 p-3">
                    <legend className="px-1 text-sm font-medium">Choose your black &amp; white layout</legend>
                    <p className="text-xs text-charcoal-600 mb-3">Your prompt names two colours. Specify their placement, or leave it open for a design concept.</p>
                    <div className="flex flex-wrap gap-2">
                      {[
                        ['White upper / black lower', 'Upper cupboard doors white; lower cupboard doors black.'],
                        ['Black upper / white lower', 'Upper cupboard doors black; lower cupboard doors white.'],
                        ['Alternating doors', 'Alternate solid white and solid black cupboard doors.'],
                      ].map(([label, instruction]) => (
                        <button key={label} type="button" disabled={submitting || prompt.length + instruction.length + 2 > 500}
                          className="rounded-md border border-sand-300 px-3 py-2 text-xs text-charcoal-800 bg-white"
                          onClick={() => setPrompt(`${prompt.trim()}. ${instruction}`)}>{label}</button>
                      ))}
                    </div>
                  </fieldset>
                )}
                {studioStatus?.providerKey === 'stability' && (
                  <p className="mt-3 text-sm text-charcoal-600">
                    Concept preview, not a colour guarantee. Review the image before sharing. For two-tone cabinetry, edit one selected region at a time; Quick Recolor cannot reliably separate upper and lower finishes.
                  </p>
                )}
              </div>

              {/* 5. Submit & Cancel Actions */}
              <div className={styles.generateBar}>
                <p id="generation-readiness" role="status" className="text-sm text-charcoal-600 mb-3">
                  {generationBlocker || 'Photo and instruction ready. You can generate your concept.'}
                </p>
                {submitting && activeJob ? (
                  <div className="space-y-3 p-4 rounded-xl bg-sand-50 border border-sand-200">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 text-xs font-medium text-charcoal-800">
                        <Loader2 className="w-4 h-4 animate-spin text-bronze-700" />
                        <span>Queued in generation pipeline... ({activeJob.status})</span>
                      </div>
                      <button
                        type="button"
                        onClick={handleCancelJob}
                        disabled={cancelling}
                        className="text-xs text-charcoal-500 hover:text-red-600 font-medium px-2 py-1 rounded hover:bg-white border border-transparent hover:border-sand-300 transition-colors"
                      >
                        {cancelling ? 'Cancelling...' : 'Cancel Job'}
                      </button>
                    </div>
                    <div className="w-full bg-sand-200 rounded-full h-1.5 overflow-hidden">
                      <div className="bg-bronze-600 h-1.5 rounded-full animate-pulse w-2/3" />
                    </div>
                    <p className="text-[10px] text-charcoal-500 italic text-center">
                      Processing your source photo and edit instructions...
                    </p>
                  </div>
                ) : (
                  <button
                    type="submit"
                    disabled={Boolean(generationBlocker)}
                    aria-describedby="generation-readiness"
                    className="w-full py-3 px-4 min-h-[44px] rounded-xl bg-bronze-700 hover:bg-bronze-800 text-white font-medium text-xs sm:text-sm flex items-center justify-center gap-2 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    <Wand2 className="w-4 h-4" />
                    <span>{editingMode === 'PRECISION_MASK' ? 'Generate Precision Edit' : 'Generate AI Concept'}</span>
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
                        <div className="flex items-center gap-1.5 flex-wrap">
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
                          {job.editingMode === 'PRECISION_MASK' && (
                            <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-bronze-50 text-bronze-800 border border-bronze-200">
                              <Crosshair className="w-2.5 h-2.5 text-bronze-700" />
                              Precision Edit
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-charcoal-400">
                          {new Date(job.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-[11px] text-charcoal-700 line-clamp-1 font-medium">
                        {job.prompt}
                      </p>
                      {job.references && job.references.length > 0 && (
                        <p className="text-[10px] text-bronze-700 mt-0.5">
                          ✦ {job.references.length} visual reference{job.references.length === 1 ? '' : 's'} attached
                        </p>
                      )}
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
        <div className={styles.canvasColumn}>
          <div className="bg-white rounded-2xl border border-sand-200 p-5 sm:p-6 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-sand-100">
              <div>
                <span className="text-[10px] font-semibold text-bronze-700 uppercase tracking-wider block">
                  YOUR DESIGN CANVAS
                </span>
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-serif text-lg text-charcoal-900 font-medium">
                    {selectedComparisonJob ? 'Review your concept' : 'Explore your space'}
                  </h3>
                  <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-sand-100 text-charcoal-700 border border-sand-200">
                    <Shield className="w-2.5 h-2.5 text-bronze-700" /> Private Concept
                  </span>
                  {selectedComparisonJob?.editingMode === 'PRECISION_MASK' && (
                    <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-bronze-50 text-bronze-800 border border-bronze-200">
                      <Crosshair className="w-2.5 h-2.5 text-bronze-700" /> Precision Edit
                    </span>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 self-start flex-wrap">
                {selectedComparisonJob?.status === 'SUCCEEDED' && selectedComparisonJob.outputMediaId && (
                  <button type="button" disabled={submitting || loadingConceptSource}
                    className="rounded-lg border border-sand-300 bg-white px-3 py-2 text-xs font-medium"
                    onClick={async () => {
                      setLoadingConceptSource(true);
                      setFormError(null);
                      try {
                        const source = await fetchMediaDetail(selectedComparisonJob.outputMediaId!, selectedComparisonJob.studioId);
                        if (source.processingStatus !== 'READY' || source.mediaType === 'CLIENT_PRIVATE') throw new Error('This result is not ready for editing.');
                        setConceptSource(source);
                        setSelectedProjectId(source.projectId);
                        setSelectedMediaId(source.id);
                        setEditingMode('PRECISION_MASK');
                        setPhotoSourceMode('gallery');
                        setPrompt('');
                        setSelectedComparisonJob(null);
                      } catch (error) {
                        setFormError(error instanceof Error ? error.message : 'Unable to open this result.');
                      } finally {
                        setLoadingConceptSource(false);
                      }
                    }}>{loadingConceptSource ? 'Opening result…' : 'Edit this result'}</button>
                )}
                {selectedComparisonJob && selectedComparisonJob.inputMediaId !== selectedMediaId && (
                  <button type="button" disabled={submitting || uploadingPhoto}
                    className="rounded-lg border border-sand-300 bg-white px-3 py-2 text-xs font-medium"
                    onClick={() => {
                      setSelectedProjectId(selectedComparisonJob.projectId);
                      setSelectedMediaId(selectedComparisonJob.inputMediaId);
                      setPhotoSourceMode('gallery');
                    }}>Edit this original photo</button>
                )}
                {selectedComparisonJob?.maskPreviewUrl && (
                  <button
                    type="button"
                    onClick={() => setShowMaskOverlay(!showMaskOverlay)}
                    className={`min-h-[36px] px-3 py-1 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-colors border ${
                      showMaskOverlay
                        ? 'bg-bronze-100 text-bronze-900 border-bronze-300 shadow-sm'
                        : 'bg-[#FAF8F5] text-charcoal-600 border-sand-200 hover:bg-sand-100'
                    }`}
                    aria-label="Toggle precision mask region overlay"
                  >
                    <Crosshair className="w-3.5 h-3.5 text-bronze-700" />
                    <span>{showMaskOverlay ? 'Hide Target Region' : 'Show Target Region'}</span>
                  </button>
                )}

                {/* View Mode Toggle */}
                <div className="flex items-center gap-1 bg-[#FAF8F5] p-1 rounded-xl border border-sand-200 text-xs">
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
            </div>

              {/* Precision Mask Region Editor */}
            {selectedProjectId && selectedMediaId && editingMode === 'PRECISION_MASK' && selectedMediaFullUrl && (
              <section aria-label="Select area to edit" className="space-y-4">
                <p className={styles.canvasNote}>Paint the surface you want to change. Describe only its new finish: for example, matte black cupboard doors. Edit one colour region at a time.</p>
                <PrecisionMaskEditor
                  key={`${selectedMediaId}-${maskSession}`}
                  sourceImageUrl={selectedMediaFullUrl}
                  sourceWidth={selectedMediaAsset?.width}
                  sourceHeight={selectedMediaAsset?.height}
                  onMaskChange={(has, ratio) => { setHasMask(has); setMaskCoverageRatio(ratio); }}
                  onExportReady={(fn) => { exportMaskRef.current = fn; }}
                  disabled={submitting}
                />
              </section>
            )}
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
                          /* eslint-disable-next-line @next/next/no-img-element */
                          <img
                            src={selectedComparisonJob.inputPreviewUrl}
                            alt="Original site room"
                            className="w-full h-full object-contain"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                            Original Photo
                          </div>
                        )}
                        {showMaskOverlay && selectedComparisonJob.maskPreviewUrl && (
                          /* eslint-disable-next-line @next/next/no-img-element */
                          <img
                            src={selectedComparisonJob.maskPreviewUrl}
                            alt="Target precision edit region"
                            className="absolute inset-0 w-full h-full object-contain pointer-events-none opacity-70"
                          />
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
                          /* eslint-disable-next-line @next/next/no-img-element */
                          <img
                            src={selectedComparisonJob.outputPreviewUrl}
                            alt="AI Concept Visualization"
                            className="w-full h-full object-contain"
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
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img
                        src={selectedComparisonJob.inputPreviewUrl}
                        alt="Original site photo"
                        className="w-full h-full object-contain"
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
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img
                        src={selectedComparisonJob.outputPreviewUrl}
                        alt="AI Concept Visualization"
                        className="w-full h-full object-contain"
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

                {/* Concept Prompt & Structure Summary */}
                <div className="p-3.5 rounded-xl bg-[#FAF8F5] border border-sand-200 text-xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider block">
                      Generation Prompt:
                    </span>
                    <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-sand-200/60 text-charcoal-800">
                      Structure: {selectedComparisonJob.preserveStructure !== false ? 'Preservation requested' : 'Flexible'}
                    </span>
                  </div>
                  <p className="text-charcoal-800 leading-relaxed font-medium">
                    &ldquo;{selectedComparisonJob.prompt}&rdquo;
                  </p>
                </div>

                {/* Visual References Snapshot Used in Job */}
                {selectedComparisonJob.references && selectedComparisonJob.references.length > 0 && (
                  <div className="p-3.5 rounded-xl bg-[#FAF8F5] border border-sand-200 text-xs space-y-2">
                    <span className="text-[10px] font-semibold text-bronze-700 uppercase tracking-wider block">
                      Visual References Used ({selectedComparisonJob.references.length}):
                    </span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {selectedComparisonJob.references.map((ref) => (
                        <div
                          key={ref.id}
                          className="flex items-center gap-2 p-2 rounded-lg bg-white border border-sand-200"
                        >
                          <div className="w-10 h-10 rounded bg-sand-100 overflow-hidden flex-shrink-0 border border-sand-200">
                            {ref.previewUrl ? (
                              /* eslint-disable-next-line @next/next/no-img-element */
                              <img
                                src={ref.previewUrl}
                                alt={ref.label || ref.purposeDisplayName}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-[10px] text-charcoal-400">
                                {ref.purpose}
                              </div>
                            )}
                          </div>
                          <div className="min-w-0 flex-1">
                            <span className="text-[9px] font-bold text-bronze-700 uppercase tracking-wider block truncate">
                              {ref.purposeDisplayName || ref.purpose}
                            </span>
                            <p className="text-xs font-medium text-charcoal-900 truncate">
                              {ref.label || 'Reference Item'}
                            </p>
                            {ref.instruction && (
                              <p className="text-[10px] text-charcoal-500 truncate">
                                &ldquo;{ref.instruction}&rdquo;
                              </p>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Permanent Legal & Structure Disclaimers */}
                <div className="p-3 rounded-xl bg-sand-50 border border-sand-200 space-y-1.5">
                  <div className="flex items-start gap-2 text-[11px] text-charcoal-600 leading-relaxed">
                    <Shield className="w-4 h-4 text-bronze-700 flex-shrink-0 mt-0.5" />
                    <p>
                      <strong className="text-charcoal-800">✦ AI Concept Visualization:</strong> AI will try to preserve the existing structure. Some geometry, colors, materials, and proportions may vary — final colors, materials, proportions, and execution may differ.
                    </p>
                  </div>
                  {selectedComparisonJob.references && selectedComparisonJob.references.length > 0 && (
                    <div className="flex items-start gap-2 text-[11px] text-amber-800/90 leading-relaxed pl-6">
                      <Palette className="w-3.5 h-3.5 text-amber-700 flex-shrink-0 mt-0.5" />
                      <p>
                        Reference colors, grains, and finishes are AI approximations and may differ under different lighting and room dimensions.
                      </p>
                    </div>
                  )}
                </div>
              </div>
            ) : editingMode === 'PRECISION_MASK' && selectedMediaFullUrl ? null : selectedMediaFullUrl ? (<div className={styles.sourcePreview}><img src={selectedMediaFullUrl} alt="Source photo on design canvas" /></div>) : (
              <div className="py-16 text-center rounded-xl border border-dashed border-sand-300 p-8 space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto">
                  <Layers className="w-6 h-6" />
                </div>
                <h4 className="font-serif text-base text-charcoal-800 font-medium">
                  Start with your space
                </h4>
                <p className="text-xs text-charcoal-500 max-w-sm mx-auto leading-relaxed">
                  Select a room photo and enter a prompt to generate an AI concept, or click on a completed concept in the history list to compare.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
      )}
    </div>
  );
}
