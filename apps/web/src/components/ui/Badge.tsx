import React from 'react';
import { CheckCircle, Sparkles, Tag } from 'lucide-react';

export type StatusVariant = 'draft' | 'published' | 'processing' | 'ready' | 'failed' | 'pending' | 'verified';

export interface StatusBadgeProps {
  status: StatusVariant;
  label?: string;
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, label, className = '' }) => {
  const statusConfig: Record<StatusVariant, { bg: string; color: string; border: string; defaultLabel: string }> = {
    draft: {
      bg: 'var(--surface-alt)',
      color: 'var(--text-muted)',
      border: 'var(--border)',
      defaultLabel: 'Draft',
    },
    published: {
      bg: 'var(--success-surface)',
      color: 'var(--success)',
      border: 'var(--success)',
      defaultLabel: 'Published',
    },
    processing: {
      bg: 'var(--info-surface)',
      color: 'var(--info)',
      border: 'var(--info)',
      defaultLabel: 'Processing',
    },
    ready: {
      bg: 'var(--success-surface)',
      color: 'var(--success)',
      border: 'var(--success)',
      defaultLabel: 'Ready',
    },
    failed: {
      bg: 'var(--danger-surface)',
      color: 'var(--danger)',
      border: 'var(--danger)',
      defaultLabel: 'Failed',
    },
    pending: {
      bg: 'var(--warning-surface)',
      color: 'var(--warning)',
      border: 'var(--warning)',
      defaultLabel: 'Pending',
    },
    verified: {
      bg: 'var(--success-surface)',
      color: 'var(--success)',
      border: 'var(--success)',
      defaultLabel: 'Verified Studio',
    },
  };

  const config = statusConfig[status];

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        fontSize: 'var(--text-caption)',
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        padding: '3px 8px',
        borderRadius: 'var(--radius-pill)',
        backgroundColor: config.bg,
        color: config.color,
        border: `1px solid ${config.border}`,
      }}
      className={`badge badge-status badge-${status} ${className}`}
    >
      <span
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: config.color,
        }}
        aria-hidden="true"
      />
      {label || config.defaultLabel}
    </span>
  );
};

export const VerifiedBadge: React.FC<{ label?: string; className?: string }> = ({
  label = 'Verified Professional',
  className = '',
}) => {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        fontSize: 'var(--text-label)',
        fontWeight: 600,
        color: 'var(--success)',
        backgroundColor: 'var(--success-surface)',
        padding: '4px 8px',
        borderRadius: 'var(--radius-pill)',
        border: '1px solid rgba(46, 93, 75, 0.2)',
      }}
      className={`badge-verified ${className}`}
    >
      <CheckCircle size={14} aria-hidden="true" />
      {label}
    </span>
  );
};

export const CategoryBadge: React.FC<{ name: string; className?: string; onClick?: () => void }> = ({
  name,
  className = '',
  onClick,
}) => {
  return (
    <span
      onClick={onClick}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        fontSize: 'var(--text-label)',
        fontWeight: 500,
        color: 'var(--text-secondary)',
        backgroundColor: 'var(--surface-alt)',
        padding: '4px 10px',
        borderRadius: 'var(--radius-pill)',
        border: '1px solid var(--border)',
        cursor: onClick ? 'pointer' : 'default',
      }}
      className={`badge-category ${className}`}
    >
      <Tag size={12} aria-hidden="true" />
      {name}
    </span>
  );
};

/**
 * MANDATORY INVARIANT: AI Concept Label.
 * Public AI visualizations must visibly bear "AI Concept Visualization" to prevent
 * misrepresentation as completed real physical construction.
 */
export const AIConceptBadge: React.FC<{ className?: string }> = ({ className = '' }) => {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        fontSize: 'var(--text-label)',
        fontWeight: 600,
        letterSpacing: '0.02em',
        color: 'var(--color-deep-charcoal)',
        backgroundColor: 'rgba(250, 248, 245, 0.95)',
        backdropFilter: 'none', // Avoid mobile performance penalties
        padding: '4px 10px',
        borderRadius: 'var(--radius-sm)',
        border: '1px solid var(--border-strong)',
        boxShadow: 'var(--shadow-subtle)',
      }}
      className={`badge-ai-concept ${className}`}
    >
      <Sparkles size={14} style={{ color: 'var(--brand)' }} aria-hidden="true" />
      AI Concept Visualization
    </span>
  );
};
