import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  fetchDiscoveryProjects,
  fetchDiscoveryProfessionals,
  fetchDiscoverySuggestions,
  fetchDiscoveryFacets,
  mapDiscoveryCardToProject,
  mapDiscoveryCardToProfessional,
  DiscoveryProjectCard,
  DiscoveryProfessionalCard,
} from '@/lib/discovery/api';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { DiscoverySearchBar } from '@/components/discovery/DiscoverySearchBar';
import { Project } from '@/lib/discovery/types';

// Mock next/navigation
const mockReplace = vi.fn();
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
  }),
  usePathname: () => '/projects',
  useSearchParams: () => new URLSearchParams(),
  notFound: vi.fn(),
}));

describe('Phase 25 — Search & Discovery Engine Frontend', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Discovery API Client & Mappers', () => {
    it('correctly maps DiscoveryProjectCard to Project domain object', () => {
      const card: DiscoveryProjectCard = {
        id: '01912345-6789-7abc-def0-123456789abc',
        slug: 'indiranagar-penthouse',
        title: 'Indiranagar Penthouse',
        shortDescription: 'Luxurious penthouse',
        categoryCode: 'LIVING_ROOM',
        categoryName: 'Living Room',
        styleCodes: ['MODERN_MINIMALIST'],
        styleNames: ['Modern Minimalist'],
        city: 'Bengaluru',
        state: 'Karnataka',
        propertyType: 'APARTMENT',
        projectScope: 'FULL_INTERIOR',
        coverImageUrl: 'https://cdn.example.com/penthouse-medium.webp',
        isAiConceptCover: false,
        studioId: '01912345-0000-7abc-def0-000000000001',
        studioSlug: 'mehta-design',
        studioName: 'Mehta Design Studio',
        professionalType: 'INTERIOR_STUDIO',
        professionalTypeLabel: 'Interior Studio',
        completionYear: 2025,
      };

      const project = mapDiscoveryCardToProject(card);
      expect(project.id).toBe(card.id);
      expect(project.slug).toBe(card.slug);
      expect(project.title).toBe('Indiranagar Penthouse');
      expect(project.coverImage).toBe('https://cdn.example.com/penthouse-medium.webp');
      expect(project.isAiConceptCover).toBe(false);
      expect(project.studioName).toBe('Mehta Design Studio');
    });

    it('correctly maps DiscoveryProfessionalCard to Professional domain object', () => {
      const card: DiscoveryProfessionalCard = {
        id: '01912345-0000-7abc-def0-000000000001',
        slug: 'mehta-design',
        name: 'Mehta Design Studio',
        professionalType: 'INTERIOR_STUDIO',
        professionalTypeLabel: 'Interior Studio',
        professionalTitle: 'Principal Designer',
        tagline: 'Crafting bespoke spaces',
        city: 'Bengaluru',
        state: 'Karnataka',
        experienceSinceYear: 2018,
        services: ['Residential Interior'],
        specialties: ['Modern Minimalist'],
        projectCount: 5,
        sampleProjectCoverUrls: ['https://cdn.example.com/sample1.webp'],
      };

      const prof = mapDiscoveryCardToProfessional(card);
      expect(prof.id).toBe(card.id);
      expect(prof.slug).toBe('mehta-design');
      expect(prof.name).toBe('Mehta Design Studio');
      expect(prof.services).toContain('Residential Interior');
      expect(prof.specialties).toContain('Modern Minimalist');
    });
  });

  describe('ProjectCard AI Concept Badge', () => {
    const baseProject: Project = {
      id: 'proj-1',
      slug: 'test-project',
      title: 'Minimalist Residence',
      category: 'living-room',
      categoryName: 'Living Room',
      location: 'bengaluru',
      locationName: 'Bengaluru',
      style: 'modern',
      styleName: 'Modern Minimalist',
      budgetRange: '1l-3l',
      budgetLabel: '₹20L - ₹30L',
      propertyType: 'apartment',
      propertyTypeName: 'Apartment',
      professionalType: 'interior-studio',
      professionalTypeName: 'Interior Studio',
      professionalSlug: 'studio-alpha',
      professionalName: 'Studio Alpha',
      studioName: 'Studio Alpha',
      description: 'A contemporary space',
      scope: 'Full Interior',
      coverImage: 'https://cdn.example.com/cover.webp',
      gallery: [],
      materials: ['Teak', 'Brass'],
      completionYear: 2025,
      duration: '4 months',
      services: ['Interior Design'],
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('renders mandatory visible ✦ AI Concept Visualization badge when isAiConceptCover is true', () => {
      const aiProject = { ...baseProject, isAiConceptCover: true };
      render(<ProjectCard project={aiProject} />);

      expect(screen.getByText('✦ AI Concept Visualization')).toBeDefined();
      expect(screen.getByLabelText('AI Concept Visualization')).toBeDefined();
    });

    it('does not render AI Concept badge when isAiConceptCover is false', () => {
      const realProject = { ...baseProject, isAiConceptCover: false };
      render(<ProjectCard project={realProject} />);

      expect(screen.queryByText('✦ AI Concept Visualization')).toBeNull();
    });

    it('has touch target at least 44px on interactive elements', () => {
      render(<ProjectCard project={baseProject} />);
      const saveBtn = screen.getByLabelText(/Save to inspiration/i);
      expect(saveBtn.className).toContain('min-h-[44px]');
      expect(saveBtn.className).toContain('min-w-[44px]');
    });
  });

  describe('DiscoverySearchBar Autocomplete & Combobox Semantics', () => {
    it('has ARIA combobox semantics and accessible input', () => {
      const handleSearch = vi.fn();
      render(<DiscoverySearchBar onSearch={handleSearch} />);

      const input = screen.getByRole('combobox');
      expect(input).toBeDefined();
      expect(input.getAttribute('aria-autocomplete')).toBe('list');
      expect(input.getAttribute('aria-expanded')).toBe('false');
    });

    it('triggers search when submitting form or clicking search button', () => {
      const handleSearch = vi.fn();
      render(<DiscoverySearchBar onSearch={handleSearch} />);

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'Modular Kitchen' } });

      const searchBtn = screen.getByRole('button', { name: /^search$/i });
      fireEvent.click(searchBtn);

      expect(handleSearch).toHaveBeenCalledWith('Modular Kitchen');
    });

    it('clears query and calls onSearch with empty string', () => {
      const handleSearch = vi.fn();
      render(<DiscoverySearchBar initialQuery="Scandinavian" onSearch={handleSearch} />);

      const clearBtn = screen.getByLabelText(/clear search input/i);
      fireEvent.click(clearBtn);

      expect(handleSearch).toHaveBeenCalledWith('');
    });
  });

  describe('Truthful Failure & Empty State Gating (No Demo Fallback)', () => {
    it('Backend 500: renders truthful error state and never renders demo projects', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockImplementationOnce(() =>
        Promise.resolve({
          ok: false,
          status: 500,
          json: () => Promise.resolve({ error: 'Internal Server Error' }),
        } as any)
      );

      const { ProjectsDiscoveryClient } = await import('@/components/discovery/ProjectsDiscoveryClient');
      render(<ProjectsDiscoveryClient initialFilters={{}} />);

      await waitFor(() => {
        expect(screen.getByText('Discovery is temporarily unavailable')).toBeDefined();
        expect(screen.getByText(/We are unable to load projects at this moment/i)).toBeDefined();
        expect(screen.getByRole('button', { name: /try again/i })).toBeDefined();
      });

      // Assert ZERO fake / demo projects are rendered
      expect(screen.queryByText(/Contemporary Teak Living Room/i)).toBeNull();
      expect(screen.queryByText(/Scandinavian Dining Space/i)).toBeNull();
      fetchSpy.mockRestore();
    });

    it('Network failure: renders truthful unavailable state and never renders fake professionals', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockImplementationOnce(() =>
        Promise.reject(new Error('Failed to fetch: Network unreachable'))
      );

      const { ProfessionalsDiscoveryClient } = await import('@/components/discovery/ProfessionalsDiscoveryClient');
      render(<ProfessionalsDiscoveryClient />);

      await waitFor(() => {
        expect(screen.getByText('Discovery is temporarily unavailable')).toBeDefined();
        expect(screen.getByText(/We are unable to load professionals at this moment/i)).toBeDefined();
        expect(screen.getByRole('button', { name: /try again/i })).toBeDefined();
      });

      // Assert ZERO fake / demo professionals are rendered
      expect(screen.queryByText(/Studio Atelier Minimal/i)).toBeNull();
      expect(screen.queryByText(/Deccan Craft Guild/i)).toBeNull();
      fetchSpy.mockRestore();
    });

    it('Timeout / Retry state: clicking Try Again initiates a retry request', async () => {
      let callCount = 0;
      const fetchSpy = vi.spyOn(global, 'fetch').mockImplementation(() => {
        callCount++;
        return Promise.reject(new Error('ETIMEDOUT: Connection timed out'));
      });

      const { ProjectsDiscoveryClient } = await import('@/components/discovery/ProjectsDiscoveryClient');
      render(<ProjectsDiscoveryClient initialFilters={{}} />);

      await waitFor(() => {
        expect(screen.getByText('Discovery is temporarily unavailable')).toBeDefined();
      });

      const initialCalls = callCount;
      const retryBtn = screen.getByRole('button', { name: /try again/i });
      fireEvent.click(retryBtn);

      await waitFor(() => {
        expect(callCount).toBeGreaterThan(initialCalls);
      });

      fetchSpy.mockRestore();
    });

    it('Empty legitimate response: renders truthful empty state without demo fallback', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          json: () =>
            Promise.resolve({
              projects: [],
              totalProjects: 0,
              totalProfessionals: 0,
              facets: { categories: [], cities: [], styles: [], professionalTypes: [] },
              hasMore: false,
            }),
        } as any)
      );

      const { ProjectsDiscoveryClient } = await import('@/components/discovery/ProjectsDiscoveryClient');
      render(<ProjectsDiscoveryClient initialFilters={{}} />);

      await waitFor(() => {
        expect(screen.getByText('No projects match these filters')).toBeDefined();
        expect(screen.getByText(/Try broadening your location/i)).toBeDefined();
        expect(screen.getByRole('button', { name: /clear all filters/i })).toBeDefined();
      });

      // Assert no fallback demo cards
      expect(screen.queryByText(/Contemporary Teak Living Room/i)).toBeNull();
      fetchSpy.mockRestore();
    });
  });
});
