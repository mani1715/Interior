/**
 * Elégance — Interior Designer Platform
 * Phase 12H / 12H.1 / 12H.2: Public Portfolio Browsing Load Test
 *
 * Simulates high-concurrency unauthenticated public visitors exploring:
 * - Public discovery endpoints (projects, search, professionals)
 * - Public reviews
 * - Minimal public health probe
 *
 * Detailed Classification:
 * - Categorizes responses into 2xx, 4xx (400, 401, 403, 404, 409, 429), 5xx (500, 502, 503), timeouts, and socket errors
 * - Tracks endpoint-level latency percentiles (p50, p90, p95, p99, max)
 * - Smooth worker ramp-up to eliminate artificial TCP connection shock waves
 */

const fs = require('fs');
const path = require('path');
const { assertTargetSafety, calculatePercentiles } = require('./safety');

async function runPublicBrowse(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080/api/v1';
  const concurrency = parseInt(options.concurrency || '10', 10);
  const durationSec = parseInt(options.duration || '60', 10);
  const studioSlug = options.slug || 'dev-studio-atelier';

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Public Portfolio Browsing Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(` Target Studio  : ${studioSlug}`);
  console.log(` Ramp-Up        : Smooth stagger (5ms interval up to 500ms)`);
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
    successes2xx: 0,
    clientErrors4xx: {
      total: 0,
      '400': 0,
      '401': 0,
      '403': 0,
      '404': 0,
      '409': 0,
      '429': 0,
      other4xx: 0
    },
    serverErrors5xx: {
      total: 0,
      '500': 0,
      '502': 0,
      '503': 0,
      other5xx: 0
    },
    timeouts: 0,
    connectionErrors: 0,
    statusCodes: {},
    latencies: [],
    routeMetrics: {}
  };

  endpoints.forEach(ep => {
    results.routeMetrics[ep.path] = {
      name: ep.name,
      count: 0,
      '2xx': 0,
      '4xx': 0,
      '5xx': 0,
      timeouts: 0,
      connectionErrors: 0,
      latencies: []
    };
  });

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

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);

  async function worker(workerId) {
    let epIndex = workerId % endpoints.length;

    while (Date.now() < stopTime) {
      const ep = endpoints[epIndex % endpoints.length];
      epIndex++;
      const routeData = results.routeMetrics[ep.path];

      const reqStart = performance.now();
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 25000);

        const response = await fetch(`${baseUrl}${ep.path}`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'User-Agent': `Elegance-LoadTest-PublicWorker/${workerId}`
          },
          signal: controller.signal
        });
        clearTimeout(timeoutId);

        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        routeData.count++;
        routeData.latencies.push(reqDuration);

        const status = response.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (status >= 200 && status < 300) {
          results.successes2xx++;
          routeData['2xx']++;
        } else if (status >= 400 && status < 500) {
          results.clientErrors4xx.total++;
          routeData['4xx']++;
          if (results.clientErrors4xx[status] !== undefined) {
            results.clientErrors4xx[status]++;
          } else {
            results.clientErrors4xx.other4xx++;
          }
        } else if (status >= 500 && status < 600) {
          results.serverErrors5xx.total++;
          routeData['5xx']++;
          if (results.serverErrors5xx[status] !== undefined) {
            results.serverErrors5xx[status]++;
          } else {
            results.serverErrors5xx.other5xx++;
          }
        }
      } catch (err) {
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        routeData.count++;
        routeData.latencies.push(reqDuration);

        if (err.name === 'AbortError') {
          results.timeouts++;
          routeData.timeouts++;
          results.statusCodes['TIMEOUT'] = (results.statusCodes['TIMEOUT'] || 0) + 1;
        } else {
          results.connectionErrors++;
          routeData.connectionErrors++;
          results.statusCodes['CONN_ERR'] = (results.statusCodes['CONN_ERR'] || 0) + 1;
        }
      }
    }
  }

  let peakActiveConns = 0;
  const pollTimer = setInterval(async () => {
    const act = await fetchMetric('hikaricp.connections.active');
    if (act !== null && act > peakActiveConns) peakActiveConns = act;
  }, 1000);

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push((async () => {
      if (i > 0) {
        await new Promise(r => setTimeout(r, Math.min(i * 5, 500)));
      }
      return worker(i + 1);
    })());
  }

  await Promise.all(workers);
  clearInterval(pollTimer);

  const postHeap = await fetchMetric('jvm.memory.used');
  const postActiveConns = await fetchMetric('hikaricp.connections.active');
  const cpuUsage = await fetchMetric('process.cpu.usage');

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const stats = calculatePercentiles(results.latencies);

  // Calculate per-route percentiles
  const routeStats = {};
  for (const [routePath, rData] of Object.entries(results.routeMetrics)) {
    const rPerc = calculatePercentiles(rData.latencies);
    const maxLat = rData.latencies.length > 0 ? Math.max(...rData.latencies) : 0;
    routeStats[routePath] = {
      name: rData.name,
      count: rData.count,
      '2xx': rData['2xx'],
      '4xx': rData['4xx'],
      '5xx': rData['5xx'],
      timeouts: rData.timeouts,
      connectionErrors: rData.connectionErrors,
      p50Ms: Math.round(rPerc.p50 * 100) / 100,
      p90Ms: Math.round(rPerc.p90 * 100) / 100,
      p95Ms: Math.round(rPerc.p95 * 100) / 100,
      p99Ms: Math.round(rPerc.p99 * 100) / 100,
      maxMs: Math.round(maxLat * 100) / 100
    };
  }

  const overallMax = results.latencies.length > 0 ? Math.max(...results.latencies) : 0;

  const summary = {
    scenario: 'Public Portfolio Browsing (100 VU Burst/Sustained)',
    concurrency,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    successes2xx: results.successes2xx,
    clientErrors4xx: results.clientErrors4xx,
    serverErrors5xx: results.serverErrors5xx,
    timeouts: results.timeouts,
    connectionErrors: results.connectionErrors,
    errorRatePct: results.totalRequests > 0 ? Math.round(((results.serverErrors5xx.total + results.timeouts + results.connectionErrors) / results.totalRequests) * 10000) / 100 : 0,
    throughputRps: throughput,
    p50Ms: Math.round(stats.p50 * 100) / 100,
    p90Ms: Math.round(stats.p90 * 100) / 100,
    p95Ms: Math.round(stats.p95 * 100) / 100,
    p99Ms: Math.round(stats.p99 * 100) / 100,
    maxMs: Math.round(overallMax * 100) / 100,
    heapUsedBeforeMb: preHeap ? Math.round((preHeap / (1024 * 1024)) * 10) / 10 : null,
    heapUsedAfterMb: postHeap ? Math.round((postHeap / (1024 * 1024)) * 10) / 10 : null,
    peakHikariActive: peakActiveConns,
    postHikariActive: postActiveConns,
    cpuUsagePct: cpuUsage !== null ? Math.round(cpuUsage * 1000) / 10 : null,
    statusCodes: results.statusCodes,
    routeLatency: routeStats
  };

  const resultsDir = path.join(__dirname, 'results');
  if (!fs.existsSync(resultsDir)) fs.mkdirSync(resultsDir, { recursive: true });
  fs.writeFileSync(path.join(resultsDir, 'public_100vu_results.json'), JSON.stringify(summary, null, 2), 'utf8');

  console.log(`Results:`);
  console.log(`- Total Requests   : ${summary.totalRequests}`);
  console.log(`- HTTP 2xx Success : ${summary.successes2xx} (${((summary.successes2xx / summary.totalRequests) * 100).toFixed(2)}%)`);
  console.log(`- HTTP 4xx Client  : ${summary.clientErrors4xx.total}`);
  console.log(`- HTTP 5xx Server  : ${summary.serverErrors5xx.total}`);
  console.log(`- Timeouts         : ${summary.timeouts}`);
  console.log(`- Connection Errs  : ${summary.connectionErrors}`);
  console.log(`- Throughput       : ${summary.throughputRps} req/s`);
  console.log(`- Latency p50      : ${summary.p50Ms} ms`);
  console.log(`- Latency p95      : ${summary.p95Ms} ms`);
  console.log(`- Latency p99      : ${summary.p99Ms} ms`);
  console.log(`- Latency Max      : ${summary.maxMs} ms`);
  console.log(`- Peak Active DB   : ${summary.peakHikariActive}`);
  console.log(`- Status Codes     : ${JSON.stringify(summary.statusCodes)}`);

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
