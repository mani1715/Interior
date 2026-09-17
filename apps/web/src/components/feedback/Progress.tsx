'use client';

import React from 'react';

export interface ProgressProps {
  value: number; // 0 to 100
  max?: number;
  label?: string;
  showPercentage?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'brand' | 'success' | 'danger';
  className?: string;
}

const sizeClasses = {
  sm: 'h-1.5',
  md: 'h-2.5',
  lg: 'h-4',
};

const variantClasses = {
  brand: 'bg-[var(--brand)]',
  success: 'bg-[var(--success)]',
  danger: 'bg-[var(--danger)]',
};

export function Progress({
  value,
  max = 100,
  label,
  showPercentage = false,
  size = 'md',
  variant = 'brand',
  className = '',
}: ProgressProps) {
  const percentage = Math.min(Math.max(Math.round((value / max) * 100), 0), 100);

  return (
    <div className={`w-full ${className}`}>
      {(label || showPercentage) && (
        <div className="flex justify-between items-center mb-1.5 text-xs">
          {label && <span className="font-medium text-[var(--foreground)]">{label}</span>}
          {showPercentage && <span className="text-[var(--muted)] font-mono">{percentage}%</span>}
        </div>
      )}
      <div
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={label || 'Progress bar'}
        className={`w-full bg-[var(--surface-sunken)] border border-[var(--border)] rounded-full overflow-hidden ${sizeClasses[size]}`}
      >
        <div
          className={`h-full transition-all duration-300 ease-out rounded-full ${variantClasses[variant]}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
