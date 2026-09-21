import { MetadataRoute } from 'next';
import { fetchSitemapEntries } from '../lib/seo/api';

export const revalidate = 3600; // revalidate every hour

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

  const staticRoutes: MetadataRoute.Sitemap = [
    {
      url: `${baseUrl}/`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1.0,
    },
    {
      url: `${baseUrl}/professionals`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 0.9,
    },
    {
      url: `${baseUrl}/projects`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 0.9,
    },
    // Canonical categories
    ...[
      'living-room',
      'kitchen',
      'bedroom',
      'bathroom',
      'dining',
      'office',
      'full-home',
    ].map((cat) => ({
      url: `${baseUrl}/categories/${cat}`,
      lastModified: new Date(),
      changeFrequency: 'weekly' as const,
      priority: 0.8,
    })),
    // Canonical locations
    ...[
      'bengaluru',
      'mumbai',
      'delhi',
      'hyderabad',
      'chennai',
      'pune',
    ].map((city) => ({
      url: `${baseUrl}/locations/${city}`,
      lastModified: new Date(),
      changeFrequency: 'weekly' as const,
      priority: 0.8,
    })),
  ];

  try {
    const dynamicItems = await fetchSitemapEntries();
    const dynamicRoutes: MetadataRoute.Sitemap = dynamicItems.map((item) => ({
      url: item.path.startsWith('http') ? item.path : `${baseUrl}${item.path}`,
      lastModified: item.lastModified ? new Date(item.lastModified) : new Date(),
      changeFrequency: (item.changefreq || 'weekly') as
        | 'always'
        | 'hourly'
        | 'daily'
        | 'weekly'
        | 'monthly'
        | 'yearly'
        | 'never',
      priority: parseFloat(item.priority || '0.7'),
    }));

    return [...staticRoutes, ...dynamicRoutes];
  } catch {
    return staticRoutes;
  }
}
