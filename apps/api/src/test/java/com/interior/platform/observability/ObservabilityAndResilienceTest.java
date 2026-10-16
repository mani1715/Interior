package com.interior.platform.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.interior.platform.admin.dto.OperationalHealthDto;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.ai.config.AiProviderHealthIndicator;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.provider.ConfigurableAiImageProvider;
import com.interior.platform.billing.config.BillingHealthIndicator;
import com.interior.platform.billing.provider.BillingProvider;
import com.interior.platform.billing.provider.DisabledBillingProvider;
import com.interior.platform.common.filter.RequestCorrelationFilter;
import com.interior.platform.common.logging.JsonLoggingLayout;
import com.interior.platform.common.metrics.PlatformMetrics;
import com.interior.platform.config.ProductionConfigurationValidator;
import com.interior.platform.email.config.TransactionalEmailHealthIndicator;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.health.web.HealthController;
import com.interior.platform.media.config.MalwareScannerHealthIndicator;
import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.media.config.StorageHealthIndicator;
import com.interior.platform.media.security.scanner.ClamAvMalwareScanner;
import com.interior.platform.media.security.scanner.DisabledMalwareScanner;
import com.interior.platform.media.storage.LocalStorageService;
import com.interior.platform.operations.config.PlatformOperationalStateHealthIndicator;
import com.interior.platform.operations.domain.PlatformOperationalStateRecord;
import com.interior.platform.operations.domain.ProviderOperationalStatus;
import com.interior.platform.operations.dto.ProviderMatrixResponse;
import com.interior.platform.operations.dto.UpdateOperationalStateRequest;
import com.interior.platform.operations.repository.PlatformOperationsRepository;
import com.interior.platform.operations.service.OperationalJobCoordinator;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ObservabilityAndResilienceTest {

    // ========================================================================
    // 1. REQUEST CORRELATION & MDC PROPAGATION TESTS
    // ========================================================================

    @Test
    @DisplayName("RequestCorrelationFilter accepts valid bounded correlation ID")
    void testValidCorrelationIdAccepted() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.addHeader("X-Request-Id", "valid-request-id-123_abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedMdc = new String[1];
        FilterChain chain = (req, res) -> capturedMdc[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertEquals("valid-request-id-123_abc", response.getHeader("X-Request-Id"));
        assertEquals("valid-request-id-123_abc", response.getHeader("X-Request-ID"));
        assertEquals("valid-request-id-123_abc", capturedMdc[0]);
        assertNull(MDC.get("requestId"), "MDC must be cleared after filter completion");
    }

    @Test
    @DisplayName("RequestCorrelationFilter replaces invalid/dangerous correlation ID with UUID")
    void testInvalidCorrelationIdReplacedWithUuid() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.addHeader("X-Request-Id", "malicious\r\nCRLF-injection-attack;DROP TABLE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedMdc = new String[1];
        FilterChain chain = (req, res) -> capturedMdc[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader("X-Request-Id");
        assertNotNull(generatedId);
        assertNotEquals("malicious\r\nCRLF-injection-attack;DROP TABLE", generatedId);
        // Assert it is a valid UUID
        assertDoesNotThrow(() -> UUID.fromString(generatedId));
        assertEquals(generatedId, capturedMdc[0]);
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("RequestCorrelationFilter sanitizes sensitive query strings in access logs")
    void testQueryStringSanitizationInAccessLogs() {
        String query = "page=1&token=SECRET_TOKEN_XYZ&password=SECRET_PASS_123&signature=SECRET_SIG_456&code=SECRET_CODE_789";
        String sanitized = RequestCorrelationFilter.sanitizeUri("/api/v1/auth/callback", query);

        assertFalse(sanitized.contains("SECRET_TOKEN_XYZ"));
        assertFalse(sanitized.contains("SECRET_PASS_123"));
        assertFalse(sanitized.contains("SECRET_SIG_456"));
        assertFalse(sanitized.contains("SECRET_CODE_789"));
        assertTrue(sanitized.contains("token=[REDACTED]"));
        assertTrue(sanitized.contains("password=[REDACTED]"));
        assertTrue(sanitized.contains("signature=[REDACTED]"));
        assertTrue(sanitized.contains("code=[REDACTED]"));
        assertTrue(sanitized.contains("page=1"));
    }

    @Test
    @DisplayName("RequestCorrelationFilter guarantees MDC is cleared on uncaught downstream exception")
    void testMdcCleanedUpOnException() {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertNotNull(MDC.get("requestId"));
            throw new RuntimeException("Simulated unexpected crash");
        };

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get("requestId"), "MDC must be cleared even when downstream filter/controller crashes");
        assertNull(MDC.get("correlationId"));
    }

    // ========================================================================
    // 2. STRUCTURED JSON LOGGING & SECRET SENTINEL REDACTION TESTS
    // ========================================================================

    @Test
    @DisplayName("JsonLoggingLayout neutralizes log injection and escapes carriage returns/newlines")
    void testLogInjectionNeutralization() {
        String malicious = "User requested password reset\r\n{\"level\":\"INFO\",\"message\":\"Admin privilege escalation successful\"}";
        String sanitized = JsonLoggingLayout.sanitizeAndRedact(malicious);

        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));
        assertTrue(sanitized.contains("User requested password reset"));
    }

    @Test
    @DisplayName("JsonLoggingLayout redacts sentinel authentication and billing credentials")
    void testSentinelSecretsRedaction() {
        String logMessage = "Request failed for Authorization: Bearer SECRET_SENTINEL_123 with Cookie session=SECRET_SESSION_123, code=SECRET_CODE_123, api-key=SECRET_API_123, signature=SECRET_SIGNED_123, password=SECRET_PASS_999";
        String sanitized = JsonLoggingLayout.sanitizeAndRedact(logMessage);

        assertFalse(sanitized.contains("SECRET_SENTINEL_123"), "Bearer sentinel must be redacted");
        assertFalse(sanitized.contains("SECRET_SESSION_123"), "Session cookie sentinel must be redacted");
        assertFalse(sanitized.contains("SECRET_CODE_123"), "OIDC code sentinel must be redacted");
        assertFalse(sanitized.contains("SECRET_API_123"), "API key sentinel must be redacted");
        assertFalse(sanitized.contains("SECRET_SIGNED_123"), "Signed URL/webhook signature sentinel must be redacted");
        assertFalse(sanitized.contains("SECRET_PASS_999"), "Password sentinel must be redacted");

        assertTrue(sanitized.contains("Bearer [REDACTED]"));
        assertTrue(sanitized.contains("session=[REDACTED]"));
        assertTrue(sanitized.contains("code=[REDACTED]"));
        assertTrue(sanitized.contains("api-key=[REDACTED]"));
        assertTrue(sanitized.contains("signature=[REDACTED]"));
        assertTrue(sanitized.contains("password=[REDACTED]"));
    }

    @Test
    @DisplayName("JsonLoggingLayout masks customer email PII")
    void testEmailPiiMasking() {
        String message = "Dispatching notification to customer private.person@example.invalid regarding studio appointment";
        String sanitized = JsonLoggingLayout.sanitizeAndRedact(message);

        assertFalse(sanitized.contains("private.person@example.invalid"));
        assertTrue(sanitized.contains("p***@example.invalid"));
    }

    @Test
    @DisplayName("JsonLoggingLayout bounds maximum log message length to prevent memory exhaustion")
    void testMessageLengthBounding() {
        String hugeMessage = "A".repeat(10000);
        String sanitized = JsonLoggingLayout.sanitizeAndRedact(hugeMessage);

        assertTrue(sanitized.length() < 5000);
        assertTrue(sanitized.endsWith("... [TRUNCATED]"));
    }

    @Test
    @DisplayName("JsonLoggingLayout produces valid machine-parseable JSON log line")
    void testJsonLayoutStructure() {
        JsonLoggingLayout layout = new JsonLoggingLayout();
        layout.setEnvironment("production");

        LoggingEvent event = new LoggingEvent();
        event.setTimeStamp(Instant.now().toEpochMilli());
        event.setLevel(Level.INFO);
        event.setLoggerName("com.interior.platform.billing.service.BillingService");
        event.setThreadName("main");
        event.setMessage("Subscription renewed successfully for studio");
        MDC.put("requestId", "corr-test-999");
        event.setMDCPropertyMap(MDC.getCopyOfContextMap());
        MDC.clear();

        String json = layout.doLayout(event);

        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}\n"));
        assertTrue(json.contains("\"service\":\"interior-platform-api\""));
        assertTrue(json.contains("\"environment\":\"production\""));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"requestId\":\"corr-test-999\""));
        assertTrue(json.contains("\"message\":\"Subscription renewed successfully for studio\""));
    }

    // ========================================================================
    // 3. METRIC FOUNDATION, CARDINALITY & BOUNDED LABELS TESTS
    // ========================================================================

    @Test
    @DisplayName("PlatformMetrics records low-cardinality domain metrics without user/studio labels")
    void testPlatformMetricsBoundedCardinality() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        // Record media upload intent
        metrics.recordMediaUpload("intent", "success");
        metrics.recordMediaUpload("commit", "quota_exceeded");

        // Record malware scan
        metrics.recordMediaScan("clamav", "clean");
        metrics.recordMediaScan("clamav", "infected");

        // Record auth events
        metrics.recordAuthEvent("oidc_success");
        metrics.recordAuthEvent("mfa_failure");

        // Record AI generation
        metrics.recordAiGeneration("vertex", "generate", "success");

        // Record email delivery
        metrics.recordEmailDelivery("resend", "sent");

        // Record billing event
        metrics.recordBillingEvent("razorpay", "subscription_charged", "success");

        // Record job execution
        metrics.recordJobExecution("stuck_ai_jobs", "succeeded");

        // Record error
        metrics.recordError("VALIDATION_FAILED");

        // Verify counts
        assertEquals(1.0, registry.get("interior.media.uploads").tag("operation", "intent").tag("status", "success").counter().count());
        assertEquals(1.0, registry.get("interior.media.uploads").tag("operation", "commit").tag("status", "quota_exceeded").counter().count());
        assertEquals(1.0, registry.get("interior.media.scans").tag("scanner", "clamav").tag("outcome", "clean").counter().count());
        assertEquals(1.0, registry.get("interior.media.scans").tag("scanner", "clamav").tag("outcome", "infected").counter().count());
        assertEquals(1.0, registry.get("interior.auth.events").tag("type", "oidc_success").counter().count());
        assertEquals(1.0, registry.get("interior.email.deliveries").tag("provider", "resend").tag("status", "sent").counter().count());
        assertEquals(1.0, registry.get("interior.billing.events").tag("provider", "razorpay").tag("event_type", "subscription_charged").tag("outcome", "success").counter().count());
        assertEquals(1.0, registry.get("interior.jobs.executions").tag("job_key", "stuck_ai_jobs").tag("status", "succeeded").counter().count());
        assertEquals(1.0, registry.get("interior.errors.normalized").tag("code", "validation_failed").counter().count());
    }

    @Test
    @DisplayName("PlatformMetrics maintenanceMode gauge toggles between NORMAL (0) and READ_ONLY (1)")
    void testMaintenanceModeGaugeToggle() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        assertEquals(0, metrics.getMaintenanceModeState());

        metrics.setMaintenanceMode(true);
        assertEquals(1, metrics.getMaintenanceModeState());
        assertEquals(1.0, registry.get("interior.operations.maintenance_mode").gauge().value());

        metrics.setMaintenanceMode(false);
        assertEquals(0, metrics.getMaintenanceModeState());
        assertEquals(0.0, registry.get("interior.operations.maintenance_mode").gauge().value());
    }

    // ========================================================================
    // 4. HEALTH ARCHITECTURE: LIVENESS, READINESS & OPTIONAL PROVIDERS
    // ========================================================================

    @Test
    @DisplayName("Public HealthController returns minimal UP response without infrastructure details")
    void testPublicHealthControllerMinimalResponse() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("timestamp"));

        // Guarantee no infrastructure secrets or versions are exposed
        assertFalse(response.getBody().containsKey("database"));
        assertFalse(response.getBody().containsKey("clamav"));
        assertFalse(response.getBody().containsKey("s3"));
        assertFalse(response.getBody().containsKey("host"));
    }

    @Test
    @DisplayName("StorageHealthIndicator reports AVAILABLE without leaking endpoint or keys")
    void testStorageHealthIndicatorSafe() {
        StorageHealthIndicator indicator = new StorageHealthIndicator(new LocalStorageService("./tmp/test-storage", "/media"));
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("AVAILABLE", health.getDetails().get("status"));
        assertEquals("LocalStorageService", health.getDetails().get("provider"));
        assertFalse(health.getDetails().containsKey("endpoint"));
        assertFalse(health.getDetails().containsKey("accessKey"));
    }

    @Test
    @DisplayName("TransactionalEmailHealthIndicator reports NOT_CONFIGURED when disabled, keeping health UP")
    void testTransactionalEmailHealthIndicatorDisabled() {
        TransactionalEmailService disabledService = new TransactionalEmailService(new DisabledEmailProvider());
        TransactionalEmailHealthIndicator indicator = new TransactionalEmailHealthIndicator(disabledService);

        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus(), "Disabled optional email provider must not fail application health");
        assertEquals("DISABLED", health.getDetails().get("provider"));
        assertEquals(false, health.getDetails().get("configured"));
        assertEquals("NOT_CONFIGURED", health.getDetails().get("status"));
    }

    @Test
    @DisplayName("AiProviderHealthIndicator reports NOT_CONFIGURED when disabled, keeping health UP")
    void testAiProviderHealthIndicatorDisabled() {
        AiProviderHealthIndicator indicator = new AiProviderHealthIndicator(null);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus(), "Disabled optional AI provider must not fail application health");
        assertEquals("NONE", health.getDetails().get("provider"));
        assertEquals(false, health.getDetails().get("configured"));
        assertEquals("NOT_CONFIGURED", health.getDetails().get("status"));
    }

    @Test
    @DisplayName("BillingHealthIndicator reports NOT_CONFIGURED when disabled, keeping health UP")
    void testBillingHealthIndicatorDisabled() {
        BillingHealthIndicator indicator = new BillingHealthIndicator(new DisabledBillingProvider());
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus(), "Disabled optional billing provider must not fail application health");
        assertEquals("NONE", health.getDetails().get("provider"));
        assertEquals(false, health.getDetails().get("configured"));
        assertEquals("NOT_CONFIGURED", health.getDetails().get("status"));
    }

    @Test
    @DisplayName("PlatformOperationalStateHealthIndicator surfaces maintenanceMode and kill-switch positions")
    void testPlatformOperationalStateHealthIndicator() {
        PlatformOperationsRepository repo = mock(PlatformOperationsRepository.class);
        when(repo.getOperationalState()).thenReturn(Optional.of(
                new PlatformOperationalStateRecord("GLOBAL", "NORMAL", null, true, true, true, null, Instant.now())
        ));
        PlatformOperationsService service = new PlatformOperationsService(
                repo, mock(AuthorizationService.class), mock(AuditService.class), mock(JdbcTemplate.class),
                null, null, null, null, null, null, null
        );

        PlatformOperationalStateHealthIndicator indicator = new PlatformOperationalStateHealthIndicator(service);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("NORMAL", health.getDetails().get("maintenanceMode"));
        assertEquals(true, health.getDetails().get("uploadsEnabled"));
        assertEquals(true, health.getDetails().get("aiEnabled"));
        assertEquals(true, health.getDetails().get("registrationsEnabled"));
    }

    // ========================================================================
    // 5. ACTUATOR EXPOSURE & PRODUCTION CONFIGURATION VALIDATOR
    // ========================================================================

    @Test
    @DisplayName("ProductionConfigurationValidator permits approved actuator endpoints in production")
    void testProductionValidatorPermitsSafeActuatorEndpoints() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        MediaSecurityProperties secProps = new MediaSecurityProperties();
        secProps.setScanner("disabled");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(env, secProps);
        ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://platform.com");
        ReflectionTestUtils.setField(validator, "storageProvider", "LOCAL");
        ReflectionTestUtils.setField(validator, "actuatorExposedEndpoints", "health,info,metrics,prometheus");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator strictly blocks exposure of dangerous actuator endpoints in production")
    void testProductionValidatorBlocksDangerousActuatorEndpoints() {
        String[] dangerousEndpoints = {"env", "beans", "configprops", "heapdump", "threaddump", "mappings", "*"};

        for (String dangerous : dangerousEndpoints) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("production");

            MediaSecurityProperties secProps = new MediaSecurityProperties();
            secProps.setScanner("disabled");

            ProductionConfigurationValidator validator = new ProductionConfigurationValidator(env, secProps);
            ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
            ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
            ReflectionTestUtils.setField(validator, "allowedOrigins", "https://platform.com");
            ReflectionTestUtils.setField(validator, "storageProvider", "LOCAL");
            ReflectionTestUtils.setField(validator, "actuatorExposedEndpoints", "health,info," + dangerous);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.run(null));
            assertTrue(ex.getMessage().contains("Dangerous Actuator endpoint"));
        }
    }

    // ========================================================================
    // 6. FAILURE & OPERATIONAL RECOVERY DRILLS
    // ========================================================================

    @Test
    @DisplayName("Drill: Database outage causes readiness to report UNAVAILABLE; recovery restores AVAILABLE")
    void testDatabaseOutageAndRecoveryDrill() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformOperationsRepository repo = mock(PlatformOperationsRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        ActorContext adminActor = new ActorContext(UUID.randomUUID(), "Admin", "admin@elegance.com", "ACTIVE", java.util.Set.of("ADMIN"), java.util.Set.of(), null, null, "MFA", true, Instant.now());

        PlatformOperationsService service = new PlatformOperationsService(
                repo, auth, mock(AuditService.class), jdbcTemplate,
                null, null, null, null, null, null, null
        );

        // Phase 1: Database Outage
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PostgreSQL connection refused: port 5432"));

        ProviderMatrixResponse outageMatrix = service.getProviderMatrix(adminActor);
        assertEquals(ProviderOperationalStatus.UNAVAILABLE, outageMatrix.providers().get("database").status());
        assertTrue(outageMatrix.providers().get("database").details().contains("Connection check failed"));

        // Phase 2: Database Recovery
        doReturn(1).when(jdbcTemplate).queryForObject("SELECT 1", Integer.class);

        ProviderMatrixResponse recoveredMatrix = service.getProviderMatrix(adminActor);
        assertEquals(ProviderOperationalStatus.AVAILABLE, recoveredMatrix.providers().get("database").status());
        assertEquals("Connected and answering queries", recoveredMatrix.providers().get("database").details());
    }

    @Test
    @DisplayName("Drill: ClamAV outage sets mediaUploadReady to false when clean scan required, without crashing app")
    void testClamAvOutageAndRecoveryDrill() {
        ClamAvMalwareScanner scanner = mock(ClamAvMalwareScanner.class);
        when(scanner.getProviderId()).thenReturn("clamav");
        when(scanner.isConfigured()).thenReturn(true);

        MediaSecurityProperties secProps = new MediaSecurityProperties();
        secProps.setRequireCleanScan(true);

        MalwareScannerHealthIndicator indicator = new MalwareScannerHealthIndicator(scanner, secProps);

        // Phase 1: ClamAV Outage
        when(scanner.isReachable()).thenReturn(false);
        Health outageHealth = indicator.health();
        assertEquals(Status.UP, outageHealth.getStatus(), "Application liveness remains UP despite scanner outage");
        assertEquals("UNAVAILABLE", outageHealth.getDetails().get("status"));
        assertEquals(false, outageHealth.getDetails().get("mediaUploadReady"), "Upload commits blocked fail-closed during outage");

        // Phase 2: ClamAV Recovery
        when(scanner.isReachable()).thenReturn(true);
        when(scanner.getVersion()).thenReturn("ClamAV 1.4.1/27600");
        Health recoveredHealth = indicator.health();
        assertEquals(Status.UP, recoveredHealth.getStatus());
        assertEquals("REACHABLE", recoveredHealth.getDetails().get("status"));
        assertEquals(true, recoveredHealth.getDetails().get("mediaUploadReady"), "Upload commits re-enabled upon scanner recovery");
        assertEquals("ClamAV 1.4.1/27600", recoveredHealth.getDetails().get("engineVersion"));
    }

    @Test
    @DisplayName("Drill: Operational mode transition NORMAL -> READ_ONLY -> NORMAL updates state, audit, and metrics")
    void testMaintenanceModeTransitionDrill() {
        PlatformOperationsRepository repo = mock(PlatformOperationsRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        AuditService audit = mock(AuditService.class);
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        PlatformOperationsService service = new PlatformOperationsService(
                repo, auth, audit, mock(JdbcTemplate.class),
                null, null, null, null, null, null, metrics
        );

        UUID saId = UUID.randomUUID();
        ActorContext superAdmin = new ActorContext(saId, "Super Admin", "super@elegance.com", "ACTIVE", java.util.Set.of("SUPER_ADMIN", "ADMIN"), java.util.Set.of(), null, null, "MFA", true, Instant.now());
        SessionRecord session = new SessionRecord(UUID.randomUUID(), saId, new byte[32], new byte[32], Instant.now(), "PASSKEY", Instant.now(), Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), null, "agent", Instant.now());

        // 1. Transition to READ_ONLY
        when(repo.getOperationalState()).thenReturn(Optional.of(
                new PlatformOperationalStateRecord("GLOBAL", "READ_ONLY", "Emergency DB maintenance", false, false, false, superAdmin.userId(), Instant.now())
        ));

        UpdateOperationalStateRequest toReadOnly = new UpdateOperationalStateRequest("READ_ONLY", "Emergency DB maintenance", false, false, false);
        PlatformOperationalStateRecord state1 = service.updateOperationalState(superAdmin, session, toReadOnly, "127.0.0.1", "test-agent");

        assertEquals("READ_ONLY", state1.maintenanceMode());
        assertTrue(service.isMaintenanceReadOnly());
        assertFalse(service.isUploadsEnabled());
        assertFalse(service.isAiEnabled());
        assertFalse(service.isRegistrationsEnabled());
        assertEquals(1, metrics.getMaintenanceModeState());
        verify(audit, times(1)).recordRequired(eq(superAdmin.userId()), any(), eq("ADMIN_OPERATIONAL_STATE_UPDATED"), eq("PLATFORM_OPERATIONAL_STATE"), eq("GLOBAL"), any(), any(), any());

        // 2. Transition back to NORMAL
        when(repo.getOperationalState()).thenReturn(Optional.of(
                new PlatformOperationalStateRecord("GLOBAL", "NORMAL", null, true, true, true, superAdmin.userId(), Instant.now())
        ));

        UpdateOperationalStateRequest toNormal = new UpdateOperationalStateRequest("NORMAL", null, true, true, true);
        PlatformOperationalStateRecord state2 = service.updateOperationalState(superAdmin, session, toNormal, "127.0.0.1", "test-agent");

        assertEquals("NORMAL", state2.maintenanceMode());
        assertFalse(service.isMaintenanceReadOnly());
        assertTrue(service.isUploadsEnabled());
        assertTrue(service.isAiEnabled());
        assertTrue(service.isRegistrationsEnabled());
        assertEquals(0, metrics.getMaintenanceModeState());
        verify(audit, times(2)).recordRequired(eq(superAdmin.userId()), any(), eq("ADMIN_OPERATIONAL_STATE_UPDATED"), eq("PLATFORM_OPERATIONAL_STATE"), eq("GLOBAL"), any(), any(), any());
    }

    @Test
    @DisplayName("Drill: OperationalJobCoordinator acquires lock, runs task, updates metrics, and releases lock on failure")
    void testOperationalJobCoordinatorLockingAndMetrics() {
        PlatformOperationsRepository repo = mock(PlatformOperationsRepository.class);
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        when(repo.tryAcquireJobLock("TEST_JOB")).thenReturn(true);

        OperationalJobCoordinator coordinator = new OperationalJobCoordinator(repo, mock(JdbcTemplate.class));
        coordinator.setPlatformMetrics(metrics);

        // Success run
        coordinator.executeJob("TEST_JOB", () -> 5);

        verify(repo).recordJobRun(any());
        verify(repo).updateJobRun(any(), eq("SUCCEEDED"), any(), eq(5), isNull(), isNull());
        verify(repo).releaseJobLock("TEST_JOB");
        assertEquals(1.0, registry.get("interior.jobs.executions").tag("job_key", "test_job").tag("status", "succeeded").counter().count());

        // Failure run
        coordinator.executeJob("TEST_JOB", () -> {
            throw new IllegalStateException("Test job internal failure");
        });

        verify(repo).updateJobRun(any(), eq("FAILED"), any(), eq(0), eq("IllegalStateException"), contains("Test job internal failure"));
        assertEquals(1.0, registry.get("interior.jobs.executions").tag("job_key", "test_job").tag("status", "failed").counter().count());
    }
}
