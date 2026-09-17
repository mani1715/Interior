import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className = '',
}) => {
  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <nav
      aria-label="Pagination Navigation"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 'var(--space-8)',
        marginTop: 'var(--space-24)',
      }}
      className={`pagination ${className}`}
    >
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        aria-label="Previous Page"
        style={{
          minWidth: 'var(--min-touch-target)',
          minHeight: 'var(--min-touch-target)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-sm)',
          backgroundColor: 'var(--surface)',
          color: 'var(--text-primary)',
          cursor: currentPage <= 1 ? 'not-allowed' : 'pointer',
          opacity: currentPage <= 1 ? 0.5 : 1,
        }}
      >
        <ChevronLeft size={16} />
      </button>

      {pages.map((p) => {
        const isCurrent = p === currentPage;
        return (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            aria-current={isCurrent ? 'page' : undefined}
            style={{
              minWidth: 'var(--min-touch-target)',
              minHeight: 'var(--min-touch-target)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderRadius: 'var(--radius-sm)',
              border: isCurrent ? '1px solid var(--brand)' : '1px solid var(--border)',
              backgroundColor: isCurrent ? 'var(--brand)' : 'var(--surface)',
              color: isCurrent ? 'var(--color-deep-charcoal)' : 'var(--text-primary)',
              fontWeight: isCurrent ? 700 : 500,
              cursor: 'pointer',
            }}
          >
            {p}
          </button>
        );
      })}

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        aria-label="Next Page"
        style={{
          minWidth: 'var(--min-touch-target)',
          minHeight: 'var(--min-touch-target)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-sm)',
          backgroundColor: 'var(--surface)',
          color: 'var(--text-primary)',
          cursor: currentPage >= totalPages ? 'not-allowed' : 'pointer',
          opacity: currentPage >= totalPages ? 0.5 : 1,
        }}
      >
        <ChevronRight size={16} />
      </button>
    </nav>
  );
};
