/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Media & Upload Intent Workload Load Test
 *
 * Simulates concurrent media operations:
 * - Upload intent requests with quota evaluation
 * - Quota accounting speed and overhead
 * - Media listing and ordering operations
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');
const { authenticatePersona } = require('./authenticated-read');

async function runMediaWorkload(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080';
  const concurrency = parseInt(options.concurrency || '5', 10);
  const durationSec = parseInt(options.duration || '10', 10);

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Media & Upload Intent Workload Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(`============================================================\n`);

  const { cookie, csrfToken } = await authenticatePersona(baseUrl);

  const results = {
    totalRequests: 0,
    successes: 0,
    quotaRejections: 0,
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
        const intentPayload = {
          projectId: "01923000-0000-7000-8000-000000000010", // sample UUIDv7
          mediaType: "REAL_PROJECT",
          expectedContentType: "image/jpeg",
          expectedSizeBytes: 500000,
          filename: `photo_${workerId}_${idx}.jpg`
        };

        const headers = {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'User-Agent': `Elegance-LoadTest-MediaWorker/${workerId}`
        };
        if (cookie) headers['Cookie'] = cookie;
        if (csrfToken) headers['X-CSRF-Token'] = csrfToken;

        const response = await fetch(`${baseUrl}/api/v1/studio/media/upload-intent`, {
          method: 'POST',
          headers,
          body: JSON.stringify(intentPayload)
        });

        const reqDuration = performance.now() - reqStart;
        results.latencies.push(reqDuration);
        results.totalRequests++;

        const status = response.status;
        results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;

        if (response.ok || status === 201) {
          results.successes++;
        } else if (status === 400) {
          // Expected when quota boundary is reached
          results.quotaRejections++;
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
    scenario: 'Media & Upload Workload',
    concurrency,
    durationSec: Math.round(totalTimeSec * 10) / 10,
    totalRequests: results.totalRequests,
    successes: results.successes,
    quotaRejections: results.quotaRejections,
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
  console.log(`- Successes      : ${summary.successes}`);
  console.log(`- Quota Rejections: ${summary.quotaRejections} (Expected on plan quota caps)`);
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
  runMediaWorkload(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runMediaWorkload };
