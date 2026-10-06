'use client';
/* eslint-disable @next/next/no-img-element -- gallery uses supplied watermarked derivatives. */
import { useEffect, useId, useRef, useState, type ImgHTMLAttributes } from 'react';
import { createPortal } from 'react-dom';
import styles from './PortfolioGallery.module.css';

export interface GalleryPhotoData {
  id: string;
  src: string;
  alt?: string | null;
  caption?: string | null;
  kind?: string;
  roomId?: string | null;
  roomLabel?: string | null;
  focalX?: number;
  focalY?: number;
  width?: number;
  height?: number;
}

export interface GalleryFolder {
  id: string;
  name: string;
  description?: string;
  photos: GalleryPhotoData[];
}

export interface GalleryViewerProps {
  folders: GalleryFolder[];
  initialFolder: string;
  initialPhoto?: number;
  onClose: () => void;
  /** Optional handoff callback when visitor clicks "Ask About This Project" */
  onEnquire?: (photo: GalleryPhotoData) => void;
  projectName?: string;
}

export function gallerySource(value: string) {
  if (!value || /[\s\\\u0000-\u001f]/.test(value)) return;
  if (value.startsWith('/api/v1/media/public/')) {
    const path = new URL(value, 'https://gallery.invalid').pathname;
    return path.startsWith('/api/v1/media/public/') && !/%2e|%2f|%5c/i.test(path) ? value : undefined;
  }
  try {
    const u = new URL(value);
    return u.protocol === 'https:' && !u.username && !u.password ? value : undefined;
  } catch {
    return undefined;
  }
}

