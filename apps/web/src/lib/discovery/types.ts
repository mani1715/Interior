// Centralized domain types for Public Discovery UI (Phase 06)
// This model aligns with future Phase 25 Search API / Phase 18 Project CMS

export type ProfessionalTypeKey =
  | 'interior-designer'
  | 'interior-studio'
  | 'architect'
  | 'architecture-studio'
  | 'custom-furniture'
  | 'woodwork-cabinetry'
  | 'turnkey-contractor';

export type PropertyTypeKey =
  | 'apartment'
  | 'villa'
  | 'independent-house'
  | 'office'
  | 'commercial'
  | 'other';

export type StyleKey =
  | 'modern'
  | 'contemporary'
  | 'minimal'
  | 'luxury'
  | 'traditional'
  | 'warm-natural'
  | 'industrial'
  | 'scandinavian';

export type BudgetRangeKey =
  | 'under-50k'
  | '50k-1l'
  | '1l-3l'
  | '3l-5l'
  | '5l-plus';

export interface GalleryItem {
  url: string;
  alt: string;
  caption?: string;
  orientation?: 'landscape' | 'portrait' | 'square';
}

export interface Project {
  id: string;
  slug: string;
  title: string;
  category: string; // slug e.g. 'tv-units'
  categoryName: string;
  location: string; // slug e.g. 'guntur'
  locationName: string;
  style: StyleKey;
  styleName: string;
  budgetRange: BudgetRangeKey;
  budgetLabel: string;
  propertyType: PropertyTypeKey;
  propertyTypeName: string;
  professionalType: ProfessionalTypeKey;
  professionalTypeName: string;
  professionalSlug: string;
  professionalName: string;
  studioName: string;
  description: string;
  scope: string;
  coverImage: string;
  gallery: GalleryItem[];
  materials: string[];
  completionYear: number;
  duration: string;
  services: string[];
  featured?: boolean;
  createdAt: string;
  viewCount?: string;
  // Optional transformation & AI study comparisons
  beforeImage?: string;
  aiImage?: string;
  realityImage?: string;
  isAiConceptCover?: boolean;
}

export interface Professional {
  id: string;
  slug: string;
  name: string;
  studioName: string;
  professionalType: ProfessionalTypeKey;
  professionalTypeLabel: string;
  location: string;
  locationName: string;
  bio: string;
  services: string[];
  specialties: string[];
  startingBudgetLabel?: string;
  serviceAreas: string[];
  featured?: boolean;
  avatarChar: string;
  avatarColor: string; // CSS or token color
}

export interface CategoryInfo {
  slug: string;
  name: string;
  description: string;
  heroDescription: string;
  popularStyles: string[];
}

export interface LocationInfo {
  slug: string;
  name: string;
  state: string;
  description: string;
  popularCategories: string[];
}

export interface FilterParams {
  q?: string;
  category?: string;
  location?: string;
  style?: string;
  budget?: string;
  propertyType?: string;
  professionalType?: string;
  sort?: 'recommended' | 'newest' | 'most_viewed';
  page?: number;
}

export interface ProfessionalFilterParams {
  q?: string;
  professionalType?: string;
  location?: string;
}
