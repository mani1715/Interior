'use client';

import React, { useState } from 'react';
import { MessageSquare, ArrowRight, Share2, Check } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { EnquirySheet } from './EnquirySheet';
import { Project } from '@/lib/discovery/types';
import { SaveToCollectionButton } from '@/components/collections/SaveToCollectionButton';

export interface ProjectDetailActionsProps {
  project: Project;
}

export function ProjectDetailActions({ project }: ProjectDetailActionsProps) {
  const [enquiryOpen, setEnquiryOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleShare = async () => {
    if (typeof window !== 'undefined' && navigator.share) {
      try {
        await navigator.share({
          title: project.title,
          text: `Check out ${project.title} by ${project.studioName} on Elégance`,
          url: window.location.href,
        });
        return;
      } catch (err) {
        // Fallback to clipboard
      }
    }
    if (typeof window !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <>
      {/* Top Action Buttons */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full sm:w-auto">
        <Button
          variant="primary"
          size="lg"
          onClick={() => setEnquiryOpen(true)}
          className="justify-center text-sm font-semibold shadow-md min-h-[44px]"
          rightIcon={<MessageSquare className="w-4 h-4" />}
        >
          I Want Something Similar
        </Button>

        <SaveToCollectionButton
          projectId={project.id}
          variant="button"
          className="min-h-[44px] justify-center px-4 text-sm font-medium"
        />

        <Button
          variant="outline"
          size="lg"
          onClick={handleShare}
          className="justify-center text-sm min-h-[44px]"
          leftIcon={copied ? <Check className="w-4 h-4 text-[var(--success)]" /> : <Share2 className="w-4 h-4 text-[var(--brand)]" />}
        >
          {copied ? 'Link Copied' : 'Share Project'}
        </Button>
      </div>

      {/* Sticky Mobile Conversion Bar at Bottom */}
      <div className="fixed bottom-0 inset-x-0 z-30 p-3 bg-[var(--surface)]/95 backdrop-blur-md border-t border-[var(--border)] sm:hidden flex items-center justify-between gap-3 shadow-lg pb-[max(0.75rem,env(safe-area-inset-bottom))]">
        <div className="truncate pr-2">
          <p className="text-xs font-semibold text-[var(--foreground)] truncate">{project.title}</p>
          <p className="text-[10px] text-[var(--muted)] truncate">{project.studioName}</p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={() => setEnquiryOpen(true)}
          className="text-xs font-semibold whitespace-nowrap flex-shrink-0 min-h-[40px] px-3"
          rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
        >
          I Want Similar
        </Button>
      </div>

      {/* Enquiry Modal */}
      <EnquirySheet
        isOpen={enquiryOpen}
        onClose={() => setEnquiryOpen(false)}
        project={project}
      />
    </>
  );
}
