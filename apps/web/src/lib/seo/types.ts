export interface SeoChecklistItem {
  key: string;
  label: string;
  passed: boolean;
  message: string;
  actionUrl?: string;
}

export interface SeoStatusResponse {
  studioId: string;
  studioName: string;
  studioSlug: string;
  publicationStatus: 'UNPUBLISHED' | 'PUBLISHED';
  publishedAt: string | null;
  canonicalUrl: string;
  metaTitle: string;
  metaDescription: string;
  metaTitleOverride: string | null;
  metaDescriptionOverride: string | null;
  canonicalUrlOverride: string | null;
  indexingEnabled: boolean;
  isPublishable: boolean;
  checklist: SeoChecklistItem[];
}

export interface UpdateSeoSettingsRequest {
  metaTitleOverride?: string | null;
  metaDescriptionOverride?: string | null;
  canonicalUrlOverride?: string | null;
  indexingEnabled: boolean;
}

export interface PublicContactDto {
  channelType: string;
  contactValue: string;
  sortOrder: number;
}

export interface PublicPortfolioDto {
  templateKey: string;
  headline?: string | null;
  subheadline?: string | null;
  bio?: string | null;
  designPhilosophy?: string | null;
  yearsOfExperience?: number | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  fontPairing?: string | null;
  sections: Array<{
    sectionType: string;
    content: string;
    sortOrder: number;
  }>;
}

export interface PublicStudioSummaryDto {
  studioId: string;
  name: string;
  slug: string;
  city?: string | null;
  state?: string | null;
}

export interface PublicMediaDto {
  id: string;
  mediaType: string;
  isAiConcept: boolean;
  sortOrder: number;
  isCover: boolean;
  altText: string;
  caption?: string | null;
  originalWidth: number;
  originalHeight: number;
  thumbnailUrl: string;
  mediumUrl: string;
  largeUrl: string;
  heroUrl?: string | null;
}

export interface PublicStudioDto {
  id: string;
  name: string;
  slug: string;
  professionalType?: string | null;
  professionalTitle?: string | null;
  tagline?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
  travelAvailable: boolean;
  publishedAt: string | null;
  canonicalUrl: string;
  metaTitle: string;
  metaDescription: string;
  indexingEnabled: boolean;
  portfolio?: PublicPortfolioDto | null;
  contacts: PublicContactDto[];
  services: string[];
  specialties: string[];
  serviceAreas: string[];
  projects: Array<{
    id: string;
    title: string;
    slug: string;
    categoryCode: string;
    city?: string | null;
    completionYear?: number | null;
    coverImageUrl?: string | null;
  }>;
}

export interface PublicProjectDetailDto {
  id: string;
  slug: string;
  title: string;
  categoryCode: string;
  shortDescription?: string | null;
  fullDescription?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
  completionYear?: number | null;
  propertyType?: string | null;
  projectScope?: string | null;
  styleCodes: string[];
  areaSqFt?: number | null;
  studio: PublicStudioSummaryDto;
  canonicalUrl: string;
  metaTitle: string;
  metaDescription: string;
  media: PublicMediaDto[];
}

export interface SitemapItemDto {
  path: string;
  priority: string;
  changefreq: string;
  lastModified?: string | null;
  imageUrl?: string | null;
  imageCaption?: string | null;
}
