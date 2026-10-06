'use client';

import React, { useState, useEffect, useMemo, useRef } from 'react';
import { ProjectSpaceShowcaseData, NormalizedRoom, NormalizedPhoto } from '@/lib/projects/showcase-types';
import { GalleryViewer, GalleryFolder, GalleryPhotoData } from '@/components/portfolio/gallery/PortfolioGallery';
import { EnquirySheet } from './EnquirySheet';
import { Project } from '@/lib/discovery/types';
import {
  evaluateProjectCinematicEligibility,
  RoomCinematicEligibility,
} from '@/lib/projects/cinematic-eligibility';
import styles from './ProjectSpaceShowcase.module.css';

export interface ProjectSpaceShowcaseProps {
  data: ProjectSpaceShowcaseData;
  actionProject: Project;
}

export function ProjectSpaceShowcase({ data, actionProject }: ProjectSpaceShowcaseProps) {
  const { rooms, additionalViews, project } = data;

  // Active room for viewer handoff
  const [activeViewerState, setActiveViewerState] = useState<{
    roomId: string;
    photoIndex: number;
  } | null>(null);

  // Active room anchor tracking
  const [activeRoomId, setActiveRoomId] = useState<string | null>(null);

  // Viewport and reduced motion state
  const [viewport, setViewport] = useState<{ width: number; height: number }>({ width: 0, height: 0 });
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  // Enquiry modal state
  const [enquiryOpen, setEnquiryOpen] = useState(false);
  const [enquiryContextPhoto, setEnquiryContextPhoto] = useState<NormalizedPhoto | null>(null);

  // Expanded room disclosures (for rooms with >6 photos)
  const [expandedRooms, setExpandedRooms] = useState<Record<string, boolean>>({});

  const toggleRoomExpansion = (roomId: string) => {
    setExpandedRooms((prev) => ({ ...prev, [roomId]: !prev[roomId] }));
  };

  // Measure viewport and motion preference
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const updateDimensions = () => {
      setViewport({ width: window.innerWidth || 0, height: window.innerHeight || 0 });
    };
    updateDimensions();

    const mediaQuery = typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-reduced-motion: reduce)')
      : null;

    if (mediaQuery) {
      setPrefersReducedMotion(mediaQuery.matches);
      const onMotionChange = (e: MediaQueryListEvent) => setPrefersReducedMotion(e.matches);
      if (typeof mediaQuery.addEventListener === 'function') {
        mediaQuery.addEventListener('change', onMotionChange);
      }
      window.addEventListener('resize', updateDimensions);

      return () => {
        if (typeof mediaQuery.removeEventListener === 'function') {
          mediaQuery.removeEventListener('change', onMotionChange);
        }
        window.removeEventListener('resize', updateDimensions);
      };
    } else {
      window.addEventListener('resize', updateDimensions);
      return () => {
        window.removeEventListener('resize', updateDimensions);
      };
    }
  }, []);

  // Track room in view for navigation pill highlighting & handle initial hash
  useEffect(() => {
    if (typeof window === 'undefined') return;

    if (window.location.hash) {
      const initialId = window.location.hash.replace(/^#room-/, '');
      if (initialId) setActiveRoomId(initialId);
    }

    if (typeof IntersectionObserver === 'undefined') return;

    const sections = document.querySelectorAll('section[id^="room-"]');
    if (!sections.length) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.find((e) => e.isIntersecting);
        if (visible && visible.target.id) {
          setActiveRoomId(visible.target.id.replace(/^room-/, ''));
        }
      },
      { rootMargin: '-20% 0px -60% 0px' }
    );

    sections.forEach((sec) => observer.observe(sec));
    return () => observer.disconnect();
  }, [rooms, additionalViews]);

  // Cinematic eligibility evaluation
  const isCinematicMode = project.presentationMode === 'CINEMATIC';

  const eligibility = useMemo(() => {
    if (!isCinematicMode || viewport.width === 0) {
      return { isEligible: false, eligibleRoomsCount: 0, roomEligibilities: {} };
    }
    return evaluateProjectCinematicEligibility(rooms, viewport, prefersReducedMotion);
  }, [isCinematicMode, rooms, viewport, prefersReducedMotion]);

  // Convert normalized rooms to GalleryFolders
  const folders: GalleryFolder[] = [
    ...rooms.map((r) => ({
      id: r.id,
      name: r.label,
      photos: r.photos.map((p) => ({
        id: p.id,
        src: p.largeUrl || p.mediumUrl,
        alt: p.altText || r.label,
        caption: p.caption,
        kind: p.isAiConcept ? 'AI_CONCEPT' : undefined,
        roomId: r.id,
        roomLabel: r.label,
        focalX: p.focalX,
        focalY: p.focalY,
        width: p.width,
        height: p.height,
      })),
    })),
    ...(additionalViews.length > 0
      ? [
          {
            id: 'additional-views',
            name: rooms.length > 0 ? 'Additional Views' : 'Project Gallery',
            photos: additionalViews.map((p) => ({
              id: p.id,
              src: p.largeUrl || p.mediumUrl,
              alt: p.altText || project.title,
              caption: p.caption,
              kind: p.isAiConcept ? 'AI_CONCEPT' : undefined,
              roomId: null,
              roomLabel: rooms.length > 0 ? 'Additional Views' : 'Project Gallery',
              focalX: p.focalX,
              focalY: p.focalY,
              width: p.width,
              height: p.height,
            })),
          },
        ]
      : []),
  ];

  const handleOpenPhoto = (roomId: string, photoIndex: number) => {
    setActiveViewerState({ roomId, photoIndex });
  };

  const handleEnquireFromViewer = (photoData: GalleryPhotoData) => {
    const matchedPhoto =
      data.allPhotos.find((p) => p.id === photoData.id) ||
      rooms.flatMap((r) => r.photos).find((p) => p.id === photoData.id) ||
      null;
    setEnquiryContextPhoto(matchedPhoto);
    setEnquiryOpen(true);
  };

  const handleAnchorClick = (e: React.MouseEvent<HTMLAnchorElement>, targetId: string) => {
    e.preventDefault();
    const el = document.getElementById(targetId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
      if (window.history.pushState) {
        window.history.pushState(null, '', `#${targetId}`);
      } else {
        window.location.hash = `#${targetId}`;
      }
      setActiveRoomId(targetId.replace(/^room-/, ''));
    }
  };

  const totalSpaces = rooms.length + (additionalViews.length > 0 ? 1 : 0);

  return (
    <div className={styles.showcase} aria-label="Project rooms showcase">
      {/* Sticky Room Navigation Bar (when more than 1 space exists) */}
      {totalSpaces > 1 && (
        <nav className={styles.stickyNav} aria-label="Rooms quick navigation">
          <div className={styles.navInner}>
            <div className={styles.navPills}>
              {rooms.map((room) => (
                <a
                  key={room.id}
                  href={`#room-${room.id}`}
                  aria-label={`${room.label} (${room.photos.length})`}
                  aria-current={activeRoomId === room.id ? 'true' : undefined}
                  onClick={(e) => handleAnchorClick(e, `room-${room.id}`)}
                >
                  {room.label}
                  <span className={styles.roomCountBadge}>({room.photos.length})</span>
                </a>
              ))}
              {additionalViews.length > 0 && (
                <a
                  href="#room-additional-views"
                  aria-label={`${rooms.length > 0 ? 'Additional Views' : 'Gallery'} (${additionalViews.length})`}
                  aria-current={activeRoomId === 'additional-views' ? 'true' : undefined}
                  onClick={(e) => handleAnchorClick(e, 'room-additional-views')}
                >
                  {rooms.length > 0 ? 'Additional Views' : 'Gallery'}
                  <span className={styles.roomCountBadge}>({additionalViews.length})</span>
                </a>
              )}
            </div>
          </div>
        </nav>
      )}

      {/* Ordered Room Stories */}
      {rooms.map((room) => {
        const isExpanded = !!expandedRooms[room.id];
        const roomElig = eligibility.roomEligibilities[room.id];
        const hasCinematicChapter = !!(roomElig && roomElig.isEligible && roomElig.candidatePhoto);
        const candidatePhoto = roomElig?.candidatePhoto;

        // If cinematic chapter is present, remaining photos exclude candidate photo
        const remainingAllPhotos = hasCinematicChapter && candidatePhoto
          ? room.photos.filter((p) => p.id !== candidatePhoto.id)
          : room.photos;

        const displayPhotos =
          remainingAllPhotos.length > 6 && !isExpanded
            ? remainingAllPhotos.slice(0, 5)
            : remainingAllPhotos;

        return (
          <section
            key={room.id}
            id={`room-${room.id}`}
            className={styles.roomSection}
            aria-labelledby={`heading-room-${room.id}`}
          >
            <div className={styles.roomHeader}>
              <div className={styles.roomTitleGroup}>
                <h2 id={`heading-room-${room.id}`}>{room.label}</h2>
                <span className={styles.roomPhotoCount}>
                  {room.photos.length} {room.photos.length === 1 ? 'photograph' : 'photographs'}
                </span>
              </div>
              <button
                type="button"
                className={styles.roomOpenAllBtn}
                onClick={() => handleOpenPhoto(room.id, 0)}
                aria-label={`Open full-screen viewer for ${room.label}`}
              >
                View space in full screen ↗
              </button>
            </div>

            {/* Optional Sticky Cinematic Chapter */}
            {hasCinematicChapter && candidatePhoto && (
              <CinematicChapter
                roomId={room.id}
                roomLabel={room.label}
                photo={candidatePhoto}
                photoIndex={room.photos.findIndex((p) => p.id === candidatePhoto.id)}
                eligibility={roomElig}
                isViewerOpen={activeViewerState !== null}
                onOpen={handleOpenPhoto}
              />
            )}

            {/* Room Story Composition for standard/remaining photos */}
            {displayPhotos.length > 0 &&
              renderRoomGrid(room.id, room.photos, displayPhotos, handleOpenPhoto)}

            {/* Disclosure button for 7+ photos */}
            {remainingAllPhotos.length > 6 && (
              <div className={styles.viewAllDisclosure}>
                <button
                  type="button"
                  onClick={() => toggleRoomExpansion(room.id)}
                  aria-expanded={isExpanded}
                >
                  {isExpanded
                    ? 'Show fewer photos'
                    : `View all ${room.photos.length} photos in ${room.label}`}
                </button>
              </div>
            )}
          </section>
        );
      })}

      {/* Additional Views Group (if populated) */}
      {additionalViews.length > 0 && (
        <section
          id="room-additional-views"
          className={styles.roomSection}
          aria-labelledby="heading-room-additional"
        >
          <div className={styles.roomHeader}>
            <div className={styles.roomTitleGroup}>
              <h2 id="heading-room-additional">
                {rooms.length > 0 ? 'Additional Views' : 'Project Gallery'}
              </h2>
              <span className={styles.roomPhotoCount}>
                {additionalViews.length}{' '}
                {additionalViews.length === 1 ? 'photograph' : 'photographs'}
              </span>
            </div>
            <button
              type="button"
              className={styles.roomOpenAllBtn}
              onClick={() => handleOpenPhoto('additional-views', 0)}
              aria-label="Open full-screen viewer for additional views"
            >
              View in full screen ↗
            </button>
          </div>

          {renderRoomGrid('additional-views', additionalViews, additionalViews, handleOpenPhoto)}
        </section>
      )}

      {/* Full-Screen Gallery Viewer */}
      {activeViewerState && (
        <GalleryViewer
          folders={folders}
          initialFolder={activeViewerState.roomId}
          initialPhoto={activeViewerState.photoIndex}
          projectName={project.title}
          onClose={() => setActiveViewerState(null)}
          onEnquire={handleEnquireFromViewer}
        />
      )}

      {/* Enquiry Sheet Modal */}
      <EnquirySheet
        isOpen={enquiryOpen}
        onClose={() => {
          setEnquiryOpen(false);
          setEnquiryContextPhoto(null);
        }}
        project={actionProject}
        studioSlug={project.studio.slug}
        studioName={project.studio.name}
        projectSlug={project.slug}
      />
    </div>
  );
}

