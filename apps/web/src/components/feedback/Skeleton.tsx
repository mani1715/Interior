'use client';

import React from 'react';

export interface SkeletonProps {
  className?: string;
  variant?: 'rectangular' | 'circular' | 'text';
  width?: string | number;
  height?: string | number;
}

export function Skeleton({
  className = '',
  variant = 'rectangular',
  width,
  height,
}: SkeletonProps) {
  const variantClass =
    variant === 'circular'
      ? 'rounded-full'
      : variant === 'text'
      ? 'rounded-md h-4'
      : 'rounded-lg';

  const style: React.CSSProperties = {
    ...(width !== undefined && { width }),
    ...(height !== undefined && { height }),
  };

  return (
    <div
      aria-hidden="true"
      style={style}
      className={`bg-[var(--surface-raised)] animate-pulse ${variantClass} ${className}`}
    />
  );
}

export function ProjectCardSkeleton() {
  return (
    <div className="rounded-xl border border-[var(--border)] bg-[var(--surface)] overflow-hidden">
      <Skeleton className="w-full aspect-[4/3]" />
      <div className="p-4 space-y-3">
        <Skeleton variant="text" className="w-2/3 h-5" />
        <Skeleton variant="text" className="w-1/2 h-3.5" />
        <div className="flex gap-2 pt-2">
          <Skeleton variant="text" className="w-16 h-6 rounded-full" />
          <Skeleton variant="text" className="w-20 h-6 rounded-full" />
        </div>
      </div>
    </div>
  );
}
