import React from 'react';

export interface SectionProps extends React.HTMLAttributes<HTMLElement> {
  children: React.ReactNode;
  className?: string;
  variant?: 'default' | 'raised' | 'sunken';
  as?: React.ElementType;
}

export function Section({
  children,
  className = '',
  variant = 'default',
  as: Component = 'section',
  ...props
}: SectionProps) {
  const bgClass =
    variant === 'raised'
      ? 'bg-[var(--surface-raised)] border-y border-[var(--border)]'
      : variant === 'sunken'
      ? 'bg-[var(--surface-sunken)] border-y border-[var(--border)]'
      : 'bg-[var(--background)]';

  return (
    <Component className={`py-8 sm:py-12 md:py-16 ${bgClass} ${className}`} {...props}>
      {children}
    </Component>
  );
}

export function SectionHeader({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`mb-6 sm:mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4 ${className}`}>
      {children}
    </div>
  );
}

export function SectionEyebrow({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <span className={`block text-xs font-semibold tracking-widest uppercase text-[var(--brand)] mb-1.5 ${className}`}>
      {children}
    </span>
  );
}

export function SectionTitle({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <h2 className={`font-serif text-2xl sm:text-3xl lg:text-4xl font-semibold tracking-tight text-[var(--foreground)] ${className}`}>
      {children}
    </h2>
  );
}

export function SectionDescription({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <p className={`text-sm sm:text-base text-[var(--muted)] max-w-2xl mt-2 leading-relaxed ${className}`}>
      {children}
    </p>
  );
}

export function SectionActions({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex items-center gap-3 flex-wrap ${className}`}>
      {children}
    </div>
  );
}
