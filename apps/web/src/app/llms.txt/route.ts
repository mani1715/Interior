import { NextResponse } from 'next/server';

export const dynamic = 'force-static';
export const revalidate = 86400; // 24 hours

export async function GET() {
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

  const content = `# Elégance — Interior Design & Architecture Discovery Platform

> Elégance is a professional discovery, portfolio showcase, and interior visualization platform connecting homeowners and commercial clients with verified interior architects, studios, and bespoke residential projects.

## Core Discovery Endpoints

- [Projects Directory](${baseUrl}/projects): Curated residential, commercial, and hospitality interior design projects with spatial breakdowns and room galleries.
- [Professionals & Studios Directory](${baseUrl}/professionals): Directory of verified interior design studios, architects, and design practices across major urban locations.
- [Interior Journey](${baseUrl}/interior-journey): Visual interactive design journey and aesthetic inspiration for bespoke spaces.

## Canonical Room Categories

- [Living Room](${baseUrl}/categories/living-room): Curated living room, lounge, and open-plan seating spaces.
- [Kitchen](${baseUrl}/categories/kitchen): Modular kitchens, island layouts, and custom cabinetry finishes.
- [Bedroom](${baseUrl}/categories/bedroom): Master suites, wardrobe storage configurations, and calm sanctuary bedrooms.
- [Bathroom](${baseUrl}/categories/bathroom): Ensuite retreats, vanity details, and luxury bath designs.
- [Dining Room](${baseUrl}/categories/dining): Formal and casual dining arrangements and bespoke tables.
- [Home Office](${baseUrl}/categories/office): Ergonomic workspaces, libraries, and executive home studios.
- [Full Home Turnkey](${baseUrl}/categories/full-home): Comprehensive end-to-end residential transformations.

## Regional Hubs & Local Practice

- [Bengaluru Interior Studios](${baseUrl}/locations/bengaluru): Verified architectural practices and residential interior studios in Bengaluru.
- [Mumbai Interior Studios](${baseUrl}/locations/mumbai): High-end apartments, sea-facing residences, and penthouses in Mumbai.
- [Delhi NCR Interior Studios](${baseUrl}/locations/delhi): Luxury farmhouses, contemporary villas, and urban estates in Delhi NCR.
- [Hyderabad Interior Studios](${baseUrl}/locations/hyderabad): Modern villas and expansive gated-community residences in Hyderabad.
- [Chennai Interior Studios](${baseUrl}/locations/chennai): Coastal residential designs and contemporary spaces in Chennai.
- [Pune Interior Studios](${baseUrl}/locations/pune): Urban studios and modern hillside residential designs in Pune.

## Technical & Agent Readiness Notes

- Structured Data: Pages serve Schema.org JSON-LD (ProfessionalService, HomeAndConstructionBusiness, CreativeWork, ImageObject).
- Sitemap: Accessible at ${baseUrl}/sitemap.xml
- Full Documentation: [llms-full.txt](${baseUrl}/llms-full.txt)
`;

  return new NextResponse(content, {
    status: 200,
    headers: {
      'Content-Type': 'text/plain; charset=utf-8',
      'Cache-Control': 'public, max-age=86400, stale-while-revalidate=3600',
    },
  });
}
