'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { PublicClientReviewResponse } from '@/lib/ai/types';
import { fetchPublicReviewSession } from '@/lib/ai/api';
import ClientReviewView from '@/components/ai/ClientReviewView';

export default function PublicClientReviewViewPage() {
  const params = useParams();
  const id = typeof params?.id === 'string' ? params.id : Array.isArray(params?.id) ? params.id[0] : '';

  const [review, setReview] = useState<PublicClientReviewResponse | null>(null);
  const [csrfToken, setCsrfToken] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadSession = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchPublicReviewSession();
      setReview(data);

      // Resolve CSRF token from session storage
      if (typeof window !== 'undefined') {
        const stored =
          sessionStorage.getItem(`review_csrf_${data.id}`) ||
          sessionStorage.getItem('review_csrf_current') ||
          '';
        setCsrfToken(stored);
      }
    } catch (err: any) {
      const msg =
        err?.envelope?.message ||
        'Unable to load concept presentation. Your session may have expired.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSession();
  }, [loadSession]);

  if (loading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: '#FAF8F5',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              border: '3px solid #E7E1D8',
              borderTopColor: '#B88A5A',
              borderRadius: '50%',
              margin: '0 auto 16px',
              animation: 'spin 1s linear infinite',
            }}
          />
          <p style={{ color: '#666', fontSize: '0.9rem' }}>Loading Presentation...</p>
          <style jsx>{`
            @keyframes spin {
              from { transform: rotate(0deg); }
              to { transform: rotate(360deg); }
            }
          `}</style>
        </div>
      </div>
    );
  }

  if (error || !review) {
    return (
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: '#FAF8F5',
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
            textAlign: 'center',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.04)',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: '#FBEBEB',
              color: '#D32F2F',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              fontSize: '20px',
            }}
          >
            ✕
          </div>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#1F1F1F', marginBottom: '8px' }}>
            Presentation Unavailable
          </h1>
          <p style={{ fontSize: '0.9rem', color: '#666', lineHeight: 1.5, marginBottom: '24px' }}>
            {error || 'This presentation session has ended or is no longer available.'}
          </p>
          <div
            style={{
              padding: '12px 16px',
              backgroundColor: '#FAF8F5',
              borderRadius: '8px',
              border: '1px solid #E7E1D8',
              fontSize: '0.85rem',
              color: '#555',
            }}
          >
            Please contact your interior design studio for assistance or to receive a new link.
          </div>
        </div>
      </div>
    );
  }

  return <ClientReviewView review={review} csrfToken={csrfToken} onRefresh={loadSession} />;
}
