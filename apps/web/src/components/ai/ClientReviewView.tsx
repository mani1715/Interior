'use client';

import React, { useState, useRef } from 'react';
import {
  PublicClientReviewResponse,
  PublicReviewItemDto,
  ClientReviewAnnotationDto,
  SubmitClientDecisionRequest,
  SubmitClientCommentRequest,
  CreateAnnotationPayload,
} from '@/lib/ai/types';
import {
  submitClientDecision,
  submitClientComment,
  submitClientAnnotation,
  setPreferredConcept,
} from '@/lib/ai/api';
import { useRealtimeSubscription } from '@/lib/realtime/RealtimeProvider';
import {
  MessageSquare,
  CheckCircle2,
  Clock,
  Sparkles,
  MapPin,
  Maximize2,
  ShieldAlert,
  SlidersHorizontal,
  ChevronRight,
  ArrowRight,
  ThumbsUp,
  AlertCircle,
  X,
  Send,
  Layers,
  Check,
} from 'lucide-react';

interface ClientReviewViewProps {
  review: PublicClientReviewResponse;
  csrfToken: string;
  onRefresh: () => Promise<void>;
}

export default function ClientReviewView({ review, csrfToken, onRefresh }: ClientReviewViewProps) {
  // Realtime live refresh on annotation, decision, or revision events
  useRealtimeSubscription(
    ['CLIENT_COMMENT_ADDED', 'CLIENT_REVISION_SHARED', 'RESYNC'],
    (event) => {
      onRefresh();
    }
  );

  // Selected concept for detail / annotation view
  const [selectedItem, setSelectedItem] = useState<PublicReviewItemDto>(
    review.items[0] || ({} as PublicReviewItemDto)
  );

  // Compare mode
  const [compareMode, setCompareMode] = useState<'concept' | 'before' | 'slider'>('concept');
  const [sliderPosition, setSliderPosition] = useState(50);

  // Pin annotation placement
  const [pinMode, setPinMode] = useState(false);
  const [pendingPin, setPendingPin] = useState<{ x: number; y: number } | null>(null);
  const [pinAuthorName, setPinAuthorName] = useState('');
  const [pinCommentText, setPinCommentText] = useState('');
  const [pinIsChangeRequest, setPinIsChangeRequest] = useState(false);
  const [savingPin, setSavingPin] = useState(false);
  const [pinError, setPinError] = useState<string | null>(null);
  const [selectedPinId, setSelectedPinId] = useState<string | null>(null);

  // Decision Modal
  const [decisionModal, setDecisionModal] = useState<{
    isOpen: boolean;
    jobId: string;
    type: 'APPROVED' | 'CHANGES_REQUESTED';
    label: string;
  } | null>(null);
  const [clientName, setClientName] = useState('');
  const [feedback, setFeedback] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState(false);
  const [decisionError, setDecisionError] = useState<string | null>(null);

  // General Comments
  const [commentName, setCommentName] = useState('');
  const [commentText, setCommentText] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);
  const [commentSuccess, setCommentSuccess] = useState(false);

  // Preferred Concept Action
  const [settingPreferred, setSettingPreferred] = useState(false);

  // Fullscreen Preview
  const [fullscreenImage, setFullscreenImage] = useState<{ url: string; label: string } | null>(null);

  const imageContainerRef = useRef<HTMLDivElement>(null);
  const isReviewActive = review.status === 'OPEN' && !review.isExpired;

  // Annotations for current selected item
  const currentAnnotations = (review.annotations || []).filter(
    (a) => a.jobId === selectedItem?.jobId && !a.parentAnnotationId
  );

  // Handle clicking on image to drop a pin
  const handleImageClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!pinMode || !isReviewActive) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const rawX = (e.clientX - rect.left) / rect.width;
    const rawY = (e.clientY - rect.top) / rect.height;

    // Clamp normalized coordinates to [0.0, 1.0]
    const clampedX = Math.max(0.01, Math.min(0.99, Number(rawX.toFixed(4))));
    const clampedY = Math.max(0.01, Math.min(0.99, Number(rawY.toFixed(4))));

    setPendingPin({ x: clampedX, y: clampedY });
    setPinError(null);
  };

  const handleSavePin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pendingPin || !selectedItem) return;
    if (!pinAuthorName.trim() || !pinCommentText.trim()) {
      setPinError('Please enter your name and feedback.');
      return;
    }

    try {
      setSavingPin(true);
      setPinError(null);
      const payload: CreateAnnotationPayload = {
        jobId: selectedItem.jobId,
        coordX: pendingPin.x,
        coordY: pendingPin.y,
        authorName: pinAuthorName.trim(),
        commentText: pinCommentText.trim(),
        isChangeRequest: pinIsChangeRequest,
      };
      await submitClientAnnotation(payload, csrfToken);
      setPendingPin(null);
      setPinCommentText('');
      setPinIsChangeRequest(false);
      setPinMode(false);
      await onRefresh();
    } catch (err: any) {
      setPinError(err?.envelope?.message || 'Failed to save annotation pin.');
    } finally {
      setSavingPin(false);
    }
  };

  const handleSetPreferred = async (jobId: string) => {
    if (!isReviewActive) return;
    try {
      setSettingPreferred(true);
      await setPreferredConcept({ jobId }, csrfToken);
      await onRefresh();
    } catch (err: any) {
      alert(err?.envelope?.message || 'Failed to set preferred concept.');
    } finally {
      setSettingPreferred(false);
    }
  };

  const handleOpenDecision = (item: PublicReviewItemDto, type: 'APPROVED' | 'CHANGES_REQUESTED') => {
    if (!isReviewActive) return;
    setDecisionModal({
      isOpen: true,
      jobId: item.jobId,
      type,
      label: item.displayLabel,
    });
    setFeedback('');
    setDecisionError(null);
  };

  const handleSubmitDecision = async () => {
    if (!decisionModal) return;
    if (!clientName.trim()) {
      setDecisionError('Please provide your name.');
      return;
    }
    if (decisionModal.type === 'CHANGES_REQUESTED' && !feedback.trim()) {
      setDecisionError('Please describe the adjustments you would like.');
      return;
    }

    try {
      setSubmittingDecision(true);
      setDecisionError(null);
      const req: SubmitClientDecisionRequest = {
        jobId: decisionModal.jobId,
        decision: decisionModal.type,
        clientName: clientName.trim(),
        feedback: feedback.trim() || undefined,
      };
      await submitClientDecision(req, csrfToken);
      setDecisionModal(null);
      await onRefresh();
    } catch (err: any) {
      setDecisionError(err?.envelope?.message || 'Failed to submit decision. Please try again.');
    } finally {
      setSubmittingDecision(false);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentName.trim() || !commentText.trim()) {
      setCommentError('Please enter both your name and feedback.');
      return;
    }

    try {
      setSubmittingComment(true);
      setCommentError(null);
      const req: SubmitClientCommentRequest = {
        authorName: commentName.trim(),
        commentText: commentText.trim(),
      };
      await submitClientComment(req, csrfToken);
      setCommentText('');
      setCommentSuccess(true);
      setTimeout(() => setCommentSuccess(false), 3000);
      await onRefresh();
    } catch (err: any) {
      setCommentError(err?.envelope?.message || 'Failed to submit comment. Please try again.');
    } finally {
      setSubmittingComment(false);
    }
  };

  const isCurrentItemApproved = selectedItem?.currentDecision === 'APPROVED';
  const isCurrentItemPreferred = review.preferredJobId === selectedItem?.jobId;

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-charcoal-900 font-sans pb-16">
      {/* Top Header: Studio Brand & Collaboration Round */}
      <header className="sticky top-0 z-30 bg-white border-b border-sand-200 px-4 sm:px-6 py-3.5 shadow-sm">
        <div className="max-w-7xl mx-auto flex items-center justify-between flex-wrap gap-3">
          <div>
            <div className="text-[11px] uppercase tracking-wider text-bronze-700 font-bold">
              {review.studioName}
            </div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-serif font-bold text-charcoal-900">
                {review.projectTitle}
              </h1>
              {review.revisionRound && review.revisionRound > 1 && (
                <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-bronze-50 text-bronze-800 border border-bronze-200">
                  Round {review.revisionRound}
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2.5">
            <span className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-sand-100 border border-sand-200 text-charcoal-600">
              <Sparkles className="w-3.5 h-3.5 text-bronze-700" />
              <span>Design Collaboration Workspace</span>
            </span>

            <span
              className={`px-3 py-1 rounded-full text-xs font-semibold ${
                isReviewActive
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                  : 'bg-red-50 text-red-800 border border-red-200'
              }`}
            >
              {isReviewActive ? 'Review Active' : review.isExpired ? 'Review Expired' : review.status}
            </span>
          </div>
        </div>
      </header>

      {/* Mandatory Non-Contractual Disclaimer Banner */}
      <div className="bg-[#F4EFEA] border-b border-sand-200 px-4 py-2 text-center text-xs text-charcoal-600">
        <span className="font-semibold text-charcoal-800">Design Consultation Notice:</span>{' '}
        AI visualizations are non-contractual aesthetic representations generated for client inspiration and spatial guidance.
      </div>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 space-y-6">
        {/* Title, Custom Message, & Concept Selector Bar */}
        <section className="bg-white rounded-2xl border border-sand-200 p-5 shadow-sm space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-serif font-semibold text-charcoal-900">
                {review.title}
              </h2>
              {review.customMessage && (
                <p className="text-sm text-charcoal-600 mt-1 max-w-3xl leading-relaxed">
                  {review.customMessage}
                </p>
              )}
            </div>
            {/* Inactive Banner */}
            {!isReviewActive && (
              <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs sm:text-sm">
                <strong>Presentation Concluded:</strong> This review is currently closed or expired. Feedback and approvals are locked.
              </div>
            )}

            {/* Overall Decision Status */}
            <div className="flex items-center gap-2 self-start md:self-auto">
              {review.currentApprovedJobId ? (
                <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-900 border border-emerald-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-700" />
                  <span>Approved Concept Direction</span>
                </span>
              ) : review.preferredJobId ? (
                <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-900 border border-amber-300">
                  <ThumbsUp className="w-3.5 h-3.5 text-amber-700" />
                  <span>Client Preferred Option Chosen</span>
                </span>
              ) : (
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs text-charcoal-500 bg-sand-100 border border-sand-200">
                  <Clock className="w-3.5 h-3.5 text-charcoal-400" />
                  <span>Awaiting Client Decision</span>
                </span>
              )}
            </div>
          </div>

          {/* Concept Tabs Selector */}
          <div className="flex items-center gap-3 overflow-x-auto pt-2 border-t border-sand-100">
            {review.items.map((item, idx) => {
              const isSelected = selectedItem?.id === item.id;
              const isApproved = item.currentDecision === 'APPROVED';
              const isPreferred = review.preferredJobId === item.jobId;

              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setSelectedItem(item);
                    setPendingPin(null);
                    setSelectedPinId(null);
                  }}
                  className={`flex items-center gap-2.5 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all flex-shrink-0 border ${
                    isSelected
                      ? 'bg-charcoal-900 text-white border-charcoal-900 shadow-sm'
                      : 'bg-[#FAF8F5] text-charcoal-700 border-sand-200 hover:border-sand-300 hover:bg-white'
                  }`}
                >
                  <div className="w-6 h-6 rounded-md overflow-hidden bg-sand-200 flex-shrink-0">
                    <img src={item.previewUrl} alt={item.displayLabel} className="w-full h-full object-cover" />
                  </div>
                  <span>{item.displayLabel || `Option ${idx + 1}`}</span>
                  {isApproved && (
                    <span className="w-2 h-2 rounded-full bg-emerald-400" title="Approved" />
                  )}
                  {isPreferred && (
                    <span className="w-2 h-2 rounded-full bg-amber-400" title="Preferred Concept" />
                  )}
                </button>
              );
            })}
          </div>
        </section>

        {/* Interactive Workspace Board: Image + Pin Overlay & Side Collaboration Drawer */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Main Visual Presentation Stage (8 cols) */}
          <div className="lg:col-span-8 bg-white rounded-2xl border border-sand-200 p-4 sm:p-5 shadow-sm space-y-4">
            {/* Top Toolbar: Pin Mode Toggle, Compare Toggle, Fullscreen */}
            <div className="flex items-center justify-between flex-wrap gap-2 pb-3 border-b border-sand-100">
              <div className="flex items-center gap-2">
                <span className="font-serif font-bold text-sm text-charcoal-900">
                  {selectedItem?.displayLabel}
                </span>

                {isCurrentItemApproved && (
                  <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                    ✓ Approved
                  </span>
                )}
                {isCurrentItemPreferred && (
                  <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                    ★ Preferred
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2">
                {/* Pin Placement Mode Button */}
                {isReviewActive && (
                  <button
                    type="button"
                    onClick={() => {
                      setPinMode(!pinMode);
                      setPendingPin(null);
                    }}
                    className={`min-h-[44px] px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all border ${
                      pinMode
                        ? 'bg-bronze-700 text-white border-bronze-700 shadow-sm animate-pulse'
                        : 'bg-[#FAF8F5] text-charcoal-700 border-sand-300 hover:bg-sand-100'
                    }`}
                  >
                    <MapPin className="w-4 h-4" />
                    <span>{pinMode ? 'Click Image to Place Pin' : 'Add Pin Note'}</span>
                  </button>
                )}

                {/* Compare Mode Switcher */}
                {review.includeOriginal && review.originalPreviewUrl && (
                  <div className="flex items-center bg-[#FAF8F5] p-1 rounded-xl border border-sand-200 text-xs font-semibold">
                    <button
                      type="button"
                      onClick={() => setCompareMode('concept')}
                      className={`px-3 py-1.5 rounded-lg transition-colors ${
                        compareMode === 'concept' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500'
                      }`}
                    >
                      Concept
                    </button>
                    <button
                      type="button"
                      onClick={() => setCompareMode('before')}
                      className={`px-3 py-1.5 rounded-lg transition-colors ${
                        compareMode === 'before' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500'
                      }`}
                    >
                      Show Existing Room
                    </button>
                    <button
                      type="button"
                      onClick={() => setCompareMode('slider')}
                      className={`px-3 py-1.5 rounded-lg transition-colors ${
                        compareMode === 'slider' ? 'bg-white text-charcoal-900 shadow-sm' : 'text-charcoal-500'
                      }`}
                    >
                      Split Slider
                    </button>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() =>
                    setFullscreenImage({
                      url: selectedItem.previewUrl,
                      label: selectedItem.displayLabel,
                    })
                  }
                  className="min-h-[44px] min-w-[44px] rounded-xl border border-sand-300 bg-[#FAF8F5] hover:bg-sand-100 flex items-center justify-center text-charcoal-600"
                  title="Expand to Fullscreen"
                >
                  <Maximize2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Interactive Image Display Area */}
            <div
              ref={imageContainerRef}
              onClick={handleImageClick}
              className={`relative rounded-xl overflow-hidden bg-sand-100 select-none ${
                pinMode ? 'cursor-crosshair' : 'cursor-default'
              }`}
              style={{ aspectRatio: '16/10', minHeight: '320px' }}
            >
              {/* Compare Mode: Before Only */}
              {compareMode === 'before' && review.originalPreviewUrl ? (
                <img
                  src={review.originalPreviewUrl}
                  alt="Original Space"
                  className="w-full h-full object-contain pointer-events-none"
                />
              ) : compareMode === 'slider' && review.originalPreviewUrl ? (
                /* Interactive Split Slider */
                <div className="relative w-full h-full overflow-hidden select-none">
                  {/* After Concept (Bottom Layer) */}
                  <img
                    src={selectedItem.previewUrl}
                    alt={selectedItem.displayLabel}
                    className="absolute inset-0 w-full h-full object-contain pointer-events-none"
                  />
                  {/* Before Original (Clipped Top Layer) */}
                  <div
                    className="absolute inset-0 overflow-hidden pointer-events-none"
                    style={{ clipPath: `inset(0 ${100 - sliderPosition}% 0 0)` }}
                  >
                    <img
                      src={review.originalPreviewUrl}
                      alt="Original Space"
                      className="absolute inset-0 w-full h-full object-contain"
                    />
                  </div>
                  {/* Divider Line */}
                  <div
                    className="absolute top-0 bottom-0 w-1 bg-white shadow-lg pointer-events-none"
                    style={{ left: `${sliderPosition}%` }}
                  />
                  {/* Interactive Range Input */}
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={sliderPosition}
                    onChange={(e) => setSliderPosition(Number(e.target.value))}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-ew-resize z-20"
                    aria-label="Before/After Split Comparison Slider"
                  />
                </div>
              ) : (
                /* Concept View with Pin Badges */
                <>
                  <img
                    src={selectedItem.previewUrl}
                    alt={selectedItem.displayLabel}
                    className="w-full h-full object-contain pointer-events-none"
                  />

                  {/* Render Numbered Annotation Pins */}
                  {currentAnnotations.map((pin) => {
                    const isSelected = selectedPinId === pin.id;
                    const isResolved = !!pin.resolvedAt;

                    return (
                      <div
                        key={pin.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedPinId(isSelected ? null : pin.id);
                        }}
                        style={{
                          left: `${pin.coordX * 100}%`,
                          top: `${pin.coordY * 100}%`,
                          transform: 'translate(-50%, -50%)',
                        }}
                        className={`absolute z-10 cursor-pointer min-w-[32px] min-h-[32px] rounded-full flex items-center justify-center font-bold text-xs shadow-md transition-transform hover:scale-110 ${
                          isResolved
                            ? 'bg-sand-400 text-charcoal-700'
                            : pin.isChangeRequest
                            ? 'bg-amber-600 text-white border-2 border-white ring-2 ring-amber-400/50'
                            : 'bg-bronze-700 text-white border-2 border-white'
                        } ${isSelected ? 'scale-125 ring-4 ring-charcoal-900' : ''}`}
                        title={`${pin.authorName}: ${pin.commentText}`}
                      >
                        {pin.pinNumber}
                      </div>
                    );
                  })}

                  {/* Pending Pin Placement Marker */}
                  {pendingPin && (
                    <div
                      style={{
                        left: `${pendingPin.x * 100}%`,
                        top: `${pendingPin.y * 100}%`,
                        transform: 'translate(-50%, -50%)',
                      }}
                      className="absolute z-20 min-w-[32px] min-h-[32px] rounded-full bg-charcoal-900 text-white font-bold text-xs flex items-center justify-center border-2 border-white animate-bounce shadow-xl"
                    >
                      +
                    </div>
                  )}
                </>
              )}

              {/* Watermark Notice */}
              <div className="absolute bottom-2 left-2 right-2 text-center pointer-events-none">
                <span className="inline-block px-3 py-1 rounded-md bg-charcoal-900/75 text-sand-50 text-[10px] tracking-wider uppercase font-semibold">
                  AI Concept • For Stylistic Guidance Only
                </span>
              </div>
            </div>

            {/* Bottom Action Strip: Decision & Preferred Controls */}
            {isReviewActive && (
              <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-sand-100">
                <div className="flex items-center gap-2">
                  {/* Set Preferred Concept Button */}
                  <button
                    type="button"
                    onClick={() => handleSetPreferred(selectedItem.jobId)}
                    disabled={settingPreferred || isCurrentItemPreferred}
                    className={`min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 border transition-all ${
                      isCurrentItemPreferred
                        ? 'bg-amber-100 text-amber-900 border-amber-300 cursor-default'
                        : 'bg-[#FAF8F5] text-charcoal-700 border-sand-300 hover:bg-sand-100'
                    }`}
                  >
                    <ThumbsUp className="w-4 h-4 text-amber-700" />
                    <span>{isCurrentItemPreferred ? '★ Preferred Concept' : 'Mark as Preferred'}</span>
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleOpenDecision(selectedItem, 'CHANGES_REQUESTED')}
                    className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-medium bg-[#FAF8F5] border border-sand-300 text-charcoal-700 hover:bg-sand-100 transition-colors"
                  >
                    Request Changes
                  </button>

                  <button
                    type="button"
                    onClick={() => handleOpenDecision(selectedItem, 'APPROVED')}
                    className={`min-h-[44px] px-5 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
                      isCurrentItemApproved
                        ? 'bg-emerald-700 text-white shadow-sm'
                        : 'bg-charcoal-900 text-white hover:bg-black shadow-sm'
                    }`}
                  >
                    <Check className="w-4 h-4" />
                    <span>{isCurrentItemApproved ? 'Approved' : 'Approve'}</span>
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Right Column: Pin Annotations & Discussion (4 cols) */}
          <div className="lg:col-span-4 space-y-5">
            {/* New Pin Form when dropped */}
            {pendingPin && (
              <div className="bg-white rounded-2xl border-2 border-bronze-600 p-5 shadow-md space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="font-serif font-semibold text-sm text-charcoal-900 flex items-center gap-1.5">
                    <MapPin className="w-4 h-4 text-bronze-700" />
                    <span>Add Pin Comment</span>
                  </h3>
                  <button
                    type="button"
                    onClick={() => setPendingPin(null)}
                    className="text-charcoal-400 hover:text-charcoal-700"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {pinError && (
                  <div className="p-2.5 bg-red-50 text-red-700 rounded-lg text-xs">
                    {pinError}
                  </div>
                )}

                <form onSubmit={handleSavePin} className="space-y-3">
                  <div>
                    <label className="block text-xs font-medium text-charcoal-700 mb-1">
                      Your Name *
                    </label>
                    <input
                      type="text"
                      value={pinAuthorName}
                      onChange={(e) => setPinAuthorName(e.target.value)}
                      placeholder="e.g. Alice"
                      required
                      className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-charcoal-700 mb-1">
                      Pin Comment *
                    </label>
                    <textarea
                      rows={2}
                      value={pinCommentText}
                      onChange={(e) => setPinCommentText(e.target.value)}
                      placeholder="e.g. Make this cabinet darker walnut veneer..."
                      required
                      className="w-full p-3 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700 resize-none"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="isChangeRequest"
                      checked={pinIsChangeRequest}
                      onChange={(e) => setPinIsChangeRequest(e.target.checked)}
                      className="w-4 h-4 text-bronze-700 rounded"
                    />
                    <label htmlFor="isChangeRequest" className="text-xs text-charcoal-700 font-medium cursor-pointer">
                      Mark as Revision Request
                    </label>
                  </div>

                  <div className="flex justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setPendingPin(null)}
                      className="px-3 py-1.5 rounded-lg border border-sand-300 text-xs text-charcoal-600 hover:bg-sand-50"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={savingPin}
                      className="px-4 py-1.5 rounded-lg bg-bronze-700 text-white text-xs font-semibold hover:bg-bronze-800 disabled:opacity-50"
                    >
                      {savingPin ? 'Saving...' : 'Drop Pin'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Pin Annotations List for Selected Concept */}
            <div className="bg-white rounded-2xl border border-sand-200 p-5 shadow-sm space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-serif font-semibold text-sm text-charcoal-900">
                  Concept Annotations ({currentAnnotations.length})
                </h3>
                {pinMode && (
                  <span className="text-[11px] text-bronze-700 font-medium">
                    Tap image to pin
                  </span>
                )}
              </div>

              {currentAnnotations.length === 0 ? (
                <p className="text-xs text-charcoal-500 italic bg-sand-50 p-4 rounded-xl text-center">
                  No pinpoint annotations placed on this concept yet. Click "Add Pin Note" and tap anywhere on the image.
                </p>
              ) : (
                <div className="space-y-2.5 max-h-96 overflow-y-auto pr-1">
                  {currentAnnotations.map((pin) => {
                    const isSelected = selectedPinId === pin.id;
                    const replies = (review.annotations || []).filter(
                      (a) => a.parentAnnotationId === pin.id
                    );

                    return (
                      <div
                        key={pin.id}
                        onClick={() => setSelectedPinId(isSelected ? null : pin.id)}
                        className={`p-3.5 rounded-xl border text-xs space-y-2 cursor-pointer transition-all ${
                          isSelected
                            ? 'bg-sand-50 border-charcoal-900 shadow-sm'
                            : 'bg-white border-sand-200 hover:border-sand-300'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-bronze-700 text-white font-bold text-[10px] flex items-center justify-center">
                              {pin.pinNumber}
                            </span>
                            <span className="font-semibold text-charcoal-900">
                              {pin.authorName}
                            </span>
                          </div>
                          {pin.isChangeRequest && (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                              Revision Request
                            </span>
                          )}
                        </div>

                        <p className="text-charcoal-700 leading-relaxed pl-7">
                          {pin.commentText}
                        </p>

                        {/* Designer Replies */}
                        {replies.length > 0 && (
                          <div className="pl-7 space-y-1.5 pt-1 border-t border-sand-200">
                            {replies.map((r) => (
                              <div key={r.id} className="bg-sand-100 p-2 rounded-lg text-[11px]">
                                <span className="font-semibold text-bronze-800">{r.authorName} (Designer): </span>
                                <span className="text-charcoal-800">{r.commentText}</span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* General Project Notes Section */}
            <div className="bg-white rounded-2xl border border-sand-200 p-5 shadow-sm space-y-4">
              <h3 className="font-serif font-semibold text-sm text-charcoal-900">
                Design Feedback & Discussion
              </h3>

              {review.comments.length === 0 ? (
                <p className="text-xs text-charcoal-500 italic bg-sand-50 p-3 rounded-xl">
                  No overall notes yet.
                </p>
              ) : (
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {review.comments.map((c) => (
                    <div key={c.id} className="p-3 bg-[#FAF8F5] rounded-xl border border-sand-200 text-xs space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-charcoal-800">{c.authorName}</span>
                        <span className="text-[10px] text-charcoal-400">
                          {new Date(c.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      <p className="text-charcoal-700">{c.commentText}</p>
                    </div>
                  ))}
                </div>
              )}

              {/* Add Comment Form */}
              {isReviewActive && (
                <form onSubmit={handleAddComment} className="space-y-2.5 pt-2 border-t border-sand-100">
                  {commentError && (
                    <div className="p-2 bg-red-50 text-red-700 rounded-lg text-xs">
                      {commentError}
                    </div>
                  )}
                  {commentSuccess && (
                    <div className="p-2 bg-emerald-50 text-emerald-800 rounded-lg text-xs">
                      ✓ Note shared with studio!
                    </div>
                  )}
                  <input
                    type="text"
                    value={commentName}
                    onChange={(e) => setCommentName(e.target.value)}
                    placeholder="e.g., Jane Doe"
                    required
                    className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700"
                  />
                  <textarea
                    rows={2}
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="Share your thoughts, specific finish questions, or preferences..."
                    required
                    className="w-full p-3 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700 resize-none"
                  />
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={submittingComment}
                      className="min-h-[44px] px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-semibold hover:bg-black disabled:opacity-50 flex items-center gap-1.5"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>{submittingComment ? 'Submitting...' : 'Send Feedback'}</span>
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </section>
      </main>

      {/* Decision Modal */}
      {decisionModal && (
        <div className="fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-xl border border-sand-200">
            <h3 className="font-serif text-lg text-charcoal-900 font-semibold">
              {decisionModal.type === 'APPROVED' ? `Approve ${decisionModal.label}` : `Request Changes on ${decisionModal.label}`}
            </h3>

            <p className="text-xs text-charcoal-600 leading-relaxed">
              {decisionModal.type === 'APPROVED'
                ? 'Approving indicates your aesthetic preference for this direction. The studio will use this style to draft quotations, technical specifications, and materials. This visualization remains private and non-contractual.'
                : 'Provide notes on materials, lighting, or room details you would like revised in the next round.'}
            </p>

            {decisionError && (
              <div className="p-2.5 bg-red-50 text-red-700 rounded-lg text-xs">
                {decisionError}
              </div>
            )}

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-charcoal-700 mb-1">
                  Your Name *
                </label>
                <input
                  type="text"
                  value={clientName}
                  onChange={(e) => setClientName(e.target.value)}
                  placeholder="e.g., Sarah Johnson"
                  className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-charcoal-700 mb-1">
                  {decisionModal.type === 'APPROVED' ? 'Approval Note (Optional)' : 'Requested Changes *'}
                </label>
                <textarea
                  rows={3}
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  placeholder={
                    decisionModal.type === 'APPROVED'
                      ? 'e.g., We love the fluted paneling and timber warmth!'
                      : 'e.g., Can we see a lighter wood tone and different cabinet handles?'
                  }
                  className="w-full p-3 rounded-xl border border-sand-300 text-xs outline-none focus:border-bronze-700 resize-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-sand-100">
              <button
                type="button"
                onClick={() => setDecisionModal(null)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-sand-300 text-xs text-charcoal-600 hover:bg-sand-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmitDecision}
                disabled={submittingDecision}
                className={`min-h-[44px] px-5 py-2 rounded-xl text-xs font-semibold text-white ${
                  decisionModal.type === 'APPROVED' ? 'bg-emerald-700 hover:bg-emerald-800' : 'bg-bronze-700 hover:bg-bronze-800'
                } disabled:opacity-50`}
              >
                {submittingDecision ? 'Submitting...' : decisionModal.type === 'APPROVED' ? 'Confirm Approval' : 'Submit Request'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fullscreen Preview Modal */}
      {fullscreenImage && (
        <div className="fixed inset-0 z-50 bg-black/90 flex flex-col">
          <div className="flex items-center justify-between p-4 bg-black/50 text-white">
            <span className="font-semibold text-sm">{fullscreenImage.label}</span>
            <button
              type="button"
              onClick={() => setFullscreenImage(null)}
              className="min-h-[44px] min-w-[44px] rounded-lg border border-white/30 flex items-center justify-center text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          <div className="flex-1 flex items-center justify-center p-4">
            <img
              src={fullscreenImage.url}
              alt={fullscreenImage.label}
              className="max-w-full max-h-full object-contain"
            />
          </div>
        </div>
      )}
    </div>
  );
}
