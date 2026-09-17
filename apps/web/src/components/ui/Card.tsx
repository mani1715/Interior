import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  variant?: 'surface' | 'elevated' | 'outlined' | 'interactive';
  className?: string;
}

export const ContentCard: React.FC<CardProps> = ({
  children,
  variant = 'surface',
  className = '',
  style,
  ...props
}) => {
  const cardStyle: React.CSSProperties = {
    backgroundColor: variant === 'elevated' ? 'var(--surface-elevated)' : 'var(--surface)',
    borderRadius: 'var(--radius-md)',
    border: '1px solid var(--border)',
    boxShadow: variant === 'elevated' ? 'var(--shadow-card)' : 'var(--shadow-subtle)',
    padding: 'var(--space-20)',
    transition: 'transform var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard)',
    ...(variant === 'interactive' && {
      cursor: 'pointer',
    }),
    ...style,
  };

  return (
    <div style={cardStyle} className={`card card-${variant} ${className}`} {...props}>
      {children}
    </div>
  );
};

export interface ProjectCardShellProps {
  title: string;
  location: string;
  category: string;
  coverImage?: React.ReactNode;
  badge?: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export const ProjectCard: React.FC<ProjectCardShellProps> = ({
  title,
  location,
  category,
  coverImage,
  badge,
  onClick,
  className = '',
}) => {
  return (
    <article
      onClick={onClick}
      style={{
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: 'var(--surface)',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border)',
        overflow: 'hidden',
        boxShadow: 'var(--shadow-subtle)',
        cursor: onClick ? 'pointer' : 'default',
        transition: 'box-shadow var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard)',
      }}
      className={`project-card ${className}`}
    >
      <div style={{ position: 'relative', width: '100%', aspectRatio: '16/10', backgroundColor: 'var(--surface-alt)' }}>
        {coverImage}
        {badge && (
          <div style={{ position: 'absolute', top: '12px', left: '12px', zIndex: 2 }}>
            {badge}
          </div>
        )}
      </div>
      <div style={{ padding: 'var(--space-16)' }}>
        <span style={{ fontSize: 'var(--text-caption)', color: 'var(--brand)', fontWeight: 600, textTransform: 'uppercase' }}>
          {category}
        </span>
        <h3 style={{ fontSize: 'var(--text-h4)', marginTop: '4px', marginBottom: '6px' }}>{title}</h3>
        <p style={{ fontSize: 'var(--text-body-small)', color: 'var(--text-muted)' }}>{location}</p>
      </div>
    </article>
  );
};

export interface MetricCardProps {
  label: string;
  value: string | number;
  change?: string;
  isPositive?: boolean;
  icon?: React.ReactNode;
}

export const MetricCard: React.FC<MetricCardProps> = ({ label, value, change, isPositive, icon }) => {
  return (
    <div
      style={{
        backgroundColor: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-md)',
        padding: 'var(--space-20)',
        boxShadow: 'var(--shadow-subtle)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
      className="metric-card"
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span style={{ fontSize: 'var(--text-label)', color: 'var(--text-muted)', fontWeight: 500 }}>{label}</span>
        {icon && <span style={{ color: 'var(--brand)' }}>{icon}</span>}
      </div>
      <div style={{ marginTop: 'var(--space-12)' }}>
        <div style={{ fontSize: 'var(--text-h2)', fontWeight: 700, color: 'var(--text-primary)' }}>{value}</div>
        {change && (
          <span
            style={{
              fontSize: 'var(--text-caption)',
              fontWeight: 600,
              color: isPositive ? 'var(--success)' : 'var(--danger)',
              marginTop: '4px',
              display: 'inline-block',
            }}
          >
            {isPositive ? '↑' : '↓'} {change}
          </span>
        )}
      </div>
    </div>
  );
};

export interface ActionCardProps {
  title: string;
  description: string;
  actionText: string;
  onClick: () => void;
  icon?: React.ReactNode;
}

export const ActionCard: React.FC<ActionCardProps> = ({ title, description, actionText, onClick, icon }) => {
  return (
    <div
      onClick={onClick}
      style={{
        backgroundColor: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-md)',
        padding: 'var(--space-24)',
        cursor: 'pointer',
        boxShadow: 'var(--shadow-subtle)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        minHeight: '140px',
      }}
      className="action-card"
    >
      <div>
        {icon && <div style={{ color: 'var(--brand)', marginBottom: '8px' }}>{icon}</div>}
        <h4 style={{ fontSize: 'var(--text-h4)', marginBottom: '6px' }}>{title}</h4>
        <p style={{ fontSize: 'var(--text-body-small)', color: 'var(--text-muted)' }}>{description}</p>
      </div>
      <span style={{ color: 'var(--brand)', fontWeight: 600, fontSize: 'var(--text-label)', marginTop: '12px' }}>
        {actionText} →
      </span>
    </div>
  );
};
