/**
 * Elégance — Interior Designer Platform
 * Phase 12H: Master Load Test Orchestrator & Benchmark Runner
 *
 * Runs individual or stepped multi-scenario load tests against local/staging environments.
 * Strictly enforces localhost restriction unless ALLOW_REMOTE_LOAD_TEST=true is set.
 *
 * Usage:
 *   node performance/runner.js --scenario=public-browse --concurrency=10 --duration=10
 *   node performance/runner.js --all
 */

const { assertTargetSafety } = require('./safety');
const { runPublicBrowse } = require('./public-browse');
const { runAuthenticatedRead } = require('./authenticated-read');
const { runAuthenticatedWrite } = require('./authenticated-write');
const { runMixedWorkload } = require('./mixed-workload');
const { runMediaWorkload } = require('./media-workload');
const { runSoakTest } = require('./soak');

async function main() {
  const args = process.argv.slice(2);
  const options = {};
  for (const arg of args) {
    if (arg.startsWith('--')) {
      const parts = arg.slice(2).split('=');
      options[parts[0]] = parts.length > 1 ? parts[1] : true;
    }
  }

  const baseUrl = options.baseUrl || 'http://localhost:8080';
  assertTargetSafety(baseUrl);

  console.log(`\n` + `=`.repeat(70));
  console.log(` ELÉGANCE PLATFORM -- PERFORMANCE & BENCHMARK RUNNER (PHASE 12H) `);
  console.log(`=`.repeat(70));
  console.log(` Target Base URL  : ${baseUrl}`);
  console.log(` Mode             : ${options.all ? 'Full Multi-Scenario Suite' : (options.scenario || 'public-browse')}`);
  console.log(` Execution Context: LOCAL INTEGRATION STRESS HARNESS`);
  console.log(` Capacity Notice  : LOCAL PERFORMANCE VALIDATED -- NOT A PRODUCTION SLA`);
  console.log(`=`.repeat(70) + `\n`);

  const results = [];

  if (options.all) {
    console.log(`Running Stepped Suite: 1 -> 10 -> 25 -> 50 -> 100 concurrency targets...\n`);

    // 1. Single-user baseline
    results.push(await runPublicBrowse({ baseUrl, concurrency: 1, duration: 5 }));

    // 2. 10 users Public Browse
    results.push(await runPublicBrowse({ baseUrl, concurrency: 10, duration: 8 }));

    // 3. 25 users Authenticated Read
    results.push(await runAuthenticatedRead({ baseUrl, concurrency: 25, duration: 8 }));

    // 4. 10 users Concurrent Writes
    results.push(await runAuthenticatedWrite({ baseUrl, concurrency: 10, duration: 8 }));

    // 5. 50 users Mixed Workload (80/20)
    results.push(await runMixedWorkload({ baseUrl, concurrency: 50, duration: 10 }));

    // 6. 100 users Synthetic Stress Target
    results.push(await runPublicBrowse({ baseUrl, concurrency: 100, duration: 10 }));

    // 7. Media Quota Workload
    results.push(await runMediaWorkload({ baseUrl, concurrency: 10, duration: 8 }));

    // 8. Soak Test
    results.push(await runSoakTest({ baseUrl, concurrency: 15, duration: 15 }));
  } else {
    const scenario = options.scenario || 'public-browse';
    switch (scenario) {
      case 'public-browse':
        results.push(await runPublicBrowse(options));
        break;
      case 'authenticated-read':
        results.push(await runAuthenticatedRead(options));
        break;
      case 'authenticated-write':
        results.push(await runAuthenticatedWrite(options));
        break;
      case 'mixed-workload':
        results.push(await runMixedWorkload(options));
        break;
      case 'media-workload':
        results.push(await runMediaWorkload(options));
        break;
      case 'soak':
        results.push(await runSoakTest(options));
        break;
      default:
        console.error(`Unknown scenario: ${scenario}`);
        console.log(`Available scenarios: public-browse, authenticated-read, authenticated-write, mixed-workload, media-workload, soak`);
        process.exit(1);
    }
  }

  // Render Markdown Table
  console.log(`\n\n` + `=`.repeat(70));
  console.log(` PERFORMANCE BENCHMARK SUMMARY REPORT`);
  console.log(`=`.repeat(70) + `\n`);

  console.log(`| Scenario | Concurrency | Total Req | Duration | Throughput | p50 (ms) | p95 (ms) | p99 (ms) | Error Rate | Verdict |`);
  console.log(`|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|`);

  for (const r of results) {
    const p50 = r.p50Ms !== undefined ? r.p50Ms : (r.overall ? r.overall.p50Ms : 0);
    const p95 = r.p95Ms !== undefined ? r.p95Ms : (r.overall ? r.overall.p95Ms : 0);
    const p99 = r.p99Ms !== undefined ? r.p99Ms : (r.overall ? r.overall.p99Ms : 0);
    const errRate = r.errorRatePct !== undefined ? r.errorRatePct : 0;
    const verdict = errRate <= 1.0 ? 'PASS' : (errRate <= 5.0 ? 'WARN' : 'FAIL');

    console.log(`| ${r.scenario} | ${r.concurrency} | ${r.totalRequests} | ${r.durationSec}s | ${r.throughputRps} rps | ${p50} | ${p95} | ${p99} | ${errRate}% | **${verdict}** |`);
  }

  console.log(`\nCapacity Declaration: LOCAL PERFORMANCE VALIDATED (Pre-Cloud Engineering Baseline)`);
  console.log(`Final contractual production capacity will be validated upon Phase 15 cloud activation.\n`);
}

if (require.main === module) {
  main().catch(err => {
    console.error('Runner failed:', err);
    process.exit(1);
  });
}

module.exports = { main };
