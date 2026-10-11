package com.interior.platform.operations;

import com.interior.platform.operations.domain.OperationalJobRunRecord;
import com.interior.platform.operations.repository.PlatformOperationsRepository;
import com.interior.platform.operations.service.OperationalJobCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9G — Operational Job Coordinator Unit Tests")
class OperationalJobCoordinatorTest {

    @Mock private PlatformOperationsRepository operationsRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private OperationalJobCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new OperationalJobCoordinator(operationsRepository, jdbcTemplate);
    }

    @Test
    @DisplayName("Successful job execution logs SUCCEEDED status and updates processed count")
    void executeJob_success() {
        when(operationsRepository.tryAcquireJobLock("TEST_JOB")).thenReturn(true);

        OperationalJobRunRecord run = coordinator.executeJob("TEST_JOB", () -> 15);

        assertNotNull(run);
        assertEquals("SUCCEEDED", run.status());
        assertEquals(15, run.processedCount());
        verify(operationsRepository).recordJobRun(any());
        verify(operationsRepository).updateJobRun(eq(run.id()), eq("SUCCEEDED"), any(), eq(15), isNull(), isNull());
        verify(operationsRepository).releaseJobLock("TEST_JOB");
    }

    @Test
    @DisplayName("Failing job logs FAILED status, records failureCode and error summary")
    void executeJob_failure() {
        when(operationsRepository.tryAcquireJobLock("FAILING_JOB")).thenReturn(true);

        OperationalJobRunRecord run = coordinator.executeJob("FAILING_JOB", () -> {
            throw new IllegalStateException("Database connection timeout during cleanup");
        });

        assertNotNull(run);
        assertEquals("FAILED", run.status());
        assertEquals("IllegalStateException", run.failureCode());
        assertTrue(run.errorSummary().contains("timeout"));
        verify(operationsRepository).updateJobRun(eq(run.id()), eq("FAILED"), any(), eq(0), eq("IllegalStateException"), any());
        verify(operationsRepository).releaseJobLock("FAILING_JOB");
    }

    @Test
    @DisplayName("Job skipped when already locked by another instance")
    void executeJob_lockedByAnotherInstance() {
        when(operationsRepository.tryAcquireJobLock("LOCKED_JOB")).thenReturn(false);

        OperationalJobRunRecord run = coordinator.executeJob("LOCKED_JOB", () -> 100);

        assertNull(run, "Should return null when job is skipped due to existing lock");
        verify(operationsRepository, never()).recordJobRun(any());
    }
}
