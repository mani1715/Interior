import React from 'react';

export interface ContainerProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
  as?: React.ElementType;
}

/**
 * PageContainer enforces 16px horizontal padding at 360-430px mobile,
 * scaling up to 24px/32px on larger screens, up to max-w-7xl (1280px).
 */
export function PageContainer({
  children,
  className = '',
  as: Component = 'div',
  ...props
}: ContainerProps) {
  return (
    <Component
      className={`w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}

/**
 * ContentContainer has a medium max-width (1024px) for readability.
 */
export function ContentContainer({
  children,
  className = '',
  as: Component = 'div',
  ...props
}: ContainerProps) {
  return (
    <Component
      className={`w-full max-w-5xl mx-auto px-4 sm:px-6 ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}

/**
 * NarrowContainer has a narrow max-width (680-768px) ideal for single-column forms and editorial reads.
 */
export function NarrowContainer({
  children,
  className = '',
  as: Component = 'div',
  ...props
}: ContainerProps) {
  return (
    <Component
      className={`w-full max-w-2xl mx-auto px-4 sm:px-6 ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}

/**
 * FullBleedSection spans the entire viewport width, useful for hero banners or immersive media.
 */
export function FullBleedSection({
  children,
  className = '',
  as: Component = 'section',
  ...props
}: ContainerProps) {
  return (
    <Component className={`w-full relative overflow-hidden ${className}`} {...props}>
      {children}
    </Component>
  );
}
