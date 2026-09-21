export type ProjectCategory =
  | 'LIVING_ROOM'
  | 'TV_UNIT'
  | 'BEDROOM'
  | 'WARDROBE'
  | 'MODULAR_KITCHEN'
  | 'POOJA_UNIT'
  | 'CROCKERY_UNIT'
  | 'STUDY_UNIT'
  | 'FALSE_CEILING'
  | 'WALL_PANELS'
  | 'SHOE_RACK'
  | 'OFFICE'
  | 'COMMERCIAL'
  | 'CUSTOM_FURNITURE'
  | 'COMPLETE_HOME_INTERIOR';

export type ProjectStyle =
  | 'MODERN_MINIMALIST'
  | 'WARM_CONTEMPORARY'
  | 'INDIAN_TRADITIONAL'
  | 'NEO_CLASSICAL'
  | 'SCANDINAVIAN'
  | 'INDUSTRIAL'
  | 'LUXURY_ECLECTIC'
  | 'BIOPHILIC';

export type PropertyType =
  | 'APARTMENT'
  | 'INDEPENDENT_HOUSE'
  | 'VILLA'
  | 'OFFICE'
  | 'RETAIL'
  | 'RESTAURANT'
  | 'COMMERCIAL'
  | 'OTHER';

export type ProjectScope =
  | 'FULL_INTERIOR'
  | 'PARTIAL_INTERIOR'
  | 'SINGLE_ROOM'
  | 'CUSTOM_FURNITURE'
  | 'WOODWORK'
  | 'RENOVATION'
  | 'ARCHITECTURAL'
  | 'TURNKEY'
  | 'OTHER';

export type ProjectStatus = 'DRAFT' | 'READY' | 'ARCHIVED';
export type VisibilityStatus = 'PRIVATE' | 'PORTFOLIO';
export type BudgetVisibility = 'HIDDEN' | 'RANGE' | 'STARTING_FROM';
export type ClientNameVisibility = 'HIDDEN' | 'DISPLAY';
export type AreaUnit = 'SQ_FT' | 'SQ_M';

export interface CategoryOption {
  code: ProjectCategory;
  label: string;
}

export const CANONICAL_CATEGORIES: CategoryOption[] = [
  { code: 'COMPLETE_HOME_INTERIOR', label: 'Complete Home Interior' },
  { code: 'LIVING_ROOM', label: 'Living Room' },
  { code: 'MODULAR_KITCHEN', label: 'Modular Kitchen' },
  { code: 'BEDROOM', label: 'Bedroom' },
  { code: 'WARDROBE', label: 'Wardrobe & Storage' },
  { code: 'TV_UNIT', label: 'TV Unit & Entertainment' },
  { code: 'POOJA_UNIT', label: 'Pooja Room & Mandir' },
  { code: 'CROCKERY_UNIT', label: 'Crockery Unit & Bar' },
  { code: 'STUDY_UNIT', label: 'Study & Home Office' },
  { code: 'FALSE_CEILING', label: 'False Ceiling & Lighting' },
  { code: 'WALL_PANELS', label: 'Wall Paneling & Cladding' },
  { code: 'SHOE_RACK', label: 'Foyer & Shoe Rack' },
  { code: 'OFFICE', label: 'Corporate Office' },
  { code: 'COMMERCIAL', label: 'Commercial & Retail' },
  { code: 'CUSTOM_FURNITURE', label: 'Bespoke Custom Furniture' },
];

export interface StyleOption {
  code: ProjectStyle;
  label: string;
}

export const CANONICAL_STYLES: StyleOption[] = [
  { code: 'MODERN_MINIMALIST', label: 'Modern Minimalist' },
  { code: 'WARM_CONTEMPORARY', label: 'Warm Contemporary' },
  { code: 'INDIAN_TRADITIONAL', label: 'Indian Traditional' },
  { code: 'NEO_CLASSICAL', label: 'Neo Classical' },
  { code: 'SCANDINAVIAN', label: 'Scandinavian' },
  { code: 'INDUSTRIAL', label: 'Industrial' },
  { code: 'LUXURY_ECLECTIC', label: 'Luxury Eclectic' },
  { code: 'BIOPHILIC', label: 'Biophilic' },
];

export interface PropertyTypeOption {
  code: PropertyType;
  label: string;
}

