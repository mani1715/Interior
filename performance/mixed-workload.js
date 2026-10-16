/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Mixed Workload (80/20 Read/Write) Load Test
 *
 * Simulates realistic production ratio:
 * - 80% Reads: Public portfolio exploration, discovery, authenticated designer queries
 * - 20% Writes: Inbound client inquiries, designer profile/project updates
 * - Supports stepped concurrency targets: 1 -> 10 -> 25 -> 50 -> 100 concurrent workers
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');
const { authenticatePersona } = require('./authenticated-read');

async function runMixedWorkload(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080/api/v1';
  const concurrency = parseInt(options.concurrency || '25', 10);
  const durationSec = parseInt(options.duration || '10', 10);
  const studioSlug = options.slug || 'dev-studio-atelier';

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Mixed Workload (80/20 Read/Write) Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(` Target Studio  : ${studioSlug}`);
  console.log(`============================================================\n`);

  const { cookie, csrfToken, studioId } = await authenticatePersona(baseUrl);

  const readEndpoints = [
    { name: 'Public Health', path: '/health' },
    { name: 'Project Discovery', path: '/public/discovery/projects' },
    { name: 'Search Discovery', path: '/public/discovery/search' },
    { name: 'Studio Reviews', path: `/public/studios/${studioSlug}/reviews` },
    { name: 'Studio Projects', path: '/projects?page=0&size=10' },
    { name: 'Studio Team', path: '/workspace/team' },
    { name: 'Studio Analytics', path: '/studio/analytics' }
  ];

  const results = {
    totalRequests: 0,
    readRequests: 0,
    writeRequests: 0,
    successes: 0,
    errors: 0,
    statusCodes: {},
    readLatencies: [],
    writeLatencies: [],
    allLatencies: []
  };

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);

  let writeCounter = 1;

  async function worker(workerId) {
    let reqIndex = workerId;

    while (Date.now() < stopTime) {
      reqIndex++;
      const isWrite = (reqIndex % 5 === 0); // 20% writes (1 in 5)

      const headers = {
        'Accept': 'application/json',
        'User-Agent': `Elegance-LoadTest-MixedWorker/${workerId}`
      };
      if (cookie) headers['Cookie'] = cookie;
      if (csrfToken) headers['X-CSRF-Token'] = csrfToken;
      if (studioId) headers['X-Studio-Id'] = studioId;

      const reqStart = performance.now();

      try {
        let response;
        if (isWrite) {
          const wId = writeCounter++;
          headers['Content-Type'] = 'application/json';
          const body = JSON.stringify({
            title: `Mixed Project ${workerId}-${wId}`,
            categoryCode: 'COMPLETE_HOME_INTERIOR',
            shortDescription: `Automated mixed load project ${wId}`
          });

          response = await fetch(`${baseUrl}/projects`, {
            method: 'POST',
            headers,
            body
          });
        } else {
          const ep = readEndpoints[reqIndex % readEndpoints.length];
          response = await fetch(`${baseUrl}${ep.path}`, {
            method: 'GET',
            headers
          });
        }

        const reqDuration = performance.now() - reqStart;
        results.allLatencies.push(reqDuration);
        results.totalRequests++;

        if (isWrite) {
          results.writeRequests++;
          results.writeLatencies.push(reqDuration);
        } else {
          results.readRequests++;
          results.readLatencies.push(reqDuration);
        }

        const status = response.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (response.ok || status === 201) {
          results.successes++;
        } else {
          results.errors++;
        }
      } catch (err) {
        const reqDuration = performance.now() - reqStart;
        results.allLatencies.push(reqDuration);
        results.totalRequests++;
        results.errors++;
        results.statusCodes['ERR'] = (results.statusCodes['ERR'] || 0) + 1;
        if (isWrite) results.writeRequests++;
        else results.readRequests++;
      }
    }
  }

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const allStats = calculatePercentiles(results.allLatencies);
  const readStats = calculatePercentiles(results.readLatencies);
  const writeStats = calculatePercentiles(results.writeLatencies);

  const summary = {
    scenario: 'Mixed Workload (80/20)',
    concurrency,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    readRequests: results.readRequests,
    writeRequests: results.writeRequests,
    successes: results.successes,
    errors: results.errors,
    errorRatePct: results.totalRequests > 0 ? Math.round((results.errors / results.totalRequests) * 10000) / 100 : 0,
    throughputRps: throughput,
    overall: {
      p50Ms: Math.round(allStats.p50 * 100) / 100,
      p95Ms: Math.round(allStats.p95 * 100) / 100,
      p99Ms: Math.round(allStats.p99 * 100) / 100
    },
    reads: {
      p50Ms: Math.round(readStats.p50 * 100) / 100,
      p95Ms: Math.round(readStats.p95 * 100) / 100
    },
    writes: {
      p50Ms: Math.round(writeStats.p50 * 100) / 100,
      p95Ms: Math.round(writeStats.p95 * 100) / 100
    },
    statusCodes: results.statusCodes
  };

  console.log(`Results:`);
  console.log(`- Total Requests : ${summary.totalRequests} (Reads: ${summary.readRequests}, Writes: ${summary.writeRequests})`);
  console.log(`- Throughput     : ${summary.throughputRps} req/s`);
  console.log(`- Error Rate     : ${summary.errorRatePct}% (${summary.errors} errors)`);
  console.log(`- Overall p50/p95/p99: ${summary.overall.p50Ms}ms / ${summary.overall.p95Ms}ms / ${summary.overall.p99Ms}ms`);
  console.log(`- Reads p50/p95  : ${summary.reads.p50Ms}ms / ${summary.reads.p95Ms}ms`);
  console.log(`- Writes p50/p95 : ${summary.writes.p50Ms}ms / ${summary.writes.p95Ms}ms`);
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
  runMixedWorkload(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runMixedWorkload };
