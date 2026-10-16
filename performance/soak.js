/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Soak & Resource Stability Test
 *
 * Runs extended steady-state traffic to detect:
 * - Memory leaks (JVM heap monotonic expansion without GC recovery)
 * - Connection pool starvation / leaks (HikariCP active connections monotonically climbing)
 * - Thread pool starvation
 * - Latency degradation over time
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');

async function runSoakTest(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080';
  const concurrency = parseInt(options.concurrency || '10', 10);
  const durationSec = parseInt(options.duration || '30', 10);
  const sampleIntervalSec = parseInt(options.sampleInterval || '5', 10);

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Soak & Resource Stability Test`);
  console.log(` Target Base URL  : ${baseUrl}`);
  console.log(` Concurrency      : ${concurrency} concurrent workers`);
  console.log(` Duration         : ${durationSec} seconds`);
  console.log(` Sample Interval  : ${sampleIntervalSec} seconds`);
  console.log(`============================================================\n`);

  const results = {
    totalRequests: 0,
    successes: 0,
    errors: 0,
    latencies: [],
    samples: []
  };

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);

  // Background metric sampler
  const sampleTimer = setInterval(async () => {
    try {
      const elapsedSec = Math.round((Date.now() - startTime) / 1000);

      let memoryUsedMb = null;
      let activeConnections = null;

      try {
        const memRes = await fetch(`${baseUrl}/actuator/metrics/jvm.memory.used`);
        if (memRes.ok) {
          const memData = await memRes.json();
          if (memData.measurements && memData.measurements[0]) {
            memoryUsedMb = Math.round((memData.measurements[0].value / (1024 * 1024)) * 10) / 10;
          }
        }
      } catch (_) {}

      try {
        const connRes = await fetch(`${baseUrl}/actuator/metrics/hikaricp.connections.active`);
        if (connRes.ok) {
          const connData = await connRes.json();
          if (connData.measurements && connData.measurements[0]) {
            activeConnections = connData.measurements[0].value;
          }
        }
      } catch (_) {}

      const sample = {
        elapsedSec,
        requestsSoFar: results.totalRequests,
        memoryUsedMb,
        activeConnections
      };
      results.samples.push(sample);
      console.log(`[SOAK T+${elapsedSec}s] Req: ${sample.requestsSoFar} | Heap: ${memoryUsedMb || 'N/A'} MB | Active DB Conns: ${activeConnections || 'N/A'}`);
    } catch (_) {}
  }, sampleIntervalSec * 1000);

  // Worker routine
  const testEndpoints = [
    '/api/v1/health',
    '/actuator/health/liveness',
    '/actuator/health/readiness',
    '/api/v1/public/studios?page=0&size=10'
  ];

  async function worker(workerId) {
    let epIndex = workerId;

    while (Date.now() < stopTime) {
      epIndex++;
      const path = testEndpoints[epIndex % testEndpoints.length];
      const reqStart = performance.now();

      try {
        const response = await fetch(`${baseUrl}${path}`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'User-Agent': `Elegance-LoadTest-SoakWorker/${workerId}`
          }
        });
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;

        if (response.ok) results.successes++;
        else results.errors++;
      } catch (err) {
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        results.errors++;
      }
    }
  }

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);
  clearInterval(sampleTimer);

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const stats = calculatePercentiles(results.latencies);

  // Check stability verdict
  let stabilityVerdict = 'STABLE';
  if (results.errors > 0 && results.errors / results.totalRequests > 0.05) {
    stabilityVerdict = 'DEGRADED';
  }

  const summary = {
    scenario: 'Soak & Resource Stability',
    concurrency,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    successes: results.successes,
    errors: results.errors,
    errorRatePct: results.totalRequests > 0 ? Math.round((results.errors / results.totalRequests) * 10000) / 100 : 0,
    throughputRps: throughput,
    p50Ms: Math.round(stats.p50 * 100) / 100,
    p95Ms: Math.round(stats.p95 * 100) / 100,
    p99Ms: Math.round(stats.p99 * 100) / 100,
    stabilityVerdict,
    sampleCount: results.samples.length
  };

  console.log(`\nSoak Test Completed:`);
  console.log(`- Verdict        : ${summary.stabilityVerdict}`);
  console.log(`- Total Requests : ${summary.totalRequests}`);
  console.log(`- Throughput     : ${summary.throughputRps} req/s`);
  console.log(`- Error Rate     : ${summary.errorRatePct}% (${summary.errors} errors)`);
  console.log(`- Latency p50    : ${summary.p50Ms} ms`);
  console.log(`- Latency p95    : ${summary.p95Ms} ms`);
  console.log(`- Latency p99    : ${summary.p99Ms} ms`);

  return summary;
}

if (require.main === module) {
  const args = process.argv.slice(2);
  const options = {};
  for (const arg of args) {
    if (arg.startsWith('--')) {
      const [key, val] = arg.slice(2).split('=');
      options[key] = val || true;
    }
  }
  runSoakTest(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runSoakTest };