/** Cinematic Chapter component providing pinned matte frame presentation */
interface CinematicChapterProps {
  roomId: string;
  roomLabel: string;
  photo: NormalizedPhoto;
  photoIndex: number;
  eligibility: RoomCinematicEligibility;
  isViewerOpen: boolean;
  onOpen: (roomId: string, index: number) => void;
}

function CinematicChapter({
  roomId,
  roomLabel,
  photo,
  photoIndex,
  eligibility,
  isViewerOpen,
  onOpen,
}: CinematicChapterProps) {
  const sceneRef = useRef<HTMLDivElement>(null);
  const figureRef = useRef<HTMLElement>(null);
  const isIntersectingRef = useRef(false);

  useEffect(() => {
    const sceneEl = sceneRef.current;
    const figureEl = figureRef.current;
    if (!sceneEl || !figureEl || typeof IntersectionObserver === 'undefined') return;

    let rafId: number | null = null;

    const updateTransform = () => {
      if (!isIntersectingRef.current || isViewerOpen) return;
      const rect = sceneEl.getBoundingClientRect();
      const viewportHeight = window.innerHeight || 800;
      const totalPinDistance = Math.max(1, rect.height - viewportHeight);
      const scrolled = -rect.top;
      const progress = Math.max(0, Math.min(1, scrolled / totalPinDistance));

      const scale = 1 + (eligibility.scaleCap - 1) * progress;
      const driftX = eligibility.maxDriftPx * (progress - 0.5) * 2;
      const captionY = 8 * (1 - progress);

      figureEl.style.setProperty('--cinematic-scale', scale.toFixed(4));
      figureEl.style.setProperty('--cinematic-drift-x', `${driftX.toFixed(1)}px`);
      figureEl.style.setProperty('--cinematic-caption-y', `${captionY.toFixed(1)}px`);
    };

    const handleScroll = () => {
      if (!rafId && isIntersectingRef.current && !isViewerOpen) {
        rafId = requestAnimationFrame(() => {
          updateTransform();
          rafId = null;
        });
      }
    };

    const observer = new IntersectionObserver(
      ([entry]) => {
        isIntersectingRef.current = entry.isIntersecting;
        if (entry.isIntersecting) {
          updateTransform();
          window.addEventListener('scroll', handleScroll, { passive: true });
        } else {
          window.removeEventListener('scroll', handleScroll);
        }
      },
      { rootMargin: '50px 0px 50px 0px' }
    );

    observer.observe(sceneEl);
    updateTransform();

    return () => {
      observer.disconnect();
      window.removeEventListener('scroll', handleScroll);
      if (rafId) cancelAnimationFrame(rafId);
    };
  }, [eligibility, isViewerOpen]);

  const focalX = photo.focalX ?? 0.5;
  const focalY = photo.focalY ?? 0.5;

  return (
    <div ref={sceneRef} className={`${styles.cinematicScene} ${styles.isSticky}`}>
      <div className={styles.cinematicPinContainer}>
        <div className={styles.cinematicMatteFrame}>
          <div className={styles.cinematicMatteInner}>
            <figure
              ref={figureRef}
              className={styles.photoFigure}
              data-cinematic-controlled="true"
              onClick={() => onOpen(roomId, photoIndex)}
              tabIndex={0}
              role="button"
              aria-label={`Open ${roomLabel} photo in viewer: ${photo.caption || photo.altText}`}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onOpen(roomId, photoIndex);
                }
              }}
              style={{
                transformOrigin: `${(focalX * 100).toFixed(1)}% ${(focalY * 100).toFixed(1)}%`,
              }}
            >
              <img
                src={photo.largeUrl || photo.mediumUrl}
                alt={photo.altText}
                loading="lazy"
                style={{
                  objectPosition: `${(focalX * 100).toFixed(1)}% ${(focalY * 100).toFixed(1)}%`,
                }}
              />
              {(photo.caption || photo.isAiConcept) && (
                <figcaption className={styles.cinematicCaption}>
                  <span>{photo.caption || photo.altText}</span>
                  {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
                </figcaption>
              )}
            </figure>
          </div>
        </div>
      </div>
    </div>
  );
}

