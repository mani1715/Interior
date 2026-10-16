/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Performance & Concurrency Safety Verification
 *
 * Strictly blocks load tests from executing against non-localhost targets
 * unless process.env.ALLOW_REMOTE_LOAD_TEST === 'true'.
 */

function assertTargetSafety(targetUrl) {
  try {
    const parsed = new URL(targetUrl);
    const hostname = parsed.hostname;
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1' || hostname === '0.0.0.0';
    if (!isLocal && process.env.ALLOW_REMOTE_LOAD_TEST !== 'true') {
      console.error('\n' + '='.repeat(70));
      console.error(' [FATAL] REMOTE LOAD TESTING STRICTLY BLOCKED');
      console.error('='.repeat(70));
      console.error(` Target Host: ${hostname}`);
      console.error(' In Phase 12H, load testing is strictly limited to local environments.');
      console.error(' To run against a remote/staging host intentionally, you must set:');
      console.error('   ALLOW_REMOTE_LOAD_TEST=true');
      console.error('='.repeat(70) + '\n');
      process.exit(1);
    }
  } catch (err) {
    console.error(`[FATAL] Invalid target URL: ${targetUrl} (${err.message})`);
    process.exit(1);
  }
}

function calculatePercentiles(latencies) {
  if (!latencies || latencies.length === 0) {
    return { count: 0, min: 0, max: 0, mean: 0, p50: 0, p90: 0, p95: 0, p99: 0 };
  }
  const sorted = [...latencies].sort((a, b) => a - b);
  const count = sorted.length;
  const sum = sorted.reduce((acc, val) => acc + val, 0);
  const mean = sum / count;

  const getP = (p) => {
    const idx = Math.min(Math.floor((p / 100) * count), count - 1);
    return sorted[idx];
  };

  return {
    count,
    min: sorted[0],
    max: sorted[count - 1],
    mean: Math.round(mean * 100) / 100,
    p50: getP(50),
    p90: getP(90),
    p95: getP(95),
    p99: getP(99)
  };
}

module.exports = {
  assertTargetSafety,
  calculatePercentiles
};
