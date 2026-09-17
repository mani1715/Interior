import React from 'next/link';
import NextLink, { LinkProps as NextLinkProps } from 'next/link';

export interface InlineLinkProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  isExternal?: boolean;
}

export const InlineLink: React.FC<InlineLinkProps> = ({
  children,
  href,
  isExternal = false,
  className = '',
  style,
  ...props
}) => {
  const linkStyle: React.CSSProperties = {
    color: 'var(--info)',
    fontWeight: 500,
    textDecoration: 'underline',
    textUnderlineOffset: '3px',
    transition: 'color var(--duration-fast) var(--ease-standard)',
    ...style,
  };

  if (isExternal) {
    return (
      <a
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        style={linkStyle}
        className={`link-inline ${className}`}
        {...props}
      >
        {children}
      </a>
    );
  }

  return (
    <NextLink href={href} style={linkStyle} className={`link-inline ${className}`} {...props}>
      {children}
    </NextLink>
  );
};

export interface NavigationLinkProps extends NextLinkProps {
  children: React.ReactNode;
  isActive?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

export const NavigationLink: React.FC<NavigationLinkProps> = ({
  children,
  href,
  isActive = false,
  className = '',
  style,
  ...props
}) => {
  const navStyle: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 'var(--space-8)',
    color: isActive ? 'var(--brand)' : 'var(--text-primary)',
    fontWeight: isActive ? 600 : 500,
    textDecoration: 'none',
    padding: '8px 12px',
    borderRadius: 'var(--radius-sm)',
    minHeight: 'var(--min-touch-target)',
    backgroundColor: isActive ? 'rgba(184, 138, 90, 0.08)' : 'transparent',
    transition: 'background-color var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard)',
    ...style,
  };

  return (
    <NextLink href={href} style={navStyle} className={`link-nav ${isActive ? 'active' : ''} ${className}`} {...props}>
      {children}
    </NextLink>
  );
};
