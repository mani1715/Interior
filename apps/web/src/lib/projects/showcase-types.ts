import { MediaDerivativeDto } from '../media/types';

export interface NormalizedPhoto {
  id: string;
  roomId?: string | null;
  isCover: boolean;
  isRoomCover: boolean;
  isAiConcept: boolean;
  mediaType: string;
  mediaTypeDisplayName?: string | null;
  sortOrder: number;
  altText: string;
  caption?: string | null;
  focalX: number; // 0.0 to 1.0 (default 0.5)
  focalY: number; // 0.0 to 1.0 (default 0.5)
  motionEnabled: boolean;
  width: number;
  height: number;
  aspectRatio: number;
  thumbnailUrl: string;
  mediumUrl: string;
  largeUrl: string;
  derivatives: MediaDerivativeDto[];
}

export interface NormalizedRoom {
  id: string;
  roomType: string;
  label: string;
  displayOrder: number;
  coverPhoto: NormalizedPhoto;
  eligiblePhotoCount: number;
  photos: NormalizedPhoto[];
}

export interface ProjectSpaceShowcaseData {
  project: {
    id: string;
    slug: string;
    title: string;
    shortDescription?: string | null;
    fullDescription?: string | null;
    categoryCode: string;
    categoryDisplayName: string;
    styleCodes: string[];
    styleDisplayNames: string[];
    city?: string | null;
    state?: string | null;
    country?: string | null;
    propertyType?: string | null;
    projectScope?: string | null;
    completionYear?: number | null;
    budgetFormatted?: string | null;
    areaFormatted?: string | null;
    coverPhoto?: NormalizedPhoto | null;
    presentationMode: 'STANDARD' | 'CINEMATIC';
    studio: {
      studioId?: string;
      slug: string;
      name: string;
      professionalType?: string | null;
      city?: string | null;
      state?: string | null;
    };
  };
  rooms: NormalizedRoom[];
  additionalViews: NormalizedPhoto[];
  allPhotos: NormalizedPhoto[];
}
