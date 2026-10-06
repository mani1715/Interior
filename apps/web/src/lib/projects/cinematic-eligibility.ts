import { NormalizedPhoto, NormalizedRoom } from './showcase-types';

export type CinematicVariant = 'FOCUS_PUSH' | 'GENTLE_DRIFT' | 'STATIC_FEATURE';

export interface ViewportDimensions {
  width: number;
  height: number;
}

export interface CinematicEligibilityResult {
  eligible: boolean;
  variant: CinematicVariant;
  stickyEligible: boolean;
  maxScale: number;
  maxDriftPx: number;
  reason?: string;
}

export interface RoomCinematicEligibility {
  roomId: string;
  isEligible: boolean;
  rejectionReason?: string;
  candidatePhoto?: NormalizedPhoto;
  scaleCap: number;
  maxDriftPx: number;
  style: 'focus-push' | 'gentle-drift';
}

export interface ProjectCinematicEligibility {
  isEligible: boolean;
  rejectionReason?: string;
  eligibleRoomsCount: number;
  roomEligibilities: Record<string, RoomCinematicEligibility>;
}

export interface CinematicEligibilityOptions {
  presentationMode: 'STANDARD' | 'CINEMATIC';
  viewport: ViewportDimensions;
  prefersReducedMotion?: boolean;
  forcedColors?: boolean;
  photoCount: number;
  roomIndex: number;
  stickyRoomsCount: number;
  candidatePhoto: NormalizedPhoto | null;
  templateKey?: string;
  imageLoaded?: boolean;
}

/** Template scale caps as per specification */
export const TEMPLATE_SCALE_CAPS: Record<string, number> = {
  BASIC: 1.02,
  MODERN: 1.03,
  LUXURY: 1.03,
  ARCHITECTURAL: 1.02,
  WARM_NATURAL: 1.02,
  DARK_CINEMATIC: 1.04,
};

export const GLOBAL_SCALE_CAP = 1.04;
export const MAX_STICKY_ROOMS_PER_PROJECT = 3;
export const MIN_VIEWPORT_WIDTH = 1280;
export const MIN_VIEWPORT_HEIGHT = 800;

/**
 * Pure, deterministic evaluation of Cinematic motion & sticky eligibility.
 */
export function evaluateCinematicEligibility(
  options: CinematicEligibilityOptions
): CinematicEligibilityResult {
  const {
    presentationMode,
    viewport,
    prefersReducedMotion = false,
    forcedColors = false,
    photoCount,
    stickyRoomsCount,
    candidatePhoto,
    templateKey = 'BASIC',
    imageLoaded = true,
  } = options;

  // 1. Presentation Mode check
  if (presentationMode !== 'CINEMATIC') {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'Presentation mode is STANDARD',
    };
  }

  // 2. Reduced motion & accessibility
  if (prefersReducedMotion) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'User prefers reduced motion',
    };
  }

  if (forcedColors) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'Forced colors mode active',
    };
  }

  // 3. Viewport threshold (Effective CSS width >= 1280 and height >= 800)
  if (viewport.width < MIN_VIEWPORT_WIDTH || viewport.height < MIN_VIEWPORT_HEIGHT) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: `Viewport dimensions (${viewport.width}x${viewport.height}) below desktop threshold (1280x800)`,
    };
  }

  // 4. Candidate photo presence
  if (!candidatePhoto) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'No candidate photo found',
    };
  }

  // 5. Image readiness & resolution
  if (!imageLoaded) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'Candidate image not loaded or decoded',
    };
  }

  // 6. per-photo motion_enabled permission
  if (!candidatePhoto.motionEnabled) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: 'Photo has motion_enabled=false',
    };
  }

  // 7. Focal point safety (focal_x and focal_y must be between 0.15 and 0.85)
  const fx = candidatePhoto.focalX;
  const fy = candidatePhoto.focalY;
  if (fx < 0.15 || fx > 0.85 || fy < 0.15 || fy > 0.85) {
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: `Focal point (${fx.toFixed(2)}, ${fy.toFixed(2)}) is outside safe region [0.15, 0.85]`,
    };
  }

  // 8. Aspect ratio & variant evaluation
  const aspect = candidatePhoto.aspectRatio;
  let variant: CinematicVariant = 'STATIC_FEATURE';

  if (aspect >= 1.2 && aspect <= 2.2) {
    variant = 'FOCUS_PUSH';
  } else if (aspect > 2.2 && aspect <= 3.0) {
    variant = 'GENTLE_DRIFT';
  } else {
    // Square, portrait, extreme panorama
    return {
      eligible: false,
      variant: 'STATIC_FEATURE',
      stickyEligible: false,
      maxScale: 1.0,
      maxDriftPx: 0,
      reason: `Aspect ratio ${aspect.toFixed(2)} outside motion eligibility ranges (1.2-2.2 or 2.2-3.0)`,
    };
  }

  // 9. Sticky eligibility:
  // - 1 photo: unpinned Focus Push (landscape), no sticky scene
  // - 2 photos / 3 photos: normal room structure, no sticky scene
  // - 4+ photos: eligible for sticky IF within max 3 sticky chapters per project
  let stickyEligible = false;
  if (photoCount >= 4 && stickyRoomsCount < MAX_STICKY_ROOMS_PER_PROJECT) {
    stickyEligible = true;
  }

  // 10. Scale and drift calculations
  const templateCap = TEMPLATE_SCALE_CAPS[templateKey] || 1.02;
  const maxScale = Math.min(templateCap, GLOBAL_SCALE_CAP);

  // Gentle Drift: max travel = min(1% of displayed image width, 16px)
  // Assuming a max desktop content width of 1280px, 1% is 12.8px, capped at 16px.
  const maxDriftPx = Math.min(16, Math.max(8, Math.round(Math.min(viewport.width, 1280) * 0.01)));

  return {
    eligible: true,
    variant,
    stickyEligible,
    maxScale: variant === 'FOCUS_PUSH' ? maxScale : 1.0,
    maxDriftPx: variant === 'GENTLE_DRIFT' ? maxDriftPx : 0,
    reason: undefined,
  };
}

