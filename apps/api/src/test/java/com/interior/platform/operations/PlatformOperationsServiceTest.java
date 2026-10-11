package com.interior.platform.operations;

import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.media.security.scanner.FakeMalwareScanner;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.operations.domain.*;
import com.interior.platform.operations.dto.*;
import com.interior.platform.operations.repository.PlatformOperationsRepository;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.mfa.MfaService;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9G — Platform Operations Service Unit Tests")
class PlatformOperationsServiceTest {

    @Mock private PlatformOperationsRepository operationsRepository;
    @Mock private AuditService auditService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MfaService mfaService;
    @Mock private StorageService storageService;
    @Mock private TransactionalEmailService emailService;
    @Mock private AiImageProvider aiImageProvider;

    private AuthorizationService authorizationService;
    private FakeMalwareScanner malwareScanner;
    private PlatformOperationsService operationsService;

    private ActorContext superAdminActor;
    private ActorContext adminActor;
    private ActorContext normalUserActor;
    private SessionRecord validSession;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        malwareScanner = new FakeMalwareScanner("FakeAV-v2.1");
        operationsService = new PlatformOperationsService(
                operationsRepository,
                authorizationService,
                auditService,
                jdbcTemplate,
                mfaService,
                storageService,
                emailService,
                aiImageProvider,
                malwareScanner
        );

        UUID saId = UuidV7.randomUuid();
        superAdminActor = new ActorContext(saId, "Super Admin", "super@elegance.com", "ACTIVE", Set.of("SUPER_ADMIN", "ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());
        adminActor = new ActorContext(UuidV7.randomUuid(), "Admin Jane", "admin@elegance.com", "ACTIVE", Set.of("ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());
        normalUserActor = new ActorContext(UuidV7.randomUuid(), "User", "user@elegance.com", "ACTIVE", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true, null);

        validSession = new SessionRecord(
                UuidV7.randomUuid(),
                saId,
                new byte[32],
                new byte[32],
                Instant.now(),
                "PASSKEY",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                null,
                "agent",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Kill switches and maintenance mode reflect repository state")
    void operationalState_readers() {
        PlatformOperationalStateRecord state = new PlatformOperationalStateRecord(
                "GLOBAL", "READ_ONLY", "Scheduled maintenance", false, false, false, superAdminActor.userId(), Instant.now()
        );
        when(operationsRepository.getOperationalState()).thenReturn(Optional.of(state));

        assertTrue(operationsService.isMaintenanceReadOnly());
        assertFalse(operationsService.isUploadsEnabled());
        assertFalse(operationsService.isAiEnabled());
        assertFalse(operationsService.isRegistrationsEnabled());
    }

    @Test
    @DisplayName("Update operational state requires SUPER_ADMIN and fresh MFA step-up")
    void updateOperationalState_superAdminWithMfa_success() {
        PlatformOperationalStateRecord state = new PlatformOperationalStateRecord(
                "GLOBAL", "NORMAL", null, true, true, true, superAdminActor.userId(), Instant.now()
        );
        when(operationsRepository.getOperationalState()).thenReturn(Optional.of(state));

        UpdateOperationalStateRequest req = new UpdateOperationalStateRequest("NORMAL", null, true, true, true);
        PlatformOperationalStateRecord result = operationsService.updateOperationalState(
                superAdminActor, validSession, req, "127.0.0.1", "TestAgent"
        );

        assertNotNull(result);
        verify(mfaService).requireFreshStepUp(eq(superAdminActor), eq(validSession));
        verify(operationsRepository).updateOperationalState(eq("NORMAL"), isNull(), eq(true), eq(true), eq(true), eq(superAdminActor.userId()));
        verify(auditService).recordRequired(eq(superAdminActor.userId()), isNull(), eq("ADMIN_OPERATIONAL_STATE_UPDATED"), eq("PLATFORM_OPERATIONAL_STATE"), eq("GLOBAL"), any(), any(), any());
    }

    @Test
    @DisplayName("Regular admin cannot update platform operational state (requires SUPER_ADMIN)")
    void updateOperationalState_regularAdmin_throws() {
        UpdateOperationalStateRequest req = new UpdateOperationalStateRequest("READ_ONLY", "Lockdown", false, false, false);
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                operationsService.updateOperationalState(adminActor, validSession, req, "127.0.0.1", "TestAgent")
        );
        verify(operationsRepository, never()).updateOperationalState(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Provider matrix reports truthful statuses without fabricating availability")
    void getProviderMatrix_truthfulReporting() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(emailService.isConfigured()).thenReturn(false);
        when(aiImageProvider.isConfigured()).thenReturn(false);

        ProviderMatrixResponse matrix = operationsService.getProviderMatrix(adminActor);

        assertNotNull(matrix);
        assertEquals(ProviderOperationalStatus.AVAILABLE, matrix.providers().get("database").status());
        assertEquals(ProviderOperationalStatus.NOT_CONFIGURED, matrix.providers().get("email").status());
        assertEquals(ProviderOperationalStatus.NOT_CONFIGURED, matrix.providers().get("ai").status());
        assertEquals(ProviderOperationalStatus.AVAILABLE, matrix.providers().get("malwareScanner").status());
        assertEquals(ProviderOperationalStatus.NOT_CONFIGURED, matrix.providers().get("payment").status());
    }
}
