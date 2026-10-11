package com.interior.platform.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Service — Failure Semantics Verification")
class AuditServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(securityRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("record() swallows repository exception and does not throw (operational best-effort)")
    void record_swallowsRepositoryException() {
        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(securityRepository).recordAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> auditService.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "VIEW_PROJECT",
                "PROJECT",
                UUID.randomUUID().toString(),
                Map.of("note", "routine user action"),
                "127.0.0.1",
                "Mozilla/5.0"
        ));
    }

    @Test
    @DisplayName("recordRequired() throws exception when repository fails to ensure fail-closed transaction rollback")
    void recordRequired_throwsExceptionOnRepositoryFailure() {
        doThrow(new DataIntegrityViolationException("Audit table disk full"))
                .when(securityRepository).recordAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThrows(DataIntegrityViolationException.class, () -> auditService.recordRequired(
                UUID.randomUUID(),
                null,
                "MFA_RESET_BY_ADMIN",
                "USER",
                UUID.randomUUID().toString(),
                Map.of("reason", "Emergency administrative reset"),
                "127.0.0.1",
                "AdminBrowser"
        ));
    }

    @Test
    @DisplayName("recordRequired() successfully persists audit event when repository is healthy")
    void recordRequired_success() {
        assertDoesNotThrow(() -> auditService.recordRequired(
                UUID.randomUUID(),
                null,
                "ADMIN_OPERATIONAL_STATE_UPDATED",
                "PLATFORM_OPERATIONAL_STATE",
                "GLOBAL",
                Map.of("mode", "READ_ONLY"),
                "127.0.0.1",
                "AdminBrowser"
        ));

        verify(securityRepository, times(1)).recordAuditEvent(
                any(), any(), any(), eq("ADMIN_OPERATIONAL_STATE_UPDATED"), eq("PLATFORM_OPERATIONAL_STATE"), any(), any(), any(), any()
        );
    }
}
