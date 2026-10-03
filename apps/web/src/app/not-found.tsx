import React from 'react';
import Link from 'next/link';
import { ArrowUpRight, Compass, Home } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';

export default function NotFound() {
  return (
    <div className="public-editorial min-h-screen bg-[#FAF8F5] text-[#1F1F1F] flex flex-col selection:bg-[#B88A5A]/20 selection:text-[#1F1F1F]">
      <PublicHeader />

      <main id="main-content" className="flex-1 flex items-center justify-center py-20 px-6 sm:px-8">
        <div className="max-w-xl text-center space-y-8">
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-[#B88A5A] block mb-2">
              404 — Page Not Found
            </span>
            <h1 className="font-serif text-3xl sm:text-5xl font-semibold tracking-tight text-[#1F1F1F]">
              Space not found.
            </h1>
            <p className="text-sm sm:text-base text-[#6B6B6B] mt-4 leading-relaxed max-w-md mx-auto">
              The page or project you are looking for doesn’t exist, has been moved, or is kept private by its studio.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <Link
              href="/"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-[#1F1F1F] text-white rounded-xl text-xs font-semibold hover:bg-black transition-colors"
            >
              <Home className="w-4 h-4" />
              <span>Return Home</span>
            </Link>
            <Link
              href="/projects"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 border border-[#D9D9D9] bg-white text-[#1F1F1F] rounded-xl text-xs font-semibold hover:bg-[#F4F4F4] transition-colors"
            >
              <Compass className="w-4 h-4 text-[#B88A5A]" />
              <span>Explore Projects</span>
              <ArrowUpRight className="w-3.5 h-3.5 opacity-60" />
            </Link>
            <Link
              href="/professionals"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 border border-[#D9D9D9] bg-white text-[#1F1F1F] rounded-xl text-xs font-semibold hover:bg-[#F4F4F4] transition-colors"
            >
              <span>Find Professionals</span>
              <ArrowUpRight className="w-3.5 h-3.5 opacity-60" />
            </Link>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
