import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="container" style={{ padding: '40px 16px', textAlign: 'center' }}>
      <h2 style={{ fontSize: '2rem', marginBottom: '12px' }}>404 — Page Not Found</h2>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        The page you are looking for does not exist or has been moved.
      </p>
      <Link href="/" className="btn btn-primary">
        Return Home
      </Link>
    </div>
  );
}
