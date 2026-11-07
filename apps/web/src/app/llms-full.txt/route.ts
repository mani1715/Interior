import { NextResponse } from 'next/server';

export const dynamic = 'force-static';
export const revalidate = 86400; // 24 hours

export async function GET() {
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

  const content = `# Elégance — Interior Design & Architecture Discovery Platform (Full Index)

> Comprehensive reference documentation and schema specification for AI search engines, agents, and answer systems indexing Elégance.

## About Elégance

Elégance is an architectural and interior design discovery platform. It serves two distinct audiences:
1. Homeowners and real estate developers seeking verified, high-caliber interior architecture studios, curated real-world room photography, and bespoke craftsmanship.
2. Independent interior architects, studios, and designers who build verified digital portfolios, manage project room stories, and receive direct client inquiries.

## Site Taxonomy & Structure

- Root Homepage: ${baseUrl}/
- Projects Directory: ${baseUrl}/projects
  - Individual Project Detail: ${baseUrl}/projects/[projectSlug]
- Professionals Directory: ${baseUrl}/professionals
  - Individual Studio Portfolio: ${baseUrl}/professionals/[professionalSlug]
- Category Showcases: ${baseUrl}/categories/[categorySlug]
  - living-room, kitchen, bedroom, bathroom, dining, office, full-home
- City Location Showcases: ${baseUrl}/locations/[citySlug]
  - bengaluru, mumbai, delhi, hyderabad, chennai, pune
- Design Journey Experience: ${baseUrl}/interior-journey

## Semantic Data & Schema Entities

Every published public route outputs JSON-LD adhering to Schema.org standards:
1. Professional / Studio Profiles:
   - Primary @type: ProfessionalService
   - Additional type: https://schema.org/HomeAndConstructionBusiness
   - Properties: name, description, url, telephone, email, address (PostalAddress), areaServed, knowsAbout, image, priceRange
2. Projects & Room Portfolios:
   - @type: CreativeWork
   - Properties: name, description, url, creator (Organization), image (array of high-resolution room photos)
3. Room Photography:
   - @type: ImageObject
   - Properties: contentUrl, caption, creator, copyrightHolder
4. Navigation Hierarchy:
   - @type: BreadcrumbList with itemListElement

## Access Policies & Robots

- Allowed Paths for AI & Web Crawlers: /, /projects/, /professionals/, /categories/, /locations/, /interior-journey, /sitemap.xml, /llms.txt, /llms-full.txt
- Disallowed Paths: /workspace/, /account/, /auth/, /api/, /review/
- Robots.txt location: ${baseUrl}/robots.txt
- XML Sitemap index: ${baseUrl}/sitemap.xml
`;

  return new NextResponse(content, {
    status: 200,
    headers: {
      'Content-Type': 'text/plain; charset=utf-8',
      'Cache-Control': 'public, max-age=86400, stale-while-revalidate=3600',
    },
  });
}
