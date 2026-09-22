'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { exchangeReviewToken } from '@/lib/ai/api';

export default function ReviewExchangePage() {
  const params = useParams();
  const router = useRouter();
  const token = typeof params?.token === 'string' ? params.token : Array.isArray(params?.token) ? params.token[0] : '';
  
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      setError('Invalid or missing review link.');
      setLoading(false);
      return;
    }

    let isMounted = true;

    async function doExchange() {
      try {
        const result = await exchangeReviewToken(token);
        if (!isMounted) return;

        // Store CSRF token scoped to this review session
        if (typeof window !== 'undefined' && result.csrfToken) {
          try {
            sessionStorage.setItem(`review_csrf_${result.reviewPublicId}`, result.csrfToken);
            sessionStorage.setItem('review_csrf_current', result.csrfToken);
          } catch {
            // Ignore sessionStorage restrictions in private browsing if any
          }
        }

        // Scrub raw token from browser address bar and history
        router.replace(result.redirectUrl);
      } catch (err: any) {
        if (!isMounted) return;
        const msg =
          err?.envelope?.message ||
          'This review link is unavailable, expired, or has been revoked by the design studio.';
        setError(msg);
        setLoading(false);
      }
    }

    doExchange();

    return () => {
      isMounted = false;
    };
  }, [token, router]);

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#FAF8F5',
        color: '#1F1F1F',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      }}
    >
      <div
        style={{
          maxWidth: '480px',
          width: '100%',
          backgroundColor: '#FFFFFF',
          borderRadius: '16px',
          border: '1px solid #E7E1D8',
          padding: '36px 32px',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.04)',
          textAlign: 'center',
        }}
      >
        {loading ? (
          <div>
            <div
              style={{
                width: '44px',
                height: '44px',
                border: '3px solid #E7E1D8',
                borderTopColor: '#B88A5A',
                borderRadius: '50%',
                margin: '0 auto 20px',
                animation: 'spin 1s linear infinite',
              }}
            />
            <h1 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '8px', color: '#1F1F1F' }}>
              Opening Presentation...
            </h1>
            <p style={{ fontSize: '0.9rem', color: '#666', lineHeight: 1.5 }}>
              Securing private session with your design studio.
            </p>
            <style jsx>{`
              @keyframes spin {
                from { transform: rotate(0deg); }
                to { transform: rotate(360deg); }
              }
            `}</style>
          </div>
        ) : (
          <div>
            <div
              style={{
                width: '52px',
                height: '52px',
                borderRadius: '50%',
                backgroundColor: '#FBEBEB',
                color: '#D32F2F',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 20px',
                fontSize: '24px',
              }}
            >
              ✕
            </div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '12px', color: '#1F1F1F' }}>
              Review Link Unavailable
            </h1>
            <p style={{ fontSize: '0.925rem', color: '#666', lineHeight: 1.6, marginBottom: '24px' }}>
              {error}
            </p>
            <div
              style={{
                padding: '14px 16px',
                backgroundColor: '#FAF8F5',
                borderRadius: '8px',
                border: '1px solid #E7E1D8',
                fontSize: '0.85rem',
                color: '#555',
                textAlign: 'left',
              }}
            >
              Please contact your interior designer directly to request a refreshed presentation link.
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
