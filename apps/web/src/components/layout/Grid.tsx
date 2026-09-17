import React from 'react';

export interface ResponsiveGridProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  columns?: 1 | 2 | 3 | 4;
  gap?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const gapClasses = {
  sm: 'gap-3 sm:gap-4',
  md: 'gap-4 sm:gap-6',
  lg: 'gap-6 sm:gap-8',
  xl: 'gap-8 sm:gap-10',
};

const columnClasses = {
  1: 'grid-cols-1',
  2: 'grid-cols-1 sm:grid-cols-2',
  3: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3',
  4: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4',
};

/**
 * ResponsiveGrid: strict mobile-first 1-column at 360-430px, expanding at 640px+ and 1024px+.
 */
export function ResponsiveGrid({
  children,
  columns = 3,
  gap = 'md',
  className = '',
  ...props
}: ResponsiveGridProps) {
  return (
    <div
      className={`grid ${columnClasses[columns]} ${gapClasses[gap]} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

/**
 * ProjectGrid: optimized 1-column on mobile, 2-column on tablet, 3-column on desktop.
 */
export function ProjectGrid({
  children,
  className = '',
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 sm:gap-6 lg:gap-8 ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

/**
 * CardGrid: 1-column on mobile, 2-column on tablet/desktop.
 */
export function CardGrid({
  children,
  className = '',
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6 ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}
