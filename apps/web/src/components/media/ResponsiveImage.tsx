'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { ImageOff } from 'lucide-react';

export interface ResponsiveImageProps {
  src: string;
  alt: string;
  aspectRatio?: '1/1' | '4/3' | '16/9' | '3/2' | '2/3';
  priority?: boolean;
  className?: string;
  fill?: boolean;
  watermarkText?: string;
}

const aspectMap = {
  '1/1': 'aspect-square',
  '4/3': 'aspect-[4/3]',
  '16/9': 'aspect-[16/9]',
  '3/2': 'aspect-[3/2]',
  '2/3': 'aspect-[2/3]',
};

export function ResponsiveImage({
  src,
  alt,
  aspectRatio = '4/3',
  priority = false,
  className = '',
  fill = true,
  watermarkText,
}: ResponsiveImageProps) {
  const [error, setError] = useState(false);
  const [loaded, setLoaded] = useState(false);

  return (
    <div
      className={`relative overflow-hidden bg-[var(--surface-sunken)] ${
        aspectRatio ? aspectMap[aspectRatio] : ''
      } ${className}`}
    >
      {!error ? (
        <>
          <Image
            src={src}
            alt={alt}
            fill={fill}
            priority={priority}
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            onLoad={() => setLoaded(true)}
            onError={() => setError(true)}
            className={`object-cover transition-opacity duration-300 ${
              loaded ? 'opacity-100' : 'opacity-0'
            }`}
          />
          {!loaded && (
            <div className="absolute inset-0 bg-[var(--surface-raised)] animate-pulse" />
          )}
        </>
      ) : (
        <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-[var(--muted)] text-center">
          <ImageOff className="w-8 h-8 mb-2 opacity-50" />
          <span className="text-xs">Image unavailable</span>
        </div>
      )}

      {/* Watermark overlay if provided */}
      {watermarkText && loaded && !error && (
        <div className="absolute bottom-3 right-3 pointer-events-none select-none bg-black/40 backdrop-blur-[2px] px-2 py-0.5 rounded text-[10px] tracking-wider uppercase font-semibold text-white/80 border border-white/10">
          {watermarkText}
        </div>
      )}
    </div>
  );
}
