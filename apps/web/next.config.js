/** @type {import('next').NextConfig} */
const securityHeaders = [
  {
    key: 'X-Content-Type-Options',
    value: 'nosniff',
  },
  {
    key: 'X-Frame-Options',
    value: 'DENY',
  },
  {
    key: 'Referrer-Policy',
    value: 'strict-origin-when-cross-origin',
  },
  {
    key: 'Permissions-Policy',
    value: 'camera=(), microphone=(self), geolocation=()',
  },
  {
    key: 'Strict-Transport-Security',
    value: 'max-age=31536000; includeSubDomains',
  },
  {
    key: 'Content-Security-Policy',
    value: "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:; font-src 'self'; connect-src 'self' http://localhost:8080 https: ws: wss:; frame-ancestors 'none'; object-src 'none';",
  },
];

function getRemotePatterns() {
  const isProd = process.env.NODE_ENV === 'production';
  const patterns = [];

  // Production remote image policy:
  // Accept ONLY explicitly configured trusted CDN hostnames from environment.
  // Strict hostname validation: RFC 1123 compliant, no wildcards, no protocols, no slashes.
  const rawHost = process.env.APP_CDN_HOSTNAME || process.env.NEXT_PUBLIC_CDN_HOSTNAME;
  if (rawHost) {
    const sanitizedHost = rawHost.trim().toLowerCase();
    const hostnameRegex = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$/;
    if (hostnameRegex.test(sanitizedHost) && !sanitizedHost.includes('*')) {
      patterns.push({
        protocol: 'https',
        hostname: sanitizedHost,
        pathname: '/**',
      });
    }
  }

  // Development mode: permit local API media paths only (no wildcard hostnames).
  if (!isProd && patterns.length === 0) {
    patterns.push(
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',
        pathname: '/api/v1/media/**',
      },
      {
        protocol: 'http',
        hostname: '127.0.0.1',
        port: '8080',
        pathname: '/api/v1/media/**',
      }
    );
  }

  return patterns;
}

const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  poweredByHeader: false,
  images: {
    qualities: [75, 90],
    remotePatterns: getRemotePatterns(),
  },
  async headers() {
    return [
      {
        source: '/:path*',
        headers: securityHeaders,
      },
      {
        source: '/invite/:path*',
        headers: [
          {
            key: 'Referrer-Policy',
            value: 'no-referrer',
          },
        ],
      },
    ];
  },
  async rewrites() {
    if (process.env.INTERNAL_API_URL) {
      return [
        {
          source: '/api/v1/:path*',
          destination: `${process.env.INTERNAL_API_URL}/:path*`,
        },
      ];
    }
    return [];
  },
};

module.exports = nextConfig;