export function GalleryViewer({
  folders,
  initialFolder,
  initialPhoto = 0,
  onClose,
  onEnquire,
  projectName,
}: GalleryViewerProps) {
  const [folderId, setFolderId] = useState(initialFolder);
  const [index, setIndex] = useState(initialPhoto);
  const [overview, setOverview] = useState(false);
  const [sheetScope, setSheetScope] = useState<'ROOM' | 'ALL'>('ROOM');

  // Zoom & Pan state
  const [zoomLevel, setZoomLevel] = useState(1); // 1 = fit, max 3
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  const isDragging = useRef(false);
  const dragStart = useRef({ x: 0, y: 0 });
  const currentOffset = useRef({ x: 0, y: 0 });
  const stageRef = useRef<HTMLDivElement>(null);

  const dialog = useRef<HTMLDialogElement>(null);
  const closeRef = useRef(onClose);
  const title = useId();
  const touch = useRef<number | null>(null);

  closeRef.current = onClose;

  const folder = folders.find((f) => f.id === folderId) || folders[0];
  const photo = folder?.photos[index] || folder?.photos[0];

  // Room-scoped navigation boundaries (no wrap into other rooms)
  const canMovePrev = index > 0;
  const canMoveNext = folder ? index < folder.photos.length - 1 : false;

  const move = (delta: number) => {
    if (!folder) return;
    const nextIdx = index + delta;
    if (nextIdx >= 0 && nextIdx < folder.photos.length) {
      setIndex(nextIdx);
      resetZoom();
    }
  };

  const resetZoom = () => {
    setZoomLevel(1);
    setPanOffset({ x: 0, y: 0 });
    currentOffset.current = { x: 0, y: 0 };
  };

  const handleZoomIn = () => {
    setZoomLevel((prev) => Math.min(3, Math.round((prev + 0.5) * 10) / 10));
  };

  const handleZoomOut = () => {
    setZoomLevel((prev) => {
      const next = Math.max(1, Math.round((prev - 0.5) * 10) / 10);
      if (next === 1) {
        setPanOffset({ x: 0, y: 0 });
        currentOffset.current = { x: 0, y: 0 };
      }
      return next;
    });
  };

  // Pan event handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    if (zoomLevel <= 1) return;
    isDragging.current = true;
    dragStart.current = { x: e.clientX - currentOffset.current.x, y: e.clientY - currentOffset.current.y };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging.current || zoomLevel <= 1) return;
    const maxOffset = 250 * (zoomLevel - 1);
    const newX = Math.max(-maxOffset, Math.min(maxOffset, e.clientX - dragStart.current.x));
    const newY = Math.max(-maxOffset, Math.min(maxOffset, e.clientY - dragStart.current.y));
    currentOffset.current = { x: newX, y: newY };
    setPanOffset({ x: newX, y: newY });
  };

  const handleMouseUp = () => {
    isDragging.current = false;
  };

  // Dialog lifecycle & keyboard shortcuts
  useEffect(() => {
    const before = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    dialog.current?.showModal();
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = overflow;
      before?.focus();
    };
  }, []);

  if (!folder || !photo) return null;

  const allPhotosList = folders.flatMap((f) =>
    f.photos.map((p) => ({ ...p, folderId: f.id, folderName: f.name }))
  );

  return createPortal(
    <dialog
      ref={dialog}
      className={styles.viewer}
      aria-labelledby={title}
      onCancel={(e) => {
        e.preventDefault();
        closeRef.current();
      }}
      onKeyDown={(e) => {
        if (e.key === 'Escape') {
          e.preventDefault();
          closeRef.current();
          return;
        }
        if (overview) return;

        // When zoomed in, arrow keys pan; when fit, arrow keys navigate
        if (zoomLevel > 1) {
          const step = 40;
          const maxOffset = 250 * (zoomLevel - 1);
          if (e.key === 'ArrowRight') {
            e.preventDefault();
            setPanOffset((p) => ({ ...p, x: Math.max(-maxOffset, p.x - step) }));
          } else if (e.key === 'ArrowLeft') {
            e.preventDefault();
            setPanOffset((p) => ({ ...p, x: Math.min(maxOffset, p.x + step) }));
          } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            setPanOffset((p) => ({ ...p, y: Math.max(-maxOffset, p.y - step) }));
          } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setPanOffset((p) => ({ ...p, y: Math.min(maxOffset, p.y + step) }));
          }
          return;
        }

        if (e.key === 'ArrowRight' && canMoveNext) {
          e.preventDefault();
          move(1);
        } else if (e.key === 'ArrowLeft' && canPrevOrNext(canMovePrev)) {
          e.preventDefault();
          move(-1);
        }
      }}
    >
      <header className={styles.viewerHeader}>
        <div>
          <span className={styles.eyebrow}>{projectName ? projectName.toUpperCase() : 'THE GALLERY'}</span>
          <h2 id={title}>{folder.name}</h2>
        </div>
        <div className={styles.headerActions}>
          {onEnquire && (
            <button
              type="button"
              className={styles.askProjectBtn}
              onClick={() => {
                closeRef.current();
                onEnquire(photo);
              }}
            >
              Ask About This Project
            </button>
          )}
          <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="Close gallery">
            Close ×
          </button>
        </div>
      </header>

      <div className={styles.layout}>
        {/* Room / Collection Selector Sidebar */}
        <nav className={styles.folders} aria-label="Gallery folders">
          <span className={styles.eyebrow}>ROOMS & VIEWS / {String(folders.length).padStart(2, '0')}</span>
          {folders.map((f) => (
            <button
              type="button"
              key={f.id}
              aria-label={`${f.name}, ${f.photos.length} ${f.photos.length === 1 ? 'photo' : 'photos'}`}
              aria-pressed={f.id === folder.id}
              onClick={() => {
                setFolderId(f.id);
                setIndex(0);
                resetZoom();
                setOverview(false);
              }}
            >
              <span>{f.name}</span>
              <small>{f.photos.length}</small>
            </button>
          ))}
        </nav>

        {/* Stage Content */}
        <div className={styles.stage}>
          {/* Stage Toolbar */}
          <div className={styles.toolbar}>
            <span aria-live="polite">
              {String(index + 1).padStart(2, '0')} / {String(folder.photos.length).padStart(2, '0')}
            </span>

            <div className={styles.toolbarGroup}>
              {!overview && (
                <>
                  <button
                    type="button"
                    onClick={handleZoomOut}
                    disabled={zoomLevel <= 1}
                    aria-label="Zoom out"
                    title="Zoom out"
                  >
                    −
                  </button>
                  <button
                    type="button"
                    onClick={resetZoom}
                    aria-label="Reset zoom to fit"
                    title="Fit to screen"
                  >
                    {Math.round(zoomLevel * 100)}%
                  </button>
                  <button
                    type="button"
                    onClick={handleZoomIn}
                    disabled={zoomLevel >= 3}
                    aria-label="Zoom in"
                    title="Zoom in (up to 3x)"
                  >
                    +
                  </button>
                </>
              )}
              <button
                type="button"
                aria-pressed={overview}
                onClick={() => {
                  setOverview((v) => !v);
                  resetZoom();
                }}
              >
                {overview ? 'Single photo' : 'Contact sheet'}
              </button>
            </div>
          </div>

          {/* Main Display: Contact Sheet vs Single Photo Stage */}
          {overview ? (
            <div className={styles.contactSheetContainer}>
              <div className={styles.contactSheetNav} role="tablist" aria-label="Contact sheet view scope">
                <button
                  type="button"
                  role="tab"
                  aria-selected={sheetScope === 'ROOM'}
                  aria-pressed={sheetScope === 'ROOM'}
                  onClick={() => setSheetScope('ROOM')}
                >
                  This Room ({folder.photos.length})
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={sheetScope === 'ALL'}
                  aria-pressed={sheetScope === 'ALL'}
                  onClick={() => setSheetScope('ALL')}
                >
                  All Rooms ({allPhotosList.length})
                </button>
              </div>

              <div className={styles.contactSheet}>
                {sheetScope === 'ROOM'
                  ? folder.photos.map((p, i) => (
                      <button
                        type="button"
                        key={p.id}
                        aria-label={`View photo ${i + 1}`}
                        aria-pressed={index === i}
                        onClick={() => {
                          setIndex(i);
                          setOverview(false);
                          resetZoom();
                        }}
                      >
                        <img src={p.src} alt={p.alt || ''} loading="lazy" />
                        <span>{p.caption || p.alt || `Photo ${String(i + 1).padStart(2, '0')}`}</span>
                      </button>
                    ))
                  : allPhotosList.map((item, i) => (
                      <button
                        type="button"
                        key={`${item.folderId}-${item.id}`}
                        aria-label={`View photo from ${item.folderName}`}
                        onClick={() => {
                          setFolderId(item.folderId);
                          const f = folders.find((fld) => fld.id === item.folderId);
                          const idx = f?.photos.findIndex((p) => p.id === item.id) ?? 0;
                          setIndex(idx >= 0 ? idx : 0);
                          setOverview(false);
                          resetZoom();
                        }}
                      >
                        <span className={styles.sheetRoomTag}>{item.folderName}</span>
                        <img src={item.src} alt={item.alt || ''} loading="lazy" />
                        <span>{item.caption || item.alt || `Photo ${String(i + 1).padStart(2, '0')}`}</span>
                      </button>
                    ))}
              </div>
            </div>
          ) : (
            <div
              ref={stageRef}
              className={styles.imageStage}
              style={{ cursor: zoomLevel > 1 ? (isDragging.current ? 'grabbing' : 'grab') : 'default' }}
              onMouseDown={handleMouseDown}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              onTouchStart={(e) => {
                if (zoomLevel <= 1 && e.touches.length === 1) {
                  touch.current = e.touches[0].clientX;
                }
              }}
              onTouchEnd={(e) => {
                if (touch.current !== null && zoomLevel <= 1) {
                  const distance = e.changedTouches[0].clientX - touch.current;
                  if (Math.abs(distance) > 50) {
                    if (distance < 0 && canMoveNext) move(1);
                    else if (distance > 0 && canMovePrev) move(-1);
                  }
                  touch.current = null;
                }
              }}
            >
              <img
                key={photo.id}
                src={photo.src}
                alt={photo.alt || folder.name}
                style={{
                  transform: `translate(${panOffset.x}px, ${panOffset.y}px) scale(${zoomLevel})`,
                  objectPosition:
                    typeof photo.focalX === 'number' && typeof photo.focalY === 'number'
                      ? `${photo.focalX * 100}% ${photo.focalY * 100}%`
                      : '50% 50%',
                }}
                draggable={false}
                onError={(e) => {
                  e.currentTarget.alt = 'This photo is temporarily unavailable';
                }}
              />

              {folder.photos.length > 1 && (
                <>
                  <button
                    type="button"
                    className={styles.previous}
                    aria-label="Previous photo"
                    disabled={!canMovePrev}
                    onClick={() => move(-1)}
                  >
                    ←
                  </button>
                  <button
                    type="button"
                    className={styles.next}
                    aria-label="Next photo"
                    disabled={!canMoveNext}
                    onClick={() => move(1)}
                  >
                    →
                  </button>
                </>
              )}
            </div>
          )}

          {/* Caption & Metadata Footer */}
          <div className={styles.caption}>
            <div className={styles.captionMeta}>
              {photo.kind === 'AI_CONCEPT' && !photo.caption ? (
                <span>AI concept visualization</span>
              ) : (
                <>
                  {photo.kind === 'AI_CONCEPT' && (
                    <span className={styles.aiTag}>AI concept visualization</span>
                  )}
                  <span>{photo.caption || photo.alt || folder.name}</span>
                </>
              )}
              {photo.roomLabel && photo.roomLabel !== folder.name && (
                <span className={styles.subCaption}>{photo.roomLabel}</span>
              )}
            </div>
            <span>{folder.description || ''}</span>
          </div>

          {/* Thumbnail Filmstrip */}
          <div className={styles.filmstrip} aria-label="Photo thumbnails">
            {folder.photos.map((p, i) => (
              <button
                type="button"
                key={p.id}
                aria-label={`Photo ${i + 1}`}
                aria-pressed={index === i}
                onClick={() => {
                  setIndex(i);
                  setOverview(false);
                  resetZoom();
                }}
              >
                <img src={p.src} alt="" loading="lazy" />
              </button>
            ))}
          </div>
        </div>
      </div>
    </dialog>,
    document.body
  );
}

