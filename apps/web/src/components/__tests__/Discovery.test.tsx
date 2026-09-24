import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Discovery queries and data
import {
  getProjects,
  getProjectBySlug,
  getRelatedProjects,
  getProfessionals,
  getProfessionalBySlug,
  getProjectsByProfessional,
  getCategories,
  getCategoryBySlug,
  getLocations,
  getLocationBySlug,
} from '@/lib/discovery/queries';
import {
  DEMO_PROJECTS as projects,
  DEMO_PROFESSIONALS as professionals,
  DEMO_CATEGORIES as categories,
  DEMO_LOCATIONS as locations,
} from '@/lib/discovery/demo-data';

// Components
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { ProfessionalCard } from '@/components/discovery/ProfessionalCard';
import { DiscoverySearchBar } from '@/components/discovery/DiscoverySearchBar';
import { ActiveFilterBar } from '@/components/discovery/ActiveFilterBar';
import { EnquirySheet } from '@/components/discovery/EnquirySheet';
import { ProjectsDiscoveryClient } from '@/components/discovery/ProjectsDiscoveryClient';
import { ProfessionalsDiscoveryClient } from '@/components/discovery/ProfessionalsDiscoveryClient';

// Route pages
import ProjectsPage from '@/app/projects/page';
import ProjectDetailPage from '@/app/projects/[projectSlug]/page';
import ProfessionalsPage from '@/app/professionals/page';
import ProfessionalDetailPage from '@/app/professionals/[professionalSlug]/page';
import CategoryPage from '@/app/categories/[categorySlug]/page';
import LocationPage from '@/app/locations/[locationSlug]/page';

// Mock next/navigation
const mockReplace = vi.fn();
const mockPush = vi.fn();
let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
  }),
  usePathname: () => '/projects',
  useSearchParams: () => mockSearchParams,
  notFound: vi.fn(() => {
    throw new Error('NEXT_NOT_FOUND');
  }),
}));

beforeEach(() => {
  mockSearchParams = new URLSearchParams();
  vi.clearAllMocks();
  global.fetch = vi.fn().mockImplementation((url: string) => {
    const urlStr = String(url);
    if (urlStr.includes('/public/discovery/professionals')) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          projects: [],
          professionals: professionals.map((p) => ({
            id: p.id,
            slug: p.slug,
            name: p.studioName,
            professionalType: p.professionalType.toUpperCase().replace(/-/g, '_'),
            professionalTypeLabel: p.professionalTypeLabel,
            professionalTitle: p.bio,
            tagline: p.bio,
            city: p.locationName.split(',')[0],
            state: p.locationName.split(',')[1]?.trim() || '',
            services: p.services,
            specialties: p.specialties,
            projectCount: p.projectCount || 2,
            sampleProjectCoverUrls: p.sampleProjectCoverUrls || [],
          })),
          totalProjects: 0,
          totalProfessionals: professionals.length,
          facets: { categories: [], cities: [], styles: [], professionalTypes: [] },
          hasMore: false,
        }),
      } as Response);
    }
    if (urlStr.includes('/public/discovery/projects')) {
      if (urlStr.includes('non-existent-search-phrase-12345')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            projects: [],
            professionals: [],
            totalProjects: 0,
            totalProfessionals: 0,
            facets: { categories: [], cities: [], styles: [], professionalTypes: [] },
            hasMore: false,
          }),
        } as Response);
      }
      return Promise.resolve({
        ok: true,
        json: async () => ({
          projects: projects.map((pr) => ({
            id: pr.id,
            slug: pr.slug,
            title: pr.title,
            shortDescription: pr.description,
            categoryCode: pr.category.toUpperCase().replace(/-/g, '_'),
            categoryName: pr.categoryName,
            styleCodes: [pr.style.toUpperCase().replace(/-/g, '_')],
            styleNames: [pr.styleName],
            city: pr.locationName,
            state: 'Andhra Pradesh',
            propertyType: pr.propertyType.toUpperCase().replace(/-/g, '_'),
            projectScope: pr.scope,
            coverImageUrl: pr.coverImage,
            isAiConceptCover: pr.isAiConceptCover,
            studioId: 'studio-1',
            studioSlug: pr.professionalSlug,
            studioName: pr.studioName,
            professionalType: pr.professionalType.toUpperCase().replace(/-/g, '_'),
            professionalTypeLabel: pr.professionalTypeName,
            completionYear: pr.completionYear,
          })),
          professionals: [],
          totalProjects: projects.length,
          totalProfessionals: 0,
          facets: { categories: [], cities: [], styles: [], professionalTypes: [] },
          hasMore: false,
        }),
      } as Response);
    }
    return Promise.resolve({
      ok: false,
      status: 404,
      json: async () => ({ error: 'Not found' }),
    } as Response);
  });
});

