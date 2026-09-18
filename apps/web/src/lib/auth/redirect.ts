/**
 * Sanitizes a redirect URL to prevent open redirect vulnerabilities.
 * Only allows safe, relative internal application paths.
 */
export function sanitizeRedirectUrl(url?: string | null, defaultUrl: string = '/account'): string {
  if (!url || typeof url !== 'string') {
    return defaultUrl;
  }

  const trimmed = url.trim();

  // Reject CRLF header injection
  if (trimmed.includes('\r') || trimmed.includes('\n')) {
    return defaultUrl;
  }

  // Reject empty, protocol-relative, backslash, or windows network share paths
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.startsWith('/\\') || trimmed.startsWith('\\')) {
    return defaultUrl;
  }

  // Disallow javascript:, data:, backslash, or encoded slashes/backslashes
  const lower = trimmed.toLowerCase();
  if (
    lower.includes('javascript:') ||
    lower.includes('data:') ||
    lower.includes('\\') ||
    lower.includes('%2f') ||
    lower.includes('%5c')
  ) {
    return defaultUrl;
  }

  return trimmed;
}
