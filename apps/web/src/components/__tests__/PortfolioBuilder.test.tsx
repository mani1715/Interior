import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import PortfolioBuilderPage from '@/app/workspace/portfolio/page';
import PortfolioPreviewPage from '@/app/workspace/portfolio/preview/page';
import { ReferenceTemplate } from '@/components/portfolio/templates/ReferenceTemplate';
import * as portfolioApi from '@/lib/portfolio/api';
import {
  PortfolioDetailResponse,
  PortfolioPreviewResponse,
  PortfolioSectionDto,
} from '@/lib/portfolio/types';

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  usePathname: () => '/workspace/portfolio',
}));

const mockSections: PortfolioSectionDto[] = [
  {
    id: 'sec-hero-1',
    sectionType: 'HERO',
    displayOrder: 0,
    isVisible: true,
    schemaVersion: 1,
    content: {
      title: 'Architectural Interiors & Refined Living',
      subtitle: 'Harmonious spatial design crafted with light and quiet materiality.',
    },
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-about-2',
    sectionType: 'ABOUT',
    displayOrder: 1,
    isVisible: true,
    schemaVersion: 1,
    content: {
      heading: 'About the Studio',
      text: 'Specializing in residential architecture and turnkey curation.',
    },
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-services-3',
    sectionType: 'SERVICES',
    displayOrder: 2,
    isVisible: true,
    schemaVersion: 1,
    content: {
      heading: 'Practice Services',
    },
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-projects-4',
    sectionType: 'FEATURED_PROJECTS',
    displayOrder: 3,
    isVisible: true,
    schemaVersion: 1,
    content: {
      heading: 'Selected Portfolios',
    },
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-philosophy-5',
    sectionType: 'DESIGN_PHILOSOPHY',
    displayOrder: 4,
    isVisible: true,
    schemaVersion: 1,
    content: {
      text: 'Design that endures through materiality and quiet balance.',
    },
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-contact-6',
    sectionType: 'CONTACT_FORM',
    displayOrder: 5,
    isVisible: true,
    schemaVersion: 1,
    content: {},
    updatedAt: '2026-09-18T12:00:00Z',
  },
  {
    id: 'sec-footer-7',
    sectionType: 'FOOTER',
    displayOrder: 6,
    isVisible: true,
    schemaVersion: 1,
    content: {},
    updatedAt: '2026-09-18T12:00:00Z',
  },
];

const mockPortfolioData: PortfolioDetailResponse = {
  id: 'portfolio-101',
  studioId: 'studio-999',
  templateKey: 'BASIC',
  status: 'DRAFT',
  headline: 'Architectural Interiors & Refined Living',
  subheadline: 'Harmonious spatial design crafted with light and quiet materiality.',
  bio: 'A multidisciplinary interior architecture atelier based in Bengaluru.',
  designPhilosophy: 'Form follows spatial harmony and honest tactile materials.',
  yearsOfExperience: 10,
  primaryColor: '#1F2937',
  secondaryColor: '#F3F4F6',
  accentColor: '#C5A880',
  fontPairing: 'PLAYFAIR_INTER',
  version: 1,
  isReadyForPublish: true,
  readinessMissingRequirements: [],
  sections: mockSections,
  recentVersions: [
    {
      id: 'ver-1',
      versionNumber: 1,
      label: 'Initial configuration',
      createdBy: 'user-123',
      createdAt: '2026-09-18T10:00:00Z',
    },
  ],
  createdAt: '2026-09-18T10:00:00Z',
  updatedAt: '2026-09-18T12:00:00Z',
};