describe('Phase 06 — Public Discovery Query Layer', () => {
  it('returns full project list with default parameters', () => {
    const res = getProjects();
    expect(res.projects.length).toBeGreaterThan(0);
    expect(res.total).toBe(projects.length);
  });

  it('filters projects by search query (title, materials, city)', () => {
    const resTeak = getProjects({ q: 'teak' });
    expect(resTeak.projects.length).toBeGreaterThan(0);
    resTeak.projects.forEach((p) => {
      const match =
        p.title.toLowerCase().includes('teak') ||
        p.materials.some((m) => m.toLowerCase().includes('teak')) ||
        p.description.toLowerCase().includes('teak');
      expect(match).toBe(true);
    });
  });

  it('filters projects by category', () => {
    const res = getProjects({ category: 'tv-units' });
    expect(res.projects.length).toBeGreaterThan(0);
    res.projects.forEach((p) => {
      expect(p.category).toBe('tv-units');
    });
  });

  it('filters projects by location', () => {
    const res = getProjects({ location: 'guntur' });
    expect(res.projects.length).toBeGreaterThan(0);
    res.projects.forEach((p) => {
      expect(p.location).toBe('guntur');
    });
  });

  it('filters projects by property type', () => {
    const res = getProjects({ propertyType: 'apartment' });
    expect(res.projects.length).toBeGreaterThan(0);
    res.projects.forEach((p) => {
      expect(p.propertyType).toBe('apartment');
    });
  });

  it('sorts projects by view count (most_viewed)', () => {
    const resPopular = getProjects({ sort: 'most_viewed' });
    for (let i = 0; i < resPopular.projects.length - 1; i++) {
      const vA = parseFloat(resPopular.projects[i].viewCount?.replace('k', '') || '0');
      const vB = parseFloat(resPopular.projects[i + 1].viewCount?.replace('k', '') || '0');
      expect(vA).toBeGreaterThanOrEqual(vB);
    }
  });

  it('finds project by valid slug and returns undefined for invalid slug', () => {
    const valid = getProjectBySlug('modern-fluted-tv-unit-guntur');
    expect(valid).toBeDefined();
    expect(valid?.title).toContain('Modern Fluted TV Unit');

    const invalid = getProjectBySlug('non-existent-slug-xyz');
    expect(invalid).toBeUndefined();
  });

  it('finds related projects excluding current project', () => {
    const current = projects[0];
    const related = getRelatedProjects(current, 3);
    expect(related.length).toBeLessThanOrEqual(3);
    expect(related.some((p) => p.id === current.id)).toBe(false);
  });

  it('filters professionals by trade type and location', () => {
    const resTrade = getProfessionals({ professionalType: 'architecture-studio' });
    expect(resTrade.length).toBeGreaterThan(0);
    resTrade.forEach((p) => {
      expect(p.professionalType).toBe('architecture-studio');
    });

    const resCity = getProfessionals({ location: 'guntur' });
    expect(resCity.length).toBeGreaterThan(0);
    resCity.forEach((p) => {
      expect(p.location).toBe('guntur');
    });
  });

  it('finds professional by slug and retrieves projects attributed to them', () => {
    const prof = getProfessionalBySlug('studio-elegance');
    expect(prof).toBeDefined();
    expect(prof?.studioName).toBe('Studio Elégance');

    const profProjects = getProjectsByProfessional('studio-elegance');
    expect(profProjects.length).toBeGreaterThan(0);
    profProjects.forEach((p) => {
      expect(p.professionalSlug).toBe('studio-elegance');
    });
  });

  it('retrieves categories and locations with slug lookups', () => {
    const allCats = getCategories();
    expect(allCats.length).toBe(categories.length);
    const cat = getCategoryBySlug('tv-units');
    expect(cat).toBeDefined();
    expect(cat?.name).toBe('TV Units');

    const allLocs = getLocations();
    expect(allLocs.length).toBe(locations.length);
    const loc = getLocationBySlug('guntur');
    expect(loc).toBeDefined();
    expect(loc?.name).toBe('Guntur');
  });
});

