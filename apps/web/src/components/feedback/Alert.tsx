'use client';

import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';

export type AlertVariant = 'info' | 'success' | 'warning' | 'error';

export interface AlertProps {
  variant?: AlertVariant;
  title?: string;
  children: React.ReactNode;
  onClose?: () => void;
  className?: string;
}

const variantStyles: Record<AlertVariant, { container: string; icon: string; iconComponent: React.ComponentType<{ className?: string }> }> = {
  info: {
    container: 'bg-[var(--info-muted)] border-[var(--info)]/20 text-[var(--foreground)]',
    icon: 'text-[var(--info)]',
    iconComponent: Info,
  },
  success: {
    container: 'bg-[var(--success-muted)] border-[var(--success)]/20 text-[var(--foreground)]',
    icon: 'text-[var(--success)]',
    iconComponent: CheckCircle2,
  },
  warning: {
    container: 'bg-[var(--warning-muted)] border-[var(--warning)]/20 text-[var(--foreground)]',
    icon: 'text-[var(--warning)]',
    iconComponent: AlertTriangle,
  },
  error: {
    container: 'bg-[var(--danger-muted)] border-[var(--danger)]/20 text-[var(--foreground)]',
    icon: 'text-[var(--danger)]',
    iconComponent: AlertCircle,
  },
};

export function Alert({
  variant = 'info',
  title,
  children,
  onClose,
  className = '',
}: AlertProps) {
  const current = variantStyles[variant];
  const Icon = current.iconComponent;

  return (
    <div
      role="alert"
      className={`relative w-full rounded-xl border p-4 flex items-start gap-3 transition-colors ${current.container} ${className}`}
    >
      <Icon className={`w-5 h-5 flex-shrink-0 mt-0.5 ${current.icon}`} />
      <div className="flex-1 text-sm">
        {title && <h5 className="font-semibold mb-1 leading-snug">{title}</h5>}
        <div className="text-sm opacity-90 leading-relaxed">{children}</div>
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          aria-label="Dismiss alert"
          className="text-[var(--muted)] hover:text-[var(--foreground)] p-1 rounded-md transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center -mr-1 -mt-1"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}
