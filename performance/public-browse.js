/**
 * Elégance — Interior Designer Platform
 * Phase 12H / 12H.1: Public Portfolio Browsing Load Test
 *
 * Simulates high-concurrency unauthenticated public visitors exploring:
 * - Public discovery endpoints (projects, search, professionals)
 * - Public reviews
 * - Minimal public health probe
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');

async function runPublicBrowse(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080/api/v1';
  const concurrency = parseInt(options.concurrency || '10', 10);
  const durationSec = parseInt(options.duration || '10', 10);
  const studioSlug = options.slug || 'dev-studio-atelier';

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Public Portfolio Browsing Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(` Target Studio  : ${studioSlug}`);
  console.log(`============================================================\n`);

  const endpoints = [
    { name: 'Public Health', path: '/health' },
    { name: 'Discovery Projects', path: '/public/discovery/projects?limit=10&offset=0' },
    { name: 'Discovery Search', path: '/public/discovery/search?q=living&limit=10' },
    { name: 'Discovery Professionals', path: '/public/discovery/professionals?limit=10&offset=0' },
    { name: 'Studio Reviews', path: `/public/studios/${studioSlug}/reviews` }
  ];

  const results = {
    totalRequests: 0,
    successes: 0,
    errors: 0,
    statusCodes: {},
    latencies: []
  };

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);

  async function worker(workerId) {
    let epIndex = workerId % endpoints.length;

    while (Date.now() < stopTime) {
      const ep = endpoints[epIndex % endpoints.length];
      epIndex++;

      const reqStart = performance.now();
      try {
        const response = await fetch(`${baseUrl}${ep.path}`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'User-Agent': `Elegance-LoadTest-PublicWorker/${workerId}`
          }
        });
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;

        const status = response.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (response.ok) {
          results.successes++;
        } else {
          results.errors++;
        }
      } catch (err) {
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        results.errors++;
        results.statusCodes['ERR'] = (results.statusCodes['ERR'] || 0) + 1;
      }
    }
  }

  const fs = require('fs');
  const path = require('path');

  async function fetchMetric(metric) {
    try {
      const res = await fetch(`${baseUrl}/actuator/metrics/${metric}`);
      if (res.ok) {
        const d = await res.json();
        return d.measurements && d.measurements[0] ? d.measurements[0].value : null;
      }
    } catch (_) {}
    return null;
  }

  const preHeap = await fetchMetric('jvm.memory.used');
  const preActiveConns = await fetchMetric('hikaricp.connections.active');

  let peakActiveConns = 0;
  const pollTimer = setInterval(async () => {
    const act = await fetchMetric('hikaricp.connections.active');
    if (act !== null && act > peakActiveConns) peakActiveConns = act;
  }, 1000);

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);
  clearInterval(pollTimer);

  const postHeap = await fetchMetric('jvm.memory.used');
  const postActiveConns = await fetchMetric('hikaricp.connections.active');
  const cpuUsage = await fetchMetric('process.cpu.usage');

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const stats = calculatePercentiles(results.latencies);

  const summary = {
    scenario: 'Public Portfolio Browsing (100 VU Burst/Sustained)',
    concurrency,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    successes: results.successes,
    errors: results.errors,
    errorRatePct: results.totalRequests > 0 ? Math.round((results.errors / results.totalRequests) * 10000) / 100 : 0,
    throughputRps: throughput,
    p50Ms: Math.round(stats.p50 * 100) / 100,
    p90Ms: Math.round(stats.p90 * 100) / 100,
    p95Ms: Math.round(stats.p95 * 100) / 100,
    p99Ms: Math.round(stats.p99 * 100) / 100,
    heapUsedBeforeMb: preHeap ? Math.round((preHeap / (1024 * 1024)) * 10) / 10 : null,
    heapUsedAfterMb: postHeap ? Math.round((postHeap / (1024 * 1024)) * 10) / 10 : null,
    peakHikariActive: peakActiveConns,
    postHikariActive: postActiveConns,
    cpuUsagePct: cpuUsage !== null ? Math.round(cpuUsage * 1000) / 10 : null,
    statusCodes: results.statusCodes
  };

  const resultsDir = path.join(__dirname, 'results');
  if (!fs.existsSync(resultsDir)) fs.mkdirSync(resultsDir, { recursive: true });
  fs.writeFileSync(path.join(resultsDir, 'public_100vu_results.json'), JSON.stringify(summary, null, 2), 'utf8');

  console.log(`Results:`);
  console.log(`- Total Requests : ${summary.totalRequests}`);
  console.log(`- Throughput     : ${summary.throughputRps} req/s`);
  console.log(`- Error Rate     : ${summary.errorRatePct}% (${summary.errors} errors)`);
  console.log(`- Latency p50    : ${summary.p50Ms} ms`);
  console.log(`- Latency p95    : ${summary.p95Ms} ms`);
  console.log(`- Latency p99    : ${summary.p99Ms} ms`);
  console.log(`- Peak Active DB : ${summary.peakHikariActive}`);
  console.log(`- Status Codes   : ${JSON.stringify(summary.statusCodes)}`);

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
  runPublicBrowse(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runPublicBrowse };
