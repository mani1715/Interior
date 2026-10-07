import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { BrandHero, brandMotion } from '../home/BrandHero';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('Brand landing experience', () => {
  it('provides a useful unpinned fallback without browser animation APIs', () => {
    vi.stubGlobal('matchMedia', undefined);
    const {container} = render(<BrandHero />);
    expect(container.querySelector('[data-motion]')).toBeNull();
    expect(container.querySelectorAll('h1')).toHaveLength(1);
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Find your kind of space');
    const exploreLinks = screen.getAllByRole('link', { name: /explore projects/i });
    expect(exploreLinks[0].getAttribute('href')).toBe('/projects');
    const professionalLinks = screen.getAllByRole('link', { name: /find a professional/i });
    expect(professionalLinks[0].getAttribute('href')).toBe('/professionals');
  });
  it.each([[true,true],[false,false]])('does not pin for reduced motion %s / desktop %s', (reduced, desktop) => {
    vi.stubGlobal('matchMedia', (query: string) => ({matches:query.includes('reduced') ? reduced : desktop, addEventListener:vi.fn(),removeEventListener:vi.fn()}));
    vi.stubGlobal('IntersectionObserver', class {observe() {} disconnect() {}});
    const {container} = render(<BrandHero />);
    expect(container.querySelector('[data-motion]')).toBeNull();
    expect(container.querySelectorAll('[inert]')).toHaveLength(0);
  });
  it('keeps chapter copy from overlapping throughout the camera path and settles at the destination', () => {
    for (let step = 0; step <= 1000; step++) {
      const state = brandMotion(step / 1000);
      expect([state.opening, state.craft, state.release].filter(opacity => opacity > .001).length).toBeLessThanOrEqual(1);
      expect(state.approach).toBeGreaterThanOrEqual(0);
      expect(state.approach).toBeLessThanOrEqual(1);
    }
    expect(brandMotion(1)).toMatchObject({release:1,opening:0,craft:0,living:1,chapter:2});
  });
});
