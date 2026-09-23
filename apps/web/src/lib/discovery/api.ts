import { env } from '../env';

export interface DiscoveryProjectCard {
  id: string;
  slug: string;
  title: string;
  shortDescription?: string;
  categoryCode: string;
  categoryName: string;
  styleCodes: string[];
  styleNames: string[];
  city?: string;
  state?: string;
  propertyType?: string;
  projectScope?: string;
  coverImageUrl?: string | null;
  isAiConceptCover: boolean;
  studioId: string;
  studioSlug: string;
  studioName: string;
  professionalType: string;
  professionalTypeLabel: string;
  completionYear?: number | null;
}

export interface DiscoveryProfessionalCard {
  id: string;
  slug: string;
  name: string;
  professionalType: string;
  professionalTypeLabel: string;
  professionalTitle?: string;
  tagline?: string;
  city?: string;
  state?: string;
  experienceSinceYear?: number | null;
  services: string[];
  specialties: string[];
  projectCount: number;
  sampleProjectCoverUrls: string[];
}

export interface DiscoveryFacetItem {
  code: string;
  label: string;
  count: number;
}

export interface DiscoveryFacets {
  categories: DiscoveryFacetItem[];
  cities: DiscoveryFacetItem[];
  styles: DiscoveryFacetItem[];
  professionalTypes: DiscoveryFacetItem[];
}

export interface DiscoverySearchResponse {
  projects: DiscoveryProjectCard[];
  professionals: DiscoveryProfessionalCard[];
  totalProjects: number;
  totalProfessionals: number;
  facets: DiscoveryFacets;
  nextCursor?: string | null;
  hasMore: boolean;
}

export interface SuggestionItem {
  text: string;
  type: 'category' | 'city' | 'style' | 'studio' | 'project';
  slug: string;
  meta?: string;
}

export interface DiscoverySuggestionsResponse {
  categories: SuggestionItem[];
  cities: SuggestionItem[];
  styles: SuggestionItem[];
  studios: SuggestionItem[];
  projects: SuggestionItem[];
}

export interface DiscoverySearchParams {
  q?: string;
  category?: string;
  style?: string;
  city?: string;
  state?: string;
  propertyType?: string;
  scope?: string;
  professionalType?: string;
  sort?: string;
  limit?: number;
  offset?: number;
  cursor?: string;
}

function buildQueryString(params: DiscoverySearchParams): string {
  const sp = new URLSearchParams();
  if (params.q) sp.set('q', params.q);
  if (params.category && params.category !== 'all') sp.set('category', params.category);
  if (params.style && params.style !== 'all') sp.set('style', params.style);
  if (params.city && params.city !== 'all') sp.set('city', params.city);
  if (params.state && params.state !== 'all') sp.set('state', params.state);
  if (params.propertyType && params.propertyType !== 'all') sp.set('propertyType', params.propertyType);
  if (params.scope && params.scope !== 'all') sp.set('scope', params.scope);
  if (params.professionalType && params.professionalType !== 'all') sp.set('professionalType', params.professionalType);
  if (params.sort) sp.set('sort', params.sort);
  if (params.limit !== undefined) sp.set('limit', String(params.limit));
  if (params.offset !== undefined) sp.set('offset', String(params.offset));
  if (params.cursor) sp.set('cursor', params.cursor);
  const str = sp.toString();
  return str ? `?${str}` : '';
}

export async function fetchDiscoveryProjects(
  params: DiscoverySearchParams = {}
): Promise<DiscoverySearchResponse> {
  const qs = buildQueryString(params);
  const res = await fetch(`${env.apiBaseUrl}/public/discovery/projects${qs}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch discovery projects: ${res.status}`);
  }

  return res.json();
}

export async function fetchDiscoveryProfessionals(
  params: DiscoverySearchParams = {}
): Promise<DiscoverySearchResponse> {
  const qs = buildQueryString(params);
  const res = await fetch(`${env.apiBaseUrl}/public/discovery/professionals${qs}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch discovery professionals: ${res.status}`);
  }

  return res.json();
}