function canPrevOrNext(condition: boolean) {
  return condition;
}

export function PortfolioGallery({ folders }: { folders: GalleryFolder[] }) {
  const [selected, setSelected] = useState<string | null>(null);
  const safe = folders
    .map((f) => ({ ...f, photos: f.photos.filter((p) => gallerySource(p.src)) }))
    .filter((f) => f.photos.length);

  if (!safe.length) return null;

  return (
    <section className={styles.gallery} aria-label="Portfolio gallery">
      <div className={styles.intro}>
        <div>
          <span className={styles.eyebrow}>SPACES, IN DETAIL</span>
          <h2>Inside the work.</h2>
        </div>
        <p>
          Explore the materials, details and spaces.
          <br />
          Open a folder to take a closer look.
        </p>
      </div>
      <div className={styles.folderGrid}>
        {safe.map((f, i) => (
          <button
            type="button"
            className={styles.folderCard}
            key={f.id}
            onClick={() => setSelected(f.id)}
          >
            <div className={styles.cover}>
              <img src={f.photos[0].src} alt={f.photos[0].alt || f.name} loading="lazy" />
              <span className={styles.open}>Open folder ↗</span>
            </div>
            <div className={styles.folderMeta}>
              <span className={styles.number}>{String(i + 1).padStart(2, '0')}</span>
              <div>
                <h3>{f.name}</h3>
                <p>
                  {f.photos.length} {f.photos.length === 1 ? 'photograph' : 'photographs'}
                </p>
              </div>
              <span aria-hidden="true">↗</span>
            </div>
          </button>
        ))}
      </div>
      {selected && <GalleryViewer folders={safe} initialFolder={selected} onClose={() => setSelected(null)} />}
    </section>
  );
}

