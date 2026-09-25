'use client';

import React, { useState } from 'react';

interface VerificationBadgeProps {
  verified: boolean;
  businessName?: string | null;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function VerificationBadge({
  verified,
  businessName,
  className = '',
  size = 'md',
}: VerificationBadgeProps) {
  const [showTooltip, setShowTooltip] = useState(false);

  if (!verified) {
    return null;
  }

  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5 gap-1',
    md: 'text-xs px-2.5 py-1 gap-1.5',
    lg: 'text-sm px-3 py-1.5 gap-2',
  }[size];

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  }[size];

  return (
    <div className={`relative inline-flex items-center ${className}`}>
      <span
        tabIndex={0}
        role="status"
        aria-label="Platform Verified Business"
        onMouseEnter={() => setShowTooltip(true)}
        onMouseLeave={() => setShowTooltip(false)}
        onFocus={() => setShowTooltip(true)}
        onBlur={() => setShowTooltip(false)}
        className={`inline-flex items-center font-medium rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200/80 cursor-help transition-colors hover:bg-emerald-100/70 select-none ${sizeClasses}`}
      >
        <svg
          className={`${iconSizes} text-emerald-600 shrink-0`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            fillRule="evenodd"
            d="M10 1.944A11.954 11.954 0 012.166 5C2.056 5.649 2 6.319 2 7c0 5.225 3.34 9.67 8 11.317C14.66 16.67 18 12.225 18 7c0-.682-.057-1.35-.166-2.001A11.954 11.954 0 0110 1.944zM13.707 8.707a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
            clipRule="evenodd"
          />
        </svg>
        <span>Verified Business</span>
      </span>

      {showTooltip && (
        <div
          role="tooltip"
          className="absolute z-50 bottom-full left-1/2 -translate-x-1/2 mb-2 w-72 p-3 text-xs leading-relaxed text-zinc-700 bg-white rounded-lg shadow-xl border border-zinc-200 pointer-events-none"
        >
          <div className="font-semibold text-zinc-900 mb-1 flex items-center gap-1.5">
            <svg
              className="w-4 h-4 text-emerald-600 shrink-0"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            Verified Business Entity
          </div>
          <p className="text-zinc-600">
            {businessName ? `${businessName}'s ` : 'This business\'s '}
            official registration documents were verified by the platform team.
          </p>
          <p className="mt-1.5 text-[11px] text-zinc-500 italic border-t border-zinc-100 pt-1.5">
            This verification confirms identity and registration records. It does not guarantee service quality or project outcomes.
          </p>
          <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-1 border-4 border-transparent border-t-white" />
        </div>
      )}
    </div>
  );
}
