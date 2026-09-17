'use client';

import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle } from 'lucide-react';

export type InlineMessageVariant = 'info' | 'success' | 'warning' | 'error';

export interface InlineMessageProps {
  variant?: InlineMessageVariant;
  children: React.ReactNode;
  className?: string;
}

const variantStyles: Record<InlineMessageVariant, { text: string; icon: string; iconComponent: React.ComponentType<{ className?: string }> }> = {
  info: {
    text: 'text-[var(--info)]',
    icon: 'text-[var(--info)]',
    iconComponent: Info,
  },
  success: {
    text: 'text-[var(--success)]',
    icon: 'text-[var(--success)]',
    iconComponent: CheckCircle2,
  },
  warning: {
    text: 'text-[var(--warning)]',
    icon: 'text-[var(--warning)]',
    iconComponent: AlertTriangle,
  },
  error: {
    text: 'text-[var(--danger)]',
    icon: 'text-[var(--danger)]',
    iconComponent: AlertCircle,
  },
};

export function InlineMessage({
  variant = 'info',
  children,
  className = '',
}: InlineMessageProps) {
  const current = variantStyles[variant];
  const Icon = current.iconComponent;

  return (
    <div className={`flex items-center gap-1.5 text-xs font-medium ${current.text} ${className}`}>
      <Icon className="w-4 h-4 flex-shrink-0" />
      <span>{children}</span>
    </div>
  );
}