/** Supplied template images remain normal images, with a native keyboard-accessible opener. */
export function GalleryPhoto(props: ImgHTMLAttributes<HTMLImageElement>) {
  const [state, setState] = useState<{ folders: GalleryFolder[]; folder: string; index: number } | null>(null);

  function open(button: HTMLButtonElement) {
    const root = button.closest('main') || button.parentElement!;
    const groups = new Map<Element, GalleryFolder>();
    let selected = '';
    let index = 0;

    root.querySelectorAll<HTMLButtonElement>('button[data-gallery-photo]').forEach((b, i) => {
      const img = b.querySelector('img');
      if (!img) return;
      const group = b.closest('article') || b.closest('section') || root;
      if (!groups.has(group)) {
        groups.set(group, {
          id: `group-${i}`,
          name: group.querySelector('h3,h2')?.textContent || 'Portfolio photographs',
          photos: [],
        });
      }
      const f = groups.get(group)!;
      if (b === button) {
        selected = f.id;
        index = f.photos.length;
      }
      f.photos.push({
        id: String(i),
        src: img.currentSrc || img.src,
        alt: img.alt,
        caption: b.closest('figure')?.querySelector('figcaption')?.textContent,
      });
    });

    setState({ folders: [...groups.values()], folder: selected, index });
  }

  return (
    <>
      <button
        className={styles.photoButton}
        type="button"
        data-gallery-photo
        aria-label={`Open photo: ${props.alt || 'Portfolio image'}`}
        onClick={(e) => open(e.currentTarget)}
      >
        <img {...props} alt={props.alt || ''} />
        <span className={styles.photoHint} aria-hidden="true">
          View photo ↗
        </span>
      </button>
      {state && (
        <GalleryViewer
          folders={state.folders}
          initialFolder={state.folder}
          initialPhoto={state.index}
          onClose={() => setState(null)}
        />
      )}
    </>
  );
}
