'use client';

import React, { useState } from 'react';
import { Dialog } from '@/components/overlay/Dialog';
import { Button } from '@/components/ui/Button';
import { TextInput, TextArea, Select } from '@/components/forms/FormControls';
import { MessageSquare, CheckCircle2, Shield, AlertCircle, ExternalLink, Loader2 } from 'lucide-react';
import { Project } from '@/lib/discovery/types';
import { BUDGET_RANGES, LOCATIONS } from '@/lib/discovery/demo-data';
import { submitPublicLead, initiatePublicWhatsAppHandoff } from '@/lib/leads/api';
import { PreferredContactChannel, PublicLeadSubmissionResponse } from '@/lib/leads/types';

export interface EnquirySheetProps {
  isOpen: boolean;
  onClose: () => void;
  project?: Project | null;
  studioSlug?: string;
  studioName?: string;
  projectSlug?: string;
}

export function EnquirySheet({
  isOpen,
  onClose,
  project,
  studioSlug: propStudioSlug,
  studioName: propStudioName,
  projectSlug: propProjectSlug,
}: EnquirySheetProps) {
  const targetStudioSlug = (project as any)?.studioSlug || project?.professionalSlug || propStudioSlug || '';
  const targetStudioName = project?.studioName || propStudioName || 'Interior Studio';
  const targetProjectSlug = project?.slug || propProjectSlug || null;
  const projectTitle = project?.title || null;

  // Form State
  const [activeTab, setActiveTab] = useState<'INQUIRY' | 'WHATSAPP'>('INQUIRY');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [city, setCity] = useState(project?.location || '');
  const [budget, setBudget] = useState(project?.budgetRange || '');
  const [preferredChannel, setPreferredChannel] = useState<PreferredContactChannel>('ANY');
  const [consentContact, setConsentContact] = useState(true);
  const [consentWhatsapp, setConsentWhatsapp] = useState(true);
  const [message, setMessage] = useState(
    projectTitle
      ? `Hello! I came across your "${projectTitle}" project. I would like to explore a similar design brief for my property.`
      : `Hello! I came across your interior design portfolio and would like to discuss my project.`
  );
  const [websiteHp, setWebsiteHp] = useState(''); // Honeypot

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState<PublicLeadSubmissionResponse | null>(null);
  const [whatsappResult, setWhatsappResult] = useState<{ url: string; studioName: string } | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleInquirySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!name.trim()) {
      setErrorMessage('Please enter your full name.');
      return;
    }
    if (!phone.trim()) {
      setErrorMessage('Please provide a valid phone number (e.g. +91 98765 43210).');
      return;
    }
    if (!consentContact) {
      setErrorMessage('Please grant consent to be contacted regarding this project.');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await submitPublicLead({
        targetStudioSlug,
        targetProjectSlug,
        name: name.trim(),
        phone: phone.trim(),
        email: email.trim() ? email.trim() : null,
        city: city.trim() ? city.trim() : null,
        projectCategory: project?.category || null,
        budgetRange: budget || null,
        message: message.trim(),
        preferredContactChannel: preferredChannel,
        consentContact,
        consentWhatsapp,
        website_hp: websiteHp || null,
      });

      setSubmissionResult(res);
    } catch (err: any) {
      setErrorMessage(
        err?.envelope?.message || err?.message || 'Unable to submit inquiry. Please check your network and try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleWhatsAppHandoff = async () => {
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const res = await initiatePublicWhatsAppHandoff({
        targetStudioSlug,
        targetProjectSlug,
        visitorName: name.trim() ? name.trim() : null,
        visitorPhone: phone.trim() ? phone.trim() : null,
        customMessage: message.trim() ? message.trim() : null,
      });

      setWhatsappResult({ url: res.whatsappUrl, studioName: res.studioName });

      // Open WhatsApp safely in new tab
      if (typeof window !== 'undefined') {
        window.open(res.whatsappUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err: any) {
      setErrorMessage(
        err?.envelope?.message ||
          err?.message ||
          'WhatsApp direct chat is not available for this professional. Please submit an inquiry instead.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setSubmissionResult(null);
    setWhatsappResult(null);
    setErrorMessage(null);
    onClose();
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={submissionResult || whatsappResult ? handleReset : onClose}
      title={projectTitle ? 'I Want Something Similar' : `Contact ${targetStudioName}`}
      className="max-w-lg"
    >
      {!submissionResult && !whatsappResult ? (
        <div className="space-y-4">
          {/* Project or Studio Context Box */}
          <div className="p-3.5 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block">
              Inquiry Recipient
            </span>
            <p className="font-serif text-sm font-semibold text-[var(--foreground)] truncate">
              {projectTitle ? projectTitle : targetStudioName}
            </p>
            <p className="text-xs text-[var(--muted)]">
              Crafted by <span className="font-medium text-[var(--foreground)]">{targetStudioName}</span>
              {project?.locationName ? ` • ${project.locationName}` : ''}
            </p>
          </div>

          {/* Mode Switcher Tabs */}
          <div className="flex border-b border-[var(--border)] text-xs font-medium">
            <button
              type="button"
              onClick={() => {
                setActiveTab('INQUIRY');
                setErrorMessage(null);
              }}
              className={`pb-2.5 px-3 border-b-2 transition-colors ${
                activeTab === 'INQUIRY'
                  ? 'border-[var(--brand)] text-[var(--foreground)] font-semibold'
                  : 'border-transparent text-[var(--muted)] hover:text-[var(--foreground)]'
              }`}
            >
              Send Detailed Inquiry
            </button>
            <button
              type="button"
              onClick={() => {
                setActiveTab('WHATSAPP');
                setErrorMessage(null);
              }}
              className={`pb-2.5 px-3 border-b-2 transition-colors flex items-center gap-1.5 ${
                activeTab === 'WHATSAPP'
                  ? 'border-[var(--brand)] text-[var(--foreground)] font-semibold'
                  : 'border-transparent text-[var(--muted)] hover:text-[var(--foreground)]'
              }`}
            >
              <span>Chat on WhatsApp</span>
              <span className="px-1.5 py-0.2 rounded text-[10px] bg-emerald-100 text-emerald-800 font-semibold">
                Direct
              </span>
            </button>
          </div>

          {errorMessage && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2 text-xs text-rose-800">
              <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {activeTab === 'INQUIRY' ? (
            <form onSubmit={handleInquirySubmit} className="space-y-4">
              {/* Hidden Honeypot Field */}
              <div style={{ display: 'none' }} aria-hidden="true">
                <label htmlFor="website_hp">Leave empty</label>
                <input
                  id="website_hp"
                  type="text"
                  name="website_hp"
                  value={websiteHp}
                  onChange={(e) => setWebsiteHp(e.target.value)}
                  tabIndex={-1}
                  autoComplete="off"
                />
              </div>

              {/* Contact Fields */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <TextInput
                  label="Your Name"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Rajesh Kumar"
                />
                <TextInput
                  label="Phone / WhatsApp"
                  required
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. +91 98765 43210"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <TextInput
                  label="Email Address (Optional)"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                />
                <Select
                  label="Preferred Contact"
                  value={preferredChannel}
                  onChange={(e) => setPreferredChannel(e.target.value as PreferredContactChannel)}
                  options={[
                    { value: 'ANY', label: 'Any Channel' },
                    { value: 'WHATSAPP', label: 'WhatsApp' },
                    { value: 'PHONE', label: 'Phone Call' },
                    { value: 'EMAIL', label: 'Email' },
                  ]}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Select
                  label="Your Property City"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  options={LOCATIONS.map((l) => ({ value: l.slug, label: `${l.name}, ${l.state}` }))}
                />
                <Select
                  label="Target Budget"
                  value={budget}
                  onChange={(e) => setBudget(e.target.value)}
                  options={BUDGET_RANGES.map((b) => ({ value: b.slug, label: b.label }))}
                />
              </div>

              <TextArea
                label="Project Notes / Scope"
                rows={3}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
              />

              {/* Explicit Consent & Privacy Safeguards */}
              <div className="space-y-2 pt-1 border-t border-[var(--border)]">
                <label className="flex items-start gap-2.5 cursor-pointer text-xs text-[var(--foreground)]">
                  <input
                    type="checkbox"
                    checked={consentContact}
                    onChange={(e) => setConsentContact(e.target.checked)}
                    className="mt-0.5 rounded border-[var(--border)] text-[var(--brand)] focus:ring-[var(--brand)]"
                  />
                  <span>
                    I consent to be contacted by <strong>{targetStudioName}</strong> regarding this project inquiry.
                  </span>
                </label>

                <label className="flex items-start gap-2.5 cursor-pointer text-xs text-[var(--muted)]">
                  <input
                    type="checkbox"
                    checked={consentWhatsapp}
                    onChange={(e) => setConsentWhatsapp(e.target.checked)}
                    className="mt-0.5 rounded border-[var(--border)] text-[var(--brand)] focus:ring-[var(--brand)]"
                  />
                  <span>I agree to receive project updates and consultation details via WhatsApp.</span>
                </label>
              </div>

              {/* Privacy Notice */}
              <div className="p-3 rounded-xl bg-[var(--surface-sunken)] border border-[var(--border)] flex items-start gap-2.5 text-xs text-[var(--muted)]">
                <Shield className="w-4 h-4 text-[var(--brand)] flex-shrink-0 mt-0.5" />
                <span>
                  <strong>Tenant Privacy:</strong> Your contact details are stored privately in {targetStudioName}'s secure workspace CRM and are never publicly indexed or shared with third parties.
                </span>
              </div>

              {/* Form Actions */}
              <div className="pt-2 flex items-center justify-end gap-2.5">
                <Button variant="ghost" size="md" type="button" onClick={onClose} disabled={isSubmitting}>
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  type="submit"
                  disabled={isSubmitting}
                  leftIcon={
                    isSubmitting ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <MessageSquare className="w-4 h-4" />
                    )
                  }
                >
                  {isSubmitting ? 'Transmitting...' : 'Send Inquiry'}
                </Button>
              </div>
            </form>
          ) : (
            /* User-Initiated WhatsApp Handoff Mode */
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 space-y-2">
                <p className="font-semibold text-sm text-emerald-950 flex items-center gap-1.5">
                  <span>Direct WhatsApp Chat</span>
                </p>
                <p className="leading-relaxed">
                  Connect immediately with <strong>{targetStudioName}</strong>. Clicking below will open WhatsApp on your device with your prefilled project inquiry so you can start chatting directly.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <TextInput
                  label="Your Name (Optional)"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Rajesh Kumar"
                />
                <TextInput
                  label="Your Phone (Optional)"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. +91 98765 43210"
                />
              </div>

              <TextArea
                label="Opening Message"
                rows={3}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
              />

              <div className="p-3 rounded-xl bg-[var(--surface-sunken)] border border-[var(--border)] text-xs text-[var(--muted)]">
                Note: WhatsApp handoff will record this session in the studio's inquiry log without falsely claiming the message was already delivered or read.
              </div>

              <div className="pt-2 flex items-center justify-end gap-2.5">
                <Button variant="ghost" size="md" type="button" onClick={onClose} disabled={isSubmitting}>
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={handleWhatsAppHandoff}
                  disabled={isSubmitting}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                  rightIcon={
                    isSubmitting ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <ExternalLink className="w-4 h-4" />
                    )
                  }
                >
                  {isSubmitting ? 'Opening WhatsApp...' : 'Open WhatsApp Chat'}
                </Button>
              </div>
            </div>
          )}
        </div>
      ) : submissionResult ? (
        /* Real Submission Success State */
        <div className="py-4 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-emerald-100 border border-emerald-300 text-emerald-700 flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-6 h-6" />
          </div>

          <div className="space-y-1">
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)] font-semibold">
              Ref: {submissionResult.referenceNumber}
            </span>
            <h4 className="font-serif text-lg font-semibold text-[var(--foreground)] pt-2">
              Inquiry Dispatched Successfully
            </h4>
            <p className="text-xs text-[var(--muted)] max-w-sm mx-auto leading-relaxed">
              Your inquiry has been routed directly to <strong className="text-[var(--foreground)]">{submissionResult.studioName}</strong>. The design team has received your brief and will connect via your preferred contact channel.
            </p>
          </div>

          <div className="pt-2">
            <Button variant="primary" size="md" onClick={handleReset} className="w-full sm:w-auto">
              Done
            </Button>
          </div>
        </div>
      ) : (
        /* WhatsApp Handoff Initiated Success State */
        <div className="py-4 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-emerald-100 border border-emerald-300 text-emerald-700 flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-6 h-6" />
          </div>

          <div className="space-y-1">
            <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
              WhatsApp Chat Window Opened
            </h4>
            <p className="text-xs text-[var(--muted)] max-w-sm mx-auto leading-relaxed">
              We opened WhatsApp with your prefilled project message for <strong className="text-[var(--foreground)]">{whatsappResult?.studioName}</strong>. If the window did not open, you can launch it manually.
            </p>
          </div>

          <div className="pt-2 flex items-center justify-center gap-3">
            <Button
              variant="outline"
              size="md"
              onClick={() => {
                if (whatsappResult?.url && typeof window !== 'undefined') {
                  window.open(whatsappResult.url, '_blank', 'noopener,noreferrer');
                }
              }}
              rightIcon={<ExternalLink className="w-4 h-4" />}
            >
              Reopen WhatsApp
            </Button>
            <Button variant="primary" size="md" onClick={handleReset}>
              Close
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
