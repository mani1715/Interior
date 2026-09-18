import React from 'react';
import { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import { Mail, Phone, Globe, Camera, MapPin, Award, CheckCircle, ArrowRight } from 'lucide-react';

export const ReferenceTemplate: React.FC<PortfolioTemplateProps> = ({
  studioName,
  studioSlug,
  professionalType,
  professionalTitle,
  studioCity,
  studioState,
  templateKey,
  headline,
  subheadline,
  bio,
  designPhilosophy,
  yearsOfExperience,
  primaryColor = '#1F2937',
  secondaryColor = '#F3F4F6',
  accentColor = '#C5A880',
  fontPairing = 'PLAYFAIR_INTER',
  publicContacts = [],
  canonicalServices = [],
  canonicalSpecialties = [],
  canonicalServiceAreas = [],
  visibleSections = [],
  isMobilePreview = false,
}) => {
  const sortedSections = [...visibleSections].sort((a, b) => a.displayOrder - b.displayOrder);

  // Derive font family styles
  const getFontFamilyHeading = () => {
    switch (fontPairing) {
      case 'PLAYFAIR_INTER':
        return 'font-serif';
      case 'CORMORANT_PLUS_JAKARTA':
        return 'font-serif';
      case 'CINZEL_MANROPE':
        return 'font-serif tracking-wider';
      case 'SYNE_SPACE_GROTESK':
        return 'font-sans font-black tracking-tight';
      case 'FRAUNCES_OUTFIT':
        return 'font-serif';
      case 'BODONI_INTER':
        return 'font-serif italic';
      default:
        return 'font-serif';
    }
  };

  const headingClass = getFontFamilyHeading();

  return (
    <div
      className={`bg-white text-charcoal-900 selection:bg-sand-200 transition-all ${
        isMobilePreview ? 'max-w-sm mx-auto border border-sand-300 rounded-3xl shadow-xl overflow-hidden' : 'w-full'
      }`}
      style={{
        // Dynamic theme CSS variables
        ['--theme-primary' as any]: primaryColor || '#1F2937',
        ['--theme-secondary' as any]: secondaryColor || '#F3F4F6',
        ['--theme-accent' as any]: accentColor || '#C5A880',
      }}
    >
      {/* Navigation Header */}
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur-md border-b border-sand-200 px-6 py-4 flex items-center justify-between">
        <div>
          <span className={`${headingClass} text-lg font-bold tracking-tight text-charcoal-900 block`}>
            {studioName || 'Studio Portfolio'}
          </span>
          {(studioCity || studioState) && (
            <span className="text-[11px] text-charcoal-500 flex items-center gap-1">
              <MapPin className="w-3 h-3 text-bronze-600 inline" />
              {[studioCity, studioState].filter(Boolean).join(', ')}
            </span>
          )}
        </div>
        <nav className="hidden sm:flex items-center gap-6 text-xs font-medium text-charcoal-600">
          <a href="#about" className="hover:text-charcoal-900 transition-colors">About</a>
          <a href="#services" className="hover:text-charcoal-900 transition-colors">Services</a>
          <a href="#projects" className="hover:text-charcoal-900 transition-colors">Portfolio</a>
          <a href="#contact" className="hover:text-charcoal-900 transition-colors">Contact</a>
        </nav>
      </header>

      {/* Dynamic Sections in Persisted Display Order */}
      <main className="divide-y divide-sand-100">
        {sortedSections.map((section) => {
          switch (section.sectionType) {
            case 'HERO':
              return (
                <section
                  key={section.sectionId}
                  id="hero"
                  className="px-6 py-16 sm:py-24 text-center bg-gradient-to-b from-sand-50/60 to-white"
                >
                  <div className="max-w-3xl mx-auto space-y-6">
                    {professionalTitle && (
                      <span className="inline-block px-3 py-1 rounded-full text-xs font-semibold tracking-wider uppercase bg-sand-200/80 text-charcoal-700">
                        {professionalTitle}
                      </span>
                    )}
                    <h1 className={`${headingClass} text-3xl sm:text-5xl font-normal tracking-tight text-charcoal-950 leading-tight`}>
                      {section.content?.title || headline || 'Designing Spaces that Elevate Daily Living'}
                    </h1>
                    <p className="text-sm sm:text-base text-charcoal-600 max-w-xl mx-auto leading-relaxed">
                      {section.content?.subtitle || subheadline || 'Thoughtful interior architecture and bespoke styling for discerning residential and commercial clients.'}
                    </p>
                    <div className="pt-4 flex flex-wrap justify-center gap-3">
                      <a
                        href="#contact"
                        className="px-6 py-3 rounded-full text-xs font-semibold uppercase tracking-wider bg-charcoal-900 text-white hover:bg-charcoal-800 transition-colors shadow-sm inline-flex items-center gap-2"
                      >
                        <span>Request Consultation</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  </div>
                </section>
              );

            case 'ABOUT':
              return (
                <section key={section.sectionId} id="about" className="px-6 py-16 sm:py-20 max-w-4xl mx-auto">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
                    <div>
                      <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
                        Our Story
                      </span>
                      <h2 className={`${headingClass} text-2xl sm:text-3xl text-charcoal-900 font-normal`}>
                        {section.content?.heading || 'About the Studio'}
                      </h2>
                      {yearsOfExperience && (
                        <div className="mt-4 p-4 rounded-xl bg-sand-50 border border-sand-200">
                          <span className="block text-2xl font-bold text-bronze-700">{yearsOfExperience}+</span>
                          <span className="text-xs text-charcoal-600">Years of Practice</span>
                        </div>
                      )}
                    </div>
                    <div className="md:col-span-2 space-y-4 text-xs sm:text-sm text-charcoal-600 leading-relaxed">
                      <p>
                        {bio || section.content?.text || 'Dedicated to crafting architectural interiors shaped by light, materiality, and enduring functional elegance.'}
                      </p>
                      {canonicalSpecialties.length > 0 && (
                        <div className="pt-2">
                          <h4 className="text-xs font-semibold text-charcoal-900 mb-2 uppercase tracking-wider">
                            Design Specialties
                          </h4>
                          <div className="flex flex-wrap gap-1.5">
                            {canonicalSpecialties.map((spec) => (
                              <span
                                key={spec.specialtyCode}
                                className="px-2.5 py-1 rounded-md bg-sand-100 text-charcoal-700 text-[11px] font-medium"
                              >
                                {spec.specialtyName}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </section>
              );

            case 'SERVICES':
              return (
                <section key={section.sectionId} id="services" className="px-6 py-16 sm:py-20 bg-sand-50/50">
                  <div className="max-w-4xl mx-auto space-y-8">
                    <div className="text-center max-w-md mx-auto">
                      <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
                        What We Do
                      </span>
                      <h2 className={`${headingClass} text-2xl sm:text-3xl text-charcoal-900 font-normal`}>
                        {section.content?.heading || 'Design Services'}
                      </h2>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {canonicalServices.length > 0 ? (
                        canonicalServices.map((srv) => (
                          <div
                            key={srv.serviceCode}
                            className="bg-white p-5 rounded-2xl border border-sand-200 shadow-sm space-y-2 hover:border-sand-300 transition-colors"
                          >
                            <CheckCircle className="w-4 h-4 text-bronze-700" />
                            <h3 className="text-sm font-semibold text-charcoal-900">{srv.serviceName}</h3>
                            <p className="text-xs text-charcoal-500 leading-relaxed">
                              Tailored end-to-end design solutions from initial space planning to final material selection.
                            </p>
                          </div>
                        ))
                      ) : (
                        <div className="col-span-full p-8 text-center bg-white rounded-2xl border border-sand-200 text-xs text-charcoal-500">
                          Services will appear here once configured in your business profile.
                        </div>
                      )}
                    </div>
                  </div>
                </section>
              );

            case 'FEATURED_PROJECTS':
            case 'PROJECT_GALLERY':
              return (
                <section key={section.sectionId} id="projects" className="px-6 py-16 sm:py-20 max-w-5xl mx-auto">
                  <div className="text-center max-w-md mx-auto mb-10">
                    <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
                      Selected Works
                    </span>
                    <h2 className={`${headingClass} text-2xl sm:text-3xl text-charcoal-900 font-normal`}>
                      {section.content?.heading || 'Featured Projects'}
                    </h2>
                  </div>

                  {/* Empty state: Gracefully tolerates 0 projects without breaking, as Project CMS is in Phase 18 */}
                  <div className="p-12 text-center bg-sand-50/60 rounded-3xl border border-dashed border-sand-300 space-y-3">
                    <Award className="w-8 h-8 text-bronze-700 mx-auto opacity-70" />
                    <h3 className="text-sm font-semibold text-charcoal-800">Projects In Curation</h3>
                    <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
                      Case studies and project photography are currently being prepared for presentation.
                    </p>
                  </div>
                </section>
              );

            case 'DESIGN_PHILOSOPHY':
              return (
                <section key={section.sectionId} className="px-6 py-16 bg-charcoal-950 text-white">
                  <div className="max-w-3xl mx-auto text-center space-y-4">
                    <span className="text-xs font-semibold uppercase tracking-widest text-bronze-400 block">
                      Philosophy
                    </span>
                    <blockquote className={`${headingClass} text-xl sm:text-2xl font-light italic leading-relaxed text-sand-100`}>
                      &ldquo;{designPhilosophy || section.content?.text || 'True design is not about decoration, but harmony between human life, natural light, and quiet materials.'}&rdquo;
                    </blockquote>
                    <span className="text-xs text-sand-400 block">— {studioName}</span>
                  </div>
                </section>
              );

            case 'PROCESS':
              return (
                <section key={section.sectionId} className="px-6 py-16 sm:py-20 max-w-4xl mx-auto">
                  <div className="text-center max-w-md mx-auto mb-10">
                    <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
                      Methodology
                    </span>
                    <h2 className={`${headingClass} text-2xl sm:text-3xl text-charcoal-900 font-normal`}>
                      Our Design Process
                    </h2>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                    {[
                      { step: '01', title: 'Consultation', desc: 'Understanding your spatial vision and lifestyle needs.' },
                      { step: '02', title: 'Concept & Plan', desc: 'Layout drafting, mood boards, and aesthetic direction.' },
                      { step: '03', title: 'Material Selection', desc: 'Curating fine finishes, bespoke joinery, and lighting.' },
                      { step: '04', title: 'Execution', desc: 'Overseeing turnkey delivery and final styling touches.' },
                    ].map((st) => (
                      <div key={st.step} className="p-5 rounded-2xl bg-sand-50 border border-sand-200 space-y-2">
                        <span className="text-xs font-mono font-bold text-bronze-700">{st.step}</span>
                        <h4 className="text-xs font-semibold text-charcoal-900">{st.title}</h4>
                        <p className="text-[11px] text-charcoal-500 leading-relaxed">{st.desc}</p>
                      </div>
                    ))}
                  </div>
                </section>
              );

            case 'CONTACT_FORM':
              return (
                <section key={section.sectionId} id="contact" className="px-6 py-16 sm:py-20 bg-sand-50/50">
                  <div className="max-w-4xl mx-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                      <div>
                        <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
                          Inquiries
                        </span>
                        <h2 className={`${headingClass} text-2xl sm:text-3xl text-charcoal-900 font-normal mb-4`}>
                          Let&apos;s Create Something Beautiful
                        </h2>
                        <p className="text-xs sm:text-sm text-charcoal-600 leading-relaxed mb-6">
                          We take on a limited number of commissions each year to ensure exceptional craftsmanship and attention to detail.
                        </p>

                        <div className="space-y-3">
                          {publicContacts.map((contact, idx) => (
                            <div key={idx} className="flex items-center gap-3 text-xs text-charcoal-700">
                              {contact.kind === 'EMAIL' && <Mail className="w-4 h-4 text-bronze-700" />}
                              {contact.kind === 'PHONE' && <Phone className="w-4 h-4 text-bronze-700" />}
                              {contact.kind === 'WHATSAPP' && <Phone className="w-4 h-4 text-emerald-600" />}
                              {contact.kind === 'WEBSITE' && <Globe className="w-4 h-4 text-bronze-700" />}
                              {contact.kind === 'INSTAGRAM' && <Camera className="w-4 h-4 text-pink-600" />}
                              <span>{contact.contactValue}</span>
                            </div>
                          ))}
                          {canonicalServiceAreas.length > 0 && (
                            <div className="pt-2 text-xs text-charcoal-500">
                              <span className="font-medium text-charcoal-700">Serving: </span>
                              {canonicalServiceAreas.map((sa) => sa.locality || sa.cityName).join(', ')}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="bg-white p-6 rounded-2xl border border-sand-200 shadow-sm space-y-4">
                        <h3 className="text-sm font-semibold text-charcoal-900">Send an Inquiry</h3>
                        <div className="space-y-3">
                          <div>
                            <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Your Name</label>
                            <input
                              type="text"
                              disabled
                              placeholder="Jane Doe"
                              className="w-full px-3 py-2 text-xs rounded-lg border border-sand-200 bg-sand-50 text-charcoal-400 cursor-not-allowed"
                            />
                          </div>
                          <div>
                            <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Email or Phone</label>
                            <input
                              type="text"
                              disabled
                              placeholder="jane@example.com"
                              className="w-full px-3 py-2 text-xs rounded-lg border border-sand-200 bg-sand-50 text-charcoal-400 cursor-not-allowed"
                            />
                          </div>
                          <div>
                            <label className="text-[11px] font-medium text-charcoal-600 block mb-1">Project Scope</label>
                            <textarea
                              rows={3}
                              disabled
                              placeholder="Tell us about your space, timeline, and location..."
                              className="w-full px-3 py-2 text-xs rounded-lg border border-sand-200 bg-sand-50 text-charcoal-400 cursor-not-allowed"
                            />
                          </div>
                          <button
                            type="button"
                            disabled
                            className="w-full py-2.5 rounded-lg bg-charcoal-900 text-white text-xs font-semibold uppercase tracking-wider opacity-60 cursor-not-allowed"
                          >
                            Inquiry Form (Active upon Live Launch)
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
              );

            case 'FOOTER':
              return (
                <footer key={section.sectionId} className="px-6 py-10 bg-white border-t border-sand-200 text-center text-xs text-charcoal-500 space-y-2">
                  <p className="font-semibold text-charcoal-800">{studioName}</p>
                  <p>© {new Date().getFullYear()} {studioName}. All rights reserved.</p>
                  <p className="text-[10px] text-charcoal-400">
                    Curated on the Platform • {studioSlug}.domain.placeholder
                  </p>
                </footer>
              );

            default:
              return (
                <section key={section.sectionId} className="px-6 py-12 max-w-4xl mx-auto">
                  <h3 className={`${headingClass} text-xl text-charcoal-900 mb-2`}>
                    {section.content?.title || section.sectionType.replace('_', ' ')}
                  </h3>
                  <p className="text-xs text-charcoal-600">
                    {section.content?.subtitle || section.content?.text || 'Section content'}
                  </p>
                </section>
              );
          }
        })}
      </main>
    </div>
  );
};
