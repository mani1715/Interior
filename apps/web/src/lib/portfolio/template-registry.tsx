import React from 'react';
import { PortfolioTemplateKey, CANONICAL_SECTION_TYPES } from './types';
import { PortfolioTemplateProps, TemplateDefinition } from './template-contract';
import { ReferenceTemplate } from '@/components/portfolio/templates/ReferenceTemplate';
import { BasicTemplate } from '@/components/portfolio/templates/basic/BasicTemplate';
import { ModernTemplate } from '@/components/portfolio/templates/modern/ModernTemplate';
import { LuxuryTemplate } from '@/components/portfolio/templates/luxury/LuxuryTemplate';
import { ArchitecturalTemplate } from '@/components/portfolio/templates/architectural/ArchitecturalTemplate';
import { WarmNaturalTemplate } from '@/components/portfolio/templates/warm-natural/WarmNaturalTemplate';
import { DarkCinematicTemplate } from '@/components/portfolio/templates/dark-cinematic/DarkCinematicTemplate';

function createScaffoldWrapper(
  name: string,
  phase: string,
  accentFallback: string
): React.FC<PortfolioTemplateProps> {
  const ScaffoldComponent: React.FC<PortfolioTemplateProps> = (props) => {
    return (
      <div className="relative">
        <div className="bg-sand-100 border-b border-sand-200 px-4 py-2 text-center text-xs text-charcoal-700 font-mono flex items-center justify-center gap-2">
          <span className="w-2 h-2 rounded-full bg-bronze-600 animate-pulse" />
          <span>Astra Theme Engine Scaffold: <strong>{name}</strong> ({phase})</span>
          <span className="text-charcoal-500 text-[11px]">— Content preserved across all themes</span>
        </div>
        <ReferenceTemplate
          {...props}
          accentColor={props.accentColor || accentFallback}
        />
      </div>
    );
  };
  ScaffoldComponent.displayName = `Scaffold_${name.replace(/\s+/g, '_')}`;
  return ScaffoldComponent;
}

export const TEMPLATE_REGISTRY: Record<PortfolioTemplateKey, TemplateDefinition> = {
  BASIC: {
    key: 'BASIC',
    name: 'Clean Editorial',
    tagline: 'Simple, typography-driven portfolio for emerging designers',
    description: 'Clean whitespace, classic layout, and direct visual hierarchy prioritizing core practice information.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 11)',
    phase: 'Phase 11',
    component: BasicTemplate,
  },
  MODERN: {
    key: 'MODERN',
    name: 'Modern Minimalist',
    tagline: 'Asymmetric grids, bold headlines, and sleek contemporary lines',
    description: 'Generous white space, high-contrast typography, and avant-garde layout for modern design practices.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 12)',
    phase: 'Phase 12',
    component: ModernTemplate,
  },
  LUXURY: {
    key: 'LUXURY',
    name: 'Luxury Atelier',
    tagline: 'Opulent serif typography, warm gold accents, and magazine-like editorial pacing',
    description: 'Tailored for high-end residential interior studios catering to ultra-high-net-worth clients.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 13)',
    phase: 'Phase 13',
    component: LuxuryTemplate,
  },
  ARCHITECTURAL: {
    key: 'ARCHITECTURAL',
    name: 'Architectural Monograph',
    tagline: 'Structured modular grid, technical drafting aesthetics, and geometric rigor',
    description: 'Engineered for spatial design, turnkey architecture, and interior construction studios.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 14)',
    phase: 'Phase 14',
    component: ArchitecturalTemplate,
  },
  WARM_NATURAL: {
    key: 'WARM_NATURAL',
    name: 'Warm & Natural',
    tagline: 'Earthy terracotta tones, soft organic curves, and biophilic tactile focus',
    description: 'Suited for sustainable, slow-living, and artisanal interior design studios.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 15)',
    phase: 'Phase 15',
    component: WarmNaturalTemplate,
  },
  DARK_CINEMATIC: {
    key: 'DARK_CINEMATIC',
    name: 'Dark Cinematic',
    tagline: 'Deep obsidian backgrounds, dramatic lighting highlights, and high-glamour finishes',
    description: 'Immersive visual aesthetic for hospitality, lounge, and dramatic residential spaces.',
    version: '1.0.0',
    status: 'AVAILABLE',
    isSelectable: true,
    supportedSections: CANONICAL_SECTION_TYPES,
    designer: 'Astra (Phase 16)',
    phase: 'Phase 16',
    component: DarkCinematicTemplate,
  },
};

export function getTemplateDefinition(key: PortfolioTemplateKey): TemplateDefinition {
  return TEMPLATE_REGISTRY[key] || TEMPLATE_REGISTRY.BASIC;
}

export function getAllTemplates(): TemplateDefinition[] {
  return Object.values(TEMPLATE_REGISTRY);
}
