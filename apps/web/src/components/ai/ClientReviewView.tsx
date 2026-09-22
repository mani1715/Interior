'use client';

import React, { useState } from 'react';
import {
  PublicClientReviewResponse,
  PublicReviewItemDto,
  SubmitClientDecisionRequest,
  SubmitClientCommentRequest,
} from '@/lib/ai/types';
import { submitClientDecision, submitClientComment } from '@/lib/ai/api';

interface ClientReviewViewProps {
  review: PublicClientReviewResponse;
  csrfToken: string;
  onRefresh: () => Promise<void>;
}

export default function ClientReviewView({ review, csrfToken, onRefresh }: ClientReviewViewProps) {
  const [selectedConcept, setSelectedConcept] = useState<PublicReviewItemDto | null>(null);
  const [fullscreenImage, setFullscreenImage] = useState<{ url: string; label: string } | null>(null);
  const [compareMode, setCompareMode] = useState<'after' | 'before'>('after');
  
  // Decision modal state
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

  // Comment state
  const [commentName, setCommentName] = useState('');
  const [commentText, setCommentText] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);
  const [commentSuccess, setCommentSuccess] = useState(false);

  const isReviewActive = review.status === 'OPEN' && !review.isExpired;

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

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#FAF8F5',
        color: '#1F1F1F',
        fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      }}
    >
      {/* Top Banner: Studio Branding & AI Disclosure */}
      <header
        style={{
          backgroundColor: '#FFFFFF',
          borderBottom: '1px solid #E7E1D8',
          padding: '16px 24px',
          position: 'sticky',
          top: 0,
          zIndex: 30,
        }}
      >
        <div
          style={{
            maxWidth: '1200px',
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div>
            <div style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.08em', color: '#B88A5A', fontWeight: 600 }}>
              {review.studioName}
            </div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: '#1F1F1F' }}>
              {review.projectTitle}
            </h1>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 12px',
                backgroundColor: '#FAF8F5',
                border: '1px solid #E7E1D8',
                borderRadius: '20px',
                fontSize: '0.75rem',
                color: '#555',
                fontWeight: 500,
              }}
            >
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#B88A5A' }} />
              AI Concept Presentation
            </span>

            <span
              style={{
                padding: '6px 12px',
                borderRadius: '20px',
                fontSize: '0.75rem',
                fontWeight: 600,
                backgroundColor: isReviewActive ? '#EBF7EE' : '#FBEBEB',
                color: isReviewActive ? '#2E7D32' : '#C62828',
              }}
            >
              {isReviewActive ? 'Review Active' : review.isExpired ? 'Expired' : review.status}
            </span>
          </div>
        </div>
      </header>

      {/* Persistent Disclaimer Banner */}
      <div
        style={{
          backgroundColor: '#F7F3EE',
          borderBottom: '1px solid #E7E1D8',
          padding: '10px 24px',
          textAlign: 'center',
          fontSize: '0.8rem',
          color: '#666',
        }}
      >
        <span>
          <strong>Design Consultation Notice:</strong> AI visualizations are non-contractual aesthetic representations generated for client inspiration and spatial guidance.
        </span>
      </div>

      <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px 64px' }}>
        {/* Review Title & Message */}
        <section style={{ marginBottom: '32px' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 600, marginBottom: '8px', color: '#1F1F1F' }}>
            {review.title}
          </h2>
          {review.customMessage && (
            <p
              style={{
                fontSize: '1rem',
                color: '#444',
                lineHeight: 1.6,
                backgroundColor: '#FFFFFF',
                padding: '18px 20px',
                borderRadius: '12px',
                border: '1px solid #E7E1D8',
                margin: '12px 0 0',
              }}
            >
              {review.customMessage}
            </p>
          )}
        </section>

        {/* Inactive Banner */}
        {!isReviewActive && (
          <div
            style={{
              padding: '16px 20px',
              backgroundColor: '#FFF8E1',
              border: '1px solid #FFE082',
              borderRadius: '12px',
              color: '#8D6E63',
              marginBottom: '32px',
              fontSize: '0.9rem',
              lineHeight: 1.5,
            }}
          >
            <strong>Presentation Concluded:</strong> This review is currently closed or expired. Feedback and approvals are locked. If you wish to make changes, please contact your designer.
          </div>
        )}

        {/* Before / After Comparison Controls if Original Included */}
        {review.includeOriginal && review.originalPreviewUrl && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '20px',
              backgroundColor: '#FFFFFF',
              padding: '12px 18px',
              borderRadius: '10px',
              border: '1px solid #E7E1D8',
            }}
          >
            <span style={{ fontSize: '0.875rem', fontWeight: 500, color: '#444' }}>
              Compare with Original Space:
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={() => setCompareMode('before')}
                style={{
                  minHeight: '44px',
                  padding: '8px 16px',
                  borderRadius: '8px',
                  border: '1px solid #E7E1D8',
                  backgroundColor: compareMode === 'before' ? '#1F1F1F' : '#FFFFFF',
                  color: compareMode === 'before' ? '#FFFFFF' : '#1F1F1F',
                  fontWeight: 500,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                }}
              >
                Show Existing Room
              </button>
              <button
                type="button"
                onClick={() => setCompareMode('after')}
                style={{
                  minHeight: '44px',
                  padding: '8px 16px',
                  borderRadius: '8px',
                  border: '1px solid #E7E1D8',
                  backgroundColor: compareMode === 'after' ? '#B88A5A' : '#FFFFFF',
                  color: compareMode === 'after' ? '#FFFFFF' : '#1F1F1F',
                  fontWeight: 500,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                }}
              >
                Show Concepts
              </button>
            </div>
          </div>
        )}

        {/* Existing Room View (when compareMode === 'before') */}
        {review.includeOriginal && review.originalPreviewUrl && compareMode === 'before' && (
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '16px',
              border: '1px solid #E7E1D8',
              overflow: 'hidden',
              marginBottom: '32px',
            }}
          >
            <div style={{ position: 'relative', width: '100%', maxHeight: '600px', overflow: 'hidden', backgroundColor: '#F0EDE8' }}>
              <img
                src={review.originalPreviewUrl}
                alt="Existing Room"
                style={{ width: '100%', maxHeight: '600px', objectFit: 'contain', display: 'block' }}
              />
              <div
                style={{
                  position: 'absolute',
                  top: '16px',
                  left: '16px',
                  backgroundColor: 'rgba(31, 31, 31, 0.8)',
                  color: '#FFFFFF',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                }}
              >
                Existing Space (Before)
              </div>
            </div>
          </div>
        )}

        {/* Concept Cards Grid */}
        {(compareMode === 'after' || !review.includeOriginal) && (
          <section style={{ marginBottom: '48px' }}>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
                gap: '24px',
              }}
            >
              {review.items.map((item, idx) => {
                const isApproved = item.currentDecision === 'APPROVED';
                const isChangesRequested = item.currentDecision === 'CHANGES_REQUESTED';

                return (
                  <div
                    key={item.id}
                    style={{
                      backgroundColor: '#FFFFFF',
                      borderRadius: '16px',
                      border: isApproved ? '2px solid #2E7D32' : '1px solid #E7E1D8',
                      overflow: 'hidden',
                      boxShadow: '0 2px 12px rgba(0, 0, 0, 0.04)',
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    {/* Image Preview */}
                    <div
                      style={{
                        position: 'relative',
                        aspectRatio: '4/3',
                        backgroundColor: '#F0EDE8',
                        cursor: 'pointer',
                        overflow: 'hidden',
                      }}
                      onClick={() => setFullscreenImage({ url: item.previewUrl, label: item.displayLabel })}
                    >
                      <img
                        src={item.previewUrl}
                        alt={item.displayLabel}
                        style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                      />

                      {/* Top Label Tag */}
                      <div
                        style={{
                          position: 'absolute',
                          top: '12px',
                          left: '12px',
                          backgroundColor: 'rgba(255, 255, 255, 0.92)',
                          backdropFilter: 'blur(4px)',
                          padding: '4px 10px',
                          borderRadius: '6px',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          color: '#1F1F1F',
                          border: '1px solid #E7E1D8',
                        }}
                      >
                        {item.displayLabel || `Option ${idx + 1}`}
                      </div>

                      {/* Zoom Indicator */}
                      <div
                        style={{
                          position: 'absolute',
                          top: '12px',
                          right: '12px',
                          backgroundColor: 'rgba(31, 31, 31, 0.6)',
                          color: '#FFFFFF',
                          padding: '4px 8px',
                          borderRadius: '6px',
                          fontSize: '0.7rem',
                        }}
                      >
                        🔍 View Full
                      </div>

                      {/* Watermarked AI Disclaimer Overlay */}
                      <div
                        style={{
                          position: 'absolute',
                          bottom: '0',
                          left: '0',
                          right: '0',
                          backgroundColor: 'rgba(31, 31, 31, 0.7)',
                          color: '#FAF8F5',
                          padding: '6px 12px',
                          fontSize: '0.7rem',
                          letterSpacing: '0.02em',
                          textAlign: 'center',
                        }}
                      >
                        AI CONCEPT • FOR STYLISTIC INSPIRATION ONLY
                      </div>
                    </div>

                    {/* Card Content & Decision Actions */}
                    <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', flex: 1, justifyContent: 'space-between' }}>
                      <div style={{ marginBottom: '16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0, color: '#1F1F1F' }}>
                            {item.displayLabel || `Concept Option ${idx + 1}`}
                          </h3>

                          {/* Decision Badges */}
                          {isApproved && (
                            <span
                              style={{
                                padding: '4px 10px',
                                backgroundColor: '#EBF7EE',
                                color: '#2E7D32',
                                borderRadius: '20px',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                              }}
                            >
                              ✓ Approved by Client
                            </span>
                          )}
                          {isChangesRequested && (
                            <span
                              style={{
                                padding: '4px 10px',
                                backgroundColor: '#FFF3E0',
                                color: '#E65100',
                                borderRadius: '20px',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                              }}
                            >
                              ⟳ Changes Requested
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Interactive Buttons (Min height 44px for touch targets) */}
                      {isReviewActive && (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                          <button
                            type="button"
                            onClick={() => handleOpenDecision(item, 'APPROVED')}
                            style={{
                              minHeight: '44px',
                              padding: '10px 14px',
                              backgroundColor: isApproved ? '#2E7D32' : '#B88A5A',
                              color: '#FFFFFF',
                              border: 'none',
                              borderRadius: '8px',
                              fontSize: '0.85rem',
                              fontWeight: 600,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              gap: '6px',
                              transition: 'background-color 0.2s',
                            }}
                          >
                            ✓ {isApproved ? 'Approved' : 'Approve'}
                          </button>

                          <button
                            type="button"
                            onClick={() => handleOpenDecision(item, 'CHANGES_REQUESTED')}
                            style={{
                              minHeight: '44px',
                              padding: '10px 14px',
                              backgroundColor: '#FFFFFF',
                              color: '#1F1F1F',
                              border: '1px solid #E7E1D8',
                              borderRadius: '8px',
                              fontSize: '0.85rem',
                              fontWeight: 500,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              gap: '6px',
                            }}
                          >
                            Request Changes
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Discussion / Comments Section */}
        <section
          style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '16px',
            border: '1px solid #E7E1D8',
            padding: '28px 24px',
          }}
        >
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '20px', color: '#1F1F1F' }}>
            Design Notes & Client Feedback
          </h3>

          {/* Existing Comments */}
          {review.comments.length === 0 ? (
            <p style={{ fontSize: '0.9rem', color: '#777', fontStyle: 'italic', marginBottom: '24px' }}>
              No comments yet. Share your thoughts or questions with the design team below.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '32px' }}>
              {review.comments.map((c) => {
                const isStudio = c.authorType === 'STUDIO';
                return (
                  <div
                    key={c.id}
                    style={{
                      padding: '14px 18px',
                      borderRadius: '10px',
                      backgroundColor: isStudio ? '#FAF8F5' : '#FFFFFF',
                      border: '1px solid #E7E1D8',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontWeight: 600, fontSize: '0.875rem', color: '#1F1F1F' }}>
                          {c.authorName}
                        </span>
                        <span
                          style={{
                            fontSize: '0.7rem',
                            padding: '2px 8px',
                            borderRadius: '12px',
                            backgroundColor: isStudio ? '#B88A5A' : '#E7E1D8',
                            color: isStudio ? '#FFFFFF' : '#1F1F1F',
                            fontWeight: 600,
                          }}
                        >
                          {isStudio ? 'Designer' : 'Client'}
                        </span>
                      </div>
                      <span style={{ fontSize: '0.75rem', color: '#888' }}>
                        {new Date(c.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p style={{ fontSize: '0.9rem', color: '#333', lineHeight: 1.5, margin: 0 }}>
                      {c.commentText}
                    </p>
                  </div>
                );
              })}
            </div>
          )}

          {/* Add Comment Form */}
          {isReviewActive && (
            <form onSubmit={handleAddComment} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 600, margin: 0, color: '#1F1F1F' }}>
                Leave a Note for your Design Studio
              </h4>

              {commentError && (
                <div style={{ padding: '10px 14px', backgroundColor: '#FBEBEB', color: '#D32F2F', borderRadius: '8px', fontSize: '0.85rem' }}>
                  {commentError}
                </div>
              )}
              {commentSuccess && (
                <div style={{ padding: '10px 14px', backgroundColor: '#EBF7EE', color: '#2E7D32', borderRadius: '8px', fontSize: '0.85rem' }}>
                  ✓ Feedback submitted to the team!
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px', color: '#444' }}>
                    Your Name *
                  </label>
                  <input
                    type="text"
                    value={commentName}
                    onChange={(e) => setCommentName(e.target.value)}
                    placeholder="e.g., Jane Doe"
                    required
                    style={{
                      width: '100%',
                      minHeight: '44px',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      border: '1px solid #E7E1D8',
                      fontSize: '0.9rem',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px', color: '#444' }}>
                  Feedback Note *
                </label>
                <textarea
                  rows={3}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Share your thoughts, specific finish questions, or preferences..."
                  required
                  style={{
                    width: '100%',
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: '1px solid #E7E1D8',
                    fontSize: '0.9rem',
                    outline: 'none',
                    resize: 'vertical',
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <button
                  type="submit"
                  disabled={submittingComment}
                  style={{
                    minHeight: '44px',
                    padding: '10px 24px',
                    backgroundColor: '#B88A5A',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '0.9rem',
                    fontWeight: 600,
                    cursor: submittingComment ? 'not-allowed' : 'pointer',
                    opacity: submittingComment ? 0.7 : 1,
                  }}
                >
                  {submittingComment ? 'Submitting...' : 'Send Feedback'}
                </button>
              </div>
            </form>
          )}
        </section>
      </main>

      {/* Decision Modal */}
      {decisionModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '24px',
            zIndex: 50,
          }}
        >
          <div
            style={{
              backgroundColor: '#FFFFFF',
              maxWidth: '520px',
              width: '100%',
              borderRadius: '16px',
              padding: '28px 24px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.16)',
            }}
          >
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '8px', color: '#1F1F1F' }}>
              {decisionModal.type === 'APPROVED' ? `Approve ${decisionModal.label}` : `Request Changes on ${decisionModal.label}`}
            </h3>

            {decisionModal.type === 'APPROVED' ? (
              <p style={{ fontSize: '0.875rem', color: '#555', lineHeight: 1.6, marginBottom: '20px' }}>
                <strong>Important Note:</strong> By approving this direction, you communicate your aesthetic preference to the studio. Your design team will proceed with technical specifications, material selections, and itemized quotations based on this style. This visualization remains private and non-contractual.
              </p>
            ) : (
              <p style={{ fontSize: '0.875rem', color: '#555', lineHeight: 1.6, marginBottom: '20px' }}>
                Let your design team know what you'd like altered (materials, colors, lighting, or room details).
              </p>
            )}

            {decisionError && (
              <div style={{ padding: '10px 14px', backgroundColor: '#FBEBEB', color: '#D32F2F', borderRadius: '8px', fontSize: '0.85rem', marginBottom: '16px' }}>
                {decisionError}
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px', color: '#333' }}>
                Your Name *
              </label>
              <input
                type="text"
                value={clientName}
                onChange={(e) => setClientName(e.target.value)}
                placeholder="e.g., Sarah Johnson"
                style={{
                  width: '100%',
                  minHeight: '44px',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #E7E1D8',
                  fontSize: '0.9rem',
                  outline: 'none',
                }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px', color: '#333' }}>
                {decisionModal.type === 'APPROVED' ? 'Approval Note (Optional)' : 'Requested Changes & Feedback *'}
              </label>
              <textarea
                rows={4}
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                placeholder={decisionModal.type === 'APPROVED' ? 'e.g., We love the fluted paneling and timber warmth!' : 'e.g., Can we see a lighter wood tone and different cabinet handles?'}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '8px',
                  border: '1px solid #E7E1D8',
                  fontSize: '0.9rem',
                  outline: 'none',
                  resize: 'vertical',
                }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setDecisionModal(null)}
                disabled={submittingDecision}
                style={{
                  minHeight: '44px',
                  padding: '10px 20px',
                  backgroundColor: '#FFFFFF',
                  color: '#555',
                  border: '1px solid #E7E1D8',
                  borderRadius: '8px',
                  fontSize: '0.9rem',
                  cursor: 'pointer',
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmitDecision}
                disabled={submittingDecision}
                style={{
                  minHeight: '44px',
                  padding: '10px 24px',
                  backgroundColor: decisionModal.type === 'APPROVED' ? '#2E7D32' : '#B88A5A',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                  cursor: submittingDecision ? 'not-allowed' : 'pointer',
                  opacity: submittingDecision ? 0.7 : 1,
                }}
              >
                {submittingDecision ? 'Submitting...' : decisionModal.type === 'APPROVED' ? 'Confirm Approval' : 'Submit Request'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fullscreen Image Modal */}
      {fullscreenImage && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.92)',
            display: 'flex',
            flexDirection: 'column',
            zIndex: 60,
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '16px 24px',
              backgroundColor: 'rgba(0, 0, 0, 0.6)',
              color: '#FFFFFF',
            }}
          >
            <div>
              <span style={{ fontSize: '1rem', fontWeight: 600 }}>{fullscreenImage.label}</span>
              <span style={{ fontSize: '0.75rem', color: '#CCC', marginLeft: '12px' }}>
                AI Concept Visualization
              </span>
            </div>
            <button
              type="button"
              onClick={() => setFullscreenImage(null)}
              style={{
                minHeight: '44px',
                minWidth: '44px',
                backgroundColor: 'transparent',
                border: '1px solid rgba(255, 255, 255, 0.3)',
                color: '#FFFFFF',
                borderRadius: '8px',
                cursor: 'pointer',
                fontSize: '1.25rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              ✕
            </button>
          </div>

          <div
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '24px',
              overflow: 'auto',
            }}
          >
            <img
              src={fullscreenImage.url}
              alt={fullscreenImage.label}
              style={{
                maxWidth: '100%',
                maxHeight: '100%',
                objectFit: 'contain',
                borderRadius: '8px',
                boxShadow: '0 8px 40px rgba(0, 0, 0, 0.5)',
              }}
            />
          </div>

          <div
            style={{
              padding: '12px 24px',
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              color: '#DDD',
              textAlign: 'center',
              fontSize: '0.75rem',
              letterSpacing: '0.04em',
            }}
          >
            AI CONCEPT VISUALIZATION • NON-CONTRACTUAL ARTISTIC REPRESENTATION
          </div>
        </div>
      )}
    </div>
  );
}
