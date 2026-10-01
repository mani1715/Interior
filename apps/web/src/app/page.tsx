import type { Metadata } from 'next';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { BrandHero } from '@/components/home/BrandHero';
import motion from '@/components/home/HomeMotion.module.css';
import { ScrollEntrances } from '@/components/home/ScrollEntrances';
import { EditorialHome } from '@/components/home/EditorialHome';
import { Footer } from '@/components/home/Footer';
import { serializeJsonLd } from '@/lib/seo/structured-data';

export const metadata: Metadata = {
  title: 'Elégance — Interior Designer Portfolio, Discovery & AI Visualizer',
  description:
    'Turn beautiful interiors into a business people can discover. Build your studio portfolio, get found on Google, visualize client ideas with AI, and capture high-intent leads across India.',
  alternates: {
    canonical: '/',
  },
  openGraph: {
    title: 'Elégance — Interior Designer Portfolio, Discovery & AI Visualizer',
    description:
      'Turn beautiful interiors into a business people can discover. Build your studio portfolio, get found on Google, visualize client ideas with AI, and capture high-intent leads across India.',
    url: '/',
    siteName: 'Elégance Interior Platform',
    locale: 'en_IN',
    type: 'website',
  },
  twitter: {
    card: 'summary',
    title: 'Elégance — Interior Designer Portfolio, Discovery & AI Visualizer',
    description:
      'Turn beautiful interiors into a business people can discover. Build your studio portfolio, get found on Google, and visualize client ideas with AI.',
  },
  robots: {
    index: true,
    follow: true,
  },
};

const jsonLd = {
  '@context': 'https://schema.org',
  '@graph': [
    {
      '@type': 'WebSite',
      name: 'Elégance Interior Platform',
      description: 'The digital portfolio, discovery, and AI visualization platform for interior professionals.',
    },
    {
      '@type': 'Organization',
      name: 'Elégance',
      description: 'Platform connecting interior designers, studios, and architects with homeowners through real projects.',
    },
  ],
};

export default function HomePage() {
  return <>
    <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: serializeJsonLd(jsonLd) }} />
    <PublicHeader />
    <main id="main-content" className={motion.home}><BrandHero /><ScrollEntrances><EditorialHome /></ScrollEntrances></main>
    <Footer />
  </>;
}
