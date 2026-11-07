package com.interior.platform.ai;

import com.interior.platform.ai.service.AiVisualizerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import static org.mockito.Mockito.*;

class AiDispatchTransactionTest {
    @Test
    void dispatchWaitsForCommitAndDoesNotRunOnRollback() {
        var service = mock(AiVisualizerService.class, CALLS_REAL_METHODS);
        var executor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(service, "executor", executor);
        TransactionSynchronizationManager.initSynchronization();
        try {
            ReflectionTestUtils.invokeMethod(service, "submitAsyncJob", UUID.randomUUID());
            verifyNoInteractions(executor);
            var callbacks = TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(c -> c.afterCompletion(1)); // rollback never dispatches
            verifyNoInteractions(executor);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.initSynchronization();
        try {
            ReflectionTestUtils.invokeMethod(service, "submitAsyncJob", UUID.randomUUID());
            verifyNoInteractions(executor);
            TransactionSynchronizationManager.getSynchronizations().forEach(c -> c.afterCommit());
            verify(executor).submit(any(Runnable.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
