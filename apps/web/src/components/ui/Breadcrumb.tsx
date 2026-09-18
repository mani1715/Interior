import React from 'react';
import Link from 'next/link';
import { ChevronRight } from 'lucide-react';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  current?: boolean;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ items, className = '' }) => {
  return (
    <nav aria-label="Breadcrumb" className={`breadcrumb ${className}`}>
      <ol
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          listStyle: 'none',
          gap: 'var(--space-8)',
          fontSize: 'var(--text-body-small)',
          color: 'var(--text-muted)',
        }}
      >
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          return (
            <li key={index} style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-8)' }}>
              {item.href && !isLast ? (
                <Link
                  href={item.href}
                  style={{
                    color: 'var(--text-secondary)',
                    textDecoration: 'none',
                  }}
                >
                  {item.label}
                </Link>
              ) : (
                <span style={{ color: 'var(--text-primary)', fontWeight: 600 }} aria-current={isLast ? 'page' : undefined}>
                  {item.label}
                </span>
              )}
              {!isLast && <ChevronRight size={14} style={{ color: 'var(--border-strong)' }} aria-hidden="true" />}
            </li>
          );
        })}
      </ol>
    </nav>
  );
};
