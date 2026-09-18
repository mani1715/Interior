export type PortfolioTemplateKey =
  | 'BASIC'
  | 'MODERN'
  | 'LUXURY'
  | 'ARCHITECTURAL'
  | 'WARM_NATURAL'
  | 'DARK_CINEMATIC';

export type PortfolioStatus = 'DRAFT' | 'READY' | 'UNPUBLISHED';

export type FontPairing =
  | 'PLAYFAIR_INTER'
  | 'CORMORANT_PLUS_JAKARTA'
  | 'CINZEL_MANROPE'
  | 'SYNE_SPACE_GROTESK'
  | 'FRAUNCES_OUTFIT'
  | 'BODONI_INTER';

export type SectionType =
  | 'HERO'
  | 'ABOUT'
  | 'SERVICES'
  | 'FEATURED_PROJECTS'
  | 'PROJECT_GALLERY'
  | 'BEFORE_AFTER'
  | 'DESIGN_PHILOSOPHY'
  | 'PROCESS'
  | 'TESTIMONIALS'
  | 'PRESS'
  | 'AWARDS'
  | 'TEAM'
  | 'FAQ'
  | 'CONTACT_FORM'
  | 'LOCATION_MAP'
  | 'INSTAGRAM_FEED'
  | 'CONSULTATION_CTA'
  | 'FOOTER';

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
  isVisible: boolean;
  content: Record<string, any>;
}

export interface ReorderSectionsRequest {
  orderedSectionIds: string[];
}

export interface SwitchTemplateRequest {
  templateKey: PortfolioTemplateKey;
  version: number;
}

export interface CreateVersionSnapshotRequest {
  label: string;
}

export interface RestoreVersionRequest {
  version: number;
}
