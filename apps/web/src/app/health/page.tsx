export const dynamic = 'force-dynamic';

export default function HealthPage() {
  return (
    <div className="container" style={{ padding: '32px 16px' }}>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '16px' }}>Frontend Health Status</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid var(--color-border)' }}>
        <tbody>
          <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
            <td style={{ padding: '12px', fontWeight: 'bold' }}>Status</td>
            <td style={{ padding: '12px', color: 'var(--color-success)' }}>UP</td>
          </tr>
          <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
            <td style={{ padding: '12px', fontWeight: 'bold' }}>Phase</td>
            <td style={{ padding: '12px' }}>03 — Security Foundation</td>
          </tr>
          <tr>
            <td style={{ padding: '12px', fontWeight: 'bold' }}>Timestamp</td>
            <td style={{ padding: '12px' }}>{new Date().toISOString()}</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
