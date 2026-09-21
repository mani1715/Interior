'use client';

import React, { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Image as ImageIcon,
  ShieldCheck,
  Filter,
  Search,
  Sparkles,
  Eye,
  EyeOff,
  Lock,
  Star,
  Settings,
  X,
  ExternalLink,
  Layers,
  AlertCircle,
  Loader2,
  CheckCircle2,
  Building,
} from 'lucide-react';
import {
  MediaDetailResponse,
  MediaType,
  MediaVisibility,
  WatermarkPosition,
  WatermarkSettings,
  CANONICAL_MEDIA_TYPES,
  WATERMARK_POSITIONS,
  MEDIA_TYPE_LABELS,
  VISIBILITY_LABELS,
} from '@/lib/media/types';
import {
  fetchStudioMedia,
  fetchWatermarkSettings,
  updateWatermarkSettings,
} from '@/lib/media/api';
import { fetchProjects } from '@/lib/projects/api';
import { ProjectSummaryDto } from '@/lib/projects/types';

export default function MediaWorkspacePage() {
  const [mediaList, setMediaList] = useState<MediaDetailResponse[]>([]);
  const [projects, setProjects] = useState<ProjectSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [projectFilter, setProjectFilter] = useState<string>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [visibilityFilter, setVisibilityFilter] = useState<string>('ALL');

  // Detail Modal
  const [selectedMedia, setSelectedMedia] = useState<MediaDetailResponse | null>(null);

  // Watermark Settings Modal
  const [isWatermarkOpen, setIsWatermarkOpen] = useState(false);
  const [watermarkSettings, setWatermarkSettings] = useState<WatermarkSettings>({
    enabled: true,
    position: 'BOTTOM_RIGHT',
    opacity: 0.35,
    sizePercentage: 15,
    useLogo: false,
    fallbackText: '',
  });
  const [savingWatermark, setSavingWatermark] = useState(false);
  const [watermarkSavedMsg, setWatermarkSavedMsg] = useState(false);

  // Load media and projects
  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        setError(null);
        const [mediaData, projectsData, wmSettings] = await Promise.all([
          fetchStudioMedia(),
          fetchProjects({ includeArchived: true }),
          fetchWatermarkSettings().catch(() => ({
            enabled: true,
            position: 'BOTTOM_RIGHT' as WatermarkPosition,
            opacity: 0.35,
            sizePercentage: 15,
            useLogo: false,
            fallbackText: '',
          })),
        ]);
        setMediaList(mediaData);
        setProjects(projectsData);
        setWatermarkSettings(wmSettings);
      } catch (err: any) {
        setError(err.message || 'Failed to load media assets');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  // Filtered Media
  const filteredMedia = useMemo(() => {
    return mediaList.filter((item) => {
      if (projectFilter !== 'ALL' && item.projectId !== projectFilter) {
        return false;
      }
      if (typeFilter !== 'ALL' && item.mediaType !== typeFilter) {
        return false;
      }
      if (visibilityFilter !== 'ALL' && item.visibility !== visibilityFilter) {
        return false;
      }
      if (searchTerm) {
        const query = searchTerm.toLowerCase();
        const matchesAlt = item.altText?.toLowerCase().includes(query);
        const matchesCaption = item.caption?.toLowerCase().includes(query);
        const matchesStorage = item.originalStorageKey?.toLowerCase().includes(query);
        if (!matchesAlt && !matchesCaption && !matchesStorage) {
          return false;
        }
      }
      return true;
    });
  }, [mediaList, projectFilter, typeFilter, visibilityFilter, searchTerm]);

  // Metrics
  const metrics = useMemo(() => {
    const total = mediaList.length;
    const portfolio = mediaList.filter(
      (m) => m.visibility === 'PORTFOLIO' || m.visibility === 'PUBLIC'
    ).length;
    const aiConcept = mediaList.filter((m) => m.mediaType === 'AI_CONCEPT').length;
    const privateAssets = mediaList.filter(
      (m) => m.visibility === 'PRIVATE' || m.mediaType === 'REFERENCE' || m.mediaType === 'CLIENT_PRIVATE'
    ).length;
    return { total, portfolio, aiConcept, privateAssets };
  }, [mediaList]);

  // Project map for title lookup
  const projectMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const p of projects) {
      map.set(p.id, p.title);
    }
    return map;
  }, [projects]);

  const handleSaveWatermark = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSavingWatermark(true);
      const updated = await updateWatermarkSettings(watermarkSettings);
      setWatermarkSettings(updated);
      setWatermarkSavedMsg(true);
      setTimeout(() => setWatermarkSavedMsg(false), 3000);
    } catch (err: any) {
      alert(`Failed to update watermark settings: ${err.message}`);
    } finally {
      setSavingWatermark(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-7xl mx-auto space-y-6">
      {/* Top Breadcrumb & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-2 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Workspace Home</span>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
              <ImageIcon className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-widest text-bronze-700 block">
                Asset Storage & Protection
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Studio Media Library
              </h1>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsWatermarkOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-sand-300 bg-white text-charcoal-800 text-xs font-medium hover:bg-sand-50 transition-colors shadow-sm"
          >
            <Settings className="w-3.5 h-3.5 text-bronze-700" />
            <span>Watermark Settings</span>
          </button>
        </div>
      </div>

      {/* Metrics Banner */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
          <span className="text-[11px] font-medium text-charcoal-500 uppercase tracking-wider block">
            Total Assets
          </span>
          <span className="text-2xl font-serif font-semibold text-charcoal-900 mt-1 block">
            {metrics.total}
          </span>
          <span className="text-[10px] text-charcoal-400 mt-0.5 block">Original private masters</span>
        </div>
        <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
          <span className="text-[11px] font-medium text-charcoal-500 uppercase tracking-wider block">
            Portfolio Media
          </span>
          <span className="text-2xl font-serif font-semibold text-emerald-700 mt-1 block">
            {metrics.portfolio}
          </span>
          <span className="text-[10px] text-charcoal-400 mt-0.5 block">Watermarked derivatives</span>
        </div>
        <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
          <span className="text-[11px] font-medium text-charcoal-500 uppercase tracking-wider block">
            AI Visualizations
          </span>
          <span className="text-2xl font-serif font-semibold text-purple-700 mt-1 block">
            {metrics.aiConcept}
          </span>
          <span className="text-[10px] text-charcoal-400 mt-0.5 block">Mandatory AI disclosure</span>
        </div>
        <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
          <span className="text-[11px] font-medium text-charcoal-500 uppercase tracking-wider block">
            Private Assets
          </span>
          <span className="text-2xl font-serif font-semibold text-charcoal-700 mt-1 block">
            {metrics.privateAssets}
          </span>
          <span className="text-[10px] text-charcoal-400 mt-0.5 block">Never public / no watermark</span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-charcoal-400" />
          <input
            type="text"
            placeholder="Search media by alt text, caption..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {/* Project Filter */}
          <select
            value={projectFilter}
            onChange={(e) => setProjectFilter(e.target.value)}
            className="text-xs px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
          >
            <option value="ALL">All Projects ({projects.length})</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title}
              </option>
            ))}
          </select>

          {/* Type Filter */}
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="text-xs px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
          >
            <option value="ALL">All Media Types</option>
            {CANONICAL_MEDIA_TYPES.map((t) => (
              <option key={t.code} value={t.code}>
                {t.label}
              </option>
            ))}
          </select>

          {/* Visibility Filter */}
          <select
            value={visibilityFilter}
            onChange={(e) => setVisibilityFilter(e.target.value)}
            className="text-xs px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
          >
            <option value="ALL">All Visibility</option>
            <option value="PORTFOLIO">Portfolio Showcase</option>
            <option value="PUBLIC">Public & Discoverable</option>
            <option value="PRIVATE">Private (Studio Only)</option>
          </select>
        </div>
      </div>

      {/* Main Content Area */}
      {loading ? (
        <div className="py-20 text-center flex flex-col items-center justify-center">
          <Loader2 className="w-8 h-8 text-bronze-700 animate-spin mb-3" />
          <p className="text-xs text-charcoal-500">Loading studio media library...</p>
        </div>
      ) : error ? (
        <div className="p-6 bg-red-50 border border-red-200 rounded-xl text-center space-y-2">
          <AlertCircle className="w-6 h-6 text-red-600 mx-auto" />
          <p className="text-xs font-semibold text-red-800">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="text-xs text-red-600 underline font-medium"
          >
            Retry
          </button>
        </div>
      ) : filteredMedia.length === 0 ? (
        <div className="bg-white border border-sand-200 rounded-2xl p-12 text-center shadow-sm space-y-3">
          <div className="w-12 h-12 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto mb-2">
            <ImageIcon className="w-6 h-6" />
          </div>
          <h3 className="font-serif text-lg text-charcoal-900">No media assets found</h3>
          <p className="text-xs text-charcoal-500 max-w-md mx-auto">
            {mediaList.length === 0
              ? 'Upload photography and renders within project stories to populate the studio media library.'
              : 'No media matches your search and filter criteria.'}
          </p>
          {mediaList.length === 0 && (
            <Link
              href="/workspace/projects"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors mt-2"
            >
              <span>Go to Projects</span>
            </Link>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {filteredMedia.map((item) => {
            const thumbUrl =
              item.derivatives.find((d) => d.variantName === 'THUMBNAIL')?.publicUrl ||
              item.derivatives.find((d) => d.variantName === 'MEDIUM')?.publicUrl;
            const projectTitle = projectMap.get(item.projectId) || 'Project';
            const displayName = item.altText || item.caption || item.id.substring(0, 8);

            return (
              <div
                key={item.id}
                onClick={() => setSelectedMedia(item)}
                className="group relative bg-white border border-sand-200 rounded-xl overflow-hidden shadow-sm hover:border-bronze-400 hover:shadow-md transition-all cursor-pointer flex flex-col"
              >
                {/* Image aspect ratio container */}
                <div className="aspect-[4/3] bg-sand-100 relative overflow-hidden flex items-center justify-center">
                  {thumbUrl ? (
                    <img
                      src={thumbUrl}
                      alt={displayName}
                      className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                      loading="lazy"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center p-3 text-center text-charcoal-400">
                      <Lock className="w-6 h-6 mb-1 text-charcoal-300" />
                      <span className="text-[10px] font-medium">Private Original</span>
                    </div>
                  )}

                  {/* Top Badges */}
                  <div className="absolute top-2 left-2 flex flex-col gap-1 items-start">
                    {item.isCover && (
                      <span className="px-1.5 py-0.5 rounded bg-bronze-700 text-white text-[9px] font-semibold flex items-center gap-1 shadow-sm">
                        <Star className="w-2.5 h-2.5 fill-white" />
                        Cover
                      </span>
                    )}
                    {item.mediaType === 'AI_CONCEPT' && (
                      <span className="px-1.5 py-0.5 rounded bg-purple-900/90 text-purple-200 text-[9px] font-semibold flex items-center gap-1 shadow-sm backdrop-blur-sm">
                        <Sparkles className="w-2.5 h-2.5 text-purple-300" />
                        AI Concept
                      </span>
                    )}
                  </div>

                  {/* Visibility Badge */}
                  <div className="absolute top-2 right-2">
                    {item.visibility === 'PORTFOLIO' || item.visibility === 'PUBLIC' ? (
                      <span className="p-1 rounded-md bg-white/90 text-emerald-800 shadow-sm backdrop-blur-sm" title={VISIBILITY_LABELS[item.visibility]}>
                        <Eye className="w-3 h-3" />
                      </span>
                    ) : (
                      <span className="p-1 rounded-md bg-black/60 text-white/90 shadow-sm backdrop-blur-sm" title="Private Asset">
                        <EyeOff className="w-3 h-3" />
                      </span>
                    )}
                  </div>
                </div>

                {/* Card Info */}
                <div className="p-2.5 flex-1 flex flex-col justify-between">
                  <div>
                    <span className="text-[9px] font-semibold uppercase tracking-wider text-bronze-700 block truncate">
                      {MEDIA_TYPE_LABELS[item.mediaType] || item.mediaType}
                    </span>
                    <p className="text-xs font-medium text-charcoal-900 truncate mt-0.5" title={displayName}>
                      {displayName}
                    </p>
                  </div>

                  <div className="mt-2 pt-2 border-t border-sand-100 flex items-center justify-between text-[10px] text-charcoal-500">
                    <span className="truncate max-w-[120px]" title={projectTitle}>
                      {projectTitle}
                    </span>
                    <span>{item.derivatives.length} variants</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Selected Media Detail Modal */}
      {selectedMedia && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-sand-300 max-h-[90vh] overflow-y-auto space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-[10px] font-semibold uppercase tracking-widest text-bronze-700 block">
                  Media Asset Inspection
                </span>
                <h2 className="font-serif text-lg text-charcoal-900 font-medium">
                  {selectedMedia.altText || selectedMedia.caption || 'Media Asset'}
                </h2>
              </div>
              <button
                onClick={() => setSelectedMedia(null)}
                className="p-1 rounded-lg text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Preview derivative */}
            <div className="aspect-video bg-sand-100 rounded-xl overflow-hidden flex items-center justify-center relative border border-sand-200">
              {selectedMedia.derivatives.find((d) => d.variantName === 'LARGE')?.publicUrl ||
              selectedMedia.derivatives.find((d) => d.variantName === 'MEDIUM')?.publicUrl ? (
                <img
                  src={
                    selectedMedia.derivatives.find((d) => d.variantName === 'LARGE')?.publicUrl ||
                    selectedMedia.derivatives.find((d) => d.variantName === 'MEDIUM')?.publicUrl
                  }
                  alt={selectedMedia.altText || 'Preview'}
                  className="w-full h-full object-contain"
                />
              ) : (
                <div className="text-center p-6 space-y-2">
                  <Lock className="w-8 h-8 text-charcoal-400 mx-auto" />
                  <p className="text-xs font-semibold text-charcoal-700">Strictly Private Asset</p>
                  <p className="text-[11px] text-charcoal-500 max-w-xs">
                    Original masters of private assets are never exposed via public endpoints.
                  </p>
                </div>
              )}

              {selectedMedia.mediaType === 'AI_CONCEPT' && (
                <div className="absolute bottom-2 left-2 px-2.5 py-1 rounded bg-black/80 text-white text-[10px] font-medium flex items-center gap-1.5 shadow-md backdrop-blur-sm">
                  <Sparkles className="w-3 h-3 text-purple-300" />
                  <span>✦ AI Concept Visualization (Truth In Advertising)</span>
                </div>
              )}
            </div>

            {/* Metadata grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Media Type</span>
                <span className="font-semibold text-charcoal-800 mt-0.5 block">
                  {selectedMedia.mediaTypeDisplayName || MEDIA_TYPE_LABELS[selectedMedia.mediaType]}
                </span>
              </div>
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Visibility</span>
                <span className="font-semibold text-charcoal-800 mt-0.5 block">
                  {VISIBILITY_LABELS[selectedMedia.visibility] || selectedMedia.visibility}
                </span>
              </div>
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Resolution</span>
                <span className="font-semibold text-charcoal-800 mt-0.5 block">
                  {selectedMedia.width && selectedMedia.height
                    ? `${selectedMedia.width} × ${selectedMedia.height} px`
                    : 'Unprocessed'}
                </span>
              </div>
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">File Size</span>
                <span className="font-semibold text-charcoal-800 mt-0.5 block">
                  {(selectedMedia.fileSize / 1024).toFixed(1)} KB
                </span>
              </div>
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Watermarked</span>
                <span className="font-semibold text-charcoal-800 mt-0.5 block">
                  {selectedMedia.watermarkEnabled ? 'Yes (Configured)' : 'No / Private'}
                </span>
              </div>
              <div className="p-3 bg-sand-50 rounded-lg border border-sand-200">
                <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Project</span>
                <Link
                  href={`/workspace/projects/${selectedMedia.projectId}`}
                  className="font-semibold text-bronze-700 hover:underline mt-0.5 flex items-center gap-1 truncate"
                >
                  <span className="truncate">{projectMap.get(selectedMedia.projectId) || 'Open Project'}</span>
                  <ExternalLink className="w-2.5 h-2.5 shrink-0" />
                </Link>
              </div>
            </div>

            {/* Generated Derivatives list */}
            {selectedMedia.derivatives.length > 0 && (
              <div className="space-y-2">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-charcoal-500 block">
                  Generated Public Variants
                </span>
                <div className="space-y-1.5">
                  {selectedMedia.derivatives.map((deriv) => (
                    <div
                      key={deriv.id}
                      className="p-2.5 rounded-lg border border-sand-200 bg-white flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2">
                        <span className="px-1.5 py-0.5 bg-sand-100 text-charcoal-700 rounded font-semibold text-[10px]">
                          {deriv.variantName}
                        </span>
                        <span className="text-charcoal-600">
                          {deriv.width} × {deriv.height} px
                        </span>
                        <span className="text-charcoal-400 text-[11px]">
                          ({(deriv.fileSize / 1024).toFixed(1)} KB)
                        </span>
                        {deriv.isWatermarked && (
                          <span className="text-emerald-700 font-medium text-[10px] bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                            Watermarked
                          </span>
                        )}
                      </div>
                      <a
                        href={deriv.publicUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-bronze-700 hover:underline flex items-center gap-1 font-medium text-[11px]"
                      >
                        <span>View Derivative</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="pt-3 border-t border-sand-200 flex justify-end">
              <button
                onClick={() => setSelectedMedia(null)}
                className="px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Watermark Settings Modal */}
      {isWatermarkOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-sand-300 space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-[10px] font-semibold uppercase tracking-widest text-bronze-700 block">
                  Copyright Protection
                </span>
                <h2 className="font-serif text-lg text-charcoal-900 font-medium">
                  Studio Watermark Settings
                </h2>
              </div>
              <button
                onClick={() => setIsWatermarkOpen(false)}
                className="p-1 rounded-lg text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveWatermark} className="space-y-4">
              {/* Enable toggle */}
              <div className="flex items-center justify-between p-3 bg-sand-50 rounded-xl border border-sand-200">
                <div>
                  <span className="text-xs font-semibold text-charcoal-900 block">
                    Automatic Watermarking
                  </span>
                  <span className="text-[11px] text-charcoal-500 block">
                    Applies to all public portfolio photos and AI concepts.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={watermarkSettings.enabled}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, enabled: e.target.checked })
                  }
                  className="w-4 h-4 rounded text-bronze-700 focus:ring-bronze-700 border-sand-300"
                />
              </div>

              {/* Custom Text */}
              <div>
                <label className="text-[11px] font-semibold uppercase tracking-wider text-charcoal-600 block mb-1">
                  Watermark Label Text
                </label>
                <input
                  type="text"
                  placeholder="e.g. Studio Arc Interiors"
                  value={watermarkSettings.fallbackText || ''}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, fallbackText: e.target.value })
                  }
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700"
                />
                <span className="text-[10px] text-charcoal-400 block mt-1">
                  Leave empty to automatically use your registered Studio Brand name.
                </span>
              </div>

              {/* Position */}
              <div>
                <label className="text-[11px] font-semibold uppercase tracking-wider text-charcoal-600 block mb-1">
                  Placement Position
                </label>
                <select
                  value={watermarkSettings.position}
                  onChange={(e) =>
                    setWatermarkSettings({
                      ...watermarkSettings,
                      position: e.target.value as WatermarkPosition,
                    })
                  }
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white"
                >
                  {WATERMARK_POSITIONS.map((p) => (
                    <option key={p.code} value={p.code}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Opacity Slider */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-semibold uppercase tracking-wider text-charcoal-600">
                    Watermark Opacity
                  </label>
                  <span className="text-xs font-semibold text-charcoal-800">
                    {Math.round(watermarkSettings.opacity * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min="0.10"
                  max="0.90"
                  step="0.05"
                  value={watermarkSettings.opacity}
                  onChange={(e) =>
                    setWatermarkSettings({
                      ...watermarkSettings,
                      opacity: parseFloat(e.target.value),
                    })
                  }
                  className="w-full accent-bronze-700"
                />
              </div>

              {/* Interactive Preview Canvas */}
              <div className="p-3 bg-sand-50 rounded-xl border border-sand-200">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-charcoal-500 block mb-2">
                  Live Watermark Position Preview
                </span>
                <div className="relative aspect-video bg-charcoal-900/80 rounded-lg overflow-hidden flex items-center justify-center border border-charcoal-800">
                  <span className="text-[11px] text-white/30 font-medium">Sample Photography Area</span>
                  <div
                    className={`absolute p-2 pointer-events-none transition-all ${
                      watermarkSettings.position === 'BOTTOM_RIGHT'
                        ? 'bottom-2 right-2'
                        : watermarkSettings.position === 'BOTTOM_LEFT'
                        ? 'bottom-2 left-2'
                        : watermarkSettings.position === 'TOP_RIGHT'
                        ? 'top-2 right-2'
                        : watermarkSettings.position === 'TOP_LEFT'
                        ? 'top-2 left-2'
                        : 'inset-0 flex items-center justify-center'
                    }`}
                  >
                    <div
                      style={{ opacity: watermarkSettings.enabled ? watermarkSettings.opacity : 0 }}
                      className="px-2 py-1 bg-black/60 rounded text-white text-[10px] tracking-wider uppercase font-semibold backdrop-blur-xs"
                    >
                      © {watermarkSettings.fallbackText?.trim() || 'STUDIO BRAND'}
                    </div>
                  </div>
                </div>
              </div>

              {watermarkSavedMsg && (
                <div className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center gap-2 text-emerald-800 text-xs">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Watermark settings updated successfully.</span>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsWatermarkOpen(false)}
                  className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 text-xs font-medium hover:bg-sand-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingWatermark}
                  className="inline-flex items-center gap-2 px-5 py-2 rounded-xl bg-bronze-700 text-white text-xs font-medium hover:bg-bronze-800 transition-colors disabled:opacity-50"
                >
                  {savingWatermark && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Save Watermark Settings</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
