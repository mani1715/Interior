import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { PublicHeader } from '../navigation/PublicHeader';

let mockAuthState = {
  user: null as any,
  isAuthenticated: false,
  logout: vi.fn(),
};

// Mock auth context
vi.mock('@/lib/auth/auth-context', () => ({
  useAuth: () => mockAuthState,
}));

describe('PublicHeader Navigation', () => {
  beforeEach(() => {
    mockAuthState = {
      user: null,
      isAuthenticated: false,
      logout: vi.fn(),
    };
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

  it('displays Admin Console link for ADMIN role', () => {
    mockAuthState = {
      isAuthenticated: true,
      logout: vi.fn(),
      user: {
        id: 'usr-admin-1',
        displayName: 'Platform Admin',
        email: 'admin@elegance.test',
        status: 'ACTIVE',
        roles: ['ADMIN'],
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'PASSWORD',
      },
    };

    render(<PublicHeader />);
    const adminLink = screen.getAllByRole('link', { name: /admin console/i });
    expect(adminLink.length).toBeGreaterThan(0);
    expect(adminLink[0].getAttribute('href')).toBe('/admin');
  });

  it('displays Admin Console link for SUPER_ADMIN role', () => {
    mockAuthState = {
      isAuthenticated: true,
      logout: vi.fn(),
      user: {
        id: 'usr-super-1',
        displayName: 'Super Admin',
        email: 'super@elegance.test',
        status: 'ACTIVE',
        roles: ['SUPER_ADMIN'],
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'PASSWORD',
      },
    };

    render(<PublicHeader />);
    const adminLink = screen.getAllByRole('link', { name: /admin console/i });
    expect(adminLink.length).toBeGreaterThan(0);
    expect(adminLink[0].getAttribute('href')).toBe('/admin');
  });

  it('hides Admin Console link for CUSTOMER and standard DESIGNER roles', () => {
    mockAuthState = {
      isAuthenticated: true,
      logout: vi.fn(),
      user: {
        id: 'usr-cust-1',
        displayName: 'Client User',
        email: 'client@elegance.test',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'PASSWORD',
      },
    };

    render(<PublicHeader />);
    const adminLinks = screen.queryAllByRole('link', { name: /admin console/i });
    expect(adminLinks.length).toBe(0);
  });
});