/**
 * Pure evaluation for an entire project's rooms, enforcing project-level limits
 * such as maximum 3 sticky chapters.
 */
export function evaluateProjectCinematicEligibility(
  rooms: NormalizedRoom[],
  viewport: ViewportDimensions,
  prefersReducedMotion: boolean = false,
  forcedColors: boolean = false
): ProjectCinematicEligibility {
  if (viewport.width < MIN_VIEWPORT_WIDTH || viewport.height < MIN_VIEWPORT_HEIGHT) {
    return {
      isEligible: false,
      rejectionReason: `Viewport dimensions (${viewport.width}x${viewport.height}) below desktop threshold (1280x800)`,
      eligibleRoomsCount: 0,
      roomEligibilities: {},
    };
  }
  if (prefersReducedMotion) {
    return {
      isEligible: false,
      rejectionReason: 'User prefers reduced motion',
      eligibleRoomsCount: 0,
      roomEligibilities: {},
    };
  }

  const roomEligibilities: Record<string, RoomCinematicEligibility> = {};
  let eligibleRoomsCount = 0;
  let stickyRoomsCount = 0;

  for (let i = 0; i < rooms.length; i++) {
    const room = rooms[i];
    let candidate = room.photos.find((p) => p.isCover && p.motionEnabled);
    if (!candidate) {
      candidate = room.photos.find((p) => p.motionEnabled);
    }

    if (!candidate) {
      roomEligibilities[room.id] = {
        roomId: room.id,
        isEligible: false,
        rejectionReason: 'No motion_enabled photo found in room',
        scaleCap: 1.0,
        maxDriftPx: 0,
        style: 'focus-push',
      };
      continue;
    }

    const evalResult = evaluateCinematicEligibility({
      presentationMode: 'CINEMATIC',
      viewport,
      prefersReducedMotion,
      forcedColors,
      photoCount: room.photos.length,
      roomIndex: i,
      stickyRoomsCount,
      candidatePhoto: candidate,
    });

    if (evalResult.eligible) {
      eligibleRoomsCount++;
      if (evalResult.stickyEligible) {
        stickyRoomsCount++;
      }
      roomEligibilities[room.id] = {
        roomId: room.id,
        isEligible: true,
        candidatePhoto: candidate,
        scaleCap: evalResult.maxScale,
        maxDriftPx: evalResult.maxDriftPx,
        style: evalResult.variant === 'GENTLE_DRIFT' ? 'gentle-drift' : 'focus-push',
      };
    } else {
      roomEligibilities[room.id] = {
        roomId: room.id,
        isEligible: false,
        rejectionReason: evalResult.reason,
        candidatePhoto: candidate,
        scaleCap: 1.0,
        maxDriftPx: 0,
        style: 'focus-push',
      };
    }
  }

  return {
    isEligible: eligibleRoomsCount > 0,
    eligibleRoomsCount,
    roomEligibilities,
  };
}
