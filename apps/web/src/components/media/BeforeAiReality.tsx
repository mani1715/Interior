'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { AIConceptBadge, StatusBadge } from '@/components/ui/Badge';
import { Sparkles, Camera, CheckCircle2 } from 'lucide-react';

export interface BeforeAiRealityProps {
  title?: string;
  subtitle?: string;
  beforeImage: string;
  aiImage: string;
  realityImage: string;
  beforeDescription?: string;
  aiDescription?: string;
  realityDescription?: string;
  className?: string;
}

export function BeforeAiReality({
  title = 'From Concept to Execution',
  subtitle = 'The complete architectural journey from raw site to AI rendering to finished space.',
  beforeImage,
  aiImage,
  realityImage,
  beforeDescription = 'Raw unfinished shell before structural and aesthetic intervention.',
  aiDescription = 'AI generated interior concept exploring walnut fluting and warm cove lighting.',
  realityDescription = 'Final built reality delivered on-site matching design specifications.',
  className = '',
}: BeforeAiRealityProps) {
  const [activeStep, setActiveStep] = useState<0 | 1 | 2>(1);

  const steps = [
    {
      label: '1. Raw Site',
      badge: <StatusBadge status="draft" label="Site Condition" />,
      image: beforeImage,
      description: beforeDescription,
      icon: Camera,
    },
    {
      label: '2. AI Visualization',
      badge: <AIConceptBadge />,
      image: aiImage,
      description: aiDescription,
      icon: Sparkles,
    },
    {
      label: '3. Executed Space',
      badge: <StatusBadge status="published" label="Built Reality" />,
      image: realityImage,
      description: realityDescription,
      icon: CheckCircle2,
    },
  ];

  return (
    <div className={`w-full rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 sm:p-6 lg:p-8 ${className}`}>
      {(title || subtitle) && (
        <div className="mb-6 text-center max-w-xl mx-auto">
          {title && (
            <h3 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-1.5">
              {title}
            </h3>
          )}
          {subtitle && <p className="text-xs sm:text-sm text-[var(--muted)]">{subtitle}</p>}
        </div>
      )}

      {/* Mobile Step Selector (Tabs) */}
      <div className="flex sm:hidden p-1 rounded-xl bg-[var(--surface-sunken)] border border-[var(--border)] mb-4">
        {steps.map((step, idx) => (
          <button
            key={step.label}
            type="button"
            onClick={() => setActiveStep(idx as 0 | 1 | 2)}
            className={`flex-1 py-2 text-xs font-medium rounded-lg transition-all text-center min-h-[44px] flex items-center justify-center gap-1.5 ${
              activeStep === idx
                ? 'bg-[var(--surface-raised)] text-[var(--brand)] font-semibold shadow-sm'
                : 'text-[var(--muted)]'
            }`}
          >
            <span>{step.label.split(' ')[1]}</span>
          </button>
        ))}
      </div>

      {/* Mobile Focused View */}
      <div className="block sm:hidden">
        <div className="relative aspect-[4/3] rounded-xl overflow-hidden mb-3 border border-[var(--border)]">
          <Image
            src={steps[activeStep].image}
            alt={steps[activeStep].label}
            fill
            className="object-cover"
          />
          <div className="absolute top-3 left-3">{steps[activeStep].badge}</div>
        </div>
        <div className="p-2">
          <h4 className="font-semibold text-sm text-[var(--foreground)] mb-1">
            {steps[activeStep].label}
          </h4>
          <p className="text-xs text-[var(--muted)] leading-relaxed">
            {steps[activeStep].description}
          </p>
        </div>
      </div>

      {/* Desktop / Tablet 3-Column Grid */}
      <div className="hidden sm:grid sm:grid-cols-3 gap-4 lg:gap-6">
        {steps.map((step) => {
          const Icon = step.icon;
          return (
            <div
              key={step.label}
              className="flex flex-col rounded-xl border border-[var(--border)] bg-[var(--surface-raised)] overflow-hidden shadow-sm hover:border-[var(--border-strong)] transition-all"
            >
              <div className="relative aspect-[4/3] w-full overflow-hidden bg-[var(--surface-sunken)]">
                <Image
                  src={step.image}
                  alt={step.label}
                  fill
                  sizes="(max-width: 1024px) 33vw, 25vw"
                  className="object-cover"
                />
                <div className="absolute top-3 left-3">{step.badge}</div>
              </div>
              <div className="p-4 flex-1 flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1.5">
                    <Icon className="w-4 h-4 text-[var(--brand)]" />
                    <h4 className="font-medium text-sm text-[var(--foreground)]">{step.label}</h4>
                  </div>
                  <p className="text-xs text-[var(--muted)] leading-relaxed">{step.description}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
