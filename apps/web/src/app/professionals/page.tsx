import type { Metadata } from 'next';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProfessionalsDiscoveryClient } from '@/components/discovery/ProfessionalsDiscoveryClient';
import { fetchDiscoveryProfessionals, mapDiscoveryCardToProfessional } from '@/lib/discovery/api';
import { Professional } from '@/lib/discovery/types';

export const metadata: Metadata = {
  title: 'Interior Designers, Studios & Architects | Elégance',
  description:
    'Discover regional interior studios, architects, custom cabinetry makers, and turnkey contractors across India. Explore real executed projects and direct contact options.',
  alternates: {
    canonical: '/professionals',
  },
  openGraph: {
    title: 'Interior Designers, Studios & Architects | Elégance',
    description:
      'Discover regional interior studios, architects, custom cabinetry makers, and turnkey contractors across India. Explore real executed projects.',
    url: '/professionals',
    siteName: 'Elégance Interior Platform',
    locale: 'en_IN',
    type: 'website',
  },
};

export default async function ProfessionalsPage() {
  let initialProfessionals: Professional[] = [];
  let initialTotal = 0;

  try {
    const res = await fetchDiscoveryProfessionals({ limit: 20 });
    initialProfessionals = (res.professionals || []).map(mapDiscoveryCardToProfessional);
    initialTotal = res.totalProfessionals || 0;
  } catch {
    // Graceful error handling: empty list on connection issue
  }

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
      <PublicHeader currentPath="/professionals" />

      <main id="main-content" className="flex-1 w-full py-6 sm:py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Breadcrumb Navigation */}
          <div className="mb-4 sm:mb-6">
            <Breadcrumb
              items={[
                { label: 'Home', href: '/' },
                { label: 'Professionals', href: '/professionals', current: true },
              ]}
            />
          </div>

          {/* Editorial Header (Single H1) */}
          <div className="mb-6 sm:mb-10 text-left">
            <span className="text-[11px] sm:text-xs font-semibold uppercase tracking-widest text-[var(--brand)] block mb-1.5">
              Practitioners & Studios
            </span>
            <h1 className="font-serif text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-[var(--foreground)] mb-2.5">
              Interior Designers, Studios & Architects Across India
            </h1>
            <p className="text-xs sm:text-base text-[var(--muted)] max-w-2xl leading-relaxed">
              Explore regional spatial practitioners who design, build, and deliver real physical spaces. Browse by trade specialty, service city, and portfolio work.
            </p>
          </div>

          <ProfessionalsDiscoveryClient
            initialProfessionals={initialProfessionals}
            initialTotal={initialTotal}
          />
        </div>
      </main>

      <Footer />
    </div>
  );
}