describe('Phase 06 — ProjectCard & ProfessionalCard Components', () => {
  const sampleProject = projects[0];
  const sampleProf = professionals[0];

  it('renders ProjectCard with title, location, category, and materials without verified badge', () => {
    render(<ProjectCard project={sampleProject} />);
    expect(screen.getByText(sampleProject.title)).toBeDefined();
    expect(screen.getByText(sampleProject.categoryName)).toBeDefined();
    expect(screen.getByText(sampleProject.locationName)).toBeDefined();
    expect(screen.getByText(sampleProject.materials[0])).toBeDefined();
    expect(screen.getByText(sampleProject.studioName)).toBeDefined();

    // Verify truthfulness: NO "verified" claims
    const verifiedElements = screen.queryAllByText(/verified/i);
    expect(verifiedElements.length).toBe(0);
  });

  it('allows toggling local demo save state on ProjectCard', () => {
    render(<ProjectCard project={sampleProject} />);
    const saveButton = screen.getByRole('button', { name: /save to inspiration/i });
    expect(saveButton).toBeDefined();

    fireEvent.click(saveButton);
    expect(screen.getByRole('button', { name: /remove from saved inspiration/i })).toBeDefined();
  });

  it('renders ProfessionalCard with trade type, city, starting budget, and preview thumbnails', () => {
    render(<ProfessionalCard professional={sampleProf} />);
    expect(screen.getByText(sampleProf.studioName)).toBeDefined();
    expect(screen.getByText(sampleProf.locationName)).toBeDefined();
    expect(screen.getByText(new RegExp(sampleProf.startingBudgetLabel || 'Starts from', 'i'))).toBeDefined();

    // Check specialties chips
    sampleProf.specialties.slice(0, 2).forEach((spec) => {
      expect(screen.getByText(spec)).toBeDefined();
    });

    // Check link to studio profile
    const studioLink = screen.getByRole('link', { name: sampleProf.studioName });
    expect(studioLink.getAttribute('href')).toBe(`/professionals/${sampleProf.slug}`);
  });
});

