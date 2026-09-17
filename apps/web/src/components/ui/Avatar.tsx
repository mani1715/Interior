import React from 'react';

export interface AvatarProps {
  name: string;
  src?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

export const Avatar: React.FC<AvatarProps> = ({ name, src, size = 'md', className = '' }) => {
  const sizePixels: Record<string, number> = {
    sm: 32,
    md: 40,
    lg: 48,
    xl: 64,
  };

  const px = sizePixels[size];
  const initials = name
    .split(' ')
    .map((n) => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const containerStyle: React.CSSProperties = {
    width: `${px}px`,
    height: `${px}px`,
    borderRadius: '50%',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    backgroundColor: 'var(--surface-alt)',
    border: '1px solid var(--border)',
    color: 'var(--text-primary)',
    fontWeight: 600,
    fontSize: size === 'sm' ? '12px' : size === 'md' ? '14px' : '18px',
    flexShrink: 0,
  };

  if (src) {
    return (
      <div style={containerStyle} className={`avatar ${className}`} aria-label={name}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={src}
          alt={name}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      </div>
    );
  }

  return (
    <div style={containerStyle} className={`avatar avatar-initials ${className}`} aria-label={name}>
      {initials}
    </div>
  );
};

export const StudioLogo: React.FC<{ name: string; src?: string; size?: number; className?: string }> = ({
  name,
  src,
  size = 48,
  className = '',
}) => {
  const containerStyle: React.CSSProperties = {
    width: `${size}px`,
    height: `${size}px`,
    borderRadius: 'var(--radius-sm)',
    backgroundColor: 'var(--surface)',
    border: '1px solid var(--border)',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    boxShadow: 'var(--shadow-subtle)',
    flexShrink: 0,
  };

  if (src) {
    return (
      <div style={containerStyle} className={`studio-logo ${className}`}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={src} alt={`${name} logo`} style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
      </div>
    );
  }

  return (
    <div style={containerStyle} className={`studio-logo ${className}`}>
      <span style={{ fontWeight: 700, fontSize: `${Math.round(size * 0.4)}px`, color: 'var(--brand)' }}>
        {name.charAt(0).toUpperCase()}
      </span>
    </div>
  );
};
