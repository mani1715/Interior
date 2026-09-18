'use client';

import React, { useState } from 'react';
import { Dialog } from '@/components/overlay/Dialog';
import { Button } from '@/components/ui/Button';
import { TextInput, TextArea, Select } from '@/components/forms/FormControls';
import { MessageSquare, Info, CheckCircle2, Shield } from 'lucide-react';
import { Project } from '@/lib/discovery/types';
import { BUDGET_RANGES, LOCATIONS } from '@/lib/discovery/demo-data';

export interface EnquirySheetProps {
  isOpen: boolean;
  onClose: () => void;
  project: Project;
}

export function EnquirySheet({ isOpen, onClose, project }: EnquirySheetProps) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [location, setLocation] = useState(project.location);
  const [budget, setBudget] = useState(project.budgetRange);
  const [message, setMessage] = useState(
    `Hello! I came across your "${project.title}" project in ${project.locationName}. I would like to explore a similar design brief for my property.`
  );
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Phase 06 honest demo submission: no fake backend persistence
    setSubmitted(true);
  };

  const handleReset = () => {
    setSubmitted(false);
    onClose();
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={submitted ? handleReset : onClose}
      title="I Want Something Similar"
      className="max-w-lg"
    >
      {!submitted ? (
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Project Context Box */}
          <div className="p-3.5 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block">
              Enquiry Reference
            </span>
            <p className="font-serif text-sm font-semibold text-[var(--foreground)] truncate">
              {project.title}
            </p>
            <p className="text-xs text-[var(--muted)]">
              Crafted by <span className="font-medium text-[var(--foreground)]">{project.studioName}</span> • {project.locationName}
            </p>
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
            <Select
              label="Your Property City"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              options={LOCATIONS.map((l) => ({ value: l.slug, label: `${l.name}, ${l.state}` }))}
            />
            <Select
              label="Target Budget"
              value={budget}
              onChange={(e) => setBudget(e.target.value as any)}
              options={BUDGET_RANGES.map((b) => ({ value: b.slug, label: b.label }))}
            />
          </div>

          <TextArea
            label="Project Notes / Scope"
            rows={3}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
          />

          {/* Development Preview Truthfulness Notice */}
          <div className="p-3 rounded-xl bg-[var(--surface-sunken)] border border-[var(--border)] flex items-start gap-2.5 text-xs text-[var(--muted)]">
            <Info className="w-4 h-4 text-[var(--brand)] flex-shrink-0 mt-0.5" />
            <span>
              <strong>Development Notice:</strong> Public Discovery Phase 06. Lead capture and live notification routing to {project.studioName} will be operational in Phase 26 (Leads CRM).
            </span>
          </div>

          {/* Form Actions */}
          <div className="pt-2 flex items-center justify-end gap-2.5">
            <Button variant="ghost" size="md" type="button" onClick={onClose}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="md"
              type="submit"
              leftIcon={<MessageSquare className="w-4 h-4" />}
            >
              Submit Enquiry (Demo)
            </Button>
          </div>
        </form>
      ) : (
        /* Honest Demonstration Success State */
        <div className="py-4 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--brand)] flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-6 h-6" />
          </div>

          <div className="space-y-1">
            <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
              Enquiry Form Demonstration
            </h4>
            <p className="text-xs text-[var(--muted)] max-w-sm mx-auto leading-relaxed">
              Your inquiry for <strong className="text-[var(--foreground)]">{project.title}</strong> has been structured. In Phase 26, this brief will be dispatched directly to <strong className="text-[var(--foreground)]">{project.studioName}</strong>.
            </p>
          </div>

          <div className="pt-2">
            <Button variant="primary" size="md" onClick={handleReset} className="w-full sm:w-auto">
              Close Preview
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