export const CANONICAL_PROPERTY_TYPES: PropertyTypeOption[] = [
  { code: 'APARTMENT', label: 'Apartment / Flat' },
  { code: 'INDEPENDENT_HOUSE', label: 'Independent House' },
  { code: 'VILLA', label: 'Villa' },
  { code: 'OFFICE', label: 'Office Space' },
  { code: 'RETAIL', label: 'Retail / Showroom' },
  { code: 'RESTAURANT', label: 'Restaurant / Cafe' },
  { code: 'COMMERCIAL', label: 'Commercial Space' },
  { code: 'OTHER', label: 'Other' },
];

export interface ScopeOption {
  code: ProjectScope;
  label: string;
}

export const CANONICAL_SCOPES: ScopeOption[] = [
  { code: 'FULL_INTERIOR', label: 'Full Home / Office Interior' },
  { code: 'PARTIAL_INTERIOR', label: 'Partial Interior' },
  { code: 'SINGLE_ROOM', label: 'Single Room' },
  { code: 'CUSTOM_FURNITURE', label: 'Custom Furniture' },
  { code: 'WOODWORK', label: 'Woodwork & Carpentry' },
  { code: 'RENOVATION', label: 'Renovation & Remodeling' },
  { code: 'ARCHITECTURAL', label: 'Architectural Interior' },
  { code: 'TURNKEY', label: 'Turnkey Project' },
  { code: 'OTHER', label: 'Other' },
];

export interface ProjectSummaryDto {
  id: string;
  slug: string;
  title: string;
  categoryCode: ProjectCategory;
  categoryDisplayName: string;
  projectStatus: ProjectStatus;
  visibilityStatus: VisibilityStatus;
  featured: boolean;
  displayOrder: number;
  city?: string | null;
  state?: string | null;
  propertyType?: PropertyType | null;
  projectScope?: ProjectScope | null;
  styleCodes: ProjectStyle[];
  styleDisplayNames: string[];
  completionYear?: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  archivedAt?: string | null;
}

export interface ProjectDetailDto {
  id: string;
  studioId: string;
  slug: string;
  title: string;
  shortDescription?: string | null;
  fullDescription?: string | null;
  categoryCode: ProjectCategory;
  categoryDisplayName: string;
  projectStatus: ProjectStatus;
  visibilityStatus: VisibilityStatus;
  featured: boolean;
  displayOrder: number;
  city?: string | null;
  district?: string | null;
  state?: string | null;
  country: string;
  propertyType?: PropertyType | null;
  projectScope?: ProjectScope | null;
  styleCodes: ProjectStyle[];
  styleDisplayNames: string[];
  completionYear?: number | null;
  budgetVisibility: BudgetVisibility;
  budgetMin?: number | null;
  budgetMax?: number | null;
  currency: string;
  clientNameVisibility: ClientNameVisibility;
  clientDisplayName?: string | null;
  areaValue?: number | null;
  areaUnit?: AreaUnit | null;
  internalNotes?: string | null;
  version: number;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
  archivedAt?: string | null;
  isReady: boolean;
  missingReadinessFields: string[];
}

export interface ProjectPresentationDto {
  id: string;
  slug: string;
  title: string;
  shortDescription?: string | null;
  fullDescription?: string | null;
  categoryCode: string;
  categoryDisplayName: string;
  styleCodes: string[];
  styleDisplayNames: string[];
  location?: string | null;
  propertyType?: string | null;
  projectScope?: string | null;
  completionYear?: number | null;
  featured: boolean;
  displayOrder: number;
  clientName?: string | null;
  budgetFormatted?: string | null;
  areaFormatted?: string | null;
  coverImageUrl?: string | null;
}

export interface CreateProjectRequest {
  title: string;
  categoryCode: ProjectCategory;
  shortDescription?: string | null;
  fullDescription?: string | null;
  propertyType?: PropertyType | null;
  projectScope?: ProjectScope | null;
  styleCodes?: ProjectStyle[];
  city?: string | null;
  district?: string | null;
  state?: string | null;
  country?: string | null;
  completionYear?: number | null;
  budgetVisibility?: BudgetVisibility;
  budgetMin?: number | null;
  budgetMax?: number | null;
  currency?: string | null;
  clientNameVisibility?: ClientNameVisibility;
  clientDisplayName?: string | null;
  areaValue?: number | null;
  areaUnit?: AreaUnit | null;
  visibilityStatus?: VisibilityStatus;
  featured?: boolean;
  internalNotes?: string | null;
}

export interface UpdateProjectRequest extends CreateProjectRequest {
  version: number;
}

export interface ReorderProjectsRequest {
  orderedProjectIds: string[];
}

export interface ProjectFilterQuery {
  status?: ProjectStatus;
  category?: ProjectCategory;
  visibility?: VisibilityStatus;
  featured?: boolean;
  includeArchived?: boolean;
}
