import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { PublicHeader } from '../navigation/PublicHeader';

// Mock auth context
vi.mock('@/lib/auth/auth-context', () => ({
  useAuth: () => ({
    user: null,
    isAuthenticated: false,
    logout: vi.fn(),
  }),
}));

describe('PublicHeader Navigation', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));
  });

  it('renders primary navigation links to Projects, Professionals, AI Visualizer, and Professional onboarding', () => {
    render(<PublicHeader />);
    const projectsLinks = screen.getAllByRole('link', { name: /projects/i });
    expect(projectsLinks.some(l => l.getAttribute('href') === '/projects')).toBe(true);

    const profLinks = screen.getAllByRole('link', { name: /professionals/i });
    expect(profLinks.some(l => l.getAttribute('href') === '/professionals')).toBe(true);

    const forProfLinks = screen.getAllByRole('link', { name: /for professionals/i });
    expect(forProfLinks.some(l => l.getAttribute('href') === '/onboarding/professional')).toBe(true);
  });

  it('renders mobile Projects shortcut and toggles mobile menu panel with accessibility', () => {
    render(<PublicHeader />);
    const shortcut = screen.getByRole('link', { name: 'Explore projects' });
    expect(shortcut.getAttribute('href')).toBe('/projects');

    const toggle = screen.getByRole('button', { name: /open menu/i });
    expect(toggle.getAttribute('aria-expanded')).toBe('false');

    fireEvent.click(toggle);
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(screen.getByRole('dialog', { name: /navigation menu/i })).toBeDefined();

    const closeBtn = screen.getByRole('button', { name: /close navigation/i });
    fireEvent.click(closeBtn);
    expect(screen.queryByRole('dialog', { name: /navigation menu/i })).toBeNull();
  });
});
