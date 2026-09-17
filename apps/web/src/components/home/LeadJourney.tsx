import React from 'react';
import { MessageCircle, ArrowRight, ShieldCheck, Check, Sparkles } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { Button } from '@/components/ui/Button';

export function LeadJourney() {
  const steps = [
    {
      title: '1. Intent-Driven Discovery',
      desc: 'Homeowner views your specific project (e.g. Modern TV Unit in Guntur) and loves the craftsmanship.',
    },
    {
      title: '2. One-Tap WhatsApp Inquiry',
      desc: 'With a single click on "Ask About This Design", a pre-filled WhatsApp message connects the homeowner directly to you.',
    },
    {
      title: '3. Project Context Included',
      desc: 'The inquiry automatically references the exact project name, photos, and estimated scope.',
    },
    {
      title: '4. Pipeline Qualification',
      desc: 'Direct communication enables swift site inspection scheduling and design brief alignment.',
    },
  ];

  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Text */}
          <div className="lg:col-span-6 space-y-6">
            <SectionHeader className="mb-4">
              <div>
                <SectionEyebrow>10 / Lead Generation</SectionEyebrow>
                <SectionTitle>Turn Project Admiration into Qualified Clients</SectionTitle>
                <SectionDescription>
                  In India, high-value architectural contracts start on WhatsApp. Elégance bridges the gap between public portfolio inspiration and private studio consultation.
                </SectionDescription>
              </div>
            </SectionHeader>

            <div className="space-y-3.5">
              {steps.map((s, idx) => (
                <div key={idx} className="flex items-start gap-3 p-3.5 rounded-xl border border-[var(--border)] bg-[var(--surface)] shadow-sm">
                  <div className="w-6 h-6 rounded-full bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                    {idx + 1}
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-[var(--foreground)]">{s.title}</h4>
                    <p className="text-xs text-[var(--muted)] leading-relaxed mt-0.5">{s.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right Visual: Mock WhatsApp Interaction Preview */}
          <div className="lg:col-span-6">
            <div className="p-5 sm:p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-xl space-y-5 max-w-md mx-auto">
              <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
                <div className="flex items-center gap-2.5">
                  <div className="w-10 h-10 rounded-full bg-[#25D366]/20 text-[#25D366] flex items-center justify-center">
                    <MessageCircle className="w-5 h-5" />
                  </div>
                  <div>
                    <h5 className="font-semibold text-xs text-[var(--foreground)]">Direct Studio WhatsApp</h5>
                    <p className="text-[10px] text-[var(--muted)]">Automated Context Link</p>
                  </div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-[var(--surface-alt)] text-[var(--muted)] font-semibold">
                  Studio Profile
                </span>
              </div>

              {/* Chat Bubble Simulation */}
              <div className="space-y-3">
                <div className="p-3.5 rounded-xl rounded-tl-none bg-[#DCF8C6]/30 border border-[#25D366]/20 text-xs text-[var(--foreground)] space-y-1">
                  <p className="font-semibold text-[11px] text-[var(--brand)]">
                    Inquiry via Elégance Portfolio:
                  </p>
                  <p className="leading-relaxed">
                    “Hello Studio Elégance! I came across your <strong>Modern TV Unit & Lounge</strong> project in Guntur. We have an upcoming 3BHK villa handover in Amaravati and would like to discuss a similar design.”
                  </p>
                  <span className="text-[9px] text-[var(--muted)] block text-right font-mono">10:42 AM</span>
                </div>
              </div>

              {/* Action Preview */}
              <div className="pt-2">
                <Button
                  variant="primary"
                  size="md"
                  className="w-full justify-center text-xs"
                  leftIcon={<MessageCircle className="w-4 h-4" />}
                >
                  Demonstration: “Ask About This Design”
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
