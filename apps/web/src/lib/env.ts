/**
 * Environment configuration validation for the web application.
 * Ensures non-secret public parameters are configured safely.
 */
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1',
  isProduction: process.env.NODE_ENV === 'production',
};