/** Helper to render responsive photo layouts depending on room photo count */
function renderRoomGrid(
  roomId: string,
  allRoomPhotos: NormalizedPhoto[],
  photos: NormalizedPhoto[],
  onOpen: (roomId: string, index: number) => void
) {
  const getIndex = (photo: NormalizedPhoto) => {
    const idx = allRoomPhotos.findIndex((p) => p.id === photo.id);
    return idx >= 0 ? idx : 0;
  };

  if (photos.length === 0) {
    return null;
  }

  if (photos.length === 1) {
    const photo = photos[0];
    const isPortrait =
      (photo.aspectRatio && photo.aspectRatio < 1) ||
      (photo.width && photo.height && photo.width < photo.height);

    return (
      <div className={`${styles.gridSingle} ${isPortrait ? styles.singlePortrait : ''}`}>
        <figure
          className={`${styles.photoFigure} ${isPortrait ? styles.singlePortrait : ''}`}
          onClick={() => onOpen(roomId, getIndex(photo))}
          tabIndex={0}
          role="button"
          aria-label={`Open photo 1 in viewer: ${photo.caption || photo.altText}`}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onOpen(roomId, getIndex(photo));
            }
          }}
        >
          <img
            src={photo.largeUrl || photo.mediumUrl}
            alt={photo.altText}
            loading="lazy"
            style={{
              objectPosition: `${photo.focalX * 100}% ${photo.focalY * 100}%`,
            }}
          />
          {(photo.caption || photo.isAiConcept) && (
            <figcaption>
              <span>{photo.caption || photo.altText}</span>
              {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
            </figcaption>
          )}
        </figure>
      </div>
    );
  }

  if (photos.length === 2) {
    return (
      <div className={styles.gridPair}>
        {photos.map((photo, i) => (
          <figure
            key={photo.id}
            className={styles.photoFigure}
            onClick={() => onOpen(roomId, getIndex(photo))}
            tabIndex={0}
            role="button"
            aria-label={`Open photo ${i + 1} in viewer`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpen(roomId, getIndex(photo));
              }
            }}
          >
            <img
              src={photo.largeUrl || photo.mediumUrl}
              alt={photo.altText}
              loading="lazy"
              style={{
                objectPosition: `${photo.focalX * 100}% ${photo.focalY * 100}%`,
              }}
            />
            {(photo.caption || photo.isAiConcept) && (
              <figcaption>
                <span>{photo.caption || photo.altText}</span>
                {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
              </figcaption>
            )}
          </figure>
        ))}
      </div>
    );
  }

  if (photos.length === 3) {
    const [lead, ...rest] = photos;
    return (
      <div className={styles.gridTriple}>
        <figure
          className={`${styles.photoFigure} ${styles.leadHero}`}
          onClick={() => onOpen(roomId, getIndex(lead))}
          tabIndex={0}
          role="button"
          aria-label="Open lead photo in viewer"
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onOpen(roomId, getIndex(lead));
            }
          }}
        >
          <img
            src={lead.largeUrl || lead.mediumUrl}
            alt={lead.altText}
            loading="lazy"
            style={{
              objectPosition: `${lead.focalX * 100}% ${lead.focalY * 100}%`,
            }}
          />
          {(lead.caption || lead.isAiConcept) && (
            <figcaption>
              <span>{lead.caption || lead.altText}</span>
              {lead.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
            </figcaption>
          )}
        </figure>
        <div className={styles.subCol}>
          {rest.map((photo, i) => (
            <figure
              key={photo.id}
              className={styles.photoFigure}
              onClick={() => onOpen(roomId, getIndex(photo))}
              tabIndex={0}
              role="button"
              aria-label={`Open photo ${i + 2} in viewer`}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onOpen(roomId, getIndex(photo));
                }
              }}
            >
              <img
                src={photo.largeUrl || photo.mediumUrl}
                alt={photo.altText}
                loading="lazy"
                style={{
                  objectPosition: `${photo.focalX * 100}% ${photo.focalY * 100}%`,
                }}
              />
              {(photo.caption || photo.isAiConcept) && (
                <figcaption>
                  <span>{photo.caption || photo.altText}</span>
                  {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
                </figcaption>
              )}
            </figure>
          ))}
        </div>
      </div>
    );
  }

  // 4+ photos: Feature composition (Hero 8 cols + 2 stacked 4 cols + remaining 3 cols grid)
  const [hero, stack1, stack2, ...remaining] = photos;
  return (
    <div className={styles.gridFeature}>
      <figure
        className={`${styles.photoFigure} ${styles.heroItem}`}
        onClick={() => onOpen(roomId, getIndex(hero))}
        tabIndex={0}
        role="button"
        aria-label="Open hero photo in viewer"
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onOpen(roomId, getIndex(hero));
          }
        }}
      >
        <img
          src={hero.largeUrl || hero.mediumUrl}
          alt={hero.altText}
          loading="lazy"
          style={{
            objectPosition: `${hero.focalX * 100}% ${hero.focalY * 100}%`,
          }}
        />
        {(hero.caption || hero.isAiConcept) && (
          <figcaption>
            <span>{hero.caption || hero.altText}</span>
            {hero.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
          </figcaption>
        )}
      </figure>

      <div className={styles.stackItems}>
        {[stack1, stack2].filter(Boolean).map((photo, i) => (
          <figure
            key={photo.id}
            className={styles.photoFigure}
            onClick={() => onOpen(roomId, getIndex(photo))}
            tabIndex={0}
            role="button"
            aria-label={`Open photo ${i + 2} in viewer`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpen(roomId, getIndex(photo));
              }
            }}
          >
            <img
              src={photo.largeUrl || photo.mediumUrl}
              alt={photo.altText}
              loading="lazy"
              style={{
                objectPosition: `${photo.focalX * 100}% ${photo.focalY * 100}%`,
              }}
            />
            {(photo.caption || photo.isAiConcept) && (
              <figcaption>
                <span>{photo.caption || photo.altText}</span>
                {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
              </figcaption>
            )}
          </figure>
        ))}
      </div>

      {remaining.length > 0 && (
        <div className={styles.supportingItems}>
          {remaining.map((photo, i) => (
            <figure
              key={photo.id}
              className={styles.photoFigure}
              onClick={() => onOpen(roomId, getIndex(photo))}
              tabIndex={0}
              role="button"
              aria-label={`Open photo ${i + 4} in viewer`}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onOpen(roomId, getIndex(photo));
                }
              }}
            >
              <img
                src={photo.largeUrl || photo.mediumUrl}
                alt={photo.altText}
                loading="lazy"
                style={{
                  objectPosition: `${photo.focalX * 100}% ${photo.focalY * 100}%`,
                }}
              />
              {(photo.caption || photo.isAiConcept) && (
                <figcaption>
                  <span>{photo.caption || photo.altText}</span>
                  {photo.isAiConcept && <span className={styles.aiBadge}>AI Concept</span>}
                </figcaption>
              )}
            </figure>
          ))}
        </div>
      )}
    </div>
  );
}
