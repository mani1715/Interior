'use client';

import React from 'react';

export interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  variant?: 'brand' | 'current' | 'muted';
  className?: string;
  label?: string;
}

const sizeClasses = {
  sm: 'w-4 h-4 border-2',
  md: 'w-6 h-6 border-2',
  lg: 'w-8 h-8 border-3',
  xl: 'w-12 h-12 border-4',
};

const variantClasses = {
  brand: 'border-[var(--brand)]/20 border-t-[var(--brand)]',
  current: 'border-current/20 border-t-current',
  muted: 'border-[var(--border-strong)] border-t-[var(--foreground)]',
};

export function Spinner({
  size = 'md',
  variant = 'brand',
  className = '',
  label = 'Loading...',
}: SpinnerProps) {
  return (
    <div
      role="status"
      aria-label={label}
      className={`inline-block rounded-full animate-spin ${sizeClasses[size]} ${variantClasses[variant]} ${className}`}
    >
      <span className="sr-only">{label}</span>
    </div>
  );
}
