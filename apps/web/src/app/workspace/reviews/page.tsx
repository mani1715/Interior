'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  MessageSquare,
  ArrowLeft,
  Star,
  Plus,
  Send,
  Copy,
  Check,
  ShieldCheck,
  AlertCircle,
  RefreshCw,
  CornerDownRight,
  Trash2,
  Edit2,
  X,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import {
  getPublicReviews,
  listStudioInvitations,
  createReviewInvitation,
  respondToReview,
  deleteReviewResponse,
} from '@/lib/reviews/api';
import {
  PublicStudioReviewDto,
  ReviewInvitationDto,
} from '@/lib/reviews/types';
import { fetchLeads } from '@/lib/leads/api';
import { LeadSummary } from '@/lib/leads/types';
import { ReviewStars } from '@/components/reviews/ReviewStars';

export default function WorkspaceReviewsPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'reviews' | 'invitations'>('reviews');
  const [reviews, setReviews] = useState<PublicStudioReviewDto[]>([]);
  const [invitations, setInvitations] = useState<ReviewInvitationDto[]>([]);
  const [wonLeads, setWonLeads] = useState<LeadSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Invite Modal
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [selectedLeadId, setSelectedLeadId] = useState('');
  const [clientName, setClientName] = useState('');
  const [clientEmail, setClientEmail] = useState('');
  const [inviting, setInviting] = useState(false);
  const [inviteSuccessLink, setInviteSuccessLink] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  // Reply state
  const [replyingReviewId, setReplyingReviewId] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [savingReply, setSavingReply] = useState(false);

  const activeStudio = user?.studios?.[0];
  const studioSlug = activeStudio?.studioSlug;

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [invitesData, reviewsData, leadsData] = await Promise.all([
        listStudioInvitations().catch(() => [] as ReviewInvitationDto[]),
        studioSlug
          ? getPublicReviews(studioSlug)
              .then((r) => r.reviews)
              .catch(() => [] as PublicStudioReviewDto[])
          : Promise.resolve([] as PublicStudioReviewDto[]),
        fetchLeads({ status: 'WON' })
          .then((res) => res.items || [])
          .catch(() => [] as LeadSummary[]),
      ]);
      setInvitations(invitesData);
      setReviews(reviewsData);
      setWonLeads(leadsData);
    } catch (err: any) {
      setError(err.message || 'Failed to load reviews and invitations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studioSlug]);

  const handleSelectLead = (leadId: string) => {
    setSelectedLeadId(leadId);
    const found = wonLeads.find((l) => l.id === leadId);
    if (found) {
      setClientName(found.name);
      setClientEmail(found.emailNormalized || '');
    }
  };

  const handleCreateInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLeadId || !clientName.trim()) return;
    try {
      setInviting(true);
      setError(null);
      const res = await createReviewInvitation({
        leadId: selectedLeadId,
        clientName: clientName.trim(),
        clientEmail: clientEmail.trim() || undefined,
      });
      const origin = typeof window !== 'undefined' ? window.location.origin : '';
      const fullUrl = res.invitationUrl.startsWith('http')
        ? res.invitationUrl
        : `${origin}${res.invitationUrl}`;
      setInviteSuccessLink(fullUrl);

      // Reload invitations
      const updatedInvites = await listStudioInvitations().catch(() => []);
      setInvitations(updatedInvites);
    } catch (err: any) {
      setError(err.message || 'Failed to create review invitation.');
    } finally {
      setInviting(false);
    }
  };

  const handleCopyLink = () => {
    if (!inviteSuccessLink) return;
    navigator.clipboard.writeText(inviteSuccessLink);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2500);
  };

  const handleSaveReply = async (reviewId: string) => {
    if (!replyText.trim()) return;
    try {
      setSavingReply(true);
      const updatedReview = await respondToReview(reviewId, {
        responseText: replyText.trim(),
      });
      setReviews((prev) =>
        prev.map((r) => (r.id === reviewId ? updatedReview : r))
      );
      setReplyingReviewId(null);
      setReplyText('');
    } catch (err: any) {
      alert(err.message || 'Failed to submit response.');
    } finally {
      setSavingReply(false);
    }
  };

  const handleDeleteReply = async (reviewId: string) => {
    if (!confirm('Are you sure you want to remove your official response?')) return;
    try {
      await deleteReviewResponse(reviewId);
      setReviews((prev) =>
        prev.map((r) =>
          r.id === reviewId ? { ...r, studioResponse: null, studioRespondedAt: null } : r
        )
      );
    } catch (err: any) {
      alert(err.message || 'Failed to delete response.');
    }
  };

  const averageRating =
    reviews.length > 0
      ? (reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length).toFixed(1)
      : null;

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-6xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Workspace Home</span>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Reputation & Client Trust
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Client Reviews & Invitations
              </h1>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            onClick={loadData}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-sand-200 text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => {
              setShowInviteModal(true);
              setInviteSuccessLink(null);
              setSelectedLeadId('');
              setClientName('');
              setClientEmail('');
            }}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors shadow-xs"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Invite Client</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center gap-3">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-amber-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="p-5 bg-white border border-sand-200 rounded-2xl shadow-xs">
          <span className="text-xs text-charcoal-500 font-medium block">Average Rating</span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="font-serif text-2xl sm:text-3xl text-charcoal-900">
              {averageRating || '—'}
            </span>
            {averageRating && (
              <span className="flex text-amber-500">
                <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
              </span>
            )}
          </div>
          <span className="text-[11px] text-charcoal-500 mt-0.5 block">From verified clients</span>
        </div>

        <div className="p-5 bg-white border border-sand-200 rounded-2xl shadow-xs">
          <span className="text-xs text-charcoal-500 font-medium block">Published Reviews</span>
          <span className="font-serif text-2xl sm:text-3xl text-charcoal-900 block mt-1">
            {reviews.length}
          </span>
          <span className="text-[11px] text-charcoal-500 mt-0.5 block">Visible on profile</span>
        </div>

        <div className="p-5 bg-white border border-sand-200 rounded-2xl shadow-xs">
          <span className="text-xs text-charcoal-500 font-medium block">Active Invitations</span>
          <span className="font-serif text-2xl sm:text-3xl text-charcoal-900 block mt-1">
            {invitations.filter((i) => i.status === 'PENDING').length}
          </span>
          <span className="text-[11px] text-charcoal-500 mt-0.5 block">Awaiting client response</span>
        </div>

        <div className="p-5 bg-white border border-sand-200 rounded-2xl shadow-xs">
          <span className="text-xs text-charcoal-500 font-medium block">Response Rate</span>
          <span className="font-serif text-2xl sm:text-3xl text-charcoal-900 block mt-1">
            {reviews.length > 0
              ? `${Math.round(
                  (reviews.filter((r) => r.studioResponse).length / reviews.length) * 100
                )}%`
              : '—'}
          </span>
          <span className="text-[11px] text-charcoal-500 mt-0.5 block">Studio replies posted</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-sand-200">
        <button
          type="button"
          onClick={() => setActiveTab('reviews')}
          className={`pb-3 text-xs font-semibold uppercase tracking-wider transition-colors relative ${
            activeTab === 'reviews'
              ? 'text-charcoal-900 border-b-2 border-bronze-700'
              : 'text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          Verified Client Reviews ({reviews.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('invitations')}
          className={`pb-3 text-xs font-semibold uppercase tracking-wider transition-colors relative ml-4 ${
            activeTab === 'invitations'
              ? 'text-charcoal-900 border-b-2 border-bronze-700'
              : 'text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          Review Invitations ({invitations.length})
        </button>
      </div>

      {/* Tab 1: Reviews */}
      {activeTab === 'reviews' && (
        <div className="space-y-4">
          {loading ? (
            <div className="space-y-4 animate-pulse">
              <div className="h-32 bg-sand-100 rounded-2xl" />
              <div className="h-32 bg-sand-100 rounded-2xl" />
            </div>
          ) : reviews.length === 0 ? (
            <div className="bg-white border border-sand-200 rounded-2xl p-10 text-center space-y-4">
              <div className="w-12 h-12 rounded-full bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto">
                <MessageSquare className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-serif text-lg text-charcoal-900">No Client Reviews Yet</h3>
                <p className="text-xs text-charcoal-600 max-w-md mx-auto mt-1">
                  Reviews build client trust. Invite homeowners from completed projects to share their verified experience with your studio.
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setShowInviteModal(true);
                  setInviteSuccessLink(null);
                }}
                className="inline-flex items-center gap-2 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Send First Client Invitation</span>
              </button>
            </div>
          ) : (
            reviews.map((review) => (
              <div
                key={review.id}
                className="bg-white border border-sand-200 rounded-2xl p-6 shadow-xs space-y-4"
              >
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <ReviewStars rating={review.rating} size="sm" />
                      <span className="text-xs font-semibold text-charcoal-900">
                        {review.displayName}
                      </span>
                      {review.verifiedClient && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-800 text-[10px] font-semibold">
                          <ShieldCheck className="w-3 h-3 text-emerald-700" />
                          <span>Verified Client</span>
                        </span>
                      )}
                    </div>
                    {review.projectTitle && (
                      <h4 className="font-serif text-base font-semibold text-charcoal-900 mt-2">
                        {review.projectTitle}
                      </h4>
                    )}
                  </div>
                  <span className="text-[11px] text-charcoal-400">
                    {new Date(review.createdAt).toLocaleDateString('en-IN', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </span>
                </div>

                <p className="text-xs sm:text-sm text-charcoal-700 leading-relaxed">
                  {review.reviewText}
                </p>

                {/* Studio Response Block */}
                {review.studioResponse ? (
                  <div className="p-4 rounded-xl bg-sand-50 border border-sand-200/80 space-y-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-charcoal-900 flex items-center gap-1.5">
                        <CornerDownRight className="w-3.5 h-3.5 text-bronze-700" />
                        Response from {activeStudio?.studioName || 'Studio'}
                      </span>
                      <div className="flex items-center gap-2 text-[11px]">
                        <button
                          type="button"
                          onClick={() => {
                            setReplyingReviewId(review.id);
                            setReplyText(review.studioResponse || '');
                          }}
                          className="text-charcoal-500 hover:text-charcoal-900"
                        >
                          <Edit2 className="w-3 h-3" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteReply(review.id)}
                          className="text-terracotta-600 hover:text-terracotta-800"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                    <p className="text-xs text-charcoal-700 pl-5">{review.studioResponse}</p>
                  </div>
                ) : replyingReviewId === review.id ? (
                  <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 space-y-3">
                    <label className="text-xs font-semibold text-charcoal-900 block">
                      Write Official Response
                    </label>
                    <textarea
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      placeholder="Thank the client and share your thoughts on the project..."
                      rows={3}
                      className="w-full text-xs p-3 rounded-lg border border-sand-300 focus:outline-hidden focus:ring-1 focus:ring-bronze-700 bg-white"
                    />
                    <div className="flex items-center justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setReplyingReviewId(null);
                          setReplyText('');
                        }}
                        className="px-3 py-1.5 text-xs text-charcoal-600 hover:text-charcoal-900"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={() => handleSaveReply(review.id)}
                        disabled={savingReply || !replyText.trim()}
                        className="px-3 py-1.5 bg-charcoal-900 text-white rounded-lg text-xs font-medium hover:bg-charcoal-800 disabled:opacity-50"
                      >
                        {savingReply ? 'Saving...' : 'Post Response'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div>
                    <button
                      type="button"
                      onClick={() => {
                        setReplyingReviewId(review.id);
                        setReplyText('');
                      }}
                      className="inline-flex items-center gap-1.5 text-xs text-bronze-700 hover:text-bronze-900 font-medium"
                    >
                      <CornerDownRight className="w-3.5 h-3.5" />
                      <span>Respond to review</span>
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab 2: Invitations */}
      {activeTab === 'invitations' && (
        <div className="space-y-4">
          {loading ? (
            <div className="space-y-3 animate-pulse">
              <div className="h-16 bg-sand-100 rounded-xl" />
              <div className="h-16 bg-sand-100 rounded-xl" />
            </div>
          ) : invitations.length === 0 ? (
            <div className="bg-white border border-sand-200 rounded-2xl p-10 text-center space-y-4">
              <div className="w-12 h-12 rounded-full bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto">
                <Send className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-serif text-lg text-charcoal-900">No Active Invitations</h3>
                <p className="text-xs text-charcoal-600 max-w-md mx-auto mt-1">
                  Send a private, cryptographically signed review link to your client after handing over a completed project.
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setShowInviteModal(true);
                  setInviteSuccessLink(null);
                }}
                className="inline-flex items-center gap-2 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Create New Invitation</span>
              </button>
            </div>
          ) : (
            <div className="bg-white border border-sand-200 rounded-2xl divide-y divide-sand-200 overflow-hidden shadow-xs">
              {invitations.map((invite) => (
                <div
                  key={invite.id}
                  className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-xs sm:text-sm text-charcoal-900">
                        {invite.clientName}
                      </span>
                      {invite.clientEmail && (
                        <span className="text-xs text-charcoal-400">({invite.clientEmail})</span>
                      )}
                      <span
                        className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                          invite.status === 'ACCEPTED'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : invite.status === 'EXPIRED'
                            ? 'bg-charcoal-100 text-charcoal-600'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}
                      >
                        {invite.status}
                      </span>
                    </div>
                    <span className="text-[11px] text-charcoal-400 block">
                      Expires: {new Date(invite.expiresAt).toLocaleDateString('en-IN')}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {invite.status === 'PENDING' && invite.invitationUrl && (
                      <button
                        type="button"
                        onClick={() => {
                          const origin = typeof window !== 'undefined' ? window.location.origin : '';
                          const fullUrl = invite.invitationUrl!.startsWith('http')
                            ? invite.invitationUrl!
                            : `${origin}${invite.invitationUrl}`;
                          navigator.clipboard.writeText(fullUrl);
                          alert('Invitation link copied to clipboard!');
                        }}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-sand-300 rounded-lg text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors"
                      >
                        <Copy className="w-3.5 h-3.5" />
                        <span>Copy Link</span>
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Create Invite Modal */}
      {showInviteModal && (
        <div
          className="fixed inset-0 z-50 bg-charcoal-950/40 backdrop-blur-xs flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-2xl border border-sand-200 max-w-lg w-full p-6 shadow-xl space-y-6">
            <div className="flex items-center justify-between border-b border-sand-200 pb-4">
              <div>
                <h3 className="font-serif text-lg text-charcoal-900">Invite Client for Review</h3>
                <p className="text-xs text-charcoal-500 mt-0.5">
                  Generate a private verification link for a completed project.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowInviteModal(false)}
                className="p-1 rounded-lg hover:bg-sand-100 text-charcoal-400 hover:text-charcoal-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {inviteSuccessLink ? (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 space-y-2">
                  <div className="flex items-center gap-2 font-medium text-xs">
                    <Check className="w-4 h-4 text-emerald-700" />
                    <span>Invitation Link Generated Successfully</span>
                  </div>
                  <p className="text-xs text-emerald-800">
                    Share this unique link with your client. They can submit their verified review without needing to create an account.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    readOnly
                    value={inviteSuccessLink}
                    className="flex-1 text-xs p-2.5 rounded-lg border border-sand-300 bg-sand-50 font-mono text-charcoal-800"
                  />
                  <button
                    type="button"
                    onClick={handleCopyLink}
                    className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-charcoal-900 text-white rounded-lg text-xs font-medium hover:bg-charcoal-800"
                  >
                    {copiedLink ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                    <span>{copiedLink ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>

                <div className="pt-4 flex justify-end">
                  <button
                    type="button"
                    onClick={() => setShowInviteModal(false)}
                    className="px-4 py-2 bg-sand-200 text-charcoal-800 rounded-xl text-xs font-medium hover:bg-sand-300"
                  >
                    Done
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleCreateInvite} className="space-y-4">
                {wonLeads.length > 0 ? (
                  <div>
                    <label className="text-xs font-medium text-charcoal-700 block mb-1">
                      Select Completed Project Client *
                    </label>
                    <select
                      value={selectedLeadId}
                      onChange={(e) => handleSelectLead(e.target.value)}
                      required
                      className="w-full text-xs p-2.5 rounded-lg border border-sand-300 focus:outline-hidden focus:ring-1 focus:ring-bronze-700 bg-white"
                    >
                      <option value="">Choose a completed client relationship...</option>
                      {wonLeads.map((lead) => (
                        <option key={lead.id} value={lead.id}>
                          {lead.name} ({lead.projectCategory || 'Interior Project'}) — {lead.city || 'India'}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <div>
                    <label className="text-xs font-medium text-charcoal-700 block mb-1">
                      Completed Lead Relationship ID *
                    </label>
                    <input
                      type="text"
                      required
                      value={selectedLeadId}
                      onChange={(e) => setSelectedLeadId(e.target.value)}
                      placeholder="Enter WON lead UUID from CRM"
                      className="w-full text-xs p-2.5 rounded-lg border border-sand-300 focus:outline-hidden focus:ring-1 focus:ring-bronze-700 font-mono"
                    />
                    <p className="text-[11px] text-charcoal-500 mt-1">
                      Under platform verification rules, reviews require a completed relationship in your Leads CRM.
                    </p>
                  </div>
                )}

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Client Full Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={clientName}
                    onChange={(e) => setClientName(e.target.value)}
                    placeholder="e.g. Vikram Sharma"
                    className="w-full text-xs p-2.5 rounded-lg border border-sand-300 focus:outline-hidden focus:ring-1 focus:ring-bronze-700"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-charcoal-700 block mb-1">
                    Client Email Address (Optional)
                  </label>
                  <input
                    type="email"
                    value={clientEmail}
                    onChange={(e) => setClientEmail(e.target.value)}
                    placeholder="e.g. client@example.com"
                    className="w-full text-xs p-2.5 rounded-lg border border-sand-300 focus:outline-hidden focus:ring-1 focus:ring-bronze-700"
                  />
                </div>

                <div className="pt-4 flex items-center justify-end gap-2 border-t border-sand-200">
                  <button
                    type="button"
                    onClick={() => setShowInviteModal(false)}
                    className="px-4 py-2 text-xs text-charcoal-600 hover:text-charcoal-900"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={inviting || !selectedLeadId || !clientName.trim()}
                    className="px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 disabled:opacity-50"
                  >
                    {inviting ? 'Generating...' : 'Generate Invitation'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
