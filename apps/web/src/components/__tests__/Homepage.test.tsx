import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import HomePage from '../../app/page';
import { BrandHero } from '../home/BrandHero';
import { EditorialHome } from '../home/EditorialHome';

// Mock auth context
vi.mock('@/lib/auth/auth-context', () => ({
  useAuth: () => ({
    user: null,
    isAuthenticated: false,
    logout: vi.fn(),
  }),
}));

// Mock discovery API
vi.mock('@/lib/discovery/api', () => ({
  fetchDiscoveryProjects: vi.fn().mockResolvedValue({
    projects: [
      {
        id: 'p1',
        slug: 'contemporary-penthouse',
        title: 'Contemporary Penthouse',
        categoryCode: 'living-room',
        categoryName: 'Living Room',
        styleCodes: ['contemporary'],
        styleNames: ['Contemporary'],
        city: 'Bangalore',
        coverImageUrl: '/images/approved/hall.png',
        isAiConceptCover: false,
        studioId: 's1',
        studioSlug: 'studio-forma',
        studioName: 'Studio Forma',
        professionalType: 'INTERIOR_DESIGNER',
        professionalTypeLabel: 'Interior Designer',
      },
    ],
    totalProjects: 1,
  }),
  fetchDiscoveryProfessionals: vi.fn().mockResolvedValue({
    professionals: [
      {
        id: 'prof1',
        slug: 'studio-forma',
        name: 'Studio Forma',
        professionalType: 'INTERIOR_DESIGNER',
        professionalTypeLabel: 'Interior Designer',
        city: 'Bangalore',
        services: ['Full Home'],
        specialties: ['Modern Living', 'Custom Cabinetry'],
        projectCount: 5,
        sampleProjectCoverUrls: ['/images/approved/kitchen.png'],
      },
    ],
    totalProfessionals: 1,
  }),
}));

describe('Homepage Production Refinement', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));
    vi.stubGlobal('IntersectionObserver', class {
      observe() {}
      disconnect() {}
      unobserve() {}
    });
  });

  it('renders single H1 in BrandHero with approved copy and primary/secondary CTAs', () => {
    const { container } = render(<BrandHero />);
    const h1s = container.querySelectorAll('h1');
    expect(h1s).toHaveLength(1);
    expect(h1s[0].textContent).toContain('Find your kind of space');
    expect(h1s[0].textContent).toContain('Meet its creators');

    const exploreLink = screen.getAllByRole('link', { name: /explore projects/i });
    expect(exploreLink[0].getAttribute('href')).toBe('/projects');

    const profLink = screen.getAllByRole('link', { name: /find a professional/i });
    expect(profLink[0].getAttribute('href')).toBe('/professionals');
  });

  it('renders Immediate Project Search with accessible label, submit, and quick categories', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /what space are you imagining\?/i })).toBeDefined();

    const input = screen.getByLabelText(/search interior projects by room, style, or feature/i);
    expect(input).toBeDefined();
    expect(input.getAttribute('name')).toBe('q');

    const searchButton = screen.getByRole('button', { name: /search/i });
    expect(searchButton).toBeDefined();

    expect(screen.getByRole('link', { name: 'Modular Kitchens' }).getAttribute('href')).toBe('/projects?category=modular-kitchens');
    expect(screen.getByRole('link', { name: 'Living Rooms' }).getAttribute('href')).toBe('/projects?category=living-room');
    expect(screen.getByRole('link', { name: 'Bedrooms' }).getAttribute('href')).toBe('/projects?category=bedroom');
    expect(screen.getByRole('link', { name: 'Wardrobes' }).getAttribute('href')).toBe('/projects?category=wardrobes');
    expect(screen.getByRole('link', { name: 'Pooja Units' }).getAttribute('href')).toBe('/projects?category=pooja-units');
    expect(screen.getByRole('link', { name: 'TV Units' }).getAttribute('href')).toBe('/projects?category=tv-units');
  });

  it('renders Projects to explore section', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /projects to explore/i })).toBeDefined();
    expect(screen.getByRole('link', { name: /explore all projects/i }).getAttribute('href')).toBe('/projects');
  });

  it('renders Professional Discovery section with heading and CTA', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /find the people/i })).toBeDefined();
    const profLinks = screen.getAllByRole('link', { name: /find a professional/i });
    expect(profLinks.length).toBeGreaterThan(0);
    expect(profLinks[0].getAttribute('href')).toBe('/professionals');
  });

  it('renders Cinematic Portfolio Introduction section', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /a closer sense/i })).toBeDefined();
    expect(screen.getByText(/selected projects unfold room by room with subtle, scroll-led movement/i)).toBeDefined();
    expect(screen.getByText(/cinematic portfolio/i)).toBeDefined();
  });

  it('renders Inspiration / Categories section with category links and AI disclosure', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /every space/i })).toBeDefined();
    const modularKitchenLinks = screen.getAllByRole('link', { name: /modular kitchens/i });
    expect(modularKitchenLinks.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/ai concept visualization — design inspiration/i)).toBeDefined();
  });

  it('renders AI Visualizer section with disclosure and workspace link noting sign-in requirement', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /explore an idea/i })).toBeDefined();
    expect(screen.getByRole('link', { name: /explore the ai workspace/i }).getAttribute('href')).toBe('/workspace/ai');
    expect(screen.getByText(/sign-in required to generate concepts/i)).toBeDefined();
    expect(screen.getByText(/concepts are creative interpretations, not completed projects/i)).toBeDefined();
  });

  it('renders Trust Explanations section with three confidence pillars', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /make a more informed choice/i })).toBeDefined();
    expect(screen.getByText('Work with attribution')).toBeDefined();
    expect(screen.getByText('Reviews with context')).toBeDefined();
    expect(screen.getByText('Verification, explained')).toBeDefined();
  });

  it('renders dual audience Call to Action for visitors and professionals', () => {
    render(<EditorialHome />);
    expect(screen.getByRole('heading', { level: 2, name: /start with work/i })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: /let your work/i })).toBeDefined();
    expect(screen.getByRole('link', { name: /create your professional profile/i }).getAttribute('href')).toBe('/onboarding/professional');
  });

  it('renders full HomePage with main landmark and valid structured data', () => {
    const { container } = render(<HomePage />);
    const main = container.querySelector('main#main-content');
    expect(main).toBeDefined();

    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script?.innerHTML).toContain('https://schema.org');
    expect(script?.innerHTML).toContain('Elégance Interior Platform');
  });
});
