export type PortfolioTemplateKey =
  | 'BASIC'
  | 'MODERN'
  | 'LUXURY'
  | 'ARCHITECTURAL'
  | 'WARM_NATURAL'
  | 'DARK_CINEMATIC';

export type PortfolioStatus = 'DRAFT' | 'READY' | 'UNPUBLISHED';

export type FontPairing =
  | 'SYSTEM_SANS'
  | 'CLASSIC_SERIF'
  | 'MODERN_CLEAN'
  | 'EDITORIAL'
  | 'WARM_EDITORIAL'
  | 'BOLD_CINEMATIC';

export type SectionType =
  | 'HERO'
  | 'ABOUT'
  | 'SERVICES'
  | 'FEATURED_PROJECTS'
  | 'PROJECT_GRID'
  | 'BEFORE_AFTER'
  | 'BEFORE_AI_REALITY'
  | 'DESIGN_PROCESS'
  | 'TESTIMONIALS'
  | 'TEAM'
  | 'AWARDS'
  | 'PRESS'
  | 'SERVICE_AREAS'
  | 'FAQ'
  | 'CONTACT'
  | 'CTA'
  | 'VIDEO'
  | 'CUSTOM_NOTE';

export const CANONICAL_SECTION_TYPES: SectionType[] = [
  'HERO',
  'ABOUT',
  'SERVICES',
  'FEATURED_PROJECTS',
  'PROJECT_GRID',
  'BEFORE_AFTER',
  'BEFORE_AI_REALITY',
  'DESIGN_PROCESS',
  'TESTIMONIALS',
  'TEAM',
  'AWARDS',
  'PRESS',
  'SERVICE_AREAS',
  'FAQ',
  'CONTACT',
  'CTA',
  'VIDEO',
  'CUSTOM_NOTE',
];

export const CANONICAL_FONT_PAIRINGS: FontPairing[] = [
  'SYSTEM_SANS',
  'CLASSIC_SERIF',
  'MODERN_CLEAN',
  'EDITORIAL',
  'WARM_EDITORIAL',
  'BOLD_CINEMATIC',
];

// Typed Section Content Models matching Backend Validator
export interface HeroSectionContent {
  badgeText?: string;
  headlineOverride?: string;
  subheadlineOverride?: string;
  ctaText?: string;
  ctaLink?: string;
}

export interface AboutSectionContent {
  narrativeOverride?: string;
  philosophyOverride?: string;
  quote?: string;
}

export interface ServicesSectionContent {
  sectionHeadline?: string;
  sectionDescription?: string;
}

export interface ServiceAreasSectionContent {
  sectionHeadline?: string;
  coverageNote?: string;
}

export interface ContactSectionContent {
  contactIntro?: string;
  preferredChannel?: string;
  inquiryFormEnabled?: boolean;
}

export interface CtaSectionContent {
  headline?: string;
  description?: string;
  buttonText?: string;
  buttonLink?: string;
}

export interface DesignProcessSectionContent {
  sectionHeadline?: string;
  steps?: Array<{ step: string; title: string; desc: string }>;
}

export interface TestimonialsSectionContent {
  sectionHeadline?: string;
  items?: Array<{ clientName: string; quote: string; projectLocation?: string }>;
}

export interface FaqSectionContent {
  sectionHeadline?: string;
  items?: Array<{ question: string; answer: string }>;
}

export interface TeamSectionContent {
  sectionHeadline?: string;
  members?: Array<{ name: string; role: string; bio?: string }>;
}

export interface CustomNoteSectionContent {
  title?: string;
  body?: string;
}

export interface PortfolioSectionDto {
  id: string;
  sectionType: SectionType;
  displayOrder: number;
  isVisible: boolean;
  schemaVersion: number;
  content: Record<string, any>;
  updatedAt: string;
}

export interface PortfolioVersionDto {
  id: string;
  versionNumber: number;
  label: string;
  createdBy: string;
  createdAt: string;
}

export interface PortfolioDetailResponse {
  id: string;
  studioId: string;
  templateKey: PortfolioTemplateKey;
  status: PortfolioStatus;
  headline: string | null;
  subheadline: string | null;
  bio: string | null;
  designPhilosophy: string | null;
  yearsOfExperience: number | null;
  primaryColor: string | null;
  secondaryColor: string | null;
  accentColor: string | null;
  fontPairing: FontPairing;
  version: number;
  isReadyForPublish: boolean;
  readinessMissingRequirements: string[];
  sections: PortfolioSectionDto[];
  recentVersions: PortfolioVersionDto[];
  createdAt: string;
  updatedAt: string;
}

export interface PreviewContactDto {
  kind: string;
  contactValue: string;
}

export interface PreviewServiceDto {
  serviceCode: string;
  serviceName: string;
}

export interface PreviewSpecialtyDto {
  specialtyCode: string;
  specialtyName: string;
}

export interface PreviewServiceAreaDto {
  cityName: string;
  locality: string;
}

export interface PreviewSectionDto {
  sectionId: string;
  sectionType: SectionType;
  displayOrder: number;
  schemaVersion: number;
  content: Record<string, any>;
}

export interface PortfolioPreviewResponse {
  portfolioId: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  professionalType: string;
  professionalTitle: string;
  studioCity: string;
  studioState: string;
  templateKey: PortfolioTemplateKey;
  status: PortfolioStatus;
  headline: string | null;
  subheadline: string | null;
  bio: string | null;
  designPhilosophy: string | null;
  yearsOfExperience: number | null;
  primaryColor: string | null;
  secondaryColor: string | null;
  accentColor: string | null;
  fontPairing: FontPairing;
  publicContacts: PreviewContactDto[];
  canonicalServices: PreviewServiceDto[];
  canonicalSpecialties: PreviewSpecialtyDto[];
  canonicalServiceAreas: PreviewServiceAreaDto[];
  visibleSections: PreviewSectionDto[];
  previewGeneratedAt: string;
}

// Canonical Request DTOs with mandatory optimistic concurrency version
export interface InitializePortfolioRequest {
  templateKey?: PortfolioTemplateKey;
}

export interface UpdatePortfolioRequest {
  headline?: string | null;
  subheadline?: string | null;
  bio?: string | null;
  designPhilosophy?: string | null;
  yearsOfExperience?: number | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  fontPairing?: FontPairing;
  status?: PortfolioStatus;
  version: number;
}

export interface UpdateSectionRequest {
  isVisible?: boolean;
  content?: Record<string, any>;
  version: number;
}

export interface ReorderSectionsRequest {
  sectionIds: string[];
  version: number;
}

export interface SwitchTemplateRequest {
  templateKey: PortfolioTemplateKey;
  version: number;
}

export interface CreateVersionSnapshotRequest {
  label: string;
  version: number;
}

export interface RestoreVersionRequest {
  version: number;
}
