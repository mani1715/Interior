'use client';

import React, { useState, useEffect } from 'react';
import {
  X,
  Star,
  Home,
  Crosshair,
  Sparkles,
  Shield,
  Trash2,
  Check,
  Loader2,
  Lock,
  Eye,
  Film,
} from 'lucide-react';
import {
  MediaDetailResponse,
  MediaVisibility,
  UpdateMediaRequest,
  VISIBILITY_LABELS,
} from '@/lib/media/types';
import { ProjectRoomDto } from '@/lib/projects/types';

interface PhotoInspectorSlideoverProps {
  isOpen: boolean;
  onClose: () => void;
  media: MediaDetailResponse | null;
  rooms: ProjectRoomDto[];
  onUpdate: (mediaId: string, data: UpdateMediaRequest) => Promise<void>;
  onDelete: (mediaId: string) => Promise<void>;
  onOpenFocalModal: (media: MediaDetailResponse) => void;
  isReadOnly?: boolean;
}

export function PhotoInspectorSlideover({
  isOpen,
  onClose,
  media,
  rooms,
  onUpdate,
  onDelete,
  onOpenFocalModal,
  isReadOnly = false,
}: PhotoInspectorSlideoverProps) {
  const [altText, setAltText] = useState('');
  const [caption, setCaption] = useState('');
  const [selectedRoomId, setSelectedRoomId] = useState<string>('unassigned');
  const [isCover, setIsCover] = useState(false);
  const [isRoomCover, setIsRoomCover] = useState(false);
  const [visibility, setVisibility] = useState<MediaVisibility>('PORTFOLIO');
  const [motionEnabled, setMotionEnabled] = useState(true);
  const [watermarkEnabled, setWatermarkEnabled] = useState(true);

  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (media && isOpen) {
      setAltText(media.altText || '');
      setCaption(media.caption || '');
      setSelectedRoomId(media.roomId || 'unassigned');
      setIsCover(Boolean(media.isCover));
      setIsRoomCover(Boolean(media.isRoomCover));
      setVisibility(media.visibility || 'PORTFOLIO');
      setMotionEnabled(media.motionEnabled !== false);
      setWatermarkEnabled(Boolean(media.watermarkEnabled));
      setError(null);
    }
  }, [media, isOpen]);

  if (!isOpen || !media) return null;

  const currentPreviewUrl =
    media.derivatives?.find((d) => d.variantName === 'LARGE')?.publicUrl ||
    media.derivatives?.find((d) => d.variantName === 'THUMBNAIL')?.publicUrl ||
    media.originalStorageKey;

  const focalX = media.focalX ?? 50;
  const focalY = media.focalY ?? 50;

  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);

      const isUnassigned = selectedRoomId === 'unassigned';
      const updateData: UpdateMediaRequest = {
        altText: altText.trim() || undefined,
        caption: caption.trim() || undefined,
        isCover,
        visibility,
        watermarkEnabled,
        roomId: isUnassigned ? undefined : selectedRoomId,
        clearRoom: isUnassigned,
        isRoomCover: isUnassigned ? false : isRoomCover,
        focalX,
        focalY,
        motionEnabled,
      };

      await onUpdate(media.id, updateData);
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to save photo properties');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this photograph? This action cannot be undone.')) {
      return;
    }
    try {
      setDeleting(true);
      setError(null);
      await onDelete(media.id);
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to delete photograph');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="photo-inspector-title"
      className="fixed inset-0 z-50 overflow-hidden bg-charcoal-950/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-lg bg-white shadow-2xl flex flex-col border-l border-sand-200">
          {/* Header */}
          <div className="px-6 py-4 border-b border-sand-100 flex items-center justify-between bg-sand-50/50">
            <div>
              <h3 id="photo-inspector-title" className="font-serif text-base font-semibold text-charcoal-900">
                Photo Inspector
              </h3>
              <p className="text-xs text-charcoal-500">
                Configure space grouping, dual covers, and presentation focal point.
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-100 rounded-lg transition-colors"
              aria-label="Close panel"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {error && (
              <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
                {error}
              </div>
            )}

            {/* Photo Preview & Focal Indicator */}
            <div className="relative rounded-xl overflow-hidden border border-sand-200 bg-charcoal-900 aspect-video shadow-inner group">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={currentPreviewUrl}
                alt={media.altText || 'Photograph preview'}
                className="w-full h-full object-cover transition-all"
                style={{
                  objectPosition: `${focalX}% ${focalY}%`,
                }}
              />

              {/* Status Badges Overlay */}
              <div className="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5 pointer-events-none">
                {isCover && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500 text-white shadow-sm">
                    <Star className="w-3 h-3 fill-current" />
                    <span>Project Cover</span>
                  </span>
                )}
                {isRoomCover && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-600 text-white shadow-sm">
                    <Home className="w-3 h-3" />
                    <span>Room Cover</span>
                  </span>
                )}
                {media.mediaType === 'AI_CONCEPT' && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-600 text-white shadow-sm">
                    <Sparkles className="w-3 h-3" />
                    <span>AI Concept</span>
                  </span>
                )}
                {visibility === 'PRIVATE' && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-charcoal-700 text-white shadow-sm">
                    <Lock className="w-3 h-3" />
                    <span>Private</span>
                  </span>
                )}
              </div>

              {/* Focal Reticle Quick Trigger */}
              {!isReadOnly && (
                <button
                  type="button"
                  onClick={() => onOpenFocalModal(media)}
                  className="absolute bottom-2.5 right-2.5 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/90 hover:bg-white text-charcoal-900 text-xs font-medium shadow-md backdrop-blur-sm transition-transform active:scale-95"
                >
                  <Crosshair className="w-3.5 h-3.5 text-bronze-700" />
                  <span>Calibrate Focal ({focalX.toFixed(0)}%, {focalY.toFixed(0)}%)</span>
                </button>
              )}
            </div>

            {/* Spatial Room Assignment */}
            <div className="bg-sand-50/60 rounded-xl p-4 border border-sand-200 space-y-3">
              <label className="block text-xs font-semibold text-charcoal-900">
                Space / Room Assignment
              </label>
              <select
                disabled={isReadOnly}
                value={selectedRoomId}
                onChange={(e) => {
                  setSelectedRoomId(e.target.value);
                  if (e.target.value === 'unassigned') {
                    setIsRoomCover(false);
                  }
                }}
                className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white text-charcoal-900"
              >
                <option value="unassigned">Project Photos (Unassigned space)</option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.displayName} ({room.photoCount} photos)
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-charcoal-500">
                Assigning this photo to a space places it inside that room in current and future presentations.
              </p>
            </div>

            {/* Dual Cover Toggles */}
            <div className="space-y-3">
              <label className="block text-xs font-semibold text-charcoal-900">
                Cover Photo Designations
              </label>

              {/* Project Cover Toggle */}
              <label className="flex items-start gap-3 p-3 rounded-xl border border-sand-200 hover:bg-sand-50/40 cursor-pointer transition-colors">
                <input
                  type="checkbox"
                  disabled={isReadOnly}
                  checked={isCover}
                  onChange={(e) => setIsCover(e.target.checked)}
                  className="mt-0.5 rounded border-sand-300 text-bronze-600 focus:ring-bronze-500"
                />
                <div className="space-y-0.5">
                  <div className="flex items-center gap-1.5 text-xs font-medium text-charcoal-900">
                    <Star className="w-3.5 h-3.5 text-amber-500 fill-current" />
                    <span>Project Primary Cover</span>
                  </div>
                  <p className="text-[11px] text-charcoal-500">
                    Appears as the lead photograph across portfolio index cards and public project previews.
                  </p>
                </div>
              </label>

              {/* Room Cover Toggle */}
              <label
                className={`flex items-start gap-3 p-3 rounded-xl border transition-colors ${
                  selectedRoomId === 'unassigned'
                    ? 'border-sand-200 bg-sand-50/30 opacity-60 cursor-not-allowed'
                    : 'border-sand-200 hover:bg-sand-50/40 cursor-pointer'
                }`}
              >
                <input
                  type="checkbox"
                  disabled={isReadOnly || selectedRoomId === 'unassigned'}
                  checked={isRoomCover}
                  onChange={(e) => setIsRoomCover(e.target.checked)}
                  className="mt-0.5 rounded border-sand-300 text-indigo-600 focus:ring-indigo-500"
                />
                <div className="space-y-0.5">
                  <div className="flex items-center gap-1.5 text-xs font-medium text-charcoal-900">
                    <Home className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Room Cover Photo</span>
                  </div>
                  <p className="text-[11px] text-charcoal-500">
                    {selectedRoomId === 'unassigned'
                      ? 'Assign to a room first to designate this photo as that room’s cover.'
                      : 'Serves as the lead banner image when visitors inspect this specific room.'}
                  </p>
                </div>
              </label>
            </div>

            {/* Motion Opt-out */}
            <div className="bg-sand-50/60 rounded-xl p-4 border border-sand-200 space-y-2">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  disabled={isReadOnly}
                  checked={motionEnabled}
                  onChange={(e) => setMotionEnabled(e.target.checked)}
                  className="mt-0.5 rounded border-sand-300 text-bronze-600 focus:ring-bronze-500"
                />
                <div className="space-y-0.5">
                  <div className="flex items-center gap-1.5 text-xs font-medium text-charcoal-900">
                    <Film className="w-3.5 h-3.5 text-bronze-600" />
                    <span>Enable Presentation Motion</span>
                  </div>
                  <p className="text-[11px] text-charcoal-500">
                    Permits subtle camera movement and zoom effects in automated presentations. Uncheck for delicate blueprints, technical drawings, or tight vertical crops.
                  </p>
                </div>
              </label>
            </div>

            {/* Narrative Captions & Alt Text */}
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Photo Caption
                </label>
                <textarea
                  rows={2}
                  maxLength={1000}
                  disabled={isReadOnly}
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                  placeholder="e.g. Bespoke fluted oak paneling with brass shadowlines..."
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Accessibility Alt Text
                </label>
                <input
                  type="text"
                  maxLength={255}
                  disabled={isReadOnly}
                  value={altText}
                  onChange={(e) => setAltText(e.target.value)}
                  placeholder="e.g. Master bedroom suite featuring floor-to-ceiling windows"
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                    Visibility
                  </label>
                  <select
                    disabled={isReadOnly}
                    value={visibility}
                    onChange={(e) => setVisibility(e.target.value as MediaVisibility)}
                    className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                  >
                    <option value="PORTFOLIO">Portfolio (Public)</option>
                    <option value="PUBLIC">Public & Discoverable</option>
                    <option value="PRIVATE">Private (Studio Only)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                    Studio Watermark
                  </label>
                  <label className="flex items-center gap-2 mt-2 cursor-pointer text-xs text-charcoal-700">
                    <input
                      type="checkbox"
                      disabled={isReadOnly}
                      checked={watermarkEnabled}
                      onChange={(e) => setWatermarkEnabled(e.target.checked)}
                      className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500"
                    />
                    <span>Protect with Watermark</span>
                  </label>
                </div>
              </div>
            </div>

            {/* Technical Metadata Details */}
            <div className="border-t border-sand-200 pt-4 text-[11px] text-charcoal-500 space-y-1">
              <div>Dimensions: {media.width} × {media.height} px</div>
              <div>File Size: {(media.fileSize / 1024 / 1024).toFixed(2)} MB</div>
              <div>Format: {media.contentType}</div>
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t border-sand-200 bg-sand-50/60 flex items-center justify-between">
            {!isReadOnly ? (
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting || saving}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-red-600 hover:text-red-700 hover:bg-red-50 text-xs font-medium transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{deleting ? 'Deleting...' : 'Delete Photo'}</span>
              </button>
            ) : (
              <div />
            )}

            <div className="flex items-center gap-2 ml-auto">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 hover:bg-sand-100 text-xs font-medium transition-colors"
              >
                Cancel
              </button>
              {!isReadOnly && (
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={saving || deleting}
                  className="inline-flex items-center gap-1.5 px-5 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors shadow-sm disabled:opacity-50"
                >
                  {saving ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Check className="w-3.5 h-3.5 text-bronze-300" />
                  )}
                  <span>{saving ? 'Saving...' : 'Save Properties'}</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
