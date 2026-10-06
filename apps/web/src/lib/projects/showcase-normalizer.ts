import { PublicMediaDto, PublicProjectDetailDto, PublicRoomDto } from '../seo/types';
import { NormalizedPhoto, NormalizedRoom, ProjectSpaceShowcaseData } from './showcase-types';

export function normalizePhoto(raw: PublicMediaDto): NormalizedPhoto {
  const derivatives = raw.derivatives || [];

  const thumbDeriv = derivatives.find((d) => d.variantName === 'THUMBNAIL');
  const medDeriv = derivatives.find((d) => d.variantName === 'MEDIUM');
  const largeDeriv = derivatives.find((d) => d.variantName === 'LARGE');

  const thumbnailUrl =
    raw.thumbnailUrl ||
    thumbDeriv?.publicUrl ||
    medDeriv?.publicUrl ||
    largeDeriv?.publicUrl ||
    '';

  const mediumUrl =
    raw.mediumUrl ||
    medDeriv?.publicUrl ||
    largeDeriv?.publicUrl ||
    thumbnailUrl ||
    '';

  const largeUrl =
    raw.largeUrl ||
    largeDeriv?.publicUrl ||
    medDeriv?.publicUrl ||
    mediumUrl ||
    '';

  // Dimension resolution
  const width =
    raw.width ||
    raw.originalWidth ||
    largeDeriv?.width ||
    medDeriv?.width ||
    thumbDeriv?.width ||
    1600;

  const height =
    raw.height ||
    raw.originalHeight ||
    largeDeriv?.height ||
    medDeriv?.height ||
    thumbDeriv?.height ||
    1000;

  const aspectRatio = height > 0 ? width / height : 1.6;

  // Focal point normalization (default 0.5, clamped 0.0 to 1.0)
  let fx = typeof raw.focalX === 'number' ? raw.focalX : 0.5;
  let fy = typeof raw.focalY === 'number' ? raw.focalY : 0.5;
  if (isNaN(fx) || fx < 0 || fx > 1) fx = 0.5;
  if (isNaN(fy) || fy < 0 || fy > 1) fy = 0.5;

  return {
    id: raw.id,
    roomId: raw.roomId || null,
    isCover: Boolean(raw.isCover),
    isRoomCover: Boolean(raw.isRoomCover),
    isAiConcept: Boolean(raw.isAiConcept),
    mediaType: raw.mediaType || 'REAL_PROJECT',
    mediaTypeDisplayName: raw.mediaTypeDisplayName || null,
    sortOrder: typeof raw.sortOrder === 'number' ? raw.sortOrder : 0,
    altText: raw.altText || '',
    caption: raw.caption || null,
    focalX: fx,
    focalY: fy,
    motionEnabled: raw.motionEnabled !== undefined ? raw.motionEnabled : true,
    width,
    height,
    aspectRatio,
    thumbnailUrl,
    mediumUrl,
    largeUrl,
    derivatives,
  };
}

export function normalizeProjectSpaceShowcase(
  project: PublicProjectDetailDto
): ProjectSpaceShowcaseData {
  const rawMediaList = project.media || [];
  const normalizedAllPhotos = rawMediaList
    .map(normalizePhoto)
    .filter((p) => p.largeUrl || p.mediumUrl || p.thumbnailUrl)
    .sort((a, b) => a.sortOrder - b.sortOrder);

  const rawRooms = project.rooms || [];
  const normalizedRooms: NormalizedRoom[] = [];

  for (const rawRoom of rawRooms) {
    const roomPhotos = (rawRoom.photos || [])
      .map(normalizePhoto)
      .filter((p) => p.largeUrl || p.mediumUrl || p.thumbnailUrl)
      .sort((a, b) => a.sortOrder - b.sortOrder);

    if (roomPhotos.length === 0) {
      continue;
    }

    const coverPhoto =
      roomPhotos.find((p) => p.isRoomCover) ||
      (rawRoom.coverPhoto ? normalizePhoto(rawRoom.coverPhoto) : null) ||
      roomPhotos[0];

    normalizedRooms.push({
      id: rawRoom.id,
      roomType: rawRoom.roomType,
      label: rawRoom.label || 'Space',
      displayOrder: rawRoom.displayOrder,
      coverPhoto,
      eligiblePhotoCount: roomPhotos.length,
      photos: roomPhotos,
    });
  }

  // Sort rooms by displayOrder
  normalizedRooms.sort((a, b) => a.displayOrder - b.displayOrder);

  // Additional views: unassigned photos (where roomId is null)
  let additionalViews: NormalizedPhoto[] = [];
  if (project.additionalViews && project.additionalViews.length > 0) {
    additionalViews = project.additionalViews
      .map(normalizePhoto)
      .filter((p) => p.largeUrl || p.mediumUrl || p.thumbnailUrl)
      .sort((a, b) => a.sortOrder - b.sortOrder);
  } else if (normalizedRooms.length > 0) {
    const roomPhotoIds = new Set(
      normalizedRooms.flatMap((r) => r.photos.map((p) => p.id))
    );
    additionalViews = normalizedAllPhotos.filter((p) => !roomPhotoIds.has(p.id));
  } else {
    // If no rooms exist at all, unassigned non-cover photos (or all photos if only 1 photo exists)
    additionalViews = normalizedAllPhotos.filter((p) => !p.isCover);
  }

  const projectCover =
    normalizedAllPhotos.find((p) => p.isCover) ||
    normalizedRooms[0]?.coverPhoto ||
    normalizedAllPhotos[0] ||
    null;

  return {
    project: {
      id: project.id,
      slug: project.slug,
      title: project.title,
      shortDescription: project.shortDescription,
      fullDescription: project.fullDescription,
      categoryCode: project.categoryCode,
      categoryDisplayName:
        project.categoryDisplayName || project.categoryCode.replace(/_/g, ' '),
      styleCodes: project.styleCodes || [],
      styleDisplayNames:
        project.styleDisplayNames ||
        (project.styleCodes || []).map((s) => s.replace(/_/g, ' ')),
      city: project.city,
      state: project.state,
      country: project.country,
      propertyType: project.propertyType,
      projectScope: project.projectScope,
      completionYear: project.completionYear,
      budgetFormatted: project.budgetFormatted,
      areaFormatted: project.areaFormatted,
      coverPhoto: projectCover,
      presentationMode: project.presentationMode === 'CINEMATIC' ? 'CINEMATIC' : 'STANDARD',
      studio: {
        studioId: project.studio?.studioId,
        slug: project.studio?.slug || '',
        name: project.studio?.name || '',
        city: project.studio?.city,
        state: project.studio?.state,
      },
    },
    rooms: normalizedRooms,
    additionalViews,
    allPhotos: normalizedAllPhotos,
  };
}
