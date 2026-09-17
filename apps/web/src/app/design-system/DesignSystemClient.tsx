'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import {
  Sparkles,
  ArrowRight,
  Check,
  FolderKanban,
  Users,
  Eye,
  Trash2,
  Share2,
  ChevronDown,
  Info,
  Sliders,
  Bell,
} from 'lucide-react';

// Design System Components
import { Button } from '@/components/ui/Button';
import { StatusBadge, CategoryBadge, VerifiedBadge, AIConceptBadge, StatusVariant } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { ProjectCard, MetricCard } from '@/components/ui/Card';
import { FilterChip } from '@/components/ui/Filter';
import { DataTable, Column } from '@/components/ui/Table';

// Form Controls
import {
  TextInput,
  TextArea,
  Select,
  Checkbox,
  Radio,
  Switch,
  SearchInput,
} from '@/components/forms/FormControls';
import { FileDropzone } from '@/components/forms/FileDropzone';

// Feedback
import { Alert } from '@/components/feedback/Alert';
import { InlineMessage } from '@/components/feedback/InlineMessage';
import { ToastProvider, useToast } from '@/components/feedback/Toast';
import { Progress } from '@/components/feedback/Progress';
import { Spinner } from '@/components/feedback/Spinner';
import { EmptyState } from '@/components/feedback/EmptyState';

// Overlay
import { Dialog } from '@/components/overlay/Dialog';
import { ConfirmDialog } from '@/components/overlay/ConfirmDialog';
import { BottomSheet } from '@/components/overlay/BottomSheet';
import { DropdownMenu } from '@/components/overlay/DropdownMenu';
import { Tooltip } from '@/components/overlay/Tooltip';

// Layout
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';

// Media
import { BeforeAfterSlider } from '@/components/media/BeforeAfterSlider';
import { BeforeAiReality } from '@/components/media/BeforeAiReality';
import { WatermarkPreview } from '@/components/media/WatermarkPreview';

// Mock SVGs
import {
  rawSiteSvg,
  aiConceptSvg,
  executedSpaceSvg,
  projectImage1Svg,
  projectImage2Svg,
  projectImage3Svg,
} from './mockData';

interface ProjectRow {
  id: string;
  name: string;
  client: string;
  location: string;
  status: StatusVariant;
  updated: string;
}

const tableData: ProjectRow[] = [
  {
    id: 'p1',
    name: 'Penthouse 402',
    client: 'R. Chandra',
    location: 'Guntur',
    status: 'processing',
    updated: '2 hrs ago',
  },
  {
    id: 'p2',
    name: 'Japandi Villa',
    client: 'K. Ananya',
    location: 'Amaravati',
    status: 'published',
    updated: '1 day ago',
  },
  {
    id: 'p3',
    name: 'Executive Studio',
    client: 'V. Reddy',
    location: 'Hyderabad',
    status: 'draft',
    updated: '3 days ago',
  },
];

export function DesignSystemClient() {
  return (
    <ToastProvider>
      <DesignSystemContent />
    </ToastProvider>
  );
}

