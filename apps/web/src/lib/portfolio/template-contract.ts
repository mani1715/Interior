import React from 'react';
import {
  FontPairing,
  PortfolioTemplateKey,
  PreviewContactDto,
  PreviewSectionDto,
  PreviewServiceAreaDto,
  PreviewServiceDto,
  PreviewSpecialtyDto,
} from './types';

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
  publicContacts: PreviewContactDto[];
  canonicalServices: PreviewServiceDto[];
  canonicalSpecialties: PreviewSpecialtyDto[];
  canonicalServiceAreas: PreviewServiceAreaDto[];
  visibleSections: PreviewSectionDto[];
  isMobilePreview?: boolean;
}

export type PortfolioTemplateComponent = React.ComponentType<PortfolioTemplateProps>;

export interface TemplateDefinition {
  key: PortfolioTemplateKey;
  name: string;
  tagline: string;
  description: string;
  status: 'SCAFFOLD' | 'AVAILABLE' | 'DEPRECATED';
  designer: string;
  phase: string;
  component: PortfolioTemplateComponent;
}