describe('Phase 06 — Discovery Filter Bar & Active Filter Bar', () => {
  it('ActiveFilterBar renders filter chips and dispatches removal', () => {
    const onRemove = vi.fn();
    const onClearAll = vi.fn();

    render(
      <ActiveFilterBar
        filters={{ category: 'tv-units', location: 'guntur', q: 'teak' }}
        totalResults={12}
        onRemoveFilter={onRemove}
        onClearAll={onClearAll}
      />
    );

    expect(screen.getByText('12')).toBeDefined();
    expect(screen.getByText('TV Units')).toBeDefined();
    expect(screen.getByText('Guntur')).toBeDefined();
    expect(screen.getByText('“teak”')).toBeDefined();

    const clearAllBtn = screen.getByRole('button', { name: /clear all/i });
    fireEvent.click(clearAllBtn);
    expect(onClearAll).toHaveBeenCalled();

    const removeSearchBtn = screen.getByRole('button', { name: /remove filter search/i });
    fireEvent.click(removeSearchBtn);
    expect(onRemove).toHaveBeenCalledWith('q');
  });

  it('DiscoverySearchBar calls onSearch on input submit and onClear when cleared', () => {
    const onSearch = vi.fn();

    render(<DiscoverySearchBar initialQuery="Kitchen" onSearch={onSearch} />);

    const input = screen.getByLabelText(/search interior projects/i);
    expect((input as HTMLInputElement).value).toBe('Kitchen');

    fireEvent.change(input, { target: { value: 'Wardrobe' } });
    const searchBtn = screen.getByRole('button', { name: /^search$/i });
    fireEvent.click(searchBtn);
    expect(onSearch).toHaveBeenCalledWith('Wardrobe');

    const clearBtn = screen.getByRole('button', { name: /clear search input/i });
    fireEvent.click(clearBtn);
    expect(onSearch).toHaveBeenCalledWith('');
  });
});

describe('Phase 26 — EnquirySheet & Lead Capture ("I Want Something Similar")', () => {
  const sampleProject = projects[0];

  it('renders real inquiry form, prefilled project context, and WhatsApp handoff tab', () => {
    render(<EnquirySheet project={sampleProject} isOpen={true} onClose={vi.fn()} />);

    // Prefilled context
    expect(screen.getByText(sampleProject.title)).toBeDefined();
    expect(screen.getAllByText(new RegExp(sampleProject.studioName, 'i')).length).toBeGreaterThan(0);

    // Form inputs and consent controls
    expect(screen.getByLabelText(/your name/i)).toBeDefined();
    expect(screen.getByLabelText(/phone \/ whatsapp/i)).toBeDefined();
    expect(screen.getByText(/I consent to be contacted/i)).toBeDefined();
    expect(screen.getByText(/Tenant Privacy:/i)).toBeDefined();

    // Mode tabs
    expect(screen.getByText(/Send Detailed Inquiry/i)).toBeDefined();
    expect(screen.getByText(/Chat on WhatsApp/i)).toBeDefined();
  });
});

describe('Phase 06 — ProjectsDiscoveryClient Island', () => {
  beforeEach(() => {
    mockSearchParams = new URLSearchParams();
  });

  it('renders project grid and filter controls with initial projects', () => {
    render(
      <ProjectsDiscoveryClient
        initialFilters={{}}
        initialProjects={projects}
        initialTotal={projects.length}
      />
    );

    expect(screen.getByText(`${projects.length}`)).toBeDefined();
    expect(screen.getByText(projects[0].title)).toBeDefined();
  });

  it('renders empty state when filter matches zero items with clear action', () => {
    render(
      <ProjectsDiscoveryClient initialFilters={{ q: 'non-existent-search-phrase-12345' }} initialProjects={[]} initialTotal={0} />
    );

    expect(screen.getByText('No projects match these filters')).toBeDefined();
    expect(screen.getByRole('button', { name: /clear all filters/i })).toBeDefined();
  });
});