function DesignSystemContent() {
  const { showToast } = useToast();

  // Overlay states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);

  // Form states
  const [textVal, setTextVal] = useState('');
  const [selectVal, setSelectVal] = useState('modern');
  const [checkVal, setCheckVal] = useState(true);
  const [radioVal, setRadioVal] = useState('option1');
  const [switchVal, setSwitchVal] = useState(true);

  // Filter state
  const [activeFilter, setActiveFilter] = useState('all');

  const tableColumns: Column<ProjectRow>[] = [
    {
      header: 'Project Name',
      accessor: (row) => (
        <span className="font-semibold text-xs text-[var(--foreground)]">{row.name}</span>
      ),
    },
    {
      header: 'Client',
      accessor: 'client',
    },
    {
      header: 'Location',
      accessor: 'location',
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Updated',
      accessor: 'updated',
    },
    {
      header: 'Actions',
      accessor: () => (
        <Button variant="ghost" size="sm">
          View
        </Button>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] pb-24 selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
      {/* Top Banner */}
      <div className="bg-[var(--surface-raised)] border-b border-[var(--border)] py-4 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-serif font-bold text-lg shadow-sm">
              E
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-serif text-lg font-bold tracking-wide">ELÉGANCE</h1>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-[var(--brand-muted)] text-[var(--brand)] border border-[var(--brand)]/30">
                  PHASE 04 DESIGN SYSTEM
                </span>
              </div>
              <p className="text-xs text-[var(--muted)]">
                Mobile-First Architectural UI Foundations & Verified Color Tokens
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[11px] px-2.5 py-1 rounded-md bg-[var(--surface)] border border-[var(--border)] text-[var(--muted)] font-mono">
              360px • 390px • 430px • 768px • 1024px • 1440px+
            </span>
            <Button
              variant="outline"
              size="sm"
              leftIcon={<Bell className="w-3.5 h-3.5 text-[var(--brand)]" />}
              onClick={() =>
                showToast('success', 'WCAG 2.2 AA Contrast Verified (5.35:1 for CTA)', 'Audit Passed')
              }
            >
              Test Toast
            </Button>
          </div>
        </div>
      </div>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 space-y-16">
        {/* ======================================================== */}
        {/* 1. LOCKED COLOR SYSTEM & CONTRAST TOKENS */}
        {/* ======================================================== */}
        <section id="colors" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>01 / Color Architecture</SectionEyebrow>
              <SectionTitle>Locked Platform Palette</SectionTitle>
              <SectionDescription>
                Architectural neutrals anchored by Warm Bronze. Tested for strict WCAG 2.2 AA contrast compliance.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#B88A5A] shadow-inner flex items-center justify-center text-[#1F1F1F] font-semibold text-xs">
                #B88A5A
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Warm Bronze</p>
                <p className="text-[10px] text-[var(--muted)]">Brand Accent</p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#1F1F1F] shadow-inner flex items-center justify-center text-[#FAF9F5] font-semibold text-xs border border-white/10">
                #1F1F1F
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Deep Charcoal</p>
                <p className="text-[10px] text-[var(--muted)]">Dark Base / Contrast Text</p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#FAF9F5] shadow-inner flex items-center justify-center text-[#1F1F1F] font-semibold text-xs border border-black/10">
                #FAF9F5
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Warm White</p>
                <p className="text-[10px] text-[var(--muted)]">Light Base Background</p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#F5F2EB] shadow-inner flex items-center justify-center text-[#1F1F1F] font-semibold text-xs border border-black/10">
                #F5F2EB
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Alabaster</p>
                <p className="text-[10px] text-[var(--muted)]">Secondary Surface</p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#4D7C67] shadow-inner flex items-center justify-center text-white font-semibold text-xs">
                #4D7C67
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Sage Green</p>
                <p className="text-[10px] text-[var(--muted)]">Biophilic Accent</p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col justify-between h-32">
              <div className="w-full h-12 rounded-lg bg-[#C2614B] shadow-inner flex items-center justify-center text-white font-semibold text-xs">
                #C2614B
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">Terracotta</p>
                <p className="text-[10px] text-[var(--muted)]">Warm Earth Accent</p>
              </div>
            </div>
          </div>

          {/* Contrast Verification Callout */}
          <div className="p-4 rounded-xl border border-[var(--border)] bg-[var(--surface-raised)] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center flex-shrink-0 font-bold">
                <Check className="w-5 h-5 stroke-[2.5]" />
              </div>
              <div>
                <p className="text-xs font-semibold text-[var(--foreground)]">
                  WCAG 2.2 AA Contrast Guarantee: 5.35:1
                </p>
                <p className="text-xs text-[var(--muted)]">
                  Warm Bronze (<code className="text-[11px] font-mono">#B88A5A</code>) paired with Deep Charcoal (<code className="text-[11px] font-mono">#1F1F1F</code>) text achieves 5.35:1 contrast, comfortably exceeding the AA requirement (4.5:1).
                </p>
              </div>
            </div>
            <div className="px-4 py-2 rounded-lg bg-[var(--brand)] text-[var(--charcoal)] font-semibold text-xs whitespace-nowrap shadow-sm">
              Primary CTA Example (5.35:1)
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 2. TYPOGRAPHY SCALE */}
        {/* ======================================================== */}
        <section id="typography" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>02 / Typography</SectionEyebrow>
              <SectionTitle>Editorial & Interface Scale</SectionTitle>
              <SectionDescription>
                Playfair Display brings luxury architectural authority to titles; Inter ensures micro-clarity across mobile viewports.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] space-y-6 divide-y divide-[var(--border)]">
            <div className="pt-2 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Display (36px / 48px)</span>
              <p className="font-serif text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight flex-1">
                Curated Architecture & Interior Living
              </p>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Heading 1 (30px / 36px)</span>
              <h1 className="font-serif text-2xl sm:text-3xl font-semibold tracking-tight flex-1">
                The Heritage Penthouse Residence
              </h1>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Heading 2 (24px / 30px)</span>
              <h2 className="font-serif text-xl sm:text-2xl font-medium tracking-tight flex-1">
                Italian Statuario Marble & Walnut Accents
              </h2>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Heading 3 (20px / 24px)</span>
              <h3 className="font-serif text-lg sm:text-xl font-medium flex-1">
                Custom Architectural Lighting & Millwork
              </h3>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Body Large (16px)</span>
              <p className="text-base text-[var(--foreground)] flex-1 leading-relaxed">
                Every space is conceived through rigorous material authenticity, structural clarity, and biophilic balance tailored for modern life in Andhra Pradesh and Telangana.
              </p>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Body Normal (14px)</span>
              <p className="text-sm text-[var(--muted)] flex-1 leading-relaxed">
                Standard interface copy, labels, metadata notes, and specification descriptions rendered with crisp subpixel antialiasing.
              </p>
            </div>

            <div className="pt-6 flex flex-col sm:flex-row sm:items-baseline justify-between gap-2">
              <span className="text-xs font-mono text-[var(--muted)] sm:w-44">Caption & Eyebrow (12px)</span>
              <div className="flex-1 space-y-1">
                <p className="text-xs font-semibold uppercase tracking-widest text-[var(--brand)]">
                  PORTFOLIO ARCHIVE • 2026
                </p>
                <p className="text-[11px] text-[var(--muted)]">
                  Secondary microcopy and disclaimer labels.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 3. BUTTONS & INTERACTIVE CONTROLS */}
        {/* ======================================================== */}
        <section id="buttons" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>03 / Interactive Controls</SectionEyebrow>
              <SectionTitle>Button Matrix & Touch Targets</SectionTitle>
              <SectionDescription>
                Strict minimum 44px touch targets on mobile viewports. Clear hover, active, focus-visible, and loading states.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] space-y-6">
            {/* Variants */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                Variants (Medium Size)
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <Button variant="primary">Primary Bronze</Button>
                <Button variant="secondary">Secondary Dark</Button>
                <Button variant="outline">Outline</Button>
                <Button variant="ghost">Ghost</Button>
                <Button variant="danger">Danger</Button>
                <Button variant="link">Inline Link</Button>
              </div>
            </div>

            {/* Sizes */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                Sizes (Sm / Md / Lg)
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <Button variant="primary" size="sm">Small (36px min)</Button>
                <Button variant="primary" size="md">Medium (44px standard)</Button>
                <Button variant="primary" size="lg">Large (50px prominent)</Button>
              </div>
            </div>

            {/* Icons & Loading States */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                States & Icons
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <Button variant="primary" leftIcon={<Sparkles className="w-4 h-4" />}>
                  AI Render
                </Button>
                <Button
                  variant="outline"
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  View Details
                </Button>
                <Button variant="primary" isLoading>
                  Generating Concept
                </Button>
                <Button variant="outline" disabled>
                  Disabled State
                </Button>
              </div>
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 4. FORM CONTROLS & FILE UPLOAD */}
        {/* ======================================================== */}
        <section id="forms" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>04 / Forms & Data Input</SectionEyebrow>
              <SectionTitle>Accessible Form Primitives</SectionTitle>
              <SectionDescription>
                Labels, helper text, error styling, touch targets, and mobile camera photo capture.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              <TextInput
                label="Project Title"
                placeholder="e.g. Luxury 3BHK Residence"
                value={textVal}
                onChange={(e) => setTextVal(e.target.value)}
                helperText="Enter official portfolio project name"
              />

              <Select
                label="Design Style"
                value={selectVal}
                onChange={(e) => setSelectVal(e.target.value)}
                options={[
                  { label: 'Modern Architectural', value: 'modern' },
                  { label: 'Japandi & Minimalist', value: 'japandi' },
                  { label: 'Neo-Classical Luxury', value: 'classical' },
                  { label: 'Industrial Fluted', value: 'industrial' },
                ]}
              />

              <SearchInput
                label="Search Catalog"
                placeholder="Search materials, finishes..."
              />
            </div>

            <TextArea
              label="Scope of Work & Client Brief"
              placeholder="Describe layout changes, false ceiling requirements, and custom joinery..."
              rows={3}
              helperText="Markdown supported for project summaries."
            />

            {/* Checkbox, Radio, Switch */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 pt-4 border-t border-[var(--border)]">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                  Checkboxes
                </p>
                <div className="space-y-2">
                  <Checkbox
                    label="Enable client watermarking"
                    checked={checkVal}
                    onChange={(e) => setCheckVal(e.target.checked)}
                  />
                  <Checkbox
                    label="Featured in public portfolio"
                    checked={false}
                    onChange={() => {}}
                  />
                </div>
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                  Radio Choices
                </p>
                <div className="space-y-2">
                  <Radio
                    name="scopeRadio"
                    label="Full Turnkey Execution"
                    checked={radioVal === 'option1'}
                    onChange={() => setRadioVal('option1')}
                  />
                  <Radio
                    name="scopeRadio"
                    label="Design Only Consultation"
                    checked={radioVal === 'option2'}
                    onChange={() => setRadioVal('option2')}
                  />
                </div>
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                  Switches
                </p>
                <div className="space-y-3">
                  <Switch
                    label="AI Concept Generation"
                    checked={switchVal}
                    onChange={setSwitchVal}
                  />
                  <Switch
                    label="Strict Watermark Enforcement"
                    checked={true}
                    onChange={() => {}}
                  />
                </div>
              </div>
            </div>

            {/* Mobile File Dropzone with Camera */}
            <div className="pt-4 border-t border-[var(--border)]">
              <FileDropzone
                label="Site Reference Photography"
                helperText="Upload JPG/PNG site photos or capture live with mobile camera"
              />
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 5. FEEDBACK, ALERTS & PROGRESS */}
        {/* ======================================================== */}
        <section id="feedback" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>05 / System Feedback</SectionEyebrow>
              <SectionTitle>Alerts, Spinners & Progress Indicators</SectionTitle>
              <SectionDescription>
                Unobtrusive status indicators, progress meters, and contextual inline notifications.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Alert variant="info" title="Site Inspection Scheduled">
                Architectural walkthrough confirmed for tomorrow at 10:30 AM IST.
              </Alert>

              <Alert variant="success" title="Watermark Applied">
                Studio branding and security hash embedded into 14 project images.
              </Alert>

              <Alert variant="warning" title="Storage Allocation 85%">
                Free tier media limit approaching. Upgrade to Studio Pro for unlimited 4K storage.
              </Alert>

              <Alert variant="error" title="Resolution Below 1080p">
                Asset rejected. Source images must meet minimum 1920x1080 for portfolio indexing.
              </Alert>
            </div>

            {/* Inline Messages */}
            <div className="flex flex-wrap gap-4 pt-4 border-t border-[var(--border)]">
              <InlineMessage variant="info">Changes auto-saved to cloud</InlineMessage>
              <InlineMessage variant="success">Specification sheet generated</InlineMessage>
              <InlineMessage variant="warning">Low resolution warning</InlineMessage>
              <InlineMessage variant="error">Required field missing</InlineMessage>
            </div>

            {/* Progress & Spinners */}
            <div className="pt-4 border-t border-[var(--border)] grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div className="space-y-3">
                <Progress value={72} label="AI Concept Diffusion" showPercentage />
                <Progress value={100} label="Asset Processing" variant="success" showPercentage />
              </div>
              <div className="flex items-center gap-6">
                <div className="flex items-center gap-2 text-xs text-[var(--muted)]">
                  <Spinner size="sm" />
                  <span>Small</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-[var(--muted)]">
                  <Spinner size="md" />
                  <span>Medium</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-[var(--muted)]">
                  <Spinner size="lg" />
                  <span>Large</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 6. BADGES, TAGS & MANDATORY AI DISCLAIMER */}
        {/* ======================================================== */}
        <section id="badges" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>06 / Badges & Status</SectionEyebrow>
              <SectionTitle>Status Badges & Mandatory AI Label</SectionTitle>
              <SectionDescription>
                Standardized tags for workflow states and required legal transparency for AI-generated renderings.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] space-y-6">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                Mandatory AI Rendering Disclaimer Badge (System Required)
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <AIConceptBadge />
                <span className="text-xs text-[var(--muted)]">
                  (Required on all simulated views to prevent deceptive marketing)
                </span>
              </div>
            </div>

            <div className="pt-4 border-t border-[var(--border)]">
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                Project & Workflow Status Badges
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <StatusBadge status="draft" />
                <StatusBadge status="processing" label="Site Execution" />
                <StatusBadge status="published" />
                <StatusBadge status="ready" />
                <StatusBadge status="failed" />
                <VerifiedBadge />
                <CategoryBadge name="Living Room" />
                <CategoryBadge name="Modular Kitchen" />
                <CategoryBadge name="Master Suite" />
              </div>
            </div>

            <div className="pt-4 border-t border-[var(--border)]">
              <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-3">
                Interactive Filter Chips
              </p>
              <div className="flex flex-wrap gap-2">
                {['All Spaces', 'Living & TV Units', 'Kitchens', 'Bedrooms', 'Penthouses'].map((f) => (
                  <FilterChip
                    key={f}
                    label={f}
                    isSelected={activeFilter === f || (f === 'All Spaces' && activeFilter === 'all')}
                    onToggle={() => setActiveFilter(f === 'All Spaces' ? 'all' : f)}
                  />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* ======================================================== */}
        {/* 7. CARDS & METRICS */}
        {/* ======================================================== */}
        <section id="cards" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>07 / Cards & Containers</SectionEyebrow>
              <SectionTitle>Project Cards & Studio Metrics</SectionTitle>
              <SectionDescription>
                Mobile-optimized cards with high information density, touch feedback, and image containment.
              </SectionDescription>
            </div>
          </SectionHeader>

          {/* Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <MetricCard
              label="Active Projects"
              value="18"
              change="+3 this month"
              isPositive
              icon={<FolderKanban className="w-5 h-5 text-[var(--brand)]" />}
            />
            <MetricCard
              label="Client Inquiries"
              value="42"
              change="+14% vs last mo"
              isPositive
              icon={<Users className="w-5 h-5 text-[var(--success)]" />}
            />
            <MetricCard
              label="Portfolio Views"
              value="4.8K"
              change="Top 5% in Guntur"
              isPositive
              icon={<Eye className="w-5 h-5 text-[var(--info)]" />}
            />
            <MetricCard
              label="AI Visualizations"
              value="126"
              change="98% approval rate"
              isPositive
              icon={<Sparkles className="w-5 h-5 text-[var(--brand)]" />}
            />
          </div>

          {/* Project Cards Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 pt-4">
            <ProjectCard
              title="Modern TV Console & Fluted Paneling"
              location="Guntur, Andhra Pradesh"
              coverImage={
                <Image
                  src={projectImage1Svg}
                  alt="Modern TV Console"
                  fill
                  className="object-cover"
                />
              }
              category="Living Room"
              badge={<StatusBadge status="published" />}
            />
            <ProjectCard
              title="Warm Oak Modular Kitchen with Island"
              location="Amaravati Capital Region"
              coverImage={
                <Image
                  src={projectImage2Svg}
                  alt="Warm Oak Kitchen"
                  fill
                  className="object-cover"
                />
              }
              category="Kitchen"
              badge={<StatusBadge status="processing" label="In Progress" />}
            />
            <ProjectCard
              title="Fluted Master Suite & Walk-in Wardrobe"
              location="Banjara Hills, Hyderabad"
              coverImage={
                <Image
                  src={projectImage3Svg}
                  alt="Fluted Master Suite"
                  fill
                  className="object-cover"
                />
              }
              category="Bedroom"
              badge={<StatusBadge status="ready" />}
            />
          </div>
        </section>

        {/* ======================================================== */}
        {/* 8. OVERLAYS, MODALS & BOTTOM SHEETS */}
        {/* ======================================================== */}
        <section id="overlays" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>08 / Overlay Systems</SectionEyebrow>
              <SectionTitle>Dialogs, Sheets & Menus</SectionTitle>
              <SectionDescription>
                Accessible modal dialogues with keyboard traps, escape keys, and mobile-friendly slide-up bottom sheets.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] flex flex-wrap items-center gap-4">
            <Button variant="primary" onClick={() => setDialogOpen(true)}>
              Open Standard Modal
            </Button>
            <Button variant="danger" onClick={() => setConfirmOpen(true)}>
              Open Destructive Dialog
            </Button>
            <Button variant="outline" onClick={() => setSheetOpen(true)}>
              Open Mobile Bottom Sheet
            </Button>

            {/* Dropdown Menu */}
            <DropdownMenu
              trigger={
                <Button variant="secondary" rightIcon={<ChevronDown className="w-4 h-4" />}>
                  Actions Menu
                </Button>
              }
              items={[
                { label: 'Edit Project', icon: <Sliders className="w-4 h-4" />, onClick: () => {} },
                { label: 'Share Portfolio', icon: <Share2 className="w-4 h-4" />, onClick: () => {} },
                { label: 'Delete Project', icon: <Trash2 className="w-4 h-4" />, onClick: () => {}, danger: true },
              ]}
            />

            {/* Tooltip */}
            <Tooltip content="Verified Studio Certificate (ISO 9001)">
              <div className="p-2 rounded-lg border border-[var(--border)] bg-[var(--surface-raised)] cursor-help text-xs text-[var(--muted)] flex items-center gap-1.5">
                <Info className="w-4 h-4 text-[var(--brand)]" />
                <span>Hover / Tap for Tooltip</span>
              </div>
            </Tooltip>
          </div>

          {/* Dialog Component Instances */}
          <Dialog
            isOpen={dialogOpen}
            onClose={() => setDialogOpen(false)}
            title="Create New Design Project"
            description="Set up your project workspace and client privacy parameters."
            footer={
              <>
                <Button variant="outline" size="sm" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" onClick={() => setDialogOpen(false)}>
                  Save Project
                </Button>
              </>
            }
          >
            <div className="space-y-4">
              <TextInput label="Project Name" placeholder="e.g. Kondapur Villa" />
              <TextInput label="Client Name" placeholder="e.g. Ramesh Chandra" />
              <p className="text-xs text-[var(--muted)]">
                All client media will be encrypted and tagged with studio watermarks by default.
              </p>
            </div>
          </Dialog>

          <ConfirmDialog
            isOpen={confirmOpen}
            onClose={() => setConfirmOpen(false)}
            onConfirm={() => {
              setConfirmOpen(false);
              showToast('error', 'Project asset deleted', 'Removed');
            }}
            title="Delete High-Resolution Render?"
            description="This will permanently delete the 4K render and cannot be undone."
          />

          <BottomSheet
            isOpen={sheetOpen}
            onClose={() => setSheetOpen(false)}
            title="Project Actions"
            footer={
              <Button variant="outline" size="md" className="w-full" onClick={() => setSheetOpen(false)}>
                Dismiss
              </Button>
            }
          >
            <div className="space-y-3">
              <button
                type="button"
                className="w-full p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-left text-sm font-medium hover:bg-[var(--surface-raised)] flex items-center justify-between"
                onClick={() => setSheetOpen(false)}
              >
                <span>Download Watermarked PDF Presentation</span>
                <ArrowRight className="w-4 h-4 text-[var(--muted)]" />
              </button>
              <button
                type="button"
                className="w-full p-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-left text-sm font-medium hover:bg-[var(--surface-raised)] flex items-center justify-between"
                onClick={() => setSheetOpen(false)}
              >
                <span>Generate New AI Concepts (3 Remaining)</span>
                <Sparkles className="w-4 h-4 text-[var(--brand)]" />
              </button>
            </div>
          </BottomSheet>
        </section>

        {/* ======================================================== */}
        {/* 9. MEDIA & VISUAL STORYTELLING */}
        {/* ======================================================== */}
        <section id="media" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>09 / Visual Storytelling & Media</SectionEyebrow>
              <SectionTitle>Before/After Slider & Progression Narrative</SectionTitle>
              <SectionDescription>
                Interactive touch comparison sliders, full progression stages (Site → AI Concept → Handcrafted Reality), and watermark controls.
              </SectionDescription>
            </div>
          </SectionHeader>

          {/* Interactive Before/After Slider */}
          <div className="space-y-2">
            <h3 className="font-serif text-base font-semibold text-[var(--foreground)]">
              Interactive Before / After Renovation Slider
            </h3>
            <p className="text-xs text-[var(--muted)]">
              Drag the center slider or use Left / Right keyboard arrow keys to compare raw site condition vs finished reality.
            </p>
            <div className="max-w-3xl mx-auto pt-2">
              <BeforeAfterSlider
                beforeImage={rawSiteSvg}
                afterImage={executedSpaceSvg}
                beforeLabel="Site Rough-In"
                afterLabel="Finished Living Room"
                aspectRatio="16/9"
              />
            </div>
          </div>

          {/* 3-Stage Visual Progression Narrative */}
          <div className="pt-6">
            <BeforeAiReality
              title="From Raw Site to AI Concept to Built Reality"
              subtitle="Full architectural progression featuring transparent AI concept tagging."
              beforeImage={rawSiteSvg}
              aiImage={aiConceptSvg}
              realityImage={executedSpaceSvg}
              beforeDescription="Unfinished concrete shell with electrical conduit layout prior to execution."
              aiDescription="AI generated photorealistic lighting study exploring warm walnut and indirect cove simulation."
              realityDescription="Turnkey hand-built execution matching the approved visualization with real Statuario marble."
            />
          </div>

          {/* Studio Watermark Engine */}
          <div className="pt-6">
            <WatermarkPreview
              sampleImage={executedSpaceSvg}
              defaultStudioName="STUDIO ELÉGANCE • ARCHITECTURE & INTERIORS"
            />
          </div>
        </section>

        {/* ======================================================== */}
        {/* 10. EMPTY STATES */}
        {/* ======================================================== */}
        <section id="empty" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>10 / Empty States</SectionEyebrow>
              <SectionTitle>Clean Architectural Empty States</SectionTitle>
              <SectionDescription>
                Thoughtful fallbacks when collections, search results, or leads are empty.
              </SectionDescription>
            </div>
          </SectionHeader>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <EmptyState
              icon={<FolderKanban className="w-7 h-7" />}
              title="No Projects in Portfolio Yet"
              description="Upload your first architectural project or generate an AI concept to kickstart your public showcase."
              actionLabel="Create Project"
              onAction={() => showToast('info', 'Opening project wizard...', 'New Project')}
              secondaryActionLabel="Explore Templates"
            />

            <EmptyState
              icon={<Sparkles className="w-7 h-7" />}
              title="No AI Visualizations Generated"
              description="Transform raw site photos into luxury concepts in seconds using our dedicated studio visualizer."
              actionLabel="Launch AI Studio"
              onAction={() => showToast('info', 'Redirecting to AI Studio...', 'AI Studio')}
            />
          </div>
        </section>

        {/* ======================================================== */}
        {/* 11. RESPONSIVE DATA TABLE */}
        {/* ======================================================== */}
        <section id="tables" className="space-y-6">
          <SectionHeader>
            <div>
              <SectionEyebrow>11 / Data Presentation</SectionEyebrow>
              <SectionTitle>Responsive Project Management Table</SectionTitle>
              <SectionDescription>
                Horizontal scroll on mobile, sortable headers, and status badges.
              </SectionDescription>
            </div>
          </SectionHeader>

          <DataTable
            data={tableData}
            columns={tableColumns}
            keyExtractor={(item) => item.id}
          />
        </section>
      </main>
    </div>
  );
}
