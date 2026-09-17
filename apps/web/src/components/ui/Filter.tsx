'use client';

import React from 'react';
import { Check, X } from 'lucide-react';

export interface FilterChipProps {
  label: string;
  isSelected?: boolean;
  onToggle: () => void;
  count?: number;
  className?: string;
}

export const FilterChip: React.FC<FilterChipProps> = ({
  label,
  isSelected = false,
  onToggle,
  count,
  className = '',
}) => {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={isSelected}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        fontSize: 'var(--text-body-small)',
        fontWeight: isSelected ? 600 : 500,
        color: isSelected ? 'var(--color-deep-charcoal)' : 'var(--text-secondary)',
        backgroundColor: isSelected ? 'var(--brand)' : 'var(--surface)',
        border: isSelected ? '1px solid var(--brand)' : '1px solid var(--border)',
        borderRadius: 'var(--radius-pill)',
        padding: '6px 14px',
        minHeight: '38px', // touch accessible
        cursor: 'pointer',
        transition: 'background-color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard)',
        whiteSpace: 'nowrap',
      }}
      className={`filter-chip ${isSelected ? 'selected' : ''} ${className}`}
    >
      {isSelected && <Check size={14} aria-hidden="true" />}
      <span>{label}</span>
      {count !== undefined && (
        <span
          style={{
            fontSize: 'var(--text-caption)',
            padding: '1px 6px',
            borderRadius: 'var(--radius-pill)',
            backgroundColor: isSelected ? 'rgba(31,31,31,0.12)' : 'var(--surface-alt)',
            color: isSelected ? 'var(--color-deep-charcoal)' : 'var(--text-muted)',
          }}
        >
          {count}
        </span>
      )}
    </button>
  );
};

export const FilterGroup: React.FC<{
  title: string;
  children: React.ReactNode;
  onClear?: () => void;
  className?: string;
}> = ({ title, children, onClear, className = '' }) => {
  return (
    <div style={{ marginBottom: 'var(--space-20)' }} className={`filter-group ${className}`}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <span style={{ fontSize: 'var(--text-label)', fontWeight: 600, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
          {title}
        </span>
        {onClear && (
          <button
            type="button"
            onClick={onClear}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--brand)',
              fontSize: 'var(--text-caption)',
              cursor: 'pointer',
            }}
          >
            Clear
          </button>
        )}
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>{children}</div>
    </div>
  );
};

export const FilterPanel: React.FC<{
  children: React.ReactNode;
  activeCount?: number;
  onResetAll?: () => void;
  onApply?: () => void;
  className?: string;
}> = ({ children, activeCount = 0, onResetAll, onApply, className = '' }) => {
  return (
    <aside
      style={{
        backgroundColor: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-md)',
        padding: 'var(--space-20)',
        boxShadow: 'var(--shadow-subtle)',
      }}
      className={`filter-panel ${className}`}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: '12px',
          borderBottom: '1px solid var(--border)',
          marginBottom: '16px',
        }}
      >
        <h4 style={{ fontSize: 'var(--text-h4)' }}>
          Filters {activeCount > 0 && `(${activeCount})`}
        </h4>
        {activeCount > 0 && onResetAll && (
          <button
            type="button"
            onClick={onResetAll}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--danger)',
              fontSize: 'var(--text-label)',
              cursor: 'pointer',
              fontWeight: 500,
            }}
          >
            Reset All
          </button>
        )}
      </div>
      <div>{children}</div>
      {onApply && (
        <button
          type="button"
          onClick={onApply}
          className="btn btn-primary"
          style={{ width: '100%', marginTop: '16px' }}
        >
          Apply Filters
        </button>
      )}
    </aside>
  );
};
