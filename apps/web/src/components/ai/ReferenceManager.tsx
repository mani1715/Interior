'use client';

import React, { useState } from 'react';
import {
  Sparkles,
  Layers,
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  Info,
  Sliders,
  Check,
  X,
  Palette,
  Eye,
  AlertTriangle,
} from 'lucide-react';
import {
  AiJobReferenceInput,
  REFERENCE_PURPOSES,
  ReferenceDetail,
  ReferencePurpose,
} from '@/lib/ai/types';
import { MediaDetailResponse } from '@/lib/media/types';

export interface SelectedReferenceItem extends AiJobReferenceInput {
  id: string; // temporary key for UI
  previewUrl?: string;
  sourceLabel?: string;
}

interface ReferenceManagerProps {
  selectedReferences: SelectedReferenceItem[];
  onChangeReferences: (references: SelectedReferenceItem[]) => void;
  preserveStructure: boolean;
  onTogglePreserveStructure: (val: boolean) => void;
  availableMedia: MediaDetailResponse[];
  libraryReferences: ReferenceDetail[];
  maxReferences?: number;
  disabled?: boolean;
}

export function ReferenceManager({
  selectedReferences,
  onChangeReferences,
  preserveStructure,
  onTogglePreserveStructure,
  availableMedia,
  libraryReferences,
  maxReferences = 4,
  disabled = false,
}: ReferenceManagerProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'library' | 'project'>('library');
  const [selectedPurposeForNew, setSelectedPurposeForNew] = useState<ReferencePurpose>('MATERIAL');

  const handleAddFromLibrary = (ref: ReferenceDetail) => {
    if (selectedReferences.length >= maxReferences) return;
    if (selectedReferences.some((r) => r.mediaId === ref.mediaId)) return;

    const newItem: SelectedReferenceItem = {
      id: `ref-${Date.now()}-${Math.random()}`,
      mediaId: ref.mediaId,
      purpose: ref.purpose,
      label: ref.label || ref.purposeDisplayName,
      instruction: ref.defaultInstruction || '',
      displayOrder: selectedReferences.length,
      previewUrl: ref.previewUrl || undefined,
      sourceLabel: ref.label || ref.purposeDisplayName,
    };

    onChangeReferences([...selectedReferences, newItem]);
    setModalOpen(false);
  };

  const handleAddFromMedia = (media: MediaDetailResponse) => {
    if (selectedReferences.length >= maxReferences) return;
    if (selectedReferences.some((r) => r.mediaId === media.id)) return;

    const preview = media.derivatives?.find(
      (d) => d.variantName === 'MEDIUM' || d.variantName === 'THUMBNAIL'
    )?.publicUrl || `/api/v1/media/${media.id}/preview`;

    const config = REFERENCE_PURPOSES.find((p) => p.code === selectedPurposeForNew);

    const newItem: SelectedReferenceItem = {
      id: `ref-${Date.now()}-${Math.random()}`,
      mediaId: media.id,
      purpose: selectedPurposeForNew,
      label: media.altText || config?.label || 'Reference Image',
      instruction: '',
      displayOrder: selectedReferences.length,
      previewUrl: preview,
      sourceLabel: media.altText || 'Project Media',
    };

    onChangeReferences([...selectedReferences, newItem]);
    setModalOpen(false);
  };

  const handleRemove = (index: number) => {
    const updated = selectedReferences.filter((_, i) => i !== index);
    // re-index displayOrder
    onChangeReferences(
      updated.map((item, idx) => ({
        ...item,
        displayOrder: idx,
      }))
    );
  };

  const handleMoveUp = (index: number) => {
    if (index === 0) return;
    const copy = [...selectedReferences];
    const temp = copy[index - 1];
    copy[index - 1] = copy[index];
    copy[index] = temp;
    onChangeReferences(copy.map((item, idx) => ({ ...item, displayOrder: idx })));
  };

  const handleMoveDown = (index: number) => {
    if (index === selectedReferences.length - 1) return;
    const copy = [...selectedReferences];
    const temp = copy[index + 1];
    copy[index + 1] = copy[index];
    copy[index] = temp;
    onChangeReferences(copy.map((item, idx) => ({ ...item, displayOrder: idx })));
  };

  const handleUpdateItem = (index: number, patch: Partial<SelectedReferenceItem>) => {
    const copy = [...selectedReferences];
    copy[index] = { ...copy[index], ...patch };
    onChangeReferences(copy);
  };

  return (
    <div className="space-y-4">
      {/* Header with Structure Preservation Toggle */}
      <div className="border border-sand-200 bg-sand-50/60 rounded-2xl p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sliders className="w-4 h-4 text-bronze-700" />
            <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-900">
              Structure Preservation
            </span>
          </div>

          <label className="relative inline-flex items-center cursor-pointer select-none">
            <input
              type="checkbox"
              checked={preserveStructure}
              onChange={(e) => onTogglePreserveStructure(e.target.checked)}
              disabled={disabled}
              className="sr-only peer"
              aria-label="Toggle structure preservation"
            />
            <div className="w-11 h-6 bg-sand-300 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-bronze-500 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-sand-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-bronze-600" />
            <span className="ml-2 text-xs font-medium text-charcoal-800">
              {preserveStructure ? 'Preserve Room Structure' : 'Flexible Structure'}
            </span>
          </label>
        </div>

        {/* Truthful Disclaimer */}
        <div className="flex items-start gap-2 text-xs text-charcoal-600 bg-white/80 p-2.5 rounded-xl border border-sand-200">
          <Info className="w-4 h-4 text-bronze-600 flex-shrink-0 mt-0.5" />
          <p className="leading-relaxed">
            {preserveStructure
              ? 'AI will try to preserve the existing structure. Some geometry, colors, materials, and proportions may vary.'
              : 'Flexible mode gives the AI freedom to alter geometry and walls. Proportions and openings will change.'}
          </p>
        </div>
      </div>

      {/* Reference Images Card */}
      <div className="border border-sand-200 bg-white rounded-2xl p-4 sm:p-5 space-y-4 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Layers className="w-4 h-4 text-bronze-700" />
              <h3 className="text-sm font-semibold text-charcoal-900">
                Visual References (Materials & Finishes)
              </h3>
            </div>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Attach wood, stone, hardware, tile, or style guides ({selectedReferences.length}/{maxReferences})
            </p>
          </div>

          {selectedReferences.length < maxReferences && (
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              disabled={disabled}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 min-h-[38px] text-xs font-semibold bg-bronze-600 text-white rounded-xl hover:bg-bronze-700 transition-colors shadow-sm disabled:opacity-50"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add Reference</span>
            </button>
          )}
        </div>

        {/* Reference Color Disclaimer */}
        {selectedReferences.length > 0 && (
          <div className="flex items-center gap-2 px-3 py-2 bg-amber-50/70 border border-amber-200/60 rounded-xl text-xs text-amber-900">
            <Palette className="w-3.5 h-3.5 text-amber-700 flex-shrink-0" />
            <span>
              Reference colors and materials may vary due to lighting, finish, and AI interpretation.
            </span>
          </div>
        )}

        {/* Empty State */}
        {selectedReferences.length === 0 && (
          <div className="text-center py-6 px-4 border border-dashed border-sand-300 rounded-xl bg-sand-50/40 space-y-2">
            <Layers className="w-8 h-8 text-sand-400 mx-auto" />
            <p className="text-xs font-medium text-charcoal-700">No visual references attached</p>
            <p className="text-[11px] text-charcoal-500 max-w-sm mx-auto">
              Optional: Add walnut laminates, brass handles, marble slabs, or wall textures to guide the AI concept.
            </p>
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              disabled={disabled}
              className="mt-2 inline-flex items-center gap-1.5 px-3.5 py-2 min-h-[44px] text-xs font-semibold text-bronze-700 bg-sand-100 hover:bg-sand-200 rounded-xl transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Choose from Reference Library</span>
            </button>
          </div>
        )}

        {/* List of Attached References */}
        {selectedReferences.length > 0 && (
          <div className="space-y-3">
            {selectedReferences.map((ref, idx) => (
              <div
                key={ref.id}
                className="flex flex-col sm:flex-row sm:items-center gap-3 p-3 rounded-xl border border-sand-200 bg-sand-50/40 text-xs"
              >
                {/* Thumbnail & Order Controls */}
                <div className="flex items-center gap-2">
                  <div className="flex flex-col gap-0.5">
                    <button
                      type="button"
                      onClick={() => handleMoveUp(idx)}
                      disabled={idx === 0 || disabled}
                      className="p-1 min-w-[28px] min-h-[28px] flex items-center justify-center text-charcoal-400 hover:text-charcoal-800 disabled:opacity-20"
                      aria-label="Move reference up"
                    >
                      <ChevronUp className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleMoveDown(idx)}
                      disabled={idx === selectedReferences.length - 1 || disabled}
                      className="p-1 min-w-[28px] min-h-[28px] flex items-center justify-center text-charcoal-400 hover:text-charcoal-800 disabled:opacity-20"
                      aria-label="Move reference down"
                    >
                      <ChevronDown className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="w-12 h-12 rounded-lg bg-sand-200 overflow-hidden flex-shrink-0 border border-sand-300 relative">
                    {ref.previewUrl ? (
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img
                        src={ref.previewUrl}
                        alt={ref.label || 'Reference'}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-sand-400 font-bold">
                        {idx + 1}
                      </div>
                    )}
                  </div>
                </div>

                {/* Form Controls for Reference */}
                <div className="flex-1 grid grid-cols-1 sm:grid-cols-12 gap-2">
                  <div className="sm:col-span-4">
                    <label className="block text-[10px] font-semibold uppercase tracking-wider text-charcoal-500 mb-0.5">
                      Purpose
                    </label>
                    <select
                      value={ref.purpose}
                      onChange={(e) =>
                        handleUpdateItem(idx, { purpose: e.target.value as ReferencePurpose })
                      }
                      disabled={disabled}
                      className="w-full bg-white border border-sand-200 rounded-lg px-2.5 py-1.5 text-xs text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-500"
                    >
                      {REFERENCE_PURPOSES.map((p) => (
                        <option key={p.code} value={p.code}>
                          {p.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="sm:col-span-4">
                    <label className="block text-[10px] font-semibold uppercase tracking-wider text-charcoal-500 mb-0.5">
                      Label
                    </label>
                    <input
                      type="text"
                      value={ref.label || ''}
                      onChange={(e) => handleUpdateItem(idx, { label: e.target.value })}
                      placeholder="e.g. Dark Walnut"
                      maxLength={100}
                      disabled={disabled}
                      className="w-full bg-white border border-sand-200 rounded-lg px-2.5 py-1.5 text-xs text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-500 placeholder:text-charcoal-400"
                    />
                  </div>

                  <div className="sm:col-span-4">
                    <label className="block text-[10px] font-semibold uppercase tracking-wider text-charcoal-500 mb-0.5">
                      Instruction
                    </label>
                    <input
                      type="text"
                      value={ref.instruction || ''}
                      onChange={(e) => handleUpdateItem(idx, { instruction: e.target.value })}
                      placeholder="e.g. Apply to shutters"
                      maxLength={300}
                      disabled={disabled}
                      className="w-full bg-white border border-sand-200 rounded-lg px-2.5 py-1.5 text-xs text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-500 placeholder:text-charcoal-400"
                    />
                  </div>
                </div>

                {/* Remove button */}
                <button
                  type="button"
                  onClick={() => handleRemove(idx)}
                  disabled={disabled}
                  className="p-2 min-w-[36px] min-h-[36px] flex items-center justify-center text-charcoal-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
                  aria-label={`Remove reference ${ref.label || idx + 1}`}
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal: Pick Reference from Library or Project Media */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl border border-sand-300 max-w-2xl w-full max-h-[85vh] flex flex-col shadow-2xl">
            {/* Modal Header */}
            <div className="p-4 sm:p-5 border-b border-sand-200 flex items-center justify-between">
              <div>
                <h3 className="font-serif text-lg font-medium text-charcoal-900">
                  Select Visual Reference Image
                </h3>
                <p className="text-xs text-charcoal-500">
                  Choose a material, finish, or style guide to condition the generation
                </p>
              </div>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="p-1.5 text-charcoal-400 hover:text-charcoal-800 rounded-lg hover:bg-sand-100"
                aria-label="Close reference modal"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Tabs */}
            <div className="flex border-b border-sand-200 px-4 pt-2">
              <button
                type="button"
                onClick={() => setActiveTab('library')}
                className={`px-4 py-2 text-xs font-semibold border-b-2 transition-colors ${
                  activeTab === 'library'
                    ? 'border-bronze-600 text-bronze-700'
                    : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
                }`}
              >
                Studio Reference Library ({libraryReferences.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('project')}
                className={`px-4 py-2 text-xs font-semibold border-b-2 transition-colors ${
                  activeTab === 'project'
                    ? 'border-bronze-600 text-bronze-700'
                    : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
                }`}
              >
                Project Images ({availableMedia.length})
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-4 sm:p-5 overflow-y-auto flex-1 space-y-4">
              {activeTab === 'library' && (
                <div>
                  {libraryReferences.length === 0 ? (
                    <div className="text-center py-8 text-charcoal-500 text-xs">
                      No reference items in studio library yet. Select &apos;Project Images&apos; tab to pick from project uploads.
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                      {libraryReferences.map((ref) => {
                        const isSelected = selectedReferences.some((r) => r.mediaId === ref.mediaId);
                        return (
                          <div
                            key={ref.id}
                            onClick={() => !isSelected && handleAddFromLibrary(ref)}
                            className={`p-2.5 rounded-xl border transition-all text-left flex flex-col justify-between ${
                              isSelected
                                ? 'border-bronze-600 bg-bronze-50/50 opacity-60 cursor-not-allowed'
                                : 'border-sand-200 hover:border-bronze-500 hover:shadow-sm cursor-pointer bg-white'
                            }`}
                          >
                            <div className="aspect-square w-full rounded-lg bg-sand-100 overflow-hidden mb-2 border border-sand-200">
                              {ref.previewUrl ? (
                                /* eslint-disable-next-line @next/next/no-img-element */
                                <img
                                  src={ref.previewUrl}
                                  alt={ref.label || ref.purposeDisplayName}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                                  {ref.purpose}
                                </div>
                              )}
                            </div>
                            <div>
                              <span className="text-[10px] font-semibold text-bronze-700 uppercase tracking-wider block">
                                {ref.purposeDisplayName}
                              </span>
                              <p className="text-xs font-medium text-charcoal-900 truncate">
                                {ref.label || 'Reference'}
                              </p>
                              {ref.defaultInstruction && (
                                <p className="text-[10px] text-charcoal-500 line-clamp-1">
                                  {ref.defaultInstruction}
                                </p>
                              )}
                            </div>
                            {isSelected && (
                              <span className="mt-1 text-[10px] font-semibold text-bronze-800 flex items-center gap-1">
                                <Check className="w-3 h-3" /> Added
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}

              {activeTab === 'project' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 mb-1">
                      Assign Purpose to Selected Image:
                    </label>
                    <select
                      value={selectedPurposeForNew}
                      onChange={(e) => setSelectedPurposeForNew(e.target.value as ReferencePurpose)}
                      className="w-full bg-white border border-sand-200 rounded-xl px-3 py-2 text-xs text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-500"
                    >
                      {REFERENCE_PURPOSES.map((p) => (
                        <option key={p.code} value={p.code}>
                          {p.label} - {p.description}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    {availableMedia.map((media) => {
                      const isSelected = selectedReferences.some((r) => r.mediaId === media.id);
                      const thumb = media.derivatives?.find(
                        (d) => d.variantName === 'MEDIUM' || d.variantName === 'THUMBNAIL'
                      )?.publicUrl || `/api/v1/media/${media.id}/preview`;

                      return (
                        <div
                          key={media.id}
                          onClick={() => !isSelected && handleAddFromMedia(media)}
                          className={`p-2.5 rounded-xl border transition-all text-left flex flex-col justify-between ${
                            isSelected
                              ? 'border-bronze-600 bg-bronze-50/50 opacity-60 cursor-not-allowed'
                              : 'border-sand-200 hover:border-bronze-500 hover:shadow-sm cursor-pointer bg-white'
                          }`}
                        >
                          <div className="aspect-square w-full rounded-lg bg-sand-100 overflow-hidden mb-2 border border-sand-200">
                            {/* eslint-disable-next-line @next/next/no-img-element */}
                            <img
                              src={thumb}
                              alt={media.altText || 'Media'}
                              className="w-full h-full object-cover"
                            />
                          </div>
                          <div>
                            <span className="text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider block">
                              {media.mediaType}
                            </span>
                            <p className="text-xs font-medium text-charcoal-900 truncate">
                              {media.altText || 'Project Media'}
                            </p>
                          </div>
                          {isSelected && (
                            <span className="mt-1 text-[10px] font-semibold text-bronze-800 flex items-center gap-1">
                              <Check className="w-3 h-3" /> Added
                            </span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-sand-200 flex justify-end">
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="px-4 py-2 min-h-[44px] text-xs font-medium text-charcoal-700 bg-sand-100 hover:bg-sand-200 rounded-xl transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
