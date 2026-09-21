import { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

  return {
    rules: [
      {
        userAgent: '*',
        allow: [
          '/',
          '/professionals/',
          '/projects/',
          '/categories/',
          '/locations/',
        ],
        disallow: [
          '/workspace/',
          '/workspace/*',
          '/account/',
          '/account/*',
          '/auth/',
          '/auth/*',
          '/sign-in',
          '/sign-up',
          '/api/',
          '/_next/',
        ],
      },
    ],
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}
