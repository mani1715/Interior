'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import {
  Users,
  ArrowLeft,
  Search,
  Filter,
  Calendar,
  MessageSquare,
  Clock,
  CheckCircle,
  XCircle,
  Archive,
  Phone,
  Mail,
  MapPin,
  Tag,
  AlertTriangle,
  RefreshCw,
  Send,
  Plus,
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  UserCheck,
} from 'lucide-react';
import {
  fetchLeads,
  fetchLeadCounts,
  fetchLeadDetail,
  updateLead,
  addLeadNote,
  archiveLead,
  fetchWhatsAppStatus,
  sendWhatsAppMessage,
} from '@/lib/leads/api';
import {
  LeadSummary,
  LeadCounts,
  LeadDetail,
  LeadStatus,
  WhatsAppProviderStatus,
} from '@/lib/leads/types';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/overlay/Dialog';

export default function LeadsWorkspacePage() {
  const [counts, setCounts] = useState<LeadCounts>({
    total: 0,
    newLeads: 0,
    active: 0,
    won: 0,
    lost: 0,
    archived: 0,
  });

  const [activeTab, setActiveTab] = useState<'ALL' | 'NEW' | 'ACTIVE' | 'WON' | 'LOST' | 'ARCHIVED'>('ALL');
  const [leads, setLeads] = useState<LeadSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOption, setSortOption] = useState('createdAt:desc');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Selected Lead Detail
  const [selectedLeadId, setSelectedLeadId] = useState<string | null>(null);
  const [selectedLead, setSelectedLead] = useState<LeadDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  // Edit / Action State
  const [newStatus, setNewStatus] = useState<LeadStatus | ''>('');
  const [lostReason, setLostReason] = useState('');
  const [nextFollowUp, setNextFollowUp] = useState('');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Notes State
  const [newNoteContent, setNewNoteContent] = useState('');
  const [isAddingNote, setIsAddingNote] = useState(false);

  // WhatsApp State
  const [whatsappStatus, setWhatsappStatus] = useState<WhatsAppProviderStatus | null>(null);
  const [waMessageBody, setWaMessageBody] = useState('');
  const [isSendingWa, setIsSendingWa] = useState(false);
  const [waError, setWaError] = useState<string | null>(null);

  const loadCounts = useCallback(async () => {
    try {
      const data = await fetchLeadCounts();
      setCounts(data);
    } catch {
      // Ignore count fetch errors if unauthenticated or offline
    }
  }, []);

  const loadLeads = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await fetchLeads({
        status: activeTab === 'ALL' ? undefined : activeTab,
        search: searchQuery.trim() || undefined,
        sort: sortOption,
        limit: 50,
      });
      setLeads(res.items);
    } catch (err: any) {
      setErrorMessage(err?.envelope?.message || err?.message || 'Unable to load leads.');
    } finally {
      setLoading(false);
    }
  }, [activeTab, searchQuery, sortOption]);

  useEffect(() => {
    loadCounts();
  }, [loadCounts]);

  useEffect(() => {
    loadLeads();
  }, [loadLeads]);

  const openLeadDetail = async (leadId: string) => {
    setSelectedLeadId(leadId);
    setDetailLoading(true);
    setDetailError(null);
    setWaError(null);

    try {
      const [detail, wa] = await Promise.all([
        fetchLeadDetail(leadId),
        fetchWhatsAppStatus(leadId).catch(() => null),
      ]);
      setSelectedLead(detail);
      setNewStatus(detail.status);
      setLostReason(detail.lostReason || '');
      setNextFollowUp(detail.nextFollowUpAt ? detail.nextFollowUpAt.substring(0, 16) : '');
      setWhatsappStatus(wa);
    } catch (err: any) {
      setDetailError(err?.envelope?.message || err?.message || 'Failed to load lead details.');
    } finally {
      setDetailLoading(false);
    }
  };

  const closeLeadDetail = () => {
    setSelectedLeadId(null);
    setSelectedLead(null);
    setNewNoteContent('');
    setWaMessageBody('');
    setWaError(null);
  };

  const handleStatusUpdate = async () => {
    if (!selectedLead) return;
    setIsUpdatingStatus(true);
    setDetailError(null);

    try {
      const updated = await updateLead(selectedLead.id, {
        status: newStatus ? (newStatus as LeadStatus) : undefined,
        lostReason: newStatus === 'LOST' ? lostReason.trim() : undefined,
        nextFollowUpAt: nextFollowUp ? new Date(nextFollowUp).toISOString() : null,
        expectedVersion: selectedLead.version,
      });

      setSelectedLead(updated);
      loadCounts();
      loadLeads();
    } catch (err: any) {
      setDetailError(err?.envelope?.message || err?.message || 'Failed to update lead.');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLead || !newNoteContent.trim()) return;

    setIsAddingNote(true);
    setDetailError(null);

    try {
      await addLeadNote(selectedLead.id, { content: newNoteContent.trim() });
      setNewNoteContent('');
      // Reload detail to refresh notes and audit activities
      const refreshed = await fetchLeadDetail(selectedLead.id);
      setSelectedLead(refreshed);
    } catch (err: any) {
      setDetailError(err?.envelope?.message || err?.message || 'Failed to add note.');
    } finally {
      setIsAddingNote(false);
    }
  };

  const handleArchive = async () => {
    if (!selectedLead) return;
    if (!confirm('Are you sure you want to archive this lead?')) return;

    try {
      await archiveLead(selectedLead.id, selectedLead.version);
      closeLeadDetail();
      loadCounts();
      loadLeads();
    } catch (err: any) {
      setDetailError(err?.envelope?.message || err?.message || 'Failed to archive lead.');
    }
  };

  const handleSendWhatsApp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLead || !waMessageBody.trim()) return;

    setIsSendingWa(true);
    setWaError(null);

    try {
      await sendWhatsAppMessage(selectedLead.id, {
        body: waMessageBody.trim(),
      });
      setWaMessageBody('');
      const refreshed = await fetchLeadDetail(selectedLead.id);
      setSelectedLead(refreshed);
    } catch (err: any) {
      setWaError(err?.envelope?.message || err?.message || 'Failed to send WhatsApp message.');
    } finally {
      setIsSendingWa(false);
    }
  };

  const getStatusBadge = (status: LeadStatus) => {
    switch (status) {
      case 'NEW':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-100 text-amber-900 border border-amber-300">
            New Inquiry
          </span>
        );
      case 'CONTACTED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-sky-100 text-sky-900 border border-sky-300">
            Contacted
          </span>
        );
      case 'QUALIFIED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-indigo-100 text-indigo-900 border border-indigo-300">
            Qualified
          </span>
        );
      case 'SITE_VISIT_PLANNED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-purple-100 text-purple-900 border border-purple-300">
            Site Visit Planned
          </span>
        );
      case 'IN_DISCUSSION':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-blue-100 text-blue-900 border border-blue-300">
            In Discussion
          </span>
        );
      case 'WON':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-900 border border-emerald-300">
            Won
          </span>
        );
      case 'LOST':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-rose-100 text-rose-900 border border-rose-300">
            Lost
          </span>
        );
      case 'ARCHIVED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-zinc-100 text-zinc-800 border border-zinc-300">
            Archived
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-6">
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
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Client Inquiries CRM
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Leads & Consultations
              </h1>
            </div>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              loadCounts();
              loadLeads();
            }}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div
          onClick={() => setActiveTab('ALL')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'ALL'
              ? 'bg-charcoal-900 text-white border-charcoal-900 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block opacity-75">
            Total Leads
          </span>
          <span className="font-serif text-2xl font-bold">{counts.total}</span>
        </div>

        <div
          onClick={() => setActiveTab('NEW')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'NEW'
              ? 'bg-amber-600 text-white border-amber-600 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block text-amber-700">
            New
          </span>
          <span className="font-serif text-2xl font-bold">{counts.newLeads}</span>
        </div>

        <div
          onClick={() => setActiveTab('ACTIVE')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'ACTIVE'
              ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block text-indigo-700">
            Active Pipeline
          </span>
          <span className="font-serif text-2xl font-bold">{counts.active}</span>
        </div>

        <div
          onClick={() => setActiveTab('WON')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'WON'
              ? 'bg-emerald-700 text-white border-emerald-700 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block text-emerald-700">
            Won Deals
          </span>
          <span className="font-serif text-2xl font-bold">{counts.won}</span>
        </div>

        <div
          onClick={() => setActiveTab('LOST')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'LOST'
              ? 'bg-rose-700 text-white border-rose-700 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block text-rose-700">
            Lost
          </span>
          <span className="font-serif text-2xl font-bold">{counts.lost}</span>
        </div>

        <div
          onClick={() => setActiveTab('ARCHIVED')}
          className={`p-3.5 rounded-xl border transition-all cursor-pointer ${
            activeTab === 'ARCHIVED'
              ? 'bg-zinc-700 text-white border-zinc-700 shadow-sm'
              : 'bg-white text-charcoal-800 border-sand-200 hover:border-sand-300'
          }`}
        >
          <span className="text-[10px] font-semibold uppercase tracking-wider block text-zinc-500">
            Archived
          </span>
          <span className="font-serif text-2xl font-bold">{counts.archived}</span>
        </div>
      </div>

      {/* Search and Filters Bar */}
      <div className="bg-white border border-sand-200 rounded-2xl p-4 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-charcoal-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search leads by name, email, or city..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl text-xs border border-sand-200 focus:outline-none focus:ring-1 focus:ring-charcoal-900 bg-sand-50"
            />
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-charcoal-500 font-medium">Sort:</span>
            <select
              value={sortOption}
              onChange={(e) => setSortOption(e.target.value)}
              className="px-3 py-2 rounded-xl text-xs border border-sand-200 bg-sand-50 text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-charcoal-900"
            >
              <option value="createdAt:desc">Newest First</option>
              <option value="createdAt:asc">Oldest First</option>
              <option value="nextFollowUpAt:asc">Next Follow-Up</option>
            </select>
          </div>
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}
      </div>

      {/* Leads List */}
      <div className="space-y-3">
        {loading ? (
          <div className="p-12 text-center bg-white rounded-2xl border border-sand-200 text-xs text-charcoal-400">
            <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-bronze-700" />
            <span>Loading studio leads...</span>
          </div>
        ) : leads.length === 0 ? (
          <div className="p-12 text-center bg-white rounded-2xl border border-sand-200 text-charcoal-500 space-y-2">
            <Users className="w-8 h-8 mx-auto text-sand-400" />
            <h3 className="font-serif text-base font-semibold text-charcoal-800">
              No leads found in this view
            </h3>
            <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
              {searchQuery
                ? `No leads matched "${searchQuery}". Try clearing search.`
                : 'Inquiries submitted by homeowners on your published portfolio and projects will appear here.'}
            </p>
          </div>
        ) : (
          leads.map((lead) => (
            <div
              key={lead.id}
              onClick={() => openLeadDetail(lead.id)}
              className="bg-white border border-sand-200 rounded-2xl p-4 sm:p-5 hover:border-sand-300 hover:shadow-sm transition-all cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              <div className="space-y-1.5 flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-serif text-base font-semibold text-charcoal-900 truncate">
                    {lead.name}
                  </h3>
                  {getStatusBadge(lead.status)}
                  {lead.possibleDuplicate && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-50 text-amber-800 border border-amber-200 flex items-center gap-1">
                      <AlertTriangle className="w-3 h-3 text-amber-600" />
                      Possible Duplicate
                    </span>
                  )}
                  {lead.hasWhatsappConsent && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                      WhatsApp Opt-in
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-3 text-xs text-charcoal-500 flex-wrap">
                  <span className="font-mono text-charcoal-700">{lead.phoneMasked}</span>
                  {lead.emailNormalized && (
                    <span className="flex items-center gap-1 truncate">
                      <Mail className="w-3 h-3" />
                      {lead.emailNormalized}
                    </span>
                  )}
                  {lead.city && (
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3 h-3" />
                      {lead.city}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 text-[11px] text-charcoal-500 flex-wrap pt-1">
                  <span className="px-2 py-0.5 rounded bg-sand-100 text-charcoal-700 font-medium">
                    {lead.sourceDisplayName}
                  </span>
                  {lead.projectTitle && (
                    <span className="text-bronze-800 font-medium">
                      Project: {lead.projectTitle}
                    </span>
                  )}
                  {lead.budgetRange && (
                    <span className="font-mono text-charcoal-600">
                      Budget: {lead.budgetRange}
                    </span>
                  )}
                </div>
              </div>

              <div className="flex md:flex-col items-center md:items-end justify-between md:justify-center gap-1 text-xs text-charcoal-500 flex-shrink-0 border-t md:border-t-0 pt-3 md:pt-0 border-sand-100">
                <span className="text-[11px] text-charcoal-400">
                  Received {new Date(lead.createdAt).toLocaleDateString()}
                </span>
                {lead.nextFollowUpAt && (
                  <span className="text-[11px] font-semibold text-indigo-700 flex items-center gap-1">
                    <Calendar className="w-3 h-3" />
                    Follow-up: {new Date(lead.nextFollowUpAt).toLocaleDateString()}
                  </span>
                )}
                <div className="flex items-center gap-1 text-bronze-700 font-semibold text-xs mt-1">
                  <span>View Details</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Full Lead Detail Drawer / Modal */}
      {selectedLeadId && (
        <Dialog
          isOpen={true}
          onClose={closeLeadDetail}
          title={selectedLead ? `Lead: ${selectedLead.name}` : 'Lead Details'}
          className="max-w-3xl"
        >
          {detailLoading ? (
            <div className="py-12 text-center text-xs text-charcoal-400">
              <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-bronze-700" />
              <span>Loading lead details...</span>
            </div>
          ) : detailError ? (
            <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 space-y-2">
              <p className="font-semibold">{detailError}</p>
              <Button size="sm" variant="outline" onClick={closeLeadDetail}>
                Close
              </Button>
            </div>
          ) : selectedLead ? (
            <div className="space-y-6">
              {/* Header Context Bar */}
              <div className="p-4 rounded-2xl bg-sand-50 border border-sand-200 space-y-2">
                <div className="flex items-center justify-between gap-3 flex-wrap">
                  <div className="flex items-center gap-2">
                    <h3 className="font-serif text-lg font-bold text-charcoal-900">
                      {selectedLead.name}
                    </h3>
                    {getStatusBadge(selectedLead.status)}
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleArchive}
                    className="text-rose-700 hover:text-rose-900"
                    leftIcon={<Archive className="w-3.5 h-3.5" />}
                  >
                    Archive
                  </Button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs text-charcoal-600 pt-2 border-t border-sand-200">
                  <div>
                    <span className="text-[10px] text-charcoal-400 uppercase font-semibold block">
                      Phone Number
                    </span>
                    <span className="font-mono font-bold text-charcoal-900">
                      {selectedLead.phoneNormalized}
                    </span>
                  </div>
                  <div>
                    <span className="text-[10px] text-charcoal-400 uppercase font-semibold block">
                      Email Address
                    </span>
                    <span>{selectedLead.emailNormalized || 'Not provided'}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-charcoal-400 uppercase font-semibold block">
                      Property Location
                    </span>
                    <span>{selectedLead.city || 'Not specified'}</span>
                  </div>
                </div>

                {selectedLead.projectTitle && (
                  <div className="text-xs text-charcoal-700 pt-1">
                    Attached Project:{' '}
                    <strong className="text-bronze-800">{selectedLead.projectTitle}</strong>
                  </div>
                )}
              </div>

              {/* Inquiry Brief / Scope */}
              <div className="space-y-1.5">
                <span className="text-xs font-semibold uppercase tracking-wider text-bronze-800 block">
                  Client Brief & Message
                </span>
                <div className="p-3.5 rounded-xl bg-white border border-sand-200 text-xs text-charcoal-800 leading-relaxed">
                  {selectedLead.message}
                </div>
              </div>

              {/* Status Update & Follow-up Controls */}
              <div className="p-4 rounded-2xl bg-white border border-sand-200 space-y-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-900 block">
                  Pipeline & Follow-Up
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] font-semibold text-charcoal-600 block mb-1">
                      Lead Pipeline Stage
                    </label>
                    <select
                      value={newStatus}
                      onChange={(e) => setNewStatus(e.target.value as LeadStatus)}
                      className="w-full px-3 py-2 rounded-xl text-xs border border-sand-200 bg-sand-50 text-charcoal-900"
                    >
                      <option value="NEW">New Inquiry</option>
                      <option value="CONTACTED">Contacted</option>
                      <option value="QUALIFIED">Qualified</option>
                      <option value="SITE_VISIT_PLANNED">Site Visit Planned</option>
                      <option value="IN_DISCUSSION">In Discussion</option>
                      <option value="WON">Won</option>
                      <option value="LOST">Lost</option>
                      <option value="ARCHIVED">Archived</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-[11px] font-semibold text-charcoal-600 block mb-1">
                      Next Follow-Up Date & Time
                    </label>
                    <input
                      type="datetime-local"
                      value={nextFollowUp}
                      onChange={(e) => setNextFollowUp(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl text-xs border border-sand-200 bg-sand-50 text-charcoal-900"
                    />
                  </div>
                </div>

                {newStatus === 'LOST' && (
                  <div>
                    <label className="text-[11px] font-semibold text-charcoal-600 block mb-1">
                      Reason for Lost Lead
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Budget mismatch, chose local carpenter, postponed timeline"
                      value={lostReason}
                      onChange={(e) => setLostReason(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl text-xs border border-sand-200 bg-sand-50 text-charcoal-900"
                    />
                  </div>
                )}

                <div className="flex justify-end pt-1">
                  <Button
                    size="sm"
                    variant="primary"
                    onClick={handleStatusUpdate}
                    disabled={isUpdatingStatus}
                  >
                    {isUpdatingStatus ? 'Saving...' : 'Update Lead State'}
                  </Button>
                </div>
              </div>

              {/* WhatsApp Quick Actions & Managed Messaging */}
              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 space-y-3">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-xs font-semibold uppercase tracking-wider text-emerald-950 block">
                    WhatsApp Communication
                  </span>
                  {selectedLead.hasWhatsappConsent ? (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-200 text-emerald-900 flex items-center gap-1">
                      <ShieldCheck className="w-3 h-3 text-emerald-700" />
                      Client Consent Granted
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-900 border border-amber-300">
                      No WhatsApp Consent
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 flex-wrap pt-1">
                  <Button
                    size="sm"
                    variant="outline"
                    className="bg-white border-emerald-300 text-emerald-800 hover:bg-emerald-100"
                    onClick={() => {
                      const cleanPhone = selectedLead.phoneNormalized.replace(/\+/g, '');
                      const text = encodeURIComponent(
                        `Hi ${selectedLead.name}, this is regarding your interior design inquiry.`
                      );
                      window.open(`https://wa.me/${cleanPhone}?text=${text}`, '_blank');
                    }}
                    leftIcon={<ExternalLink className="w-3.5 h-3.5" />}
                  >
                    Launch WhatsApp Chat Directly
                  </Button>
                </div>

                {/* Managed WhatsApp Compose (truthful error if disabled) */}
                {selectedLead.hasWhatsappConsent && (
                  <form onSubmit={handleSendWhatsApp} className="pt-2 border-t border-emerald-200 space-y-2">
                    <span className="text-[11px] font-semibold text-emerald-950 block">
                      Send Managed WhatsApp Message
                    </span>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder={
                          whatsappStatus && !whatsappStatus.configured
                            ? 'Managed WhatsApp provider not configured (use direct chat above)'
                            : 'Type message to dispatch via verified WhatsApp provider...'
                        }
                        disabled={whatsappStatus ? !whatsappStatus.configured : false}
                        value={waMessageBody}
                        onChange={(e) => setWaMessageBody(e.target.value)}
                        className="flex-1 px-3 py-1.5 rounded-xl text-xs border border-emerald-300 bg-white focus:outline-none disabled:bg-sand-100 disabled:text-charcoal-400"
                      />
                      <Button
                        size="sm"
                        variant="primary"
                        type="submit"
                        disabled={isSendingWa || (whatsappStatus ? !whatsappStatus.configured : false)}
                        className="bg-emerald-700 hover:bg-emerald-800 text-white disabled:opacity-50"
                        leftIcon={<Send className="w-3.5 h-3.5" />}
                      >
                        {isSendingWa ? 'Sending...' : 'Send'}
                      </Button>
                    </div>

                    {waError && (
                      <p className="text-[11px] text-rose-700 font-medium">{waError}</p>
                    )}

                    {whatsappStatus && !whatsappStatus.configured && (
                      <p className="text-[10px] text-amber-800 bg-amber-50 p-1.5 rounded border border-amber-200">
                        Provider Status: {whatsappStatus.status} (Managed messaging requires configured provider)
                      </p>
                    )}
                  </form>
                )}
              </div>

              {/* Internal Notes Section */}
              <div className="space-y-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-900 block">
                  Internal Studio Notes
                </span>

                <form onSubmit={handleAddNote} className="flex gap-2">
                  <input
                    type="text"
                    placeholder="Add an internal note for your design team..."
                    value={newNoteContent}
                    onChange={(e) => setNewNoteContent(e.target.value)}
                    className="flex-1 px-3 py-2 rounded-xl text-xs border border-sand-200 bg-sand-50 focus:outline-none focus:ring-1 focus:ring-charcoal-900"
                  />
                  <Button
                    size="sm"
                    variant="primary"
                    type="submit"
                    disabled={isAddingNote || !newNoteContent.trim()}
                    leftIcon={<Plus className="w-3.5 h-3.5" />}
                  >
                    Add Note
                  </Button>
                </form>

                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {selectedLead.notes.length === 0 ? (
                    <p className="text-xs text-charcoal-400 italic">No notes added yet.</p>
                  ) : (
                    selectedLead.notes.map((n) => (
                      <div key={n.id} className="p-3 rounded-xl bg-sand-50 border border-sand-200 text-xs">
                        <div className="flex items-center justify-between text-[10px] text-charcoal-400 mb-1">
                          <span className="font-semibold text-charcoal-700">{n.authorName}</span>
                          <span>{new Date(n.createdAt).toLocaleString()}</span>
                        </div>
                        <p className="text-charcoal-800 leading-relaxed">{n.content}</p>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Immutable Activity Timeline */}
              <div className="space-y-3 pt-3 border-t border-sand-200">
                <span className="text-xs font-semibold uppercase tracking-wider text-charcoal-900 block">
                  Audit History
                </span>
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {selectedLead.activities.map((a) => (
                    <div key={a.id} className="flex items-start gap-2.5 text-xs text-charcoal-600">
                      <div className="w-2 h-2 rounded-full bg-bronze-600 mt-1.5 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <span className="font-semibold text-charcoal-900">
                          {a.activityTypeDisplayName}
                        </span>
                        {a.actorName && (
                          <span className="text-charcoal-500"> by {a.actorName}</span>
                        )}
                        <span className="text-[10px] text-charcoal-400 block">
                          {new Date(a.createdAt).toLocaleString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
        </Dialog>
      )}
    </div>
  );
}
