/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Authenticated Designer Read Load Test
 *
 * Simulates concurrent authenticated designers loading:
 * - Designer studio dashboard & profile
 * - Studio projects & rooms
 * - Studio team members & permissions
 * - Studio leads CRM
 * - Studio analytics overview
 */

const { assertTargetSafety, calculatePercentiles } = require('./safety');

async function authenticatePersona(baseUrl, persona = 'DESIGNER_ADMIN') {
  try {
    const res = await fetch(`${baseUrl}/auth/dev-login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ persona })
    });

    if (!res.ok) {
      console.warn(`[WARN] Dev login returned ${res.status}. Running in unauthenticated fallback.`);
      return { cookie: null, csrfToken: null };
    }

    const cookieHeader = res.headers.get('set-cookie');
    const cookie = cookieHeader ? cookieHeader.split(';')[0] : null;

    let csrfToken = null;
    try {
      const csrfRes = await fetch(`${baseUrl}/auth/csrf`, {
        headers: cookie ? { 'Cookie': cookie } : {}
      });
      if (csrfRes.ok) {
        const csrfData = await csrfRes.json();
        csrfToken = csrfData.token;
      }
    } catch (_) {}

    return { cookie, csrfToken };
  } catch (err) {
    console.warn(`[WARN] Dev login error: ${err.message}. Proceeding with mock/fallback headers.`);
    return { cookie: null, csrfToken: null };
  }
}

async function runAuthenticatedRead(options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080';
  const concurrency = parseInt(options.concurrency || '10', 10);
  const durationSec = parseInt(options.duration || '10', 10);

  assertTargetSafety(baseUrl);

  console.log(`\n============================================================`);
  console.log(` SCENARIO: Authenticated Designer Read Load Test`);
  console.log(` Target Base URL: ${baseUrl}`);
  console.log(` Concurrency    : ${concurrency} concurrent workers`);
  console.log(` Duration       : ${durationSec} seconds`);
  console.log(`============================================================\n`);

  const { cookie, csrfToken } = await authenticatePersona(baseUrl);

  const endpoints = [
    { name: 'Current Identity', path: '/auth/me' },
    { name: 'Studio Projects', path: '/api/v1/studio/projects?page=0&size=20' },
    { name: 'Studio Team', path: '/api/v1/studio/team' },
    { name: 'Studio Leads CRM', path: '/api/v1/studio/leads?page=0&size=20' },
    { name: 'Studio Analytics', path: '/api/v1/studio/analytics/overview' }
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

      const headers = {
        'Accept': 'application/json',
        'User-Agent': `Elegance-LoadTest-AuthReadWorker/${workerId}`
      };
      if (cookie) headers['Cookie'] = cookie;
      if (csrfToken) headers['X-CSRF-Token'] = csrfToken;

      const reqStart = performance.now();
      try {
        const response = await fetch(`${baseUrl}${ep.path}`, {
          method: 'GET',
          headers
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

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i + 1));
  }

  await Promise.all(workers);

  const totalTimeSec = (Date.now() - startTime) / 1000;
  const throughput = Math.round((results.totalRequests / totalTimeSec) * 100) / 100;
  const stats = calculatePercentiles(results.latencies);

  const summary = {
    scenario: 'Authenticated Designer Read',
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
  runAuthenticatedRead(options).catch(err => {
    console.error('Test execution failed:', err);
    process.exit(1);
  });
}

module.exports = { runAuthenticatedRead, authenticatePersona };
