import Link from 'next/link';
import styles from './EditorialHome.module.css';
export function Footer() {
  return <footer className={styles.footer}>
    <div className={styles.footerTop}><div><Link href="/" className={styles.wordmark}>elégance</Link><p>Thoughtful spaces.<br />The people who bring them to life.</p></div>
      <nav aria-label="Footer discovery"><span>DISCOVER</span><Link href="/projects">Interior projects</Link><Link href="/interior-journey">The interior journey</Link><Link href="/professionals">Professionals & studios</Link><Link href="/categories/modular-kitchens">Modular kitchens</Link><Link href="/categories/wardrobes">Wardrobes & storage</Link></nav>
      <nav aria-label="Footer professionals"><span>FOR PROFESSIONALS</span><Link href="/onboarding/professional">Build your portfolio</Link><Link href="/workspace">Your workspace</Link><Link href="/#ai-visualizer">AI visualization</Link><Link href="/account/collections">Your collections</Link></nav>
      <nav aria-label="Footer trust and support"><span>TRUST & SUPPORT</span><Link href="/help">Help center</Link><Link href="/feedback">Send feedback</Link><Link href="/privacy">Privacy policy</Link><Link href="/terms">Terms of service</Link></nav>
    </div><div className={styles.footerBottom}><span>© {new Date().getFullYear()} Elégance Interior Platform</span><span>Considered design. Across India.</span></div>
  </footer>;
}
