'use client';

import React, { useState } from 'react';
import { ProjectSpaceShowcaseData, NormalizedRoom, NormalizedPhoto } from '@/lib/projects/showcase-types';
import { GalleryViewer, GalleryFolder, GalleryPhotoData } from '@/components/portfolio/gallery/PortfolioGallery';
import { EnquirySheet } from './EnquirySheet';
import { Project } from '@/lib/discovery/types';
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

  // Enquiry modal state
  const [enquiryOpen, setEnquiryOpen] = useState(false);
  const [enquiryContextPhoto, setEnquiryContextPhoto] = useState<NormalizedPhoto | null>(null);

  // Expanded room disclosures (for rooms with >6 photos)
  const [expandedRooms, setExpandedRooms] = useState<Record<string, boolean>>({});

  const toggleRoomExpansion = (roomId: string) => {
    setExpandedRooms((prev) => ({ ...prev, [roomId]: !prev[roomId] }));
  };

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

  const scrollToRoom = (id: string) => {
    const el = document.getElementById(`room-${id}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
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
                <button
                  type="button"
                  key={room.id}
                  aria-label={`${room.label} (${room.photos.length})`}
                  onClick={() => scrollToRoom(room.id)}
                >
                  {room.label}
                  <span className={styles.roomCountBadge}>({room.photos.length})</span>
                </button>
              ))}
              {additionalViews.length > 0 && (
                <button
                  type="button"
                  aria-label={`${rooms.length > 0 ? 'Additional Views' : 'Gallery'} (${additionalViews.length})`}
                  onClick={() => scrollToRoom('additional-views')}
                >
                  {rooms.length > 0 ? 'Additional Views' : 'Gallery'}
                  <span className={styles.roomCountBadge}>({additionalViews.length})</span>
                </button>
              )}
            </div>
          </div>
        </nav>
      )}

      {/* Ordered Room Stories */}
      {rooms.map((room) => {
        const isExpanded = !!expandedRooms[room.id];
        const displayPhotos =
          room.photos.length > 6 && !isExpanded
            ? room.photos.slice(0, 5)
            : room.photos;

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

            {/* Room Story Composition by photo count */}
            {renderRoomGrid(room.id, displayPhotos, handleOpenPhoto)}

            {/* Disclosure button for 7+ photos */}
            {room.photos.length > 6 && (
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

          {renderRoomGrid('additional-views', additionalViews, handleOpenPhoto)}
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

/** Helper to render responsive photo layouts depending on room photo count */
function renderRoomGrid(
  roomId: string,
  photos: NormalizedPhoto[],
  onOpen: (roomId: string, index: number) => void
) {
  if (photos.length === 1) {
    const photo = photos[0];
    return (
      <div className={styles.gridSingle}>
        <figure
          className={styles.photoFigure}
          onClick={() => onOpen(roomId, 0)}
          tabIndex={0}
          role="button"
          aria-label={`Open photo 1 in viewer: ${photo.caption || photo.altText}`}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onOpen(roomId, 0);
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
            onClick={() => onOpen(roomId, i)}
            tabIndex={0}
            role="button"
            aria-label={`Open photo ${i + 1} in viewer`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpen(roomId, i);
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
          onClick={() => onOpen(roomId, 0)}
          tabIndex={0}
          role="button"
          aria-label="Open lead photo in viewer"
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onOpen(roomId, 0);
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
              onClick={() => onOpen(roomId, i + 1)}
              tabIndex={0}
              role="button"
              aria-label={`Open photo ${i + 2} in viewer`}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onOpen(roomId, i + 1);
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
        onClick={() => onOpen(roomId, 0)}
        tabIndex={0}
        role="button"
        aria-label="Open hero photo in viewer"
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onOpen(roomId, 0);
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
            onClick={() => onOpen(roomId, i + 1)}
            tabIndex={0}
            role="button"
            aria-label={`Open photo ${i + 2} in viewer`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpen(roomId, i + 1);
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
              onClick={() => onOpen(roomId, i + 3)}
              tabIndex={0}
              role="button"
              aria-label={`Open photo ${i + 4} in viewer`}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onOpen(roomId, i + 3);
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
