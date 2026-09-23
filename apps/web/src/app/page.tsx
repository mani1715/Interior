import type { Metadata } from 'next';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Hero } from '@/components/home/Hero';
import { CoreValueStrip } from '@/components/home/CoreValueStrip';
import { TransformationExperience } from '@/components/home/TransformationExperience';
import { PortfolioSection } from '@/components/home/PortfolioSection';
import { DiscoverySection } from '@/components/home/DiscoverySection';
import { AiVisualizerSection } from '@/components/home/AiVisualizerSection';
import { BeforeAiRealitySection } from '@/components/home/BeforeAiRealitySection';
import { SeoSection } from '@/components/home/SeoSection';
import { ProjectInspiration } from '@/components/home/ProjectInspiration';
import { HowItWorks } from '@/components/home/HowItWorks';
import { ProfessionalTypes } from '@/components/home/ProfessionalTypes';
import { LeadJourney } from '@/components/home/LeadJourney';
import { MobileWorkflow } from '@/components/home/MobileWorkflow';
import { FinalCta } from '@/components/home/FinalCta';
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
  return (
    <>
      {/* Truthful Homepage Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: serializeJsonLd(jsonLd) }}
      />

      <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
        {/* Public Header with responsive drawer */}
        <PublicHeader
          navLinks={[
            { label: 'Projects', href: '/projects' },
            { label: 'Professionals', href: '/professionals' },
            { label: 'Portfolio', href: '#portfolio-builder' },
            { label: 'AI Visualizer', href: '#ai-visualizer' },
            { label: 'Transformation', href: '#transformation-experience' },
          ]}
        />

        <main id="main-content" className="flex-1 w-full">
          {/* 01 Hero (Single H1) */}
          <Hero />

          {/* 02 Core Value Pillars Strip */}
          <CoreValueStrip />

          {/* 03 Transformation Signature Experience (7-Stage Architectural Evolution) */}
          <TransformationExperience />

          {/* 04 Dedicated Portfolio Builder Section */}
          <div id="portfolio-builder">
            <PortfolioSection />
          </div>

          {/* 05 Real Project Discovery */}
          <DiscoverySection />

          {/* 06 AI Spatial Visualizer (Formula, References & Disclaimer) */}
          <AiVisualizerSection />

          {/* 07 Signature 3-Stage Narrative (Site -> AI -> Built Reality) */}
          <BeforeAiRealitySection />

          {/* 08 Organic Google Discoverability */}
          <SeoSection />

          {/* 09 Project Inspiration Gallery */}
          <ProjectInspiration />

          {/* 10 Professional How It Works */}
          <HowItWorks />

          {/* 11 Professional Types Supported */}
          <ProfessionalTypes />

          {/* 12 Lead Journey & Direct WhatsApp Workflow */}
          <LeadJourney />

          {/* 13 Mobile-First Job-Site Utility */}
          <MobileWorkflow />

          {/* 14 Final Call to Action */}
          <FinalCta />
        </main>

        {/* 15 Production Footer */}
        <Footer />
      </div>
    </>
  );
}
