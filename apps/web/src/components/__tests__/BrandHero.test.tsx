import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { BrandHero, brandMotion } from '../home/BrandHero';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('Brand landing experience', () => {
  it('renders editorial split hero with single architectural image and approved copy', () => {
    const { container } = render(<BrandHero />);

    // Single H1 heading
    const h1s = container.querySelectorAll('h1');
    expect(h1s).toHaveLength(1);
    expect(h1s[0].textContent).toContain('Find your kind of space');
    expect(h1s[0].textContent).toContain('Meet its creators');

    // Description text
    expect(screen.getByText(/explore interior projects, room by room/i)).toBeDefined();

    // Primary and secondary CTAs
    const exploreLinks = screen.getAllByRole('link', { name: /explore projects/i });
    expect(exploreLinks[0].getAttribute('href')).toBe('/projects');

    const professionalLinks = screen.getAllByRole('link', { name: /find a professional/i });
    expect(professionalLinks[0].getAttribute('href')).toBe('/professionals');

    // Exactly ONE hero image in the DOM - no duplicate mobile/background images
    const images = container.querySelectorAll('img');
    expect(images).toHaveLength(1);
    expect(images[0].getAttribute('src')).toContain('hero-architectural-walnut.jpg');
    expect(images[0].getAttribute('alt')).toContain('walnut joinery');

    // No 180svh scroll-pinned data-motion attribute
    expect(container.querySelector('[data-motion]')).toBeNull();
  });

  it.each([[true, true], [false, false]])('does not pin for reduced motion %s / desktop %s', (reduced, desktop) => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query.includes('reduced') ? reduced : desktop,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));
    vi.stubGlobal('IntersectionObserver', class {
      observe() {}
      disconnect() {}
    });
    const { container } = render(<BrandHero />);
    expect(container.querySelector('[data-motion]')).toBeNull();
    expect(container.querySelectorAll('[inert]')).toHaveLength(0);
  });

  it('keeps brandMotion compatibility helper valid', () => {
    const state0 = brandMotion(0);
    expect(state0.opening).toBe(1);
    expect(state0.chapter).toBe(0);

    const state1 = brandMotion(1);
    expect(state1.release).toBe(1);
    expect(state1.chapter).toBe(2);
  });
});
