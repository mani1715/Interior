'use client';

import React, { forwardRef } from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'link';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  isIconOnly?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      isIconOnly = false,
      disabled,
      className = '',
      style,
      ...props
    },
    ref
  ) => {
    // Base styles ensuring minimum 44px touch targets on mobile
    const baseStyle: React.CSSProperties = {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 600,
      fontFamily: 'var(--font-sans)',
      borderRadius: 'var(--radius-sm)',
      border: '1px solid transparent',
      cursor: disabled || isLoading ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.6 : 1,
      transition: 'background-color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard)',
      textDecoration: 'none',
      whiteSpace: 'nowrap',
      gap: 'var(--space-8)',
      position: 'relative',
      minHeight: 'var(--min-touch-target)', // 44px minimum touch target
      ...style,
    };

    // Size styles
    const sizeStyles: Record<ButtonSize, React.CSSProperties> = {
      sm: {
        fontSize: 'var(--text-body-small)',
        padding: isIconOnly ? '8px' : '8px 14px',
        minWidth: isIconOnly ? 'var(--min-touch-target)' : 'auto',
      },
      md: {
        fontSize: 'var(--text-body)',
        padding: isIconOnly ? '10px' : '10px 20px',
        minWidth: isIconOnly ? 'var(--min-touch-target)' : 'auto',
      },
      lg: {
        fontSize: 'var(--text-body-large)',
        padding: isIconOnly ? '12px' : '14px 28px',
        minHeight: '48px',
        minWidth: isIconOnly ? '48px' : 'auto',
      },
    };

    // Variant styles conforming strictly to locked palette and accessible contrast
    const variantStyles: Record<ButtonVariant, React.CSSProperties> = {
      primary: {
        backgroundColor: 'var(--btn-cta-bg)', // Warm Bronze #B88A5A
        color: 'var(--btn-cta-text)', // Deep Charcoal #1F1F1F (5.35:1 AA Accessible contrast)
        borderColor: 'var(--btn-cta-bg)',
      },
      secondary: {
        backgroundColor: 'var(--color-deep-charcoal)', // #1F1F1F
        color: 'var(--color-white)', // #FFFFFF (15.5:1 AAA contrast)
        borderColor: 'var(--color-deep-charcoal)',
      },
      outline: {
        backgroundColor: 'transparent',
        color: 'var(--text-primary)',
        borderColor: 'var(--border-strong)',
      },
      ghost: {
        backgroundColor: 'transparent',
        color: 'var(--text-primary)',
        borderColor: 'transparent',
      },
      danger: {
        backgroundColor: 'var(--danger)',
        color: 'var(--color-white)',
        borderColor: 'var(--danger)',
      },
      link: {
        backgroundColor: 'transparent',
        color: 'var(--info)',
        borderColor: 'transparent',
        padding: 0,
        minHeight: 'auto',
      },
    };

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        aria-busy={isLoading}
        style={{
          ...baseStyle,
          ...sizeStyles[size],
          ...variantStyles[variant],
        }}
        className={`btn btn-${variant} btn-${size} ${className}`}
        {...props}
      >
        {isLoading && <Loader2 className="animate-spin" size={size === 'sm' ? 14 : 18} aria-hidden="true" />}
        {!isLoading && leftIcon}
        {children}
        {!isLoading && rightIcon}
      </button>
    );
  }
);

Button.displayName = 'Button';
