package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OidcSecurityTest {

    private OidcTransactionStore transactionStore;
    private SecurityRepository securityRepository;
    private AuditService auditService;
    private AuthSecurityProperties properties;
    private Clock clock;
    private OidcService oidcService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);
        transactionStore = new OidcTransactionStore(clock);
        securityRepository = mock(SecurityRepository.class);
        auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        properties = new AuthSecurityProperties();
        oidcService = new OidcService(properties, transactionStore, securityRepository, auditService, clock);
    }

    @Test
    @DisplayName("OIDC state is strictly single-use to prevent replay attacks")
    void testStateSingleUseReplayPrevention() {
        OidcTransaction tx = new OidcTransaction(
                "state-abc",
                "nonce-123",
                "verifier-xyz",
                "/account",
                "CUSTOMER",
                clock.instant().plusSeconds(300)
        );
        transactionStore.save(tx);

        Optional<OidcTransaction> firstTake = transactionStore.take("state-abc");
        assertTrue(firstTake.isPresent());
        assertEquals("nonce-123", firstTake.get().nonce());

        // Second take MUST be empty
        Optional<OidcTransaction> secondTake = transactionStore.take("state-abc");
        assertTrue(secondTake.isEmpty());
    }

    @Test
    @DisplayName("Expired OIDC state is rejected by transaction store")
    void testExpiredStateRejected() {
        OidcTransaction expiredTx = new OidcTransaction(
                "state-expired",
                "nonce-123",
                "verifier-xyz",
                "/account",
                "CUSTOMER",
                clock.instant().minusSeconds(10) // already expired
        );
        transactionStore.save(expiredTx);

        Optional<OidcTransaction> takeOpt = transactionStore.take("state-expired");
        assertTrue(takeOpt.isEmpty());
    }

    @Test
    @DisplayName("New OIDC registration creates user with baseline CUSTOMER role")
    void testNewOidcUserGetsCustomerRole() {
        when(securityRepository.findUserByExternalIdentity(any(), any())).thenReturn(Optional.empty());
        when(securityRepository.findUserByEmail(any())).thenReturn(Optional.empty());

        UserRecord createdUser = oidcService.resolveExternalUser(
                "https://accounts.google.com",
                "sub-123456",
                "newcustomer@example.com",
                "New Customer"
        );

        assertNotNull(createdUser);
        assertEquals("newcustomer@example.com", createdUser.email());
        assertEquals("ACTIVE", createdUser.status());

        // Verify CUSTOMER role assigned
        verify(securityRepository).assignUserRole(any(), eq(createdUser.id()), eq("CUSTOMER"), any());
        // Verify external identity linked
        verify(securityRepository).linkExternalIdentity(any(), eq(createdUser.id()), eq("https://accounts.google.com"), eq("sub-123456"), any());
    }

    @Test
    @DisplayName("Email collision with unlinked account prevents silent account takeover")
    void testEmailCollisionPreventsTakeover() {
        UUID existingUserId = UUID.randomUUID();
        UserRecord existingUser = new UserRecord(
                existingUserId,
                "Legitimate Owner",
                "owner@example.com",
                null,
                "ACTIVE",
                clock.instant().minusSeconds(86400),
                clock.instant().minusSeconds(86400),
                0L
        );

        when(securityRepository.findUserByExternalIdentity("https://accounts.google.com", "malicious-sub"))
                .thenReturn(Optional.empty());
        when(securityRepository.findUserByEmail("owner@example.com"))
                .thenReturn(Optional.of(existingUser));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> oidcService.resolveExternalUser(
                        "https://accounts.google.com",
                        "malicious-sub",
                        "owner@example.com",
                        "Attacker"
                )
        );

        assertTrue(exception.getMessage().contains("An account with this email already exists"));

        // Verify audit event recorded in repository
        verify(securityRepository).recordAuditEvent(
                any(),
                isNull(),
                eq(existingUserId),
                eq("AUTH_EMAIL_COLLISION_REJECTED"),
                eq("USER"),
                eq(existingUserId),
                any(),
                any(),
                any()
        );
    }
}
