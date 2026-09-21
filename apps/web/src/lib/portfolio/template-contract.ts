import React from 'react';
import {
  FontPairing,
  PortfolioTemplateKey,
  PreviewContactDto,
  PreviewSectionDto,
  PreviewServiceAreaDto,
  PreviewServiceDto,
  PreviewSpecialtyDto,
  SectionType,
} from './types';

export interface NavItem {
  sectionId: string;
  sectionType: SectionType;
  label: string;
  anchor: string;
}

export interface NavigationSettings {
  sticky: boolean;
  navItems: NavItem[];
  showPrimaryCta: boolean;
  primaryCtaLabel: string;
  primaryCtaAnchor: string;
}

export interface PortfolioTemplateProps {
  portfolioId: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  professionalType?: string;
  professionalTitle?: string;
  studioCity?: string;
  studioState?: string;
  templateKey: PortfolioTemplateKey;
  headline?: string | null;
  subheadline?: string | null;
  bio?: string | null;
  designPhilosophy?: string | null;
  yearsOfExperience?: number | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  fontPairing?: FontPairing;
  navigationSettings: NavigationSettings;
  publicContacts: PreviewContactDto[];
  canonicalServices: PreviewServiceDto[];
  canonicalSpecialties: PreviewSpecialtyDto[];
  canonicalServiceAreas: PreviewServiceAreaDto[];
  visibleSections: PreviewSectionDto[];
  portfolioProjects?: import('../projects/types').ProjectPresentationDto[];
  isMobilePreview?: boolean;
}

export type PortfolioTemplateComponent = React.ComponentType<PortfolioTemplateProps>;

export interface TemplateDefinition {
  key: PortfolioTemplateKey;
  name: string;
  tagline: string;
  description: string;
  version: string;
  status: 'SCAFFOLD' | 'AVAILABLE' | 'DEPRECATED';
  isSelectable: boolean;
  supportedSections: SectionType[];
  designer: string;
  phase: string;
  component: PortfolioTemplateComponent;
}
