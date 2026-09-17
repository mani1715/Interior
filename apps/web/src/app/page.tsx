import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="container" style={{ paddingTop: '40px', paddingBottom: '40px' }}>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '1.75rem', marginBottom: '8px', color: 'var(--color-text-primary)' }}>
          Interior Platform — Foundation
        </h1>
        <p style={{ color: 'var(--color-text-muted)' }}>
          Phase 03 Security &amp; Monorepo Shell Established
        </p>
      </header>

      <section
        style={{
          backgroundColor: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: '8px',
          padding: '24px',
          marginBottom: '24px',
        }}
      >
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>System Status</h2>
        <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <li>✓ Security Headers Enforced</li>
          <li>✓ Mobile-First Tokens (360px–430px Base)</li>
          <li>✓ Locked Palette System Active</li>
          <li>✓ OpenAPI Backend Integration Ready</li>
        </ul>
      </section>

      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
        <Link href="/health" className="btn btn-primary">
          View Health Diagnostic
        </Link>
      </div>
    </main>
  );
}