const mockPreviewData: PortfolioPreviewResponse = {
  portfolioId: 'portfolio-101',
  studioId: 'studio-999',
  studioName: 'Aarav Design Atelier',
  studioSlug: 'aarav-atelier',
  professionalType: 'INTERIOR_STUDIO',
  professionalTitle: 'Principal Architect',
  studioCity: 'Bengaluru',
  studioState: 'Karnataka',
  templateKey: 'BASIC',
  status: 'DRAFT',
  headline: 'Architectural Interiors & Refined Living',
  subheadline: 'Harmonious spatial design crafted with light and quiet materiality.',
  bio: 'A multidisciplinary interior architecture atelier based in Bengaluru.',
  designPhilosophy: 'Form follows spatial harmony and honest tactile materials.',
  yearsOfExperience: 10,
  primaryColor: '#1F2937',
  secondaryColor: '#F3F4F6',
  accentColor: '#C5A880',
  fontPairing: 'PLAYFAIR_INTER',
  publicContacts: [
    { kind: 'EMAIL', contactValue: 'studio@aarav.in' },
    { kind: 'PHONE', contactValue: '+919876543210' },
  ],
  canonicalServices: [
    { serviceCode: 'RESIDENTIAL', serviceName: 'Residential Interior Design' },
  ],
  canonicalSpecialties: [
    { specialtyCode: 'WARM_CONTEMPORARY', specialtyName: 'Warm Contemporary' },
  ],
  canonicalServiceAreas: [
    { cityName: 'Bengaluru', locality: 'Indiranagar' },
  ],
  visibleSections: mockSections.map((s) => ({
    sectionId: s.id,
    sectionType: s.sectionType,
    displayOrder: s.displayOrder,
    schemaVersion: s.schemaVersion,
    content: s.content,
  })),
  previewGeneratedAt: '2026-09-18T12:30:00Z',
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('Phase 10: Portfolio Builder Engine', () => {
  it('renders PortfolioBuilderPage with studio details, status badge, and sections', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Portfolio Builder').length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.getByText('DRAFT')).toBeDefined();
    expect(screen.getByText('v1')).toBeDefined();
    expect(screen.getByText('Hero Headline')).toBeDefined();
    expect(screen.getByDisplayValue('Architectural Interiors & Refined Living')).toBeDefined();
  });

  it('navigates through tabs: Content, Sections, Design & Themes, and Snapshots', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Editorial Content')).toBeDefined();
    });

    // Switch to Sections tab
    const sectionsTab = screen.getByRole('button', { name: /sections/i });
    fireEvent.click(sectionsTab);
    expect(screen.getByText('Portfolio Structure')).toBeDefined();
    expect(screen.getByText('HERO')).toBeDefined();
    expect(screen.getByText('ABOUT')).toBeDefined();
    expect(screen.getByText('SERVICES')).toBeDefined();

    // Switch to Design tab
    const designTab = screen.getByRole('button', { name: /design & themes/i });
    fireEvent.click(designTab);
    expect(screen.getByText('Aesthetic Themes')).toBeDefined();
    expect(screen.getByText('Clean Editorial')).toBeDefined();
    expect(screen.getByText('Modern Minimalist')).toBeDefined();
    expect(screen.getByText('Luxury Atelier')).toBeDefined();
    expect(screen.getByText('Architectural Monograph')).toBeDefined();
    expect(screen.getByText('Warm & Natural')).toBeDefined();
    expect(screen.getByText('Dark Cinematic')).toBeDefined();

    // Switch to Snapshots tab
    const versionsTab = screen.getByRole('button', { name: /snapshots/i });
    fireEvent.click(versionsTab);
    expect(screen.getByText('Version History & Snapshots')).toBeDefined();
    expect(screen.getByText('Initial configuration')).toBeDefined();
    expect(screen.getAllByText('v1').length).toBeGreaterThanOrEqual(1);
  });

  it('edits content and saves updates with aggregate version tracking', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);
    const updateSpy = vi.spyOn(portfolioApi, 'updatePortfolio').mockResolvedValue({
      ...mockPortfolioData,
      headline: 'Updated Contemporary Interiors',
      version: 2,
    });

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Architectural Interiors & Refined Living')).toBeDefined();
    });

    const headlineInput = screen.getByDisplayValue('Architectural Interiors & Refined Living');
    fireEvent.change(headlineInput, { target: { value: 'Updated Contemporary Interiors' } });

    // Save button becomes active
    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    expect(saveBtn.hasAttribute('disabled')).toBe(false);

    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          headline: 'Updated Contemporary Interiors',
          version: 1,
        })
      );
      expect(screen.getByText('Portfolio settings saved successfully.')).toBeDefined();
    });
  });

  it('toggles section visibility and reorders sections with accessible Move Up/Down buttons', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);
    const toggleSpy = vi.spyOn(portfolioApi, 'updateSection').mockResolvedValue({
      ...mockPortfolioData,
      sections: mockSections.map((s) => (s.id === 'sec-hero-1' ? { ...s, isVisible: false } : s)),
    });
    const reorderSpy = vi.spyOn(portfolioApi, 'reorderSections').mockResolvedValue(mockPortfolioData);

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getByText('Hero Headline')).toBeDefined();
    });

    // Go to Sections tab
    fireEvent.click(screen.getByRole('button', { name: /sections/i }));

    // Toggle hero visibility
    const toggleHeroBtn = screen.getByRole('button', { name: /toggle visibility for HERO/i });
    fireEvent.click(toggleHeroBtn);

    expect(toggleSpy).toHaveBeenCalledWith(
      'sec-hero-1',
      expect.objectContaining({
        isVisible: false,
      })
    );

    // Move About section up (it's at index 1)
    const moveAboutUpBtn = screen.getByRole('button', { name: /move ABOUT up/i });
    fireEvent.click(moveAboutUpBtn);

    expect(reorderSpy).toHaveBeenCalledWith({
      orderedSectionIds: expect.arrayContaining(['sec-about-2', 'sec-hero-1']),
    });
  });

  it('switches templates and preserves all content (Content != Template)', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);
    const switchSpy = vi.spyOn(portfolioApi, 'switchTemplate').mockResolvedValue({
      ...mockPortfolioData,
      templateKey: 'MODERN',
      version: 2,
    });

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getByText('Hero Headline')).toBeDefined();
    });

    // Go to Design tab
    fireEvent.click(screen.getByRole('button', { name: /design & themes/i }));

    // Click "Modern Minimalist" card
    const modernCard = screen.getByText('Modern Minimalist');
    fireEvent.click(modernCard);

    await waitFor(() => {
      expect(switchSpy).toHaveBeenCalledWith({
        templateKey: 'MODERN',
        version: 1,
      });
      expect(screen.getByText(/Switched template to MODERN/i)).toBeDefined();
    });
  });

  it('creates and restores version snapshots', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolio').mockResolvedValue(mockPortfolioData);
    const createVerSpy = vi.spyOn(portfolioApi, 'createVersionSnapshot').mockResolvedValue({
      ...mockPortfolioData,
      version: 2,
      recentVersions: [
        {
          id: 'ver-2',
          versionNumber: 2,
          label: 'Editorial Milestone',
          createdBy: 'user-123',
          createdAt: '2026-09-18T12:00:00Z',
        },
        ...mockPortfolioData.recentVersions,
      ],
    });
    const restoreVerSpy = vi.spyOn(portfolioApi, 'restoreVersionSnapshot').mockResolvedValue(mockPortfolioData);

    // Mock confirm dialog
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<PortfolioBuilderPage />);

    await waitFor(() => {
      expect(screen.getByText('Hero Headline')).toBeDefined();
    });

    // Go to Snapshots tab
    fireEvent.click(screen.getByRole('button', { name: /snapshots/i }));

    // Fill snapshot label
    const labelInput = screen.getByPlaceholderText(/e.g. Before spring editorial redesign/i);
    fireEvent.change(labelInput, { target: { value: 'Editorial Milestone' } });

    const snapshotBtn = screen.getByRole('button', { name: 'Snapshot' });
    fireEvent.click(snapshotBtn);

    await waitFor(() => {
      expect(createVerSpy).toHaveBeenCalledWith({
        label: 'Editorial Milestone',
      });
    });

    // Restore v1
    const restoreBtn = screen.getAllByRole('button', { name: 'Restore' })[0];
    fireEvent.click(restoreBtn);

    await waitFor(() => {
      expect(restoreVerSpy).toHaveBeenCalledWith(2, {
        version: 2,
      });
    });
  });

  it('renders ReferenceTemplate with graceful empty states for projects and privacy-filtered contacts', () => {
    render(
      <ReferenceTemplate
        portfolioId="portfolio-101"
        studioId="studio-999"
        studioName="Aarav Design Atelier"
        studioSlug="aarav-atelier"
        templateKey="BASIC"
        headline="Architectural Interiors & Refined Living"
        subheadline="Harmonious spatial design crafted with light and quiet materiality."
        bio="A multidisciplinary interior architecture atelier."
        publicContacts={[
          { kind: 'EMAIL', contactValue: 'studio@aarav.in' },
        ]}
        canonicalServices={[
          { serviceCode: 'RESIDENTIAL', serviceName: 'Residential Design' },
        ]}
        canonicalSpecialties={[]}
        canonicalServiceAreas={[]}
        visibleSections={mockSections.map((s) => ({
          sectionId: s.id,
          sectionType: s.sectionType,
          displayOrder: s.displayOrder,
          schemaVersion: s.schemaVersion,
          content: s.content,
        }))}
      />
    );

    // Studio details
    expect(screen.getAllByText('Aarav Design Atelier').length).toBeGreaterThanOrEqual(1);
    // Headline
    expect(screen.getByText('Architectural Interiors & Refined Living')).toBeDefined();
    // Zero projects graceful empty state
    expect(screen.getByText('Projects In Curation')).toBeDefined();
    expect(screen.getByText(/case studies and project photography are currently being prepared/i)).toBeDefined();
    // Public contact present
    expect(screen.getByText('studio@aarav.in')).toBeDefined();
  });

  it('renders PortfolioPreviewPage with noindex nofollow robots meta protection', async () => {
    vi.spyOn(portfolioApi, 'fetchPortfolioPreview').mockResolvedValue(mockPreviewData);

    render(<PortfolioPreviewPage />);

    await waitFor(() => {
      expect(screen.getByText('Private Studio Preview')).toBeDefined();
      expect(screen.getByText('NOT PUBLISHED')).toBeDefined();
    });

    // Ensure link back to editor
    expect(screen.getByRole('link', { name: /editor/i })).toBeDefined();
  });
});
