# Phase 12E: Transactional Email Security & Quality Test Matrix

> **Scope:** Verification of all Phase 12E Email Subsystem Invariants  
> **Status:** 40 / 40 Tests PASS (100% Pass Rate)

---

## 1. Test Suite Summary Matrix

| Test Suite | File Location | Tests | Pass | Fail | Key Focus Areas |
| :--- | :--- | :---: | :---: | :---: | :--- |
| `ResendEmailProviderContractTest` | `apps/api/src/test/.../email/provider/` | 11 | 11 | 0 | HTTP/2 wire protocol, 400, 401, 422, 429, 503, timeouts, CRLF defense, missing ID safety |
| `SesEmailProviderContractTest` | `apps/api/src/test/.../email/provider/` | 8 | 8 | 0 | AWS SES SDK v2, 429 quota, domain unverified, bad request, 503 unavailable, client timeout, CRLF defense |
| `EmailSecurityAndEscapingTest` | `apps/api/src/test/.../email/service/` | 7 | 7 | 0 | OWASP HTML escaping, CRLF subject/recipient injection, multilingual UTF-8, canonical URL base resolution |
| `EmailDisabledWorkflowTest` | `apps/api/src/test/.../email/` | 5 | 5 | 0 | Safe operation when disabled, out-of-order webhook event defense (DELIVERED guard, BOUNCE guard), unknown ID |
| `ProductionConfigurationValidatorEmailTest`| `apps/api/src/test/.../config/` | 9 | 9 | 0 | Fail-fast in production: reject mock/fake, reject missing/placeholder keys, reject invalid sender, validate SES/Resend |
| **Total** | | **40** | **40** | **0** | **All Security & Contract Invariants Verified** |

---

## 2. Detailed Test Cases

### Suite 1: `ResendEmailProviderContractTest` (11 Tests)
1. `testSendSuccess`: Valid JSON payload dispatched, returns HTTP 200, parses `id="re_msg_abc123xyz"`, status `SENT`.
2. `testSend400BadRequest`: HTTP 400 maps to permanent failure (`FAILED`), extracts error description.
3. `testSend401Unauthorized`: HTTP 401 maps to authentication failure, logs warning without leaking key.
4. `testSend422UnprocessableEntity`: HTTP 422 (e.g. invalid MX records or unverified domain) maps to permanent failure.
5. `testSend429RateLimitExceeded`: HTTP 429 maps to transient retryable failure (`FAILED` with retryable flag).
6. `testSend503ServiceUnavailable`: HTTP 503 upstream error maps to retryable failure.
7. `testSendTimeout`: Socket / connection timeout triggers bounded fallback without hanging thread.
8. `testSendMalformedResponseMissingId`: HTTP 200 but missing `id` property handled gracefully without NPE.
9. `testCrlfInjectionSanitizedInRecipient`: Injected `\r\nBcc: victim@example.com` is stripped from recipient address.
10. `testCrlfInjectionSanitizedInSubject`: Injected `\r\nSubject: Spoofed` is stripped from subject line.
11. `testUnconfiguredApiKey`: Provider fails safely when API key is null or empty.

### Suite 2: `SesEmailProviderContractTest` (8 Tests)
1. `testSendSuccess`: Dispatches through `SesV2Client`, captures AWS message ID, returns `SENT`.
2. `testLimitExceededQuotaException`: Rate limit or daily quota exception maps to retryable failure.
3. `testMailFromDomainNotVerifiedException`: Unverified domain exception maps to permanent failure.
4. `testBadRequestException`: Invalid parameters map to permanent failure without crashing caller.
5. `testServiceUnavailable503Exception`: AWS regional service failure maps to retryable failure.
6. `testSdkClientTimeoutException`: SDK client timeout maps to transient failure.
7. `testCrlfInjectionSanitized`: CRLF stripped before AWS SES API parameters are constructed.
8. `testUnconfiguredRegionOrFrom`: Missing AWS SES region or from-address safely aborts dispatch.

### Suite 3: `EmailSecurityAndEscapingTest` (7 Tests)
1. `testTeamInvitationEscaping`: Script tags (`<script>alert(1)</script>`) in studio name and inviter name properly escaped to `&lt;script&gt;`.
2. `testClientReviewInvitationEscaping`: HTML tags in project name, client name, and designer notes sanitized.
3. `testPrivacyExportEscaping`: Script injection in user name sanitized; secure portal download link generated.
4. `testCrlfHeaderInjectionDefense`: Carriage returns and newlines in subjects and recipients stripped.
5. `testMultilingualUtf8Support`: Non-ASCII characters in Telugu, Hindi, Tamil, and accented European scripts preserved intact in HTML body.
6. `testLargePayloadBounds`: Exceptionally large designer notes or client messages handled without buffer overflow.
7. `testBaseUrlNormalization`: All action buttons and CTA links use canonical base URL without trailing slash issues.

### Suite 4: `EmailDisabledWorkflowTest` (5 Tests)
1. `testTeamInvitationSentWithDisabledProvider`: Inviting a designer succeeds locally, records delivery as `SENT` with `disabled_provider` ID, and returns invitation token.
2. `testClientReviewInvitationWithDisabledProvider`: Review invitation dispatches cleanly without throwing errors when provider is disabled.
3. `testOutOfOrderSentEventIgnoredIfAlreadyDelivered`: Late-arriving `SENT` webhook does NOT overwrite already `DELIVERED` status.
4. `testOutOfOrderDeliveredEventIgnoredIfAlreadyBounced`: Late-arriving `DELIVERED` webhook does NOT overwrite terminal `BOUNCED` status.
5. `testWebhookUnknownProviderMessageId`: Webhook event for unknown message ID logs structured warning and exits safely without throwing exception.

### Suite 5: `ProductionConfigurationValidatorEmailTest` (9 Tests)
1. `testDisabledEmailProviderCleanPass`: `EMAIL_PROVIDER=disabled` passes cleanly in production (Phase 15 Pending).
2. `testFakeEmailProviderRejectedInProduction`: `EMAIL_PROVIDER=fake` throws `IllegalStateException`.
3. `testMockEmailProviderRejectedInProduction`: `EMAIL_PROVIDER=mock` throws `IllegalStateException`.
4. `testTestEmailProviderRejectedInProduction`: `EMAIL_PROVIDER=test` throws `IllegalStateException`.
5. `testResendWithPlaceholderApiKeyRejected`: `RESEND_API_KEY=your-api-key-here` throws `IllegalStateException`.
6. `testResendWithPlaceholderFromAddressRejected`: `EMAIL_FROM_ADDRESS=placeholder@example.com` throws `IllegalStateException`.
7. `testResendWithValidConfigurationPasses`: Valid Resend configuration passes startup validation.
8. `testSesWithMissingRegionRejected`: `EMAIL_PROVIDER=ses` with missing region throws `IllegalStateException`.
9. `testSesWithValidConfigurationPasses`: Valid AWS SES configuration passes startup validation.
