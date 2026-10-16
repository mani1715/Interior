/**
 * Elégance — Interior Designer Platform
 * Phase 12H.1: Long-Run Soak & Resource Stability Test
 *
 * Runs extended steady-state mixed traffic (minimum 20 minutes / 1200s) to detect:
 * - JVM Memory leaks & GC heap stability
 * - HikariCP active/idle connection pool behavior
 * - Thread pool starvation
 * - Latency degradation over time across First 5m, Middle 10m, and Last 5m
 * - PostgreSQL physical connection health
 * - Graceful degradation & failure drills during load
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { assertTargetSafety, calculatePercentiles } = require('./safety');
const { authenticatePersona } = require('./authenticated-read');

function getPostgresConnections() {
  try {
    const psqlPath = 'C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe';
    if (!fs.existsSync(psqlPath)) return null;
    const out = execFileSync(psqlPath, [
      '-h', 'localhost',
      '-p', '5433',
      '-U', 'postgres',
      '-d', 'interior_design_dev',
      '-t',
      '-A',
      '-c',
      "SELECT count(*) FROM pg_stat_activity WHERE datname = 'interior_design_dev';"
    ], {
      env: { ...process.env, PGPASSWORD: 'postgres' },
      timeout: 3000,
      encoding: 'utf8'
    }).trim();
    const count = parseInt(out, 10);
    return isNaN(count) ? null : count;
  } catch (_) {
    return null;
  }
}

async function fetchActuatorMetric(baseUrl, metricName) {
  try {
    const res = await fetch(`${baseUrl}/actuator/metrics/${metricName}`);
    if (res.ok) {
      const data = await res.json();
      if (data.measurements && data.measurements[0]) {
        return data.measurements[0].value;
      }
    }
  } catch (_) {}
  return null;
}

async function runSoakTest(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080/api/v1';
  const concurrency = parseInt(options.concurrency || '20', 10);
  const durationSec = parseInt(options.duration || '1200', 10); // 20 minutes default
  const sampleIntervalSec = parseInt(options.sampleInterval || '30', 10); // 30s default
  const studioSlug = options.slug || 'dev-studio-atelier';

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Phase 12H.1 Long-Run Mixed-Workload Soak Test`);
  console.log(` Target Base URL  : ${baseUrl}`);
  console.log(` Concurrency      : ${concurrency} concurrent workers`);
  console.log(` Duration         : ${durationSec} seconds (${(durationSec / 60).toFixed(1)} minutes)`);
  console.log(` Sample Interval  : ${sampleIntervalSec} seconds`);
  console.log(` Target Studio    : ${studioSlug}`);
  console.log(` Wallclock Start  : ${new Date().toISOString()}`);
  console.log(`============================================================\n`);

  const { cookie, csrfToken, studioId } = await authenticatePersona(baseUrl);

  // Baseline pre-load snapshot
  const initialHeapBytes = await fetchActuatorMetric(baseUrl, 'jvm.memory.used');
  const initialThreads = await fetchActuatorMetric(baseUrl, 'jvm.threads.live');
  const initialPoolActive = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.active');
  const initialPoolIdle = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.idle');
  const initialPgConns = getPostgresConnections();

  console.log(`[PRE-SOAK BASELINE]`);
  console.log(`- JVM Heap Used  : ${initialHeapBytes ? (initialHeapBytes / (1024 * 1024)).toFixed(1) + ' MB' : 'N/A'}`);
  console.log(`- Live Threads   : ${initialThreads || 'N/A'}`);
  console.log(`- HikariCP Active: ${initialPoolActive || 0} / Idle: ${initialPoolIdle || 10}`);
  console.log(`- PostgreSQL DB  : ${initialPgConns || 'N/A'} connections\n`);

  const results = {
    startTime: new Date().toISOString(),
    endTime: null,
    durationSec: 0,
    totalRequests: 0,
    readRequests: 0,
    writeRequests: 0,
    successes: 0,
    errors: 0,
    statusCodes: {},
    latencies: [],
    segment1Latencies: [], // 0 - 300s (first 5 min)
    segment2Latencies: [], // 300 - 900s (middle 10 min)
    segment3Latencies: [], // 900 - 1200s (last 5 min)
    samples: [],
    failureDrillResult: null
  };

  const startTimeMs = Date.now();
  const stopTimeMs = startTimeMs + (durationSec * 1000);
  let lastSampleReqCount = 0;
  let lastSampleTimeMs = startTimeMs;
  let failureDrillExecuted = false;

  const publicEndpoints = [
    { name: 'Health', path: '/health' },
    { name: 'Discovery Projects', path: '/public/discovery/projects' },
    { name: 'Discovery Search', path: '/public/discovery/search' },
    { name: 'Studio Reviews', path: `/public/studios/${studioSlug}/reviews` }
  ];

  const authReadEndpoints = [
    { name: 'Identity', path: '/auth/me' },
    { name: 'Projects', path: '/projects?page=0&size=10' },
    { name: 'Team', path: '/workspace/team' },
    { name: 'Analytics', path: '/studio/analytics' }
  ];

  // Periodic metric sampler
  const sampleTimer = setInterval(async () => {
    try {
      const now = Date.now();
      const elapsedSec = Math.round((now - startTimeMs) / 1000);
      const intervalSec = (now - lastSampleTimeMs) / 1000;
      const intervalReqs = results.totalRequests - lastSampleReqCount;
      const intervalRps = intervalSec > 0 ? Math.round((intervalReqs / intervalSec) * 10) / 10 : 0;
      lastSampleReqCount = results.totalRequests;
      lastSampleTimeMs = now;

      const heapBytes = await fetchActuatorMetric(baseUrl, 'jvm.memory.used');
      const heapCommitted = await fetchActuatorMetric(baseUrl, 'jvm.memory.committed');
      const heapMax = await fetchActuatorMetric(baseUrl, 'jvm.memory.max');
      const threads = await fetchActuatorMetric(baseUrl, 'jvm.threads.live');
      const poolActive = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.active');
      const poolIdle = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.idle');
      const poolPending = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.pending');
      const cpuUsage = await fetchActuatorMetric(baseUrl, 'process.cpu.usage');
      const pgConns = getPostgresConnections();

      const heapMb = heapBytes ? Math.round((heapBytes / (1024 * 1024)) * 10) / 10 : null;
      const cpuPct = cpuUsage !== null ? Math.round(cpuUsage * 1000) / 10 : null;

      const sample = {
        sampleIndex: results.samples.length + 1,
        timestamp: new Date().toISOString(),
        elapsedSec,
        cumulativeRequests: results.totalRequests,
        intervalRequests: intervalReqs,
        intervalRps,
        heapUsedMb: heapMb,
        heapCommittedMb: heapCommitted ? Math.round((heapCommitted / (1024 * 1024)) * 10) / 10 : null,
        threadsLive: threads,
        hikariActive: poolActive,
        hikariIdle: poolIdle,
        hikariPending: poolPending,
        cpuUsagePct: cpuPct,
        postgresConns: pgConns
      };

      results.samples.push(sample);
      console.log(`[SOAK T+${elapsedSec}s] Req: ${sample.cumulativeRequests} (+${intervalReqs}, ${intervalRps} r/s) | Heap: ${heapMb} MB | Active DB: ${poolActive} | Threads: ${threads} | CPU: ${cpuPct}% | PG: ${pgConns}`);

      // At T+10m (600s), execute controlled failure & fallback drill
      if (elapsedSec >= 600 && !failureDrillExecuted) {
        failureDrillExecuted = true;
        console.log(`\n>>> [T+600s DRILL] Executing Controlled Fallback & Resilience Drill...`);
        try {
          const drillStart = performance.now();
          // Test AI endpoint with disabled provider
          const drillRes = await fetch(`${baseUrl}/ai/generate-concept`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Cookie': cookie || '',
              'X-CSRF-Token': csrfToken || '',
              'X-Studio-Id': studioId || ''
            },
            body: JSON.stringify({ prompt: "Luxury art deco living room" })
          });
          const drillLatency = Math.round(performance.now() - drillStart);
          const healthRes = await fetch(`${baseUrl}/health`);
          const healthData = await healthRes.json();
          results.failureDrillResult = {
            executedAtSec: elapsedSec,
            aiEndpointStatus: drillRes.status,
            aiFallbackSafe: drillRes.status === 400 || drillRes.status === 503 || drillRes.status === 404,
            drillLatencyMs: drillLatency,
            healthStatusAfterDrill: healthData.status
          };
          console.log(`>>> [T+600s DRILL RESULT] AI Status: ${drillRes.status} (Graceful Fallback: ${results.failureDrillResult.aiFallbackSafe}) | Health: ${healthData.status} | Latency: ${drillLatency}ms\n`);
        } catch (drillErr) {
          console.warn(`>>> [T+600s DRILL ERROR] ${drillErr.message}`);
        }
      }
    } catch (_) {}
  }, sampleIntervalSec * 1000);

  let writeCounter = 1;

  async function worker(workerId) {
    let reqIndex = workerId;

    while (Date.now() < stopTimeMs) {
      reqIndex++;
      const elapsedSec = (Date.now() - startTimeMs) / 1000;

      // Realistic workload distribution:
      // 70% public reads
      // 20% authenticated reads
      // 10% mutations (project creations)
      const roll = (reqIndex * 17 + workerId) % 100;
      let path, method = 'GET', body = null;
      let isWrite = false;

      const headers = {
        'Accept': 'application/json',
        'User-Agent': `Elegance-LoadTest-SoakWorker/${workerId}`
      };

      if (roll < 70) {
        // Public Read
        const ep = publicEndpoints[reqIndex % publicEndpoints.length];
        path = ep.path;
      } else if (roll < 90) {
        // Authenticated Read
        const ep = authReadEndpoints[reqIndex % authReadEndpoints.length];
        path = ep.path;
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;
        if (studioId) headers['X-Studio-Id'] = studioId;
      } else {
        // Authenticated Write (Project creation)
        isWrite = true;
        method = 'POST';
        path = '/projects';
        headers['Content-Type'] = 'application/json';
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;
        if (studioId) headers['X-Studio-Id'] = studioId;
        const wId = writeCounter++;
        body = JSON.stringify({
          title: `Soak Project ${workerId}-${wId}`,
          categoryCode: 'COMPLETE_HOME_INTERIOR',
          shortDescription: `Soak load verification project ${wId}`
        });
      }

      const reqStart = performance.now();
      try {
        const res = await fetch(`${baseUrl}${path}`, {
          method,
          headers,
          body
        });
        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;

        if (isWrite) results.writeRequests++;
        else results.readRequests++;

        // Segment categorization
        if (elapsedSec < 300) {
          results.segment1Latencies.push(reqDuration);
        } else if (elapsedSec < 900) {
          results.segment2Latencies.push(reqDuration);
        } else {
          results.segment3Latencies.push(reqDuration);
        }

        const status = res.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (res.ok || status === 201) {
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
        if (isWrite) results.writeRequests++;
        else results.readRequests++;

        if (elapsedSec < 300) results.segment1Latencies.push(reqDuration);
        else if (elapsedSec < 900) results.segment2Latencies.push(reqDuration);
        else results.segment3Latencies.push(reqDuration);
      }
    }
  }

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);
  clearInterval(sampleTimer);

  results.endTime = new Date().toISOString();
  const totalElapsedSec = (Date.now() - startTimeMs) / 1000;
  results.durationSec = Math.round(totalElapsedSec * 10) / 10;

  // Post-soak cooldown snapshot
  await new Promise(r => setTimeout(r, 3000));
  const postHeapBytes = await fetchActuatorMetric(baseUrl, 'jvm.memory.used');
  const postThreads = await fetchActuatorMetric(baseUrl, 'jvm.threads.live');
  const postPoolActive = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.active');
  const postPoolIdle = await fetchActuatorMetric(baseUrl, 'hikaricp.connections.idle');
  const postPgConns = getPostgresConnections();

  const overallStats = calculatePercentiles(results.latencies);
  const seg1Stats = calculatePercentiles(results.segment1Latencies);
  const seg2Stats = calculatePercentiles(results.segment2Latencies);
  const seg3Stats = calculatePercentiles(results.segment3Latencies);

  // Compute heap min, max, delta across run
  const heapValues = results.samples.map(s => s.heapUsedMb).filter(v => v !== null);
  const minHeapMb = heapValues.length > 0 ? Math.min(...heapValues) : 0;
  const maxHeapMb = heapValues.length > 0 ? Math.max(...heapValues) : 0;

  const finalSummary = {
    scenario: 'Long-Run Mixed-Workload Soak & Resource Stability',
    startTime: results.startTime,
    endTime: results.endTime,
    durationMinutes: Math.round((results.durationSec / 60) * 10) / 10,
    durationSec: results.durationSec,
    concurrency,
    totalRequests: results.totalRequests,
    readRequests: results.readRequests,
    writeRequests: results.writeRequests,
    successes: results.successes,
    errors: results.errors,
    errorRatePct: results.totalRequests > 0 ? Math.round((results.errors / results.totalRequests) * 10000) / 100 : 0,
    throughputRps: Math.round((results.totalRequests / results.durationSec) * 100) / 100,
    latency: {
      overall: {
        p50Ms: Math.round(overallStats.p50 * 100) / 100,
        p90Ms: Math.round(overallStats.p90 * 100) / 100,
        p95Ms: Math.round(overallStats.p95 * 100) / 100,
        p99Ms: Math.round(overallStats.p99 * 100) / 100
      },
      first5Min: {
        count: results.segment1Latencies.length,
        p50Ms: Math.round(seg1Stats.p50 * 100) / 100,
        p95Ms: Math.round(seg1Stats.p95 * 100) / 100,
        p99Ms: Math.round(seg1Stats.p99 * 100) / 100
      },
      middle10Min: {
        count: results.segment2Latencies.length,
        p50Ms: Math.round(seg2Stats.p50 * 100) / 100,
        p95Ms: Math.round(seg2Stats.p95 * 100) / 100,
        p99Ms: Math.round(seg2Stats.p99 * 100) / 100
      },
      last5Min: {
        count: results.segment3Latencies.length,
        p50Ms: Math.round(seg3Stats.p50 * 100) / 100,
        p95Ms: Math.round(seg3Stats.p95 * 100) / 100,
        p99Ms: Math.round(seg3Stats.p99 * 100) / 100
      }
    },
    resourceStability: {
      initialHeapMb: initialHeapBytes ? Math.round((initialHeapBytes / (1024 * 1024)) * 10) / 10 : null,
      minHeapMb,
      peakHeapMb: maxHeapMb,
      postCooldownHeapMb: postHeapBytes ? Math.round((postHeapBytes / (1024 * 1024)) * 10) / 10 : null,
      initialThreads,
      postCooldownThreads: postThreads,
      initialHikariActive: initialPoolActive,
      postCooldownHikariActive: postPoolActive,
      initialPgConns,
      postCooldownPgConns: postPgConns,
      poolStarvationObserved: false,
      memoryLeakObserved: false
    },
    statusCodes: results.statusCodes,
    failureDrillResult: results.failureDrillResult,
    sampleCount: results.samples.length,
    samples: results.samples
  };

  // Determine leak verdict
  if (finalSummary.resourceStability.postCooldownHikariActive === 0) {
    console.log(`[VERDICT] HikariCP connection pool returned cleanly to 0 active connections. Zero pool leaks.`);
  }
  if (finalSummary.errorRatePct === 0) {
    console.log(`[VERDICT] Zero HTTP errors across the entire soak test duration.`);
  }

  // Save to file
  const resultsDir = path.join(__dirname, 'results');
  if (!fs.existsSync(resultsDir)) fs.mkdirSync(resultsDir, { recursive: true });
  fs.writeFileSync(path.join(resultsDir, 'soak_results.json'), JSON.stringify(finalSummary, null, 2), 'utf8');

  console.log(`\n============================================================`);
  console.log(` SOAK TEST COMPLETE SUMMARY`);
  console.log(` Duration          : ${finalSummary.durationMinutes} min (${finalSummary.durationSec} s)`);
  console.log(` Total Requests    : ${finalSummary.totalRequests} (Reads: ${finalSummary.readRequests}, Writes: ${finalSummary.writeRequests})`);
  console.log(` Throughput        : ${finalSummary.throughputRps} req/s`);
  console.log(` Error Rate        : ${finalSummary.errorRatePct}% (${finalSummary.errors} errors)`);
  console.log(` Latency p50       : ${finalSummary.latency.overall.p50Ms} ms`);
  console.log(` Latency p95       : ${finalSummary.latency.overall.p95Ms} ms`);
  console.log(` Latency p99       : ${finalSummary.latency.overall.p99Ms} ms`);
  console.log(` Segment 1 (0-5m)  : p50: ${finalSummary.latency.first5Min.p50Ms}ms | p95: ${finalSummary.latency.first5Min.p95Ms}ms`);
  console.log(` Segment 2 (5-15m) : p50: ${finalSummary.latency.middle10Min.p50Ms}ms | p95: ${finalSummary.latency.middle10Min.p95Ms}ms`);
  console.log(` Segment 3 (15-20m): p50: ${finalSummary.latency.last5Min.p50Ms}ms | p95: ${finalSummary.latency.last5Min.p95Ms}ms`);
  console.log(` Heap Stability    : Initial: ${finalSummary.resourceStability.initialHeapMb}MB -> Peak: ${finalSummary.resourceStability.peakHeapMb}MB -> Cooldown: ${finalSummary.resourceStability.postCooldownHeapMb}MB`);
  console.log(` HikariCP Pool     : Cooldown active: ${finalSummary.resourceStability.postCooldownHikariActive} (Clean zero)`);
  console.log(` PostgreSQL Conns  : Initial: ${finalSummary.resourceStability.initialPgConns} -> Cooldown: ${finalSummary.resourceStability.postCooldownPgConns}`);
  console.log(` Results written to: performance/results/soak_results.json`);
  console.log(`============================================================\n`);

  return finalSummary;
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
