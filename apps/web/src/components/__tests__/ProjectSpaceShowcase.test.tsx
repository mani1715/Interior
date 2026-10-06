import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { ProjectSpaceShowcase } from '../discovery/ProjectSpaceShowcase';
import { ProjectSpaceShowcaseData } from '@/lib/projects/showcase-types';
import { Project } from '@/lib/discovery/types';

beforeEach(() => {
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute('open', '');
  };
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

afterEach(cleanup);

const mockShowcaseData: ProjectSpaceShowcaseData = {
  project: {
    id: 'proj-1',
    slug: 'guntur-residence',
    title: 'Guntur Residence',
    presentationMode: 'STANDARD',
    categoryCode: 'LIVING_ROOM',
    categoryDisplayName: 'Living Room',
    styleCodes: ['MODERN'],
    styleDisplayNames: ['Modern'],
    studio: {
      slug: 'studio-elegance',
      name: 'Studio Elégance',
    },
    coverPhoto: {
      id: 'photo-cov',
      roomId: 'room-1',
      isCover: true,
      isRoomCover: true,
      isAiConcept: false,
      mediaType: 'REAL_PROJECT',
      sortOrder: 0,
      altText: 'Living Room Cover',
      focalX: 0.5,
      focalY: 0.5,
      motionEnabled: true,
      width: 1600,
      height: 1000,
      aspectRatio: 1.6,
      thumbnailUrl: 'https://example.com/thumb.jpg',
      mediumUrl: 'https://example.com/med.jpg',
      largeUrl: 'https://example.com/large.jpg',
      derivatives: [],
    },
  },
  rooms: [
    {
      id: 'room-1',
      roomType: 'LIVING_ROOM',
      label: 'Main Living Room',
      displayOrder: 1,
      coverPhoto: {
        id: 'photo-cov',
        roomId: 'room-1',
        isCover: true,
        isRoomCover: true,
        isAiConcept: false,
        mediaType: 'REAL_PROJECT',
        sortOrder: 0,
        altText: 'Living Room Cover',
        focalX: 0.5,
        focalY: 0.5,
        motionEnabled: true,
        width: 1600,
        height: 1000,
        aspectRatio: 1.6,
        thumbnailUrl: 'https://example.com/thumb.jpg',
        mediumUrl: 'https://example.com/med.jpg',
        largeUrl: 'https://example.com/large.jpg',
        derivatives: [],
      },
      eligiblePhotoCount: 2,
      photos: [
        {
          id: 'photo-1',
          roomId: 'room-1',
          isCover: true,
          isRoomCover: true,
          isAiConcept: false,
          mediaType: 'REAL_PROJECT',
          sortOrder: 0,
          altText: 'Living Room Main View',
          caption: 'North-facing light',
          focalX: 0.5,
          focalY: 0.5,
          motionEnabled: true,
          width: 1600,
          height: 1000,
          aspectRatio: 1.6,
          thumbnailUrl: 'https://example.com/thumb1.jpg',
          mediumUrl: 'https://example.com/med1.jpg',
          largeUrl: 'https://example.com/large1.jpg',
          derivatives: [],
        },
        {
          id: 'photo-2',
          roomId: 'room-1',
          isCover: false,
          isRoomCover: false,
          isAiConcept: true,
          mediaType: 'AI_CONCEPT',
          sortOrder: 1,
          altText: 'Living Room Lighting Concept',
          caption: null,
          focalX: 0.4,
          focalY: 0.6,
          motionEnabled: false,
          width: 1600,
          height: 1000,
          aspectRatio: 1.6,
          thumbnailUrl: 'https://example.com/thumb2.jpg',
          mediumUrl: 'https://example.com/med2.jpg',
          largeUrl: 'https://example.com/large2.jpg',
          derivatives: [],
        },
      ],
    },
    {
      id: 'room-2',
      roomType: 'BEDROOM',
      label: 'Master Bedroom',
      displayOrder: 2,
      coverPhoto: {
        id: 'photo-3',
        roomId: 'room-2',
        isCover: false,
        isRoomCover: true,
        isAiConcept: false,
        mediaType: 'REAL_PROJECT',
        sortOrder: 0,
        altText: 'Master Bedroom Headboard',
        focalX: 0.5,
        focalY: 0.5,
        motionEnabled: true,
        width: 1600,
        height: 1000,
        aspectRatio: 1.6,
        thumbnailUrl: 'https://example.com/thumb3.jpg',
        mediumUrl: 'https://example.com/med3.jpg',
        largeUrl: 'https://example.com/large3.jpg',
        derivatives: [],
      },
      eligiblePhotoCount: 1,
      photos: [
        {
          id: 'photo-3',
          roomId: 'room-2',
          isCover: false,
          isRoomCover: true,
          isAiConcept: false,
          mediaType: 'REAL_PROJECT',
          sortOrder: 0,
          altText: 'Master Bedroom Headboard',
          caption: 'Acoustic fluted walnut headboard',
          focalX: 0.5,
          focalY: 0.5,
          motionEnabled: true,
          width: 1600,
          height: 1000,
          aspectRatio: 1.6,
          thumbnailUrl: 'https://example.com/thumb3.jpg',
          mediumUrl: 'https://example.com/med3.jpg',
          largeUrl: 'https://example.com/large3.jpg',
          derivatives: [],
        },
      ],
    },
  ],
  additionalViews: [
    {
      id: 'photo-add-1',
      roomId: null,
      isCover: false,
      isRoomCover: false,
      isAiConcept: false,
      mediaType: 'REAL_PROJECT',
      sortOrder: 0,
      altText: 'Material detail flatlay',
      caption: 'Italian marble and veneer samples',
      focalX: 0.5,
      focalY: 0.5,
      motionEnabled: true,
      width: 1600,
      height: 1000,
      aspectRatio: 1.6,
      thumbnailUrl: 'https://example.com/thumb-add.jpg',
      mediumUrl: 'https://example.com/med-add.jpg',
      largeUrl: 'https://example.com/large-add.jpg',
      derivatives: [],
    },
  ],
  allPhotos: [],
};

const mockActionProject: Project = {
  id: 'proj-1',
  slug: 'guntur-residence',
  title: 'Guntur Residence',
  category: 'living-room',
  categoryName: 'Living Room',
  location: 'guntur',
  locationName: 'Guntur',
  style: 'modern',
  styleName: 'Modern',
  budgetRange: '',
  budgetLabel: '',
  propertyType: 'apartment',
  propertyTypeName: 'Apartment',
  coverImage: 'https://example.com/large.jpg',
  professionalType: 'interior-studio',
  professionalTypeName: 'Interior Studio',
  professionalSlug: 'studio-elegance',
  professionalName: 'Studio Elégance',
  studioName: 'Studio Elégance',
  description: 'Guntur apartment living space',
  scope: 'Full design',
  gallery: [],
  materials: [],
  duration: '3 months',
  services: [],
  createdAt: '2026-01-01',
};

describe('ProjectSpaceShowcase & Room Stories', () => {
  it('renders sticky navigation with room labels and photo counts', () => {
    render(<ProjectSpaceShowcase data={mockShowcaseData} actionProject={mockActionProject} />);

    const nav = screen.getByRole('navigation', { name: 'Rooms quick navigation' });
    expect(nav).toBeDefined();
    const livingLink = screen.getByRole('link', { name: 'Main Living Room (2)' });
    expect(livingLink).toBeDefined();
    expect(livingLink.getAttribute('href')).toBe('#room-room-1');

    const bedroomLink = screen.getByRole('link', { name: 'Master Bedroom (1)' });
    expect(bedroomLink).toBeDefined();
    expect(bedroomLink.getAttribute('href')).toBe('#room-room-2');

    const addLink = screen.getByRole('link', { name: 'Additional Views (1)' });
    expect(addLink).toBeDefined();
    expect(addLink.getAttribute('href')).toBe('#room-additional-views');
  });

  it('renders room sections and opens room-scoped viewer on click', () => {
    render(<ProjectSpaceShowcase data={mockShowcaseData} actionProject={mockActionProject} />);

    // Check headings
    expect(screen.getByRole('heading', { level: 2, name: 'Main Living Room' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'Master Bedroom' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'Additional Views' })).toBeDefined();

    // Open viewer for Main Living Room
    fireEvent.click(screen.getByRole('button', { name: 'Open full-screen viewer for Main Living Room' }));
    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeDefined();

    // Verify room-scoped counter 01 / 02
    expect(screen.getByText('01 / 02')).toBeDefined();

    // Next photo in living room
    fireEvent.keyDown(dialog, { key: 'ArrowRight' });
    expect(screen.getByText('02 / 02')).toBeDefined();

    // Boundary reached: Next button disabled, arrow right does not advance
    const nextBtn = screen.getByRole('button', { name: 'Next photo' });
    expect(nextBtn.hasAttribute('disabled')).toBe(true);

    // Switch to Master Bedroom collection inside viewer
    fireEvent.click(screen.getByRole('button', { name: 'Master Bedroom, 1 photo' }));
    expect(screen.getByText('01 / 01')).toBeDefined();

    // Close viewer
    fireEvent.click(screen.getByRole('button', { name: 'Close gallery' }));
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('supports zoom in/out, fit reset, and contact sheet toggling', () => {
    render(<ProjectSpaceShowcase data={mockShowcaseData} actionProject={mockActionProject} />);

    fireEvent.click(screen.getByRole('button', { name: 'Open full-screen viewer for Main Living Room' }));
    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeDefined();

    // Zoom controls
    const zoomInBtn = screen.getByRole('button', { name: 'Zoom in' });
    fireEvent.click(zoomInBtn);
    expect(screen.getByText('150%')).toBeDefined();

    const resetZoomBtn = screen.getByRole('button', { name: 'Reset zoom to fit' });
    fireEvent.click(resetZoomBtn);
    expect(screen.getByText('100%')).toBeDefined();

    // Contact Sheet toggle
    const contactSheetBtn = screen.getByRole('button', { name: 'Contact sheet' });
    fireEvent.click(contactSheetBtn);

    // Verify contact sheet scope tabs
    expect(screen.getByRole('tab', { name: /This Room/ })).toBeDefined();
    expect(screen.getByRole('tab', { name: /All Rooms/ })).toBeDefined();

    // Switch to All Rooms tab
    fireEvent.click(screen.getByRole('tab', { name: /All Rooms/ }));
    expect(screen.getAllByText('Italian marble and veneer samples').length).toBeGreaterThan(0);
  });

  it('triggers enquiry sheet modal when clicking Ask About This Project in viewer', () => {
    render(<ProjectSpaceShowcase data={mockShowcaseData} actionProject={mockActionProject} />);

    fireEvent.click(screen.getByRole('button', { name: 'Open full-screen viewer for Main Living Room' }));
    const askBtn = screen.getByRole('button', { name: 'Ask About This Project' });
    expect(askBtn).toBeDefined();

    fireEvent.click(askBtn);

    // Viewer closes, Enquiry modal opens
    expect(screen.queryByRole('dialog', { name: 'Main Living Room' })).toBeNull();
    expect(screen.getByText('I Want Something Similar')).toBeDefined();
    expect(screen.getAllByText(/Guntur Residence/).length).toBeGreaterThan(0);
  });

  it('applies portrait containment styling when single room photo has aspect ratio < 1', () => {
    const portraitData: ProjectSpaceShowcaseData = {
      ...mockShowcaseData,
      rooms: [
        {
          id: 'room-powder',
          roomType: 'BATHROOM',
          label: 'Powder Room',
          displayOrder: 1,
          coverPhoto: {
            ...mockShowcaseData.rooms[0].photos[0],
            id: 'photo-portrait',
            width: 800,
            height: 1200,
            aspectRatio: 0.67,
          },
          eligiblePhotoCount: 1,
          photos: [
            {
              ...mockShowcaseData.rooms[0].photos[0],
              id: 'photo-portrait',
              width: 800,
              height: 1200,
              aspectRatio: 0.67,
            },
          ],
        },
      ],
      additionalViews: [],
    };

    const { container } = render(
      <ProjectSpaceShowcase data={portraitData} actionProject={mockActionProject} />
    );

    const singleGrid = container.querySelector('[class*="gridSingle"]');
    expect(singleGrid).not.toBeNull();
    expect(singleGrid?.className).toContain('singlePortrait');

    const figure = singleGrid?.querySelector('figure');
    expect(figure?.className).toContain('singlePortrait');
  });

  it('renders standard layout without cinematic controller when presentationMode is STANDARD', () => {
    const { container } = render(
      <ProjectSpaceShowcase data={mockShowcaseData} actionProject={mockActionProject} />
    );

    const cinematicControlled = container.querySelectorAll('[data-cinematic-controlled="true"]');
    expect(cinematicControlled.length).toBe(0);
  });

  it('renders cinematic chapter with transform ownership when presentationMode is CINEMATIC and eligible', () => {
    // Mock desktop viewport and IntersectionObserver
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1440 });
    Object.defineProperty(window, 'innerHeight', { writable: true, configurable: true, value: 900 });

    class MockIntersectionObserver {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    }
    window.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;

    const cinematicData: ProjectSpaceShowcaseData = {
      ...mockShowcaseData,
      project: {
        ...mockShowcaseData.project,
        presentationMode: 'CINEMATIC',
      },
      rooms: [
        {
          id: 'room-hero-1',
          roomType: 'LIVING_ROOM',
          label: 'Grand Hall',
          displayOrder: 1,
          coverPhoto: mockShowcaseData.rooms[0].photos[0],
          eligiblePhotoCount: 4,
          photos: [
            {
              ...mockShowcaseData.rooms[0].photos[0],
              id: 'p1',
              aspectRatio: 1.6,
              focalX: 0.5,
              focalY: 0.5,
              motionEnabled: true,
            },
            { ...mockShowcaseData.rooms[0].photos[0], id: 'p2' },
            { ...mockShowcaseData.rooms[0].photos[0], id: 'p3' },
            { ...mockShowcaseData.rooms[0].photos[0], id: 'p4' },
          ],
        },
      ],
      additionalViews: [],
    };

    const { container } = render(
      <ProjectSpaceShowcase data={cinematicData} actionProject={mockActionProject} />
    );

    const controlled = container.querySelectorAll('[data-cinematic-controlled="true"]');
    expect(controlled.length).toBe(1);
  });
});
