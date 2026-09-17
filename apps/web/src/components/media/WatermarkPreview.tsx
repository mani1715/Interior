'use client';

import React, { useState } from 'react';
import Image from 'next/image';

export type WatermarkPosition =
  | 'bottom-right'
  | 'bottom-left'
  | 'top-right'
  | 'top-left'
  | 'center'
  | 'tiled';

export interface WatermarkPreviewProps {
  sampleImage?: string;
  defaultStudioName?: string;
  className?: string;
}

const positionClasses: Record<Exclude<WatermarkPosition, 'tiled'>, string> = {
  'bottom-right': 'bottom-4 right-4 text-right',
  'bottom-left': 'bottom-4 left-4 text-left',
  'top-right': 'top-4 right-4 text-right',
  'top-left': 'top-4 left-4 text-left',
  center: 'top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center',
};

export function WatermarkPreview({
  sampleImage = 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=1200&q=80',
  defaultStudioName = 'STUDIO ELÉGANCE • ARCHITECTURE & INTERIORS',
  className = '',
}: WatermarkPreviewProps) {
  const [studioName, setStudioName] = useState(defaultStudioName);
  const [position, setPosition] = useState<WatermarkPosition>('bottom-right');
  const [opacity, setOpacity] = useState(65);
  const [scale, setScale] = useState(14); // font size in px

  return (
    <div className={`rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-6 ${className}`}>
      <div className="mb-4">
        <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
          Studio Watermark Engine
        </h4>
        <p className="text-xs text-[var(--muted)]">
          Configure real-time studio branding and copyright protection overlays.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Interactive Controls */}
        <div className="space-y-4 bg-[var(--surface-raised)] p-4 rounded-xl border border-[var(--border)]">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-1.5">
              Studio Watermark Text
            </label>
            <input
              type="text"
              value={studioName}
              onChange={(e) => setStudioName(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-1.5">
              Placement Position
            </label>
            <div className="grid grid-cols-2 gap-1.5">
              {(
                [
                  'bottom-right',
                  'bottom-left',
                  'top-right',
                  'top-left',
                  'center',
                  'tiled',
                ] as WatermarkPosition[]
              ).map((pos) => (
                <button
                  key={pos}
                  type="button"
                  onClick={() => setPosition(pos)}
                  className={`px-2.5 py-1.5 text-xs rounded-lg border capitalize transition-colors text-left ${
                    position === pos
                      ? 'border-[var(--brand)] bg-[var(--brand-muted)] text-[var(--brand)] font-semibold'
                      : 'border-[var(--border)] bg-[var(--surface)] text-[var(--muted)] hover:text-[var(--foreground)]'
                  }`}
                >
                  {pos.replace('-', ' ')}
                </button>
              ))}
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center text-xs text-[var(--muted)] mb-1">
              <span className="font-semibold uppercase tracking-wider">Opacity</span>
              <span className="font-mono">{opacity}%</span>
            </div>
            <input
              type="range"
              min="10"
              max="100"
              value={opacity}
              onChange={(e) => setOpacity(Number(e.target.value))}
              className="w-full accent-[var(--brand)] cursor-pointer"
            />
          </div>

          <div>
            <div className="flex justify-between items-center text-xs text-[var(--muted)] mb-1">
              <span className="font-semibold uppercase tracking-wider">Scale Size</span>
              <span className="font-mono">{scale}px</span>
            </div>
            <input
              type="range"
              min="10"
              max="26"
              value={scale}
              onChange={(e) => setScale(Number(e.target.value))}
              className="w-full accent-[var(--brand)] cursor-pointer"
            />
          </div>
        </div>

        {/* Live Preview Screen */}
        <div className="lg:col-span-2 relative aspect-[16/10] rounded-xl overflow-hidden border border-[var(--border)] bg-[var(--surface-sunken)] select-none shadow-md">
          <Image
            src={sampleImage}
            alt="Watermark preview room"
            fill
            sizes="(max-width: 1024px) 100vw, 66vw"
            className="object-cover"
          />

          {/* Watermark layer */}
          {position === 'tiled' ? (
            <div
              className="absolute inset-0 grid grid-cols-3 grid-rows-3 p-4 pointer-events-none"
              style={{ opacity: opacity / 100 }}
            >
              {Array.from({ length: 9 }).map((_, i) => (
                <div key={i} className="flex items-center justify-center p-2 transform -rotate-12">
                  <span
                    style={{ fontSize: `${scale * 0.85}px` }}
                    className="font-serif tracking-widest text-white uppercase drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] font-semibold border border-white/20 px-2 py-1 rounded"
                  >
                    {studioName}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div
              className={`absolute pointer-events-none transition-all ${positionClasses[position]}`}
              style={{ opacity: opacity / 100 }}
            >
              <div
                style={{ fontSize: `${scale}px` }}
                className="font-serif tracking-wider text-white uppercase drop-shadow-[0_2px_6px_rgba(0,0,0,0.85)] font-semibold px-3 py-1.5 rounded-lg bg-black/35 backdrop-blur-[2px] border border-white/20 inline-block"
              >
                {studioName}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
