import React from 'react';
import Link from 'next/link';
import { ArrowRight, Compass } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export function FinalCta() {
  return (
    <section className="w-full py-16 sm:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
        <span className="text-xs font-semibold uppercase tracking-widest text-[var(--brand)] block">
          Get Started Today
        </span>

        <h2 className="font-serif text-2xl sm:text-4xl md:text-5xl font-semibold tracking-tight text-[var(--foreground)] max-w-3xl mx-auto leading-tight">
          Your best work should keep working even after the project is finished.
        </h2>

        <p className="text-sm sm:text-base text-[var(--muted)] max-w-xl mx-auto leading-relaxed">
          Turn your completed interiors into an enduring, search-optimized portfolio that attracts high-intent homeowners in your city.
        </p>

        <div className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-3 max-w-md mx-auto">
          <Link href="/register" className="w-full sm:w-auto">
            <Button
              variant="primary"
              size="lg"
              className="w-full justify-center text-sm"
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Create Your Portfolio
            </Button>
          </Link>
          <Link href="#discover-projects" className="w-full sm:w-auto">
            <Button
              variant="outline"
              size="lg"
              className="w-full justify-center text-sm"
              leftIcon={<Compass className="w-4 h-4 text-[var(--brand)]" />}
            >
              Explore Projects
            </Button>
          </Link>
        </div>
      </div>
    </section>
  );
}
