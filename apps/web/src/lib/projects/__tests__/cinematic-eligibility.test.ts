import { describe, expect, it } from 'vitest';
import {
  evaluateCinematicEligibility,
  evaluateProjectCinematicEligibility,
  GLOBAL_SCALE_CAP,
  MAX_STICKY_ROOMS_PER_PROJECT,
  TEMPLATE_SCALE_CAPS,
} from '../cinematic-eligibility';
import { NormalizedPhoto, NormalizedRoom } from '../showcase-types';

function createMockPhoto(overrides: Partial<NormalizedPhoto> = {}): NormalizedPhoto {
  return {
    id: 'photo-test-1',
    roomId: 'room-1',
    isCover: true,
    isRoomCover: true,
    isAiConcept: false,
    mediaType: 'REAL_PROJECT',
    sortOrder: 0,
    altText: 'Test Room Photo',
    caption: 'Sample photo caption',
    focalX: 0.5,
    focalY: 0.5,
    motionEnabled: true,
    width: 1920,
    height: 1080,
    aspectRatio: 1.77,
    thumbnailUrl: 'https://example.com/thumb.jpg',
    mediumUrl: 'https://example.com/med.jpg',
    largeUrl: 'https://example.com/large.jpg',
    derivatives: [],
    ...overrides,
  };
}