export async function fetchDiscoverySearch(
  params: DiscoverySearchParams = {}
): Promise<DiscoverySearchResponse> {
  const qs = buildQueryString(params);
  const res = await fetch(`${env.apiBaseUrl}/public/discovery/search${qs}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to execute discovery search: ${res.status}`);
  }

  return res.json();
}

export async function fetchDiscoverySuggestions(
  query: string
): Promise<DiscoverySuggestionsResponse> {
  if (!query || query.trim().length < 2) {
    return {
      categories: [],
      cities: [],
      styles: [],
      studios: [],
      projects: [],
    };
  }

  const res = await fetch(
    `${env.apiBaseUrl}/public/discovery/suggestions?q=${encodeURIComponent(query.trim())}`,
    {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    }
  );

  if (!res.ok) {
    return {
      categories: [],
      cities: [],
      styles: [],
      studios: [],
      projects: [],
    };
  }

  return res.json();
}

export async function fetchDiscoveryFacets(
  params: DiscoverySearchParams = {}
): Promise<DiscoveryFacets> {
  const qs = buildQueryString(params);
  const res = await fetch(`${env.apiBaseUrl}/public/discovery/facets${qs}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    next: { revalidate: 300 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch discovery facets: ${res.status}`);
  }

  return res.json();
}

import type { Project, Professional } from './types';

export function mapDiscoveryCardToProject(c: DiscoveryProjectCard): Project {
  return {
    id: c.id,
    slug: c.slug,
    title: c.title,
    category: c.categoryCode.toLowerCase().replace(/_/g, '-'),
    categoryName: c.categoryName,
    location: (c.city || 'bengaluru').toLowerCase().replace(/\s+/g, '-'),
    locationName: c.city || 'Pan India',
    style: (c.styleCodes[0]?.toLowerCase().replace(/_/g, '-') as any) || 'modern',
    styleName: c.styleNames[0] || 'Contemporary',
    budgetRange: '1l-3l',
    budgetLabel: 'Verified Project',
    propertyType: (c.propertyType?.toLowerCase().replace(/_/g, '-') as any) || 'apartment',
    propertyTypeName: c.propertyType || 'Apartment',
    professionalType: (c.professionalType.toLowerCase().replace(/_/g, '-') as any) || 'interior-studio',
    professionalTypeName: c.professionalTypeLabel,
    professionalSlug: c.studioSlug,
    professionalName: c.studioName,
    studioName: c.studioName,
    description: c.shortDescription || '',
    scope: c.projectScope || 'Full Home Interior',
    coverImage: c.coverImageUrl || '/images/placeholders/project-placeholder.jpg',
    isAiConceptCover: c.isAiConceptCover,
    gallery: [],
    materials: [],
    completionYear: c.completionYear || 2025,
    duration: '3-6 months',
    services: [c.categoryName],
    featured: false,
    createdAt: new Date().toISOString(),
  };
}

export function mapDiscoveryCardToProfessional(c: DiscoveryProfessionalCard): Professional {
  return {
    id: c.id,
    slug: c.slug,
    name: c.name,
    studioName: c.name,
    professionalType: (c.professionalType.toLowerCase().replace(/_/g, '-') as any) || 'interior-studio',
    professionalTypeLabel: c.professionalTypeLabel,
    location: (c.city || 'bengaluru').toLowerCase().replace(/\s+/g, '-'),
    locationName: c.city ? `${c.city}${c.state ? `, ${c.state}` : ''}` : 'India',
    bio: c.tagline || c.professionalTitle || 'Verified interior and architecture professional.',
    services: c.services.length > 0 ? c.services : ['Residential Interior'],
    specialties: c.specialties.length > 0 ? c.specialties : ['Modern Minimalist'],
    startingBudgetLabel: 'Starting from ₹50,000',
    serviceAreas: [c.city || 'Pan-India'],
    featured: false,
    avatarChar: c.name.charAt(0).toUpperCase(),
    avatarColor: 'var(--brand)',
  };
}

