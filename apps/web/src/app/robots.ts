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
          '/interior-journey',
          '/llms.txt',
          '/llms-full.txt',
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
          '/review/',
          '/review/*',
          '/api/',
          '/_next/',
        ],
      },
      // Explicit AI search and agent crawling directives (GEO & Agentic Readiness)
      {
        userAgent: [
          'GPTBot',
          'ChatGPT-User',
          'ClaudeBot',
          'PerplexityBot',
          'Google-Extended',
          'Applebot-Extended',
          'Amazonbot',
        ],
        allow: [
          '/',
          '/professionals/',
          '/projects/',
          '/categories/',
          '/locations/',
          '/interior-journey',
          '/llms.txt',
          '/llms-full.txt',
        ],
        disallow: [
          '/workspace/',
          '/account/',
          '/auth/',
          '/api/',
          '/review/',
        ],
      },
    ],
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}