describe('Cinematic Eligibility Engine', () => {
  const desktopViewport = { width: 1440, height: 900 };

  describe('Presentation Mode & System Accessibility', () => {
    it('rejects motion when presentationMode is STANDARD', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'STANDARD',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.variant).toBe('STATIC_FEATURE');
      expect(result.maxScale).toBe(1.0);
      expect(result.reason).toContain('STANDARD');
    });

    it('rejects motion when prefersReducedMotion is active', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        prefersReducedMotion: true,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.reason).toContain('reduced motion');
    });

    it('rejects motion when forcedColors is active', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        forcedColors: true,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.reason).toContain('Forced colors');
    });
  });

  describe('Viewport Desktop Thresholds', () => {
    it('rejects mobile viewports (e.g. 390x844)', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: { width: 390, height: 844 },
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.reason).toContain('below desktop threshold');
    });

    it('rejects tablet viewports (e.g. 1024x768)', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: { width: 1024, height: 768 },
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
    });

    it('rejects narrow desktop viewports below 800px height (e.g. 1280x720)', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: { width: 1280, height: 720 },
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
    });

    it('accepts viewports >= 1280x800', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: { width: 1280, height: 800 },
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
    });
  });

  describe('Focal Safe Zone [0.15, 0.85]', () => {
    it('accepts focal points safely within boundary', () => {
      const photo = createMockPhoto({ focalX: 0.15, focalY: 0.85 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
    });

    it('rejects focal point outside boundary (focalX < 0.15)', () => {
      const photo = createMockPhoto({ focalX: 0.10, focalY: 0.50 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.reason).toContain('outside safe region');
    });

    it('rejects focal point outside boundary (focalY > 0.85)', () => {
      const photo = createMockPhoto({ focalX: 0.50, focalY: 0.90 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
    });
  });

  describe('Aspect Ratio Variants & Caps', () => {
    it('assigns FOCUS_PUSH for aspect ratios between 1.2 and 2.2', () => {
      const photo = createMockPhoto({ aspectRatio: 1.6 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
        templateKey: 'MODERN',
      });

      expect(result.eligible).toBe(true);
      expect(result.variant).toBe('FOCUS_PUSH');
      expect(result.maxScale).toBe(1.03);
      expect(result.maxDriftPx).toBe(0);
    });

    it('assigns GENTLE_DRIFT for aspect ratios between 2.2 and 3.0', () => {
      const photo = createMockPhoto({ aspectRatio: 2.5 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
      expect(result.variant).toBe('GENTLE_DRIFT');
      expect(result.maxScale).toBe(1.0);
      expect(result.maxDriftPx).toBeLessThanOrEqual(16);
      expect(result.maxDriftPx).toBeGreaterThanOrEqual(8);
    });

    it('rejects portrait aspect ratios (< 1.2)', () => {
      const photo = createMockPhoto({ aspectRatio: 0.8 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
      expect(result.reason).toContain('outside motion eligibility ranges');
    });

    it('rejects extreme panoramas (> 3.0)', () => {
      const photo = createMockPhoto({ aspectRatio: 3.5 });
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(false);
    });

    it('respects template caps and never exceeds GLOBAL_SCALE_CAP (1.04)', () => {
      const photo = createMockPhoto({ aspectRatio: 1.5 });
      const darkResult = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
        templateKey: 'DARK_CINEMATIC',
      });
      expect(darkResult.maxScale).toBe(1.04);
      expect(darkResult.maxScale).toBeLessThanOrEqual(GLOBAL_SCALE_CAP);

      const basicResult = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
        templateKey: 'BASIC',
      });
      expect(basicResult.maxScale).toBe(1.02);
    });
  });

  describe('Sticky Pin Chapters Cap', () => {
    it('grants sticky pin eligibility for rooms with >= 4 photos under 3-room cap', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 0,
        stickyRoomsCount: 2,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
      expect(result.stickyEligible).toBe(true);
    });

    it('denies sticky pin eligibility when project already has 3 sticky chapters', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 4,
        roomIndex: 3,
        stickyRoomsCount: 3,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
      expect(result.stickyEligible).toBe(false);
    });

    it('denies sticky pin eligibility for rooms with < 4 photos (unpinned motion only)', () => {
      const photo = createMockPhoto();
      const result = evaluateCinematicEligibility({
        presentationMode: 'CINEMATIC',
        viewport: desktopViewport,
        photoCount: 2,
        roomIndex: 0,
        stickyRoomsCount: 0,
        candidatePhoto: photo,
      });

      expect(result.eligible).toBe(true);
      expect(result.stickyEligible).toBe(false);
    });
  });

  describe('evaluateProjectCinematicEligibility (Project Multi-room)', () => {
    const mockRooms: NormalizedRoom[] = [
      {
        id: 'room-1',
        roomType: 'LIVING_ROOM',
        label: 'Living Room',
        displayOrder: 1,
        coverPhoto: createMockPhoto({ id: 'p1', roomId: 'room-1' }),
        eligiblePhotoCount: 4,
        photos: [
          createMockPhoto({ id: 'p1', roomId: 'room-1', isCover: true, motionEnabled: true }),
          createMockPhoto({ id: 'p2', roomId: 'room-1', isCover: false, motionEnabled: false }),
          createMockPhoto({ id: 'p3', roomId: 'room-1', isCover: false, motionEnabled: false }),
          createMockPhoto({ id: 'p4', roomId: 'room-1', isCover: false, motionEnabled: false }),
        ],
      },
      {
        id: 'room-2',
        roomType: 'BEDROOM',
        label: 'Bedroom',
        displayOrder: 2,
        coverPhoto: createMockPhoto({ id: 'p5', roomId: 'room-2' }),
        eligiblePhotoCount: 1,
        photos: [
          createMockPhoto({ id: 'p5', roomId: 'room-2', isCover: true, motionEnabled: false }),
        ],
      },
    ];

    it('evaluates multiple rooms and tags eligible vs ineligible', () => {
      const result = evaluateProjectCinematicEligibility(mockRooms, desktopViewport);

      expect(result.isEligible).toBe(true);
      expect(result.eligibleRoomsCount).toBe(1);
      expect(result.roomEligibilities['room-1'].isEligible).toBe(true);
      expect(result.roomEligibilities['room-1'].style).toBe('focus-push');
      expect(result.roomEligibilities['room-2'].isEligible).toBe(false);
      expect(result.roomEligibilities['room-2'].rejectionReason).toContain('motion_enabled');
    });

    it('short-circuits project evaluation on reduced motion', () => {
      const result = evaluateProjectCinematicEligibility(mockRooms, desktopViewport, true);

      expect(result.isEligible).toBe(false);
      expect(result.eligibleRoomsCount).toBe(0);
      expect(result.rejectionReason).toContain('reduced motion');
    });
  });
});
