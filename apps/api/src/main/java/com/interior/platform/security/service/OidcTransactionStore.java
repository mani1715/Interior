package com.interior.platform.security.service;

import com.interior.platform.security.domain.OidcTransaction;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OidcTransactionStore {

    private final Clock clock;
    private final Map<String, OidcTransaction> store = new ConcurrentHashMap<>();

    public OidcTransactionStore(Clock clock) {
        this.clock = clock;
    }

    public void save(OidcTransaction transaction) {
        evictExpired();
        store.put(transaction.state(), transaction);
    }

    /**
     * Atomically retrieves and removes the transaction for one-time use.
     * Returns empty if missing or expired.
     */
    public Optional<OidcTransaction> take(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        OidcTransaction tx = store.remove(state);
        if (tx == null) {
            return Optional.empty();
        }
        if (tx.isExpired(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(tx);
    }

    private void evictExpired() {
        Instant now = clock.instant();
        store.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public void reset() {
        store.clear();
    }
}
