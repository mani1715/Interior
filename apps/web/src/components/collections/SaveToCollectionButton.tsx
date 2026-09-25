'use client';

import React, { useState } from 'react';
import { SaveToCollectionModal } from './SaveToCollectionModal';

interface SaveToCollectionButtonProps {
  projectId: string;
  className?: string;
  variant?: 'icon' | 'button';
  onSaved?: (collectionTitle: string) => void;
}

export function SaveToCollectionButton({
  projectId,
  className = '',
  variant = 'icon',
  onSaved,
}: SaveToCollectionButtonProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [savedFeedback, setSavedFeedback] = useState<string | null>(null);

  const handleSaved = (collectionTitle: string) => {
    setSavedFeedback(collectionTitle);
    onSaved?.(collectionTitle);
    setTimeout(() => {
      setSavedFeedback(null);
    }, 2500);
  };

  return (
    <>
      {variant === 'icon' ? (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            e.preventDefault();
            setModalOpen(true);
          }}
          aria-label="Save project to collection"
          className={`p-2 rounded-full bg-white/90 backdrop-blur-xs text-zinc-700 hover:text-zinc-950 hover:bg-white shadow-sm transition-all ${className}`}
        >
          {savedFeedback ? (
            <svg className="w-4 h-4 text-emerald-600" viewBox="0 0 20 20" fill="currentColor">
              <path
                fillRule="evenodd"
                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                clipRule="evenodd"
              />
            </svg>
          ) : (
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              strokeWidth="2"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
              />
            </svg>
          )}
        </button>
      ) : (
        <button
          type="button"
          onClick={() => setModalOpen(true)}
          className={`inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-zinc-300 bg-white hover:bg-zinc-50 text-zinc-800 transition-colors ${className}`}
        >
          {savedFeedback ? (
            <>
              <svg className="w-3.5 h-3.5 text-emerald-600" viewBox="0 0 20 20" fill="currentColor">
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
              <span>Saved to {savedFeedback}</span>
            </>
          ) : (
            <>
              <svg
                className="w-3.5 h-3.5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                strokeWidth="2"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
                />
              </svg>
              <span>Save to Collection</span>
            </>
          )}
        </button>
      )}

      <SaveToCollectionModal
        projectId={projectId}
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={handleSaved}
      />
    </>
  );
}
