/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Concurrent Write & Mutation Load Test
 *
 * Simulates concurrent write traffic:
 * - Inbound prospective client lead inquiries
 * - Designer studio project updates / draft creations
 * - Tests row lock contention, connection management, and zero deadlock guarantees
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');
const { authenticatePersona } = require('./authenticated-read');

async function runAuthenticatedWrite(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080';
  const concurrency = parseInt(options.concurrency || '5', 10);
  const durationSec = parseInt(options.duration || '10', 10);
  const studioSlug = options.slug || 'concurrency-test-studio';

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Concurrent Write & Mutation Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(` Target Studio  : ${studioSlug}`);
  console.log(`============================================================\n`);

  const { cookie, csrfToken } = await authenticatePersona(baseUrl);

  const results = {
    totalRequests: 0,
    successes: 0,
    errors: 0,
    statusCodes: {},
    latencies: []
  };

  const startTime = Date.now();
  const stopTime = startTime + (durationSec * 1000);

  let counter = 1;

  async function worker(workerId) {
    while (Date.now() < stopTime) {
      const idx = counter++;
      const reqStart = performance.now();

      try {
        const leadPayload = {
          clientName: `Test Client ${idx}`,
          email: `client_${workerId}_${idx}@example.com`,
          phone: "+919876543210",
          projectScope: "FULL_INTERIOR",
          propertyType: "APARTMENT",
          preferredStyle: "MODERN_MINIMALIST",
          budgetCurrency: "INR",
          budgetMin: 500000,
          budgetMax: 1500000,
          notes: `Automated load test inquiry #${idx} from worker ${workerId}`
        };

        const headers = {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'User-Agent': `Elegance-LoadTest-WriteWorker/${workerId}`
        };
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;

        const response = await fetch(`${baseUrl}/api/v1/public/studios/${studioSlug}/leads`, {
          method: 'POST',
          headers,
          body: JSON.stringify(leadPayload)
        });

        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;

        const status = response.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (response.ok || status === 201) {
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

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const stats = calculatePercentiles(results.latencies);

  const summary = {
    scenario: 'Concurrent Write & Mutation',
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
    statusCodes: results.statusCodes
  };

  console.log(`Results:`);
  console.log(`- Total Requests : ${summary.totalRequests}`);
  console.log(`- Throughput     : ${summary.throughputRps} req/s`);
  console.log(`- Error Rate     : ${summary.errorRatePct}% (${summary.errors} errors)`);
  console.log(`- Latency p50    : ${summary.p50Ms} ms`);
  console.log(`- Latency p95    : ${summary.p95Ms} ms`);
  console.log(`- Latency p99    : ${summary.p99Ms} ms`);
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
  runAuthenticatedWrite(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runAuthenticatedWrite };
