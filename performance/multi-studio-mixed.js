/**
 * Elégance — Interior Designer Platform
 * Phase 12H.2: Representative Multi-Studio Mixed-Workload Benchmark
 *
 * Runs a 5-minute (300 seconds), 20-VU mixed workload:
 * - 70% public reads
 * - 20% authenticated reads
 * - 10% authenticated project creations
 * Spreads mutations across 10 distinct synthetic studios so that workers
 * do NOT artificially contend on a single studio quota row lock.
 *
 * Measures route-level latency distributions (p50, p90, p95, p99, max)
 * to distinguish Contention Stress from Representative Multi-Studio Load.
 */

const fs = require('fs');
const path = require('path');
const { assertTargetSafety, calculatePercentiles } = require('./safety');
const { authenticatePersona } = require('./authenticated-read');
const { seedSyntheticStudios } = require('./seed-synthetic-studios');

async function runMultiStudioMixed(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080/api/v1';
  const concurrency = parseInt(options.concurrency || '20', 10);
  const durationSec = parseInt(options.duration || '300', 10); // 5 minutes default

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Phase 12H.2 Representative Multi-Studio Mixed Test`);
  console.log(` Target Base URL  : ${baseUrl}`);
  console.log(` Concurrency      : ${concurrency} concurrent workers`);
  console.log(` Duration         : ${durationSec} seconds (${(durationSec / 60).toFixed(1)} minutes)`);
  console.log(` Studios          : Multi-studio distribution (10 synthetic studios)`);
  console.log(` Workload Mix     : 70% Public Reads / 20% Auth Reads / 10% Writes`);
  console.log(` Wallclock Start  : ${new Date().toISOString()}`);
  console.log(`============================================================\n`);

  // Ensure studios are seeded
  const studios = seedSyntheticStudios();
  if (studios.length === 0) {
    throw new Error('Failed to load synthetic studios');
  }

  const { cookie, csrfToken } = await authenticatePersona(baseUrl);

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

  const publicEndpoints = [
    { name: 'Public Health', path: '/health' },
    { name: 'Discovery Projects', path: '/public/discovery/projects' },
    { name: 'Discovery Search', path: '/public/discovery/search' },
    { name: 'Discovery Professionals', path: '/public/discovery/professionals' }
  ];

  const authReadEndpoints = [
    { name: 'Auth Identity', path: '/auth/me' },
    { name: 'Studio Projects', path: '/projects?page=0&size=10' },
    { name: 'Workspace Team', path: '/workspace/team' },
    { name: 'Studio Analytics', path: '/studio/analytics' }
  ];

  const results = {
    totalRequests: 0,
    readRequests: 0,
    writeRequests: 0,
    successes2xx: 0,
    clientErrors4xx: { total: 0, '400': 0, '401': 0, '403': 0, '404': 0, '409': 0, '429': 0, other: 0 },
    serverErrors5xx: { total: 0, '500': 0, '502': 0, '503': 0, other: 0 },
    timeouts: 0,
    connectionErrors: 0,
    statusCodes: {},
    latencies: [],
    routeMetrics: {}
  };

  function registerRoute(path, name) {
    if (!results.routeMetrics[path]) {
      results.routeMetrics[path] = {
        name,
        count: 0,
        '2xx': 0,
        '4xx': 0,
        '5xx': 0,
        timeouts: 0,
        connectionErrors: 0,
        latencies: []
      };
    }
    return results.routeMetrics[path];
  }

  publicEndpoints.forEach(ep => registerRoute(ep.path, ep.name));
  authReadEndpoints.forEach(ep => registerRoute(ep.path, ep.name));
  registerRoute('/projects', 'Create Project Mutation');

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);
  let writeCounter = 1;

  async function worker(workerId) {
    const workerStudio = studios[(workerId - 1) % studios.length];
    let reqIndex = workerId;

    while (Date.now() < stopTime) {
      reqIndex++;
      const roll = (reqIndex * 17 + workerId) % 100;
      let path, method = 'GET', body = null;
      let isWrite = false;
      let routePath = null;
      let routeName = null;

      const headers = {
        'Accept': 'application/json',
        'User-Agent': `Elegance-LoadTest-MultiStudioWorker/${workerId}`
      };

      if (roll < 70) {
        // Public Read
        const ep = publicEndpoints[reqIndex % publicEndpoints.length];
        path = ep.path;
        routePath = ep.path;
        routeName = ep.name;
      } else if (roll < 90) {
        // Authenticated Read (scoped to worker's assigned synthetic studio)
        const ep = authReadEndpoints[reqIndex % authReadEndpoints.length];
        path = ep.path;
        routePath = ep.path;
        routeName = ep.name;
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;
        headers['X-Studio-Id'] = workerStudio.id;
      } else {
        // Authenticated Write (Project creation targeting worker's distinct studio)
        isWrite = true;
        method = 'POST';
        path = '/projects';
        routePath = '/projects';
        routeName = 'Create Project Mutation';
        headers['Content-Type'] = 'application/json';
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;
        headers['X-Studio-Id'] = workerStudio.id;
        const wId = writeCounter++;
        body = JSON.stringify({
          title: `Multi-Studio Proj W${workerId}-${wId}`,
          categoryCode: 'COMPLETE_HOME_INTERIOR',
          shortDescription: `Representative load verification project ${wId}`
        });
      }

      const routeData = registerRoute(routePath, routeName);
      const reqStart = performance.now();
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 25000);

        const res = await fetch(`${baseUrl}${path}`, {
          method,
          headers,
          body,
          signal: controller.signal
        });
        clearTimeout(timeoutId);

        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        routeData.count++;
        routeData.latencies.push(reqDuration);

        if (isWrite) results.writeRequests++;
        else results.readRequests++;

        const status = res.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (status >= 200 && status < 300) {
          results.successes2xx++;
          routeData['2xx']++;
        } else if (status >= 400 && status < 500) {
          results.clientErrors4xx.total++;
          routeData['4xx']++;
          if (results.clientErrors4xx[status] !== undefined) results.clientErrors4xx[status]++;
          else results.clientErrors4xx.other++;
        } else if (status >= 500 && status < 600) {
          results.serverErrors5xx.total++;
          routeData['5xx']++;
          if (results.serverErrors5xx[status] !== undefined) results.serverErrors5xx[status]++;
          else results.serverErrors5xx.other++;
        }
      } catch (err) {
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;
        routeData.count++;
        routeData.latencies.push(reqDuration);

        if (isWrite) results.writeRequests++;
        else results.readRequests++;

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
      if (i > 0) await new Promise(r => setTimeout(r, Math.min(i * 10, 500)));
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
    scenario: 'Representative Multi-Studio Mixed Load (20 VUs, 10 Studios)',
    concurrency,
    studiosCount: studios.length,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    readRequests: results.readRequests,
    writeRequests: results.writeRequests,
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
  fs.writeFileSync(path.join(resultsDir, 'multi_studio_results.json'), JSON.stringify(summary, null, 2), 'utf8');

  console.log(`\n============================================================`);
  console.log(` REPRESENTATIVE MULTI-STUDIO BENCHMARK RESULTS`);
  console.log(`============================================================`);
  console.log(`- Total Requests   : ${summary.totalRequests} (Reads: ${summary.readRequests}, Writes: ${summary.writeRequests})`);
  console.log(`- HTTP 2xx Success : ${summary.successes2xx} (${((summary.successes2xx / summary.totalRequests) * 100).toFixed(2)}%)`);
  console.log(`- HTTP 4xx Client  : ${summary.clientErrors4xx.total}`);
  console.log(`- HTTP 5xx Server  : ${summary.serverErrors5xx.total}`);
  console.log(`- Timeouts         : ${summary.timeouts}`);
  console.log(`- Connection Errs  : ${summary.connectionErrors}`);
  console.log(`- Throughput       : ${summary.throughputRps} req/s`);
  console.log(`- Overall Latency  : p50=${summary.p50Ms}ms | p90=${summary.p90Ms}ms | p95=${summary.p95Ms}ms | p99=${summary.p99Ms}ms | max=${summary.maxMs}ms`);
  console.log(`\nRoute Latency Breakdown:`);
  for (const [rPath, rStat] of Object.entries(summary.routeLatency)) {
    console.log(`  * ${rStat.name} (${rPath}): count=${rStat.count} | p50=${rStat.p50Ms}ms | p95=${rStat.p95Ms}ms | p99=${rStat.p99Ms}ms | max=${rStat.maxMs}ms | 5xx=${rStat['5xx']}`);
  }
  console.log(`============================================================\n`);

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
  runMultiStudioMixed(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runMultiStudioMixed };
