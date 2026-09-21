'use client';

import React, { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import {
  Upload,
  Image as ImageIcon,
  CheckCircle,
  AlertCircle,
  Trash2,
  Star,
  Edit2,
  ArrowLeft,
  ArrowRight,
  Shield,
  Sparkles,
  Lock,
  Loader2,
  Eye,
  Sliders,
  X,
  Wand2,
} from 'lucide-react';
import {
  MediaDetailResponse,
  MediaType,
  MediaVisibility,
  MEDIA_TYPE_LABELS,
  UpdateMediaRequest,
  WatermarkSettings,
} from '@/lib/media/types';
import {
  fetchProjectMedia,
  uploadMediaFile,
  updateMedia,
  reorderProjectMedia,
  deleteMedia,
  fetchWatermarkSettings,
  updateWatermarkSettings,
} from '@/lib/media/api';

interface ProjectMediaManagerProps {
  projectId: string;
  studioId?: string;
  isReadOnly?: boolean;
  onCoverChanged?: (mediaId: string) => void;
}

export function ProjectMediaManager({
  projectId,
  studioId,
  isReadOnly = false,
  onCoverChanged,
}: ProjectMediaManagerProps) {
  const [mediaList, setMediaList] = useState<MediaDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Upload state
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [selectedMediaType, setSelectedMediaType] = useState<MediaType>('REAL_PROJECT');
  const [uploadAltText, setUploadAltText] = useState('');
  const [uploadCaption, setUploadCaption] = useState('');
  const [uploadWatermark, setUploadWatermark] = useState(true);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Edit modal state
  const [editingMedia, setEditingMedia] = useState<MediaDetailResponse | null>(null);
  const [editAlt, setEditAlt] = useState('');
  const [editCaption, setEditCaption] = useState('');
  const [editVisibility, setEditVisibility] = useState<MediaVisibility>('PORTFOLIO');
  const [editWatermark, setEditWatermark] = useState(true);
  const [savingEdit, setSavingEdit] = useState(false);

  // Watermark Settings modal state
  const [showWatermarkModal, setShowWatermarkModal] = useState(false);
  const [watermarkSettings, setWatermarkSettings] = useState<WatermarkSettings | null>(null);
  const [savingWatermark, setSavingWatermark] = useState(false);

  const loadMedia = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchProjectMedia(projectId, studioId);
      setMediaList(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load project media');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMedia();
  }, [projectId, studioId]);

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    const file = files[0];

    // Enforce 25MB max
    if (file.size > 26214400) {
      setError('Selected image exceeds the 25MB maximum limit.');
      return;
    }

    try {
      setUploading(true);
      setUploadProgress(10);
      setError(null);

      const isPrivateOnly = selectedMediaType === 'REFERENCE' || selectedMediaType === 'CLIENT_PRIVATE';
      const visibility: MediaVisibility = isPrivateOnly ? 'PRIVATE' : 'PORTFOLIO';

      await uploadMediaFile(file, projectId, {
        mediaType: selectedMediaType,
        altText: uploadAltText.trim() || undefined,
        caption: uploadCaption.trim() || undefined,
        watermarkEnabled: isPrivateOnly ? false : uploadWatermark,
        visibility,
        studioId,
        onProgress: (p) => setUploadProgress(p),
      });

      // Clear upload form
      setUploadAltText('');
      setUploadCaption('');
      if (fileInputRef.current) fileInputRef.current.value = '';

      // Reload gallery
      await loadMedia();
    } catch (err: any) {
      setError(err?.message || 'Failed to upload media file');
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  const handleSetCover = async (mediaId: string) => {
    if (isReadOnly) return;
    try {
      await updateMedia(mediaId, { isCover: true }, studioId);
      onCoverChanged?.(mediaId);
      await loadMedia();
    } catch (err: any) {
      setError(err?.message || 'Failed to set cover image');
    }
  };

  const handleMoveOrder = async (index: number, direction: 'left' | 'right') => {
    if (isReadOnly) return;
    const targetIdx = direction === 'left' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= mediaList.length) return;

    const newList = [...mediaList];
    const [moved] = newList.splice(index, 1);
    newList.splice(targetIdx, 0, moved);
    setMediaList(newList);

    try {
      await reorderProjectMedia(
        projectId,
        newList.map((m) => m.id),
        studioId
      );
    } catch (err: any) {
      setError(err?.message || 'Failed to reorder media');
      await loadMedia();
    }
  };

  const handleDelete = async (mediaId: string) => {
    if (isReadOnly) return;
    if (!confirm('Are you sure you want to delete this media asset? This will revoke all public derivative links.')) {
      return;
    }

    try {
      await deleteMedia(mediaId, studioId);
      await loadMedia();
    } catch (err: any) {
      setError(err?.message || 'Failed to delete media');
    }
  };

  const openEditModal = (media: MediaDetailResponse) => {
    setEditingMedia(media);
    setEditAlt(media.altText || '');
    setEditCaption(media.caption || '');
    setEditVisibility(media.visibility);
    setEditWatermark(media.watermarkEnabled);
  };

  const handleSaveEdit = async () => {
    if (!editingMedia || isReadOnly) return;
    try {
      setSavingEdit(true);
      const isPrivateOnly = editingMedia.mediaType === 'REFERENCE' || editingMedia.mediaType === 'CLIENT_PRIVATE';
      const visibility = isPrivateOnly ? 'PRIVATE' : editVisibility;

      await updateMedia(
        editingMedia.id,
        {
          altText: editAlt.trim() || undefined,
          caption: editCaption.trim() || undefined,
          visibility,
          watermarkEnabled: editWatermark,
        },
        studioId
      );

      setEditingMedia(null);
      await loadMedia();
    } catch (err: any) {
      setError(err?.message || 'Failed to save media metadata');
    } finally {
      setSavingEdit(false);
    }
  };

  const openWatermarkSettings = async () => {
    try {
      const settings = await fetchWatermarkSettings(studioId);
      setWatermarkSettings(settings);
      setShowWatermarkModal(true);
    } catch (err: any) {
      setError(err?.message || 'Failed to load watermark settings');
    }
  };

  const handleSaveWatermarkSettings = async () => {
    if (!watermarkSettings || isReadOnly) return;
    try {
      setSavingWatermark(true);
      await updateWatermarkSettings(watermarkSettings, studioId);
      setShowWatermarkModal(false);
      await loadMedia();
    } catch (err: any) {
      setError(err?.message || 'Failed to update watermark settings');
    } finally {
      setSavingWatermark(false);
    }
  };

  const getThumbnailUrl = (media: MediaDetailResponse): string => {
    if (media.visibility === 'PRIVATE') {
      return `/api/v1/media/${media.id}/preview`;
    }
    const thumb = media.derivatives.find((d) => d.variantName === 'THUMBNAIL');
    if (thumb) return thumb.publicUrl;
    const med = media.derivatives.find((d) => d.variantName === 'MEDIUM');
    if (med) return med.publicUrl;
    return `/api/v1/media/${media.id}/preview`;
  };

  return (
    <div className="bg-white border border-sand-200 rounded-xl p-5 sm:p-6 shadow-sm space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-sand-200">
        <div>
          <div className="flex items-center gap-2">
            <ImageIcon className="w-5 h-5 text-bronze-700" />
            <h3 className="font-serif text-lg text-charcoal-900 font-medium">
              Project Photography & Media
            </h3>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-sand-100 text-charcoal-700">
              {mediaList.length} {mediaList.length === 1 ? 'Asset' : 'Assets'}
            </span>
          </div>
          <p className="text-xs text-charcoal-600 mt-1">
            Originals remain clean and private. Responsive variants are automatically generated and watermarked for public presentation.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={openWatermarkSettings}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-sand-300 text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors"
          >
            <Sliders className="w-3.5 h-3.5 text-bronze-700" />
            <span>Watermark Settings</span>
          </button>
        </div>
      </div>

      {/* Error alert */}
      {error && (
        <div className="p-3.5 rounded-xl bg-red-50/70 border border-red-200 text-xs text-red-700 flex items-start gap-2">
          <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 shrink-0" />
          <div className="flex-1">
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-red-500 hover:text-red-700 font-bold"
          >
            ×
          </button>
        </div>
      )}

      {/* Upload Zone */}
      {!isReadOnly && (
        <div className="border border-dashed border-sand-300 rounded-xl p-4 sm:p-5 bg-sand-50/40 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-medium text-charcoal-700 mb-1">
                Asset Type
              </label>
              <select
                value={selectedMediaType}
                onChange={(e) => setSelectedMediaType(e.target.value as MediaType)}
                className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800"
              >
                {Object.entries(MEDIA_TYPE_LABELS).map(([k, label]) => (
                  <option key={k} value={k}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-charcoal-700 mb-1">
                Alt Text (SEO & Accessibility)
              </label>
              <input
                type="text"
                placeholder="e.g. Master bedroom with teak wardrobe"
                value={uploadAltText}
                onChange={(e) => setUploadAltText(e.target.value)}
                className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-charcoal-700 mb-1">
                Caption (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. Bespoke Italian marble vanity"
                value={uploadCaption}
                onChange={(e) => setUploadCaption(e.target.value)}
                className="w-full text-xs px-3 py-1.5 rounded-lg border border-sand-300 bg-white text-charcoal-800"
              />
            </div>
          </div>

          {selectedMediaType === 'AI_CONCEPT' && (
            <div className="p-2.5 rounded-lg bg-sand-100 border border-sand-200 flex items-center gap-2 text-xs text-charcoal-700">
              <Sparkles className="w-4 h-4 text-bronze-700 shrink-0" />
              <span>
                <strong>Platform Integrity Rule:</strong> AI Concept visualizations receive a mandatory, permanent "AI Concept Visualization" badge on all public variants.
              </span>
            </div>
          )}

          {(selectedMediaType === 'REFERENCE' || selectedMediaType === 'CLIENT_PRIVATE') && (
            <div className="p-2.5 rounded-lg bg-amber-50 border border-amber-200 flex items-center gap-2 text-xs text-amber-800">
              <Lock className="w-4 h-4 text-amber-600 shrink-0" />
              <span>
                <strong>Strict Privacy Rule:</strong> Reference and client confidential specs are strictly private to your studio team and are never published to portfolios or public galleries.
              </span>
            </div>
          )}

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2">
            <div className="flex items-center gap-4 text-xs text-charcoal-600">
              {selectedMediaType !== 'REFERENCE' && selectedMediaType !== 'CLIENT_PRIVATE' && (
                <label className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={uploadWatermark}
                    onChange={(e) => setUploadWatermark(e.target.checked)}
                    className="rounded border-sand-300 text-bronze-700 focus:ring-bronze-700"
                  />
                  <span>Apply studio watermark</span>
                </label>
              )}
              <span className="text-[11px] text-charcoal-500">
                JPEG, PNG, WebP up to 25MB
              </span>
            </div>

            <div>
              <input
                type="file"
                ref={fileInputRef}
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFileSelect}
                className="hidden"
                id="media-file-input"
                disabled={uploading}
              />
              <label
                htmlFor="media-file-input"
                className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium cursor-pointer transition-all ${
                  uploading
                    ? 'bg-sand-200 text-charcoal-400 cursor-not-allowed'
                    : 'bg-charcoal-900 hover:bg-charcoal-800 text-white shadow-sm'
                }`}
              >
                {uploading ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Uploading ({uploadProgress}%)...</span>
                  </>
                ) : (
                  <>
                    <Upload className="w-3.5 h-3.5 text-bronze-300" />
                    <span>Upload Photography</span>
                  </>
                )}
              </label>
            </div>
          </div>

          {uploading && (
            <div className="w-full bg-sand-200 rounded-full h-1.5 overflow-hidden">
              <div
                className="bg-bronze-700 h-1.5 transition-all duration-300"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          )}
        </div>
      )}

      {/* Gallery Grid */}
      {loading ? (
        <div className="py-12 flex flex-col items-center justify-center text-charcoal-400 space-y-2">
          <Loader2 className="w-6 h-6 animate-spin text-bronze-700" />
          <span className="text-xs">Loading media library...</span>
        </div>
      ) : mediaList.length === 0 ? (
        <div className="py-10 text-center rounded-xl border border-sand-200 bg-sand-50/30 p-6">
          <ImageIcon className="w-8 h-8 text-sand-400 mx-auto mb-2" />
          <h4 className="text-xs font-semibold text-charcoal-800">No project media uploaded yet</h4>
          <p className="text-[11px] text-charcoal-500 max-w-sm mx-auto mt-1">
            Upload high-resolution photography, before/after site photos, or AI conceptual renders to showcase in your portfolio.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {mediaList.map((media, idx) => {
            const thumbUrl = getThumbnailUrl(media);
            const isPrivate = media.visibility === 'PRIVATE';

            return (
              <div
                key={media.id}
                className={`group relative rounded-xl border overflow-hidden flex flex-col transition-all bg-white ${
                  media.isCover ? 'ring-2 ring-bronze-700 border-bronze-300 shadow-sm' : 'border-sand-200 hover:border-sand-300'
                }`}
              >
                {/* Media Image Thumbnail */}
                <div className="relative aspect-[4/3] bg-sand-100 overflow-hidden flex items-center justify-center">
                  {thumbUrl ? (
                    <img
                      src={thumbUrl}
                      alt={media.altText || 'Project Photo'}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="flex flex-col items-center text-charcoal-400 p-2 text-center">
                      <Lock className="w-6 h-6 mb-1 text-sand-500" />
                      <span className="text-[10px]">Private Original</span>
                    </div>
                  )}

                  {/* Badges Overlay */}
                  <div className="absolute top-2 left-2 flex flex-wrap gap-1">
                    {media.isCover && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-bronze-700 text-white shadow-sm">
                        <Star className="w-2.5 h-2.5 fill-current" /> Cover
                      </span>
                    )}
                    {media.mediaType === 'AI_CONCEPT' && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-charcoal-900/90 text-bronze-300 border border-bronze-700/50 shadow-sm backdrop-blur-sm">
                        <Sparkles className="w-2.5 h-2.5" /> AI Concept
                      </span>
                    )}
                    {media.mediaType === 'BEFORE' && (
                      <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-charcoal-800 text-sand-100 shadow-sm">
                        Before
                      </span>
                    )}
                    {media.mediaType === 'AFTER' && (
                      <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-emerald-800 text-emerald-100 shadow-sm">
                        After
                      </span>
                    )}
                    {isPrivate && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-sand-200 text-charcoal-700 shadow-sm">
                        <Lock className="w-2.5 h-2.5" /> Private
                      </span>
                    )}
                  </div>

                  {/* Watermark indicator */}
                  {media.watermarkEnabled && !isPrivate && (
                    <div className="absolute bottom-2 right-2 bg-charcoal-900/70 text-white p-1 rounded backdrop-blur-sm" title="Watermark Applied">
                      <Shield className="w-3 h-3 text-bronze-300" />
                    </div>
                  )}
                </div>

                {/* Media Meta */}
                <div className="p-3 flex-1 flex flex-col justify-between space-y-2">
                  <div>
                    <div className="flex items-center justify-between gap-1">
                      <span className="text-[10px] font-semibold text-bronze-700 uppercase tracking-wider">
                        {MEDIA_TYPE_LABELS[media.mediaType]}
                      </span>
                      <span className="text-[10px] text-charcoal-400">
                        {media.width}×{media.height}
                      </span>
                    </div>
                    {media.altText ? (
                      <p className="text-xs font-medium text-charcoal-800 line-clamp-1 mt-0.5" title={media.altText}>
                        {media.altText}
                      </p>
                    ) : (
                      <p className="text-xs text-charcoal-400 italic mt-0.5">No alt text specified</p>
                    )}
                    {media.caption && (
                      <p className="text-[11px] text-charcoal-500 line-clamp-1 mt-0.5" title={media.caption}>
                        {media.caption}
                      </p>
                    )}
                  </div>

                  {/* Card Actions */}
                  {!isReadOnly && (
                    <div className="pt-2 border-t border-sand-100 flex items-center justify-between text-charcoal-600">
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => handleMoveOrder(idx, 'left')}
                          className="p-1 rounded hover:bg-sand-100 disabled:opacity-25 transition-colors"
                          title="Move Left"
                        >
                          <ArrowLeft className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          disabled={idx === mediaList.length - 1}
                          onClick={() => handleMoveOrder(idx, 'right')}
                          className="p-1 rounded hover:bg-sand-100 disabled:opacity-25 transition-colors"
                          title="Move Right"
                        >
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div className="flex items-center gap-1">
                        {!isPrivate && media.mediaType !== 'CLIENT_PRIVATE' && media.mediaType !== 'AI_CONCEPT' && (
                          <Link
                            href={`/workspace/ai?projectId=${projectId}&mediaId=${media.id}`}
                            className="p-1 rounded hover:bg-sand-100 text-charcoal-600 hover:text-bronze-700 transition-colors"
                            title="Visualize with AI"
                          >
                            <Wand2 className="w-3.5 h-3.5" />
                          </Link>
                        )}
                        {!media.isCover && !isPrivate && (
                          <button
                            type="button"
                            onClick={() => handleSetCover(media.id)}
                            className="p-1 rounded hover:bg-sand-100 text-charcoal-600 hover:text-bronze-700 transition-colors"
                            title="Set as Project Cover"
                          >
                            <Star className="w-3.5 h-3.5" />
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => openEditModal(media)}
                          className="p-1 rounded hover:bg-sand-100 text-charcoal-600 hover:text-charcoal-900 transition-colors"
                          title="Edit Details"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(media.id)}
                          className="p-1 rounded hover:bg-red-50 text-charcoal-400 hover:text-red-600 transition-colors"
                          title="Delete Asset"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Edit Metadata Modal */}
      {editingMedia && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4 border border-sand-200">
            <div className="flex items-center justify-between pb-3 border-b border-sand-200">
              <h3 className="font-serif text-base text-charcoal-900 font-medium">
                Edit Media Asset
              </h3>
              <button
                type="button"
                onClick={() => setEditingMedia(null)}
                className="text-charcoal-400 hover:text-charcoal-800 p-1"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Alt Text (SEO & Accessibility)
                </label>
                <input
                  type="text"
                  maxLength={255}
                  value={editAlt}
                  onChange={(e) => setEditAlt(e.target.value)}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Caption
                </label>
                <textarea
                  rows={2}
                  maxLength={1000}
                  value={editCaption}
                  onChange={(e) => setEditCaption(e.target.value)}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700"
                />
              </div>

              {editingMedia.mediaType !== 'REFERENCE' && editingMedia.mediaType !== 'CLIENT_PRIVATE' && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                      Publication Visibility
                    </label>
                    <select
                      value={editVisibility}
                      onChange={(e) => setEditVisibility(e.target.value as MediaVisibility)}
                      className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 bg-white"
                    >
                      <option value="PORTFOLIO">Portfolio & Website</option>
                      <option value="PUBLIC">Public & Discoverable</option>
                      <option value="PRIVATE">Private / Studio Only</option>
                    </select>
                  </div>

                  <div className="pt-2">
                    <label className="flex items-center gap-2 cursor-pointer text-xs text-charcoal-800">
                      <input
                        type="checkbox"
                        checked={editWatermark}
                        onChange={(e) => setEditWatermark(e.target.checked)}
                        className="rounded border-sand-300 text-bronze-700 focus:ring-bronze-700"
                      />
                      <span>Watermark Protection (Studio Logo/Signature)</span>
                    </label>
                  </div>
                </>
              )}
            </div>

            <div className="flex items-center justify-end gap-2 pt-4 border-t border-sand-200">
              <button
                type="button"
                onClick={() => setEditingMedia(null)}
                className="px-4 py-2 rounded-xl text-xs font-medium border border-sand-300 hover:bg-sand-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={savingEdit}
                onClick={handleSaveEdit}
                className="px-4 py-2 rounded-xl text-xs font-medium bg-charcoal-900 hover:bg-charcoal-800 text-white disabled:opacity-50"
              >
                {savingEdit ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Watermark Settings Modal */}
      {showWatermarkModal && watermarkSettings && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-5 border border-sand-200">
            <div className="flex items-center justify-between pb-3 border-b border-sand-200">
              <div className="flex items-center gap-2">
                <Shield className="w-5 h-5 text-bronze-700" />
                <h3 className="font-serif text-base text-charcoal-900 font-medium">
                  Studio Watermark & Copyright Protection
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowWatermarkModal(false)}
                className="text-charcoal-400 hover:text-charcoal-800 p-1"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 rounded-xl bg-sand-50 border border-sand-200">
                <div>
                  <h4 className="text-xs font-semibold text-charcoal-900">Enable Automated Watermark</h4>
                  <p className="text-[11px] text-charcoal-500">
                    Apply copyright branding to public portfolio and website derivatives.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={watermarkSettings.enabled}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, enabled: e.target.checked })
                  }
                  className="rounded border-sand-300 text-bronze-700 focus:ring-bronze-700 w-4 h-4"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                    Placement Position
                  </label>
                  <select
                    value={watermarkSettings.position}
                    onChange={(e) =>
                      setWatermarkSettings({
                        ...watermarkSettings,
                        position: e.target.value as any,
                      })
                    }
                    className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 bg-white"
                  >
                    <option value="BOTTOM_RIGHT">Bottom Right (Standard)</option>
                    <option value="BOTTOM_LEFT">Bottom Left</option>
                    <option value="TOP_RIGHT">Top Right</option>
                    <option value="TOP_LEFT">Top Left</option>
                    <option value="CENTER">Center (Full Protection)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                    Opacity: {Math.round(watermarkSettings.opacity * 100)}%
                  </label>
                  <input
                    type="range"
                    min="0.10"
                    max="1.00"
                    step="0.05"
                    value={watermarkSettings.opacity}
                    onChange={(e) =>
                      setWatermarkSettings({
                        ...watermarkSettings,
                        opacity: parseFloat(e.target.value),
                      })
                    }
                    className="w-full accent-bronze-700 mt-2"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Watermark Signature Text
                </label>
                <input
                  type="text"
                  maxLength={100}
                  placeholder="e.g. Studio Name"
                  value={watermarkSettings.fallbackText || ''}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, fallbackText: e.target.value })
                  }
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700"
                />
                <span className="text-[10px] text-charcoal-500 mt-0.5 block">
                  Rendered as "© {watermarkSettings.fallbackText || 'Studio Name'}" with dark pill backing.
                </span>
              </div>

              {/* Real-time Preview Pill */}
              <div className="p-4 rounded-xl bg-charcoal-900 text-white relative aspect-[16/7] flex items-center justify-center overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-tr from-stone-800 via-stone-700 to-stone-600 opacity-60" />
                <span className="text-xs text-sand-300 z-0">Sample Room Photography</span>
                <div
                  className={`absolute z-10 px-2.5 py-1 rounded-md text-[11px] font-bold bg-black/60 text-white backdrop-blur-xs flex items-center gap-1.5 ${
                    watermarkSettings.position === 'BOTTOM_RIGHT'
                      ? 'bottom-3 right-3'
                      : watermarkSettings.position === 'BOTTOM_LEFT'
                      ? 'bottom-3 left-3'
                      : watermarkSettings.position === 'TOP_RIGHT'
                      ? 'top-3 right-3'
                      : watermarkSettings.position === 'TOP_LEFT'
                      ? 'top-3 left-3'
                      : 'top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2'
                  }`}
                  style={{ opacity: watermarkSettings.opacity }}
                >
                  <span>© {watermarkSettings.fallbackText || 'Studio Name'}</span>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-4 border-t border-sand-200">
              <button
                type="button"
                onClick={() => setShowWatermarkModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-medium border border-sand-300 hover:bg-sand-50"
              >
                Close
              </button>
              <button
                type="button"
                disabled={savingWatermark}
                onClick={handleSaveWatermarkSettings}
                className="px-4 py-2 rounded-xl text-xs font-medium bg-charcoal-900 hover:bg-charcoal-800 text-white disabled:opacity-50"
              >
                {savingWatermark ? 'Saving...' : 'Update Settings'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