describe('Phase 06 — ProfessionalsDiscoveryClient Island', () => {
  it('renders trade filter tabs and professional cards', () => {
    render(
      <ProfessionalsDiscoveryClient
        initialProfessionals={professionals}
        initialTotal={professionals.length}
      />
    );

    expect(screen.getByText('All Professionals')).toBeDefined();
    expect(screen.getAllByText('Architecture Studio').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Interior Designer').length).toBeGreaterThan(0);
    expect(screen.getByText(professionals[0].studioName)).toBeDefined();
  });
});

describe('Phase 06 — Route Integration & Truthfulness', () => {
  it('/projects route renders semantic H1, breadcrumbs, and initial project catalog', async () => {
    const page = await ProjectsPage({ searchParams: Promise.resolve({}) });
    render(page);

    const headings = screen.getAllByRole('heading', { level: 1 });
    expect(headings.length).toBe(1);
    expect(headings[0].textContent).toContain('Discover Interior Projects Across India');
    expect(screen.getByText('Project-First Discovery')).toBeDefined();
  });

  it('/projects/[projectSlug] route renders detail story with specs, gallery, and attribution', async () => {
    const sampleSlug = 'modern-fluted-tv-unit-guntur';
    const page = await ProjectDetailPage({
      params: Promise.resolve({ projectSlug: sampleSlug }),
    });
    render(page);

    // Hero title
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain(
      'Modern Fluted TV Unit & Statuario Console'
    );

    // Technical specifications
    expect(screen.getByText('Project Specifications')).toBeDefined();
    expect(screen.getByText('Italian Statuario Composite')).toBeDefined();

    // Attribution
    expect(screen.getAllByText('Studio Elégance').length).toBeGreaterThan(0);

    // AI Concept Visualization badge is present
    expect(screen.getAllByText('AI Concept Visualization').length).toBeGreaterThan(0);
  });

  it('/projects/[projectSlug] throws notFound() for invalid project slug', async () => {
    await expect(
      ProjectDetailPage({
        params: Promise.resolve({ projectSlug: 'invalid-non-existent-project' }),
      })
    ).rejects.toThrow('NEXT_NOT_FOUND');
  });

  it('/professionals route renders directory heading and professional list', async () => {
    const page = await ProfessionalsPage();
    render(page);

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain(
      'Interior Designers, Studios & Architects Across India'
    );
    expect(screen.getAllByText(professionals[0].studioName).length).toBeGreaterThan(0);
  });

  it('/professionals/[professionalSlug] renders studio profile and projects', async () => {
    const page = await ProfessionalDetailPage({
      params: Promise.resolve({ professionalSlug: 'studio-elegance' }),
    });
    render(page);

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Studio Elégance');
    expect(screen.getAllByText('Interior Studio').length).toBeGreaterThan(0);
    expect(screen.getByText('Published Projects')).toBeDefined();
  });

  it('/professionals/[professionalSlug] throws notFound() for invalid studio slug', async () => {
    await expect(
      ProfessionalDetailPage({
        params: Promise.resolve({ professionalSlug: 'unknown-studio' }),
      })
    ).rejects.toThrow('NEXT_NOT_FOUND');
  });

  it('/categories/[categorySlug] renders category title, SEO description, and filtered projects', async () => {
    const page = await CategoryPage({
      params: Promise.resolve({ categorySlug: 'tv-units' }),
    });
    render(page);

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain(
      'TV Units Design Projects'
    );
    expect(screen.getByText(/From floating marble consoles/i)).toBeDefined();
    expect(screen.getByText('Modern Fluted TV Unit & Statuario Console')).toBeDefined();
  });

  it('/categories/[categorySlug] throws notFound() for invalid category slug', async () => {
    await expect(
      CategoryPage({
        params: Promise.resolve({ categorySlug: 'invalid-category-slug' }),
      })
    ).rejects.toThrow('NEXT_NOT_FOUND');
  });

  it('/locations/[locationSlug] renders location title, regional context, and local projects', async () => {
    const page = await LocationPage({
      params: Promise.resolve({ locationSlug: 'guntur' }),
    });
    render(page);

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain(
      'Interior Design & Architecture in Guntur'
    );
    expect(screen.getByText(/Projects in Guntur/i)).toBeDefined();
  });

  it('/locations/[locationSlug] throws notFound() for invalid location slug', async () => {
    await expect(
      LocationPage({
        params: Promise.resolve({ locationSlug: 'invalid-city' }),
      })
    ).rejects.toThrow('NEXT_NOT_FOUND');
  });
});
