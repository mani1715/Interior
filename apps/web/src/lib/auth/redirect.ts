/**
 * Sanitizes a redirect URL to prevent open redirect vulnerabilities.
 * Only allows relative internal application paths.
 */
export function sanitizeRedirectUrl(url?: string | null, defaultUrl: string = '/account'): string {
  if (!url || typeof url !== 'string') {
    return defaultUrl;
  }

  const trimmed = url.trim();

  // Reject empty or protocol-relative or backslash-based paths
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.startsWith('/\\')) {
    return defaultUrl;
  }

  // Reject CRLF header injection
  if (trimmed.includes('\r') || trimmed.includes('\n')) {
    return defaultUrl;
  }

  return trimmed;
}
