'use client';

import { useEffect } from 'react';

export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error securely without leaking sensitive attributes
    console.error('Unhandled application error:', error.message);
  }, [error]);

  return (
    <div className="container" style={{ padding: '40px 16px', textAlign: 'center' }}>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '12px' }}>Something went wrong!</h2>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        An unexpected error occurred. Please try again.
      </p>
      <button onClick={() => reset()} className="btn btn-primary">
        Try again
      </button>
    </div>
  );
}
