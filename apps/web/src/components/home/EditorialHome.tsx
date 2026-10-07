import Image from 'next/image';
import Link from 'next/link';
import { ArrowUpRight, Check, ShieldCheck, MessageSquare, Search } from 'lucide-react';
import { EditorialProjects } from './EditorialProjects';
import { EditorialProfessionals } from './EditorialProfessionals';
import styles from './EditorialHome.module.css';

const categories = [
  { name: 'Modular kitchens', slug: 'modular-kitchens', image: 'kitchen', note: 'The heart of the home' },
  { name: 'Wardrobes & storage', slug: 'wardrobes', image: 'wardrobe', note: 'Every inch, considered' },
  { name: 'TV & living units', slug: 'tv-units', image: 'tv-unit', note: 'Room to come together' },
];

export function EditorialHome() {
  return (
    <>
      {/* 3. Immediate Project Search */}
      <section id="immediate-search" className={styles.searchSection} aria-labelledby="search-section-title">
        <div className={styles.searchContainer}>
          <span className={styles.kicker}>01 / INSTANT DISCOVERY</span>
          <h2 id="search-section-title" className={styles.searchTitle}>What space are you imagining?</h2>
          <form method="get" action="/projects" role="search" className={styles.searchForm}>
            <label htmlFor="homepage-project-search" className={styles.searchLabel}>
              Search interior projects by room, style, or feature
            </label>
            <div className={styles.searchInputRow}>
              <div className={styles.searchInputWrapper}>
                <Search size={18} className={styles.searchIcon} aria-hidden="true" />
                <input
                  id="homepage-project-search"
                  name="q"
                  type="search"
                  placeholder='Try "modern kitchen", "minimal wardrobe", "Bangalore living room"'
                  className={styles.searchInput}
                />
              </div>
              <button type="submit" className={styles.searchButton}>
                Search <ArrowUpRight size={16} aria-hidden="true" />
              </button>
            </div>
          </form>
          <div className={styles.searchChips}>
            <span className={styles.chipHeading}>QUICK EXPLORE:</span>
            <Link href="/projects?category=modular-kitchens" className={styles.chip}>Modular Kitchens</Link>
            <Link href="/projects?category=living-room" className={styles.chip}>Living Rooms</Link>
            <Link href="/projects?category=bedroom" className={styles.chip}>Bedrooms</Link>
            <Link href="/projects?category=wardrobes" className={styles.chip}>Wardrobes</Link>
            <Link href="/projects?category=pooja-units" className={styles.chip}>Pooja Units</Link>
            <Link href="/projects?category=tv-units" className={styles.chip}>TV Units</Link>
          </div>
        </div>
      </section>

      {/* 4. Projects to Explore */}
      <EditorialProjects />

      {/* 5. Professional Discovery */}
      <EditorialProfessionals />

      {/* 6. Cinematic Portfolio Introduction */}
      <section id="cinematic-intro" className={styles.cinematicSection} aria-labelledby="cinematic-intro-title">
        <div className={styles.headingRow}>
          <div>
            <span className={styles.kicker}>04 / PROGRESSIVE ENHANCEMENT</span>
            <h2 id="cinematic-intro-title">A closer sense<br /><em>of the space.</em></h2>
          </div>
          <div>
            <p>Selected projects unfold room by room with subtle, scroll-led movement. Open the gallery whenever you want to see every photograph.</p>
            <Link className={styles.textLink} href="/projects">
              Explore projects <ArrowUpRight size={18} />
            </Link>
          </div>
        </div>
        <div className={styles.cinematicPreview}>
          <div className={styles.cinematicCard} data-motion-image="right">
            <div className={styles.cinematicMedia}>
              <Image
                src="/images/approved/hall.png"
                alt="Living room in warm timber with architectural lighting — Cinematic Portfolio presentation"
                fill
                sizes="(max-width: 1023px) 100vw, 85vw"
              />
              <div className={styles.cinematicBadge}>
                <span>LIVING ROOM</span>
                <span className={styles.badgeDivider}>·</span>
                <span>CINEMATIC PORTFOLIO</span>
              </div>
              <div className={styles.cinematicCaption}>
                <span>Room by room story · Real project photography</span>
              </div>
            </div>
            <div className={styles.cinematicFooter}>
              <div>
                <h3>Scroll-led room progression</h3>
                <p>Eligible projects feature smooth photographic movement that lets you feel the flow between rooms.</p>
              </div>
              <Link href="/projects" className={styles.primaryLink}>
                Explore projects <ArrowUpRight size={18} />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 7. Inspiration / Categories */}
      <section id="explore-interiors" className={styles.section} aria-labelledby="explore-title">
        <div className={styles.headingRow}>
          <div>
            <span className={styles.kicker}>05 / FIND YOUR INSPIRATION</span>
            <h2 id="explore-title">Every space.<br /><em>Its own possibilities.</em></h2>
          </div>
          <div>
            <p>From the kitchen you gather in to the storage that simplifies your day. Start with the space you want to make your own.</p>
            <Link className={styles.textLink} href="/projects">
              Explore all spaces <ArrowUpRight size={18} />
            </Link>
          </div>
        </div>
        <div className={styles.categoryGrid}>
          {categories.map((category, index) => (
            <Link key={category.slug} href={`/categories/${category.slug}`} className={styles.categoryCard}>
              <div data-motion-image={index % 2 === 0 ? 'left' : 'right'} className={styles.categoryImage}>
                <Image
                  src={`/images/approved/${category.image}.png`}
                  alt={`${category.name} — editorial inspiration`}
                  fill
                  sizes="(max-width: 767px) 90vw, 30vw"
                />
              </div>
              <span className={styles.categoryMeta}>0{index + 1} / {category.note}</span>
              <h3>{category.name}<ArrowUpRight size={21} /></h3>
            </Link>
          ))}
        </div>
        <div className={styles.categoryMore}>
          <span>ALSO EXPLORE</span>
          {[
            ['Modular cupboards', 'wardrobes'],
            ['Pooja units', 'pooja-units'],
            ['Living media walls', 'tv-units'],
            ['Custom furniture', 'custom-furniture'],
            ['Bedrooms & suites', 'bedroom'],
            ['Commercial interiors', 'commercial'],
          ].map(([name, slug]) => (
            <Link key={name} href={`/categories/${slug}`}>{name}<ArrowUpRight size={14} /></Link>
          ))}
        </div>
        <p className={styles.credit}>AI Concept Visualization — design inspiration, not completed platform projects.</p>
      </section>

      {/* 8. AI Visualizer */}
      <section id="ai-visualizer" className={`${styles.section} ${styles.aiSection}`} aria-labelledby="ai-title">
        <div>
          <span className={styles.kicker}>06 / SUPPORTING EXPLORATION</span>
          <h2 id="ai-title">Explore an idea.<br /><em>Before the next step.</em></h2>
          <p>Explore materials, finishes and layout ideas with your professional before making decisions on site.</p>
          <div className={styles.aiCtaGroup}>
            <Link className={styles.textLink} href="/workspace/ai">
              Explore the AI workspace <ArrowUpRight size={18} />
            </Link>
            <span className={styles.aiSignInNote}>Sign-in required to generate concepts</span>
          </div>
          <p className={styles.disclaimer}>
            AI Concept Visualization — concepts are creative interpretations, not completed projects or construction specifications. Colors, materials and geometry may vary.
          </p>
        </div>
        <div className={styles.aiProcess} aria-label="AI visualization workflow">
          <span>YOUR SPACE → YOUR POSSIBILITIES</span>
          <ol>
            <li>
              <span>01</span>
              <div>
                <h3>Start with your room.</h3>
                <p>Use a photograph of your space.</p>
              </div>
            </li>
            <li>
              <span>02</span>
              <div>
                <h3>Explore a direction.</h3>
                <p>Guide materials and finishes with your references.</p>
              </div>
            </li>
            <li>
              <span>03</span>
              <div>
                <h3>Discuss the concept.</h3>
                <p>Review ideas together. Verify details before building.</p>
              </div>
            </li>
          </ol>
        </div>
      </section>

      {/* 9. Trust Explanations */}
      <section id="trust-explanations" className={styles.trust} aria-labelledby="trust-title">
        <span className={styles.kicker}>07 / CONFIDENCE, WITH CONTEXT</span>
        <h2 id="trust-title">Make a more informed choice.</h2>
        <div>
          {[
            {
              icon: Check,
              title: 'Work with attribution',
              copy: 'Explore published project stories and the professionals credited for them.',
            },
            {
              icon: MessageSquare,
              title: 'Reviews with context',
              copy: 'Invited client reviews are separate from studio-authored testimonials.',
            },
            {
              icon: ShieldCheck,
              title: 'Verification, explained',
              copy: 'Verified Business means evidence has been reviewed. It is not a quality guarantee.',
            },
          ].map(({ icon: Icon, title, copy }) => (
            <article key={title}>
              <Icon size={22} strokeWidth={1.3} />
              <h3>{title}</h3>
              <p>{copy}</p>
            </article>
          ))}
        </div>
      </section>

      {/* 10. Visitor + Professional Calls to Action */}
      <section id="portfolio-builder" className={styles.dualCtaSection} aria-label="Explore or join Elégance">
        <div className={styles.dualCtaContainer}>
          <div className={styles.ctaCard}>
            <span className={styles.kicker}>FOR VISITORS & HOMEOWNERS</span>
            <h2>Start with work<br /><em>you love.</em></h2>
            <p>Explore real spaces, find ideas, and discover the studios that built them.</p>
            <div className={styles.ctaButtonGroup}>
              <Link href="/projects" className={styles.primaryLink}>
                Explore projects <ArrowUpRight size={18} />
              </Link>
              <Link href="/professionals" className={styles.secondaryButton}>
                Find a professional <ArrowUpRight size={18} />
              </Link>
            </div>
          </div>
          <div className={`${styles.ctaCard} ${styles.professionalCtaCard}`}>
            <span className={styles.kicker}>FOR INTERIOR PROFESSIONALS</span>
            <h2>Let your work<br /><em>introduce you.</em></h2>
            <p>Create room-by-room project stories and connect with clients looking for your style.</p>
            <div className={styles.ctaButtonGroup}>
              <Link href="/onboarding/professional" className={styles.primaryLink}>
                Create your professional profile <ArrowUpRight size={18} />
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
