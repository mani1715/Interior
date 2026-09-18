// Query boundary layer for Discovery UI (Phase 06)
// Provides clean abstraction that will be replaced by Phase 25 Search API / Elasticsearch / PostgreSQL search document projections

import {
  Project,
  Professional,
  CategoryInfo,
  LocationInfo,
  FilterParams,
  ProfessionalFilterParams,
} from './types';
import {
  DEMO_PROJECTS,
  DEMO_PROFESSIONALS,
  DEMO_CATEGORIES,
  DEMO_LOCATIONS,
} from './demo-data';

const DEFAULT_PAGE_SIZE = 12;

export function getProjects(params: FilterParams = {}): {
  projects: Project[];
  total: number;
  hasMore: boolean;
} {
  let results = [...DEMO_PROJECTS];

  // 1. Text Search across title, category, style, location, professional, description, materials
  if (params.q && params.q.trim().length > 0) {
    const q = params.q.trim().toLowerCase();
    results = results.filter((p) => {
      const matchTitle = p.title.toLowerCase().includes(q);
      const matchCategory = p.categoryName.toLowerCase().includes(q);
      const matchLocation = p.locationName.toLowerCase().includes(q);
      const matchStyle = p.styleName.toLowerCase().includes(q);
      const matchStudio = p.studioName.toLowerCase().includes(q);
      const matchDesc = p.description.toLowerCase().includes(q);
      const matchMaterials = p.materials.some((m) => m.toLowerCase().includes(q));
      const matchProperty = p.propertyTypeName.toLowerCase().includes(q);
      return (
        matchTitle ||
        matchCategory ||
        matchLocation ||
        matchStyle ||
        matchStudio ||
        matchDesc ||
        matchMaterials ||
        matchProperty
      );
    });
  }

  // 2. Category Filter (slug)
  if (params.category && params.category !== 'all') {
    results = results.filter((p) => p.category === params.category);
  }

  // 3. Location Filter (slug)
  if (params.location && params.location !== 'all') {
    results = results.filter((p) => p.location === params.location);
  }

  // 4. Style Filter
  if (params.style && params.style !== 'all') {
    results = results.filter((p) => p.style === params.style);
  }

  // 5. Budget Range Filter
  if (params.budget && params.budget !== 'all') {
    results = results.filter((p) => p.budgetRange === params.budget);
  }

  // 6. Property Type Filter
  if (params.propertyType && params.propertyType !== 'all') {
    results = results.filter((p) => p.propertyType === params.propertyType);
  }

  // 7. Professional Type Filter
  if (params.professionalType && params.professionalType !== 'all') {
    results = results.filter((p) => p.professionalType === params.professionalType);
  }

  // 8. Sorting
  const sort = params.sort || 'recommended';
  if (sort === 'newest') {
    results.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  } else if (sort === 'most_viewed') {
    results.sort((a, b) => {
      const vA = parseFloat(a.viewCount?.replace('k', '') || '0');
      const vB = parseFloat(b.viewCount?.replace('k', '') || '0');
      return vB - vA;
    });
  } else {
    // Recommended: featured first, then newest
    results.sort((a, b) => {
      if (a.featured && !b.featured) return -1;
      if (!a.featured && b.featured) return 1;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }

  const total = results.length;
  const page = Math.max(1, params.page || 1);
  const limit = page * DEFAULT_PAGE_SIZE;
  const paginated = results.slice(0, limit);

  return {
    projects: paginated,
    total,
    hasMore: limit < total,
  };
}

export function getProjectBySlug(slug: string): Project | undefined {
  return DEMO_PROJECTS.find((p) => p.slug === slug);
}

export function getRelatedProjects(project: Project, limit = 3): Project[] {
  return DEMO_PROJECTS.filter(
    (p) =>
      p.id !== project.id &&
      (p.category === project.category ||
        p.location === project.location ||
        p.style === project.style)
  ).slice(0, limit);
}

export function getProfessionals(params: ProfessionalFilterParams = {}): Professional[] {
  let results = [...DEMO_PROFESSIONALS];

  if (params.q && params.q.trim().length > 0) {
    const q = params.q.trim().toLowerCase();
    results = results.filter((p) => {
      const matchName = p.name.toLowerCase().includes(q);
      const matchStudio = p.studioName.toLowerCase().includes(q);
      const matchType = p.professionalTypeLabel.toLowerCase().includes(q);
      const matchLocation = p.locationName.toLowerCase().includes(q);
      const matchSpecialties = p.specialties.some((s) => s.toLowerCase().includes(q));
      return matchName || matchStudio || matchType || matchLocation || matchSpecialties;
    });
  }

  if (params.professionalType && params.professionalType !== 'all') {
    results = results.filter((p) => p.professionalType === params.professionalType);
  }

  if (params.location && params.location !== 'all') {
    results = results.filter((p) => p.location === params.location);
  }

  return results;
}

export function getProfessionalBySlug(slug: string): Professional | undefined {
  return DEMO_PROFESSIONALS.find((p) => p.slug === slug);
}

export function getProjectsByProfessional(professionalSlug: string): Project[] {
  return DEMO_PROJECTS.filter((p) => p.professionalSlug === professionalSlug);
}

export function getCategories(): CategoryInfo[] {
  return DEMO_CATEGORIES;
}

export function getCategoryBySlug(slug: string): CategoryInfo | undefined {
  return DEMO_CATEGORIES.find((c) => c.slug === slug);
}

export function getLocations(): LocationInfo[] {
  return DEMO_LOCATIONS;
}

export function getLocationBySlug(slug: string): LocationInfo | undefined {
  return DEMO_LOCATIONS.find((l) => l.slug === slug);
}
