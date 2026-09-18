package com.interior.platform.security.service;

import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.repository.SecurityRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;

@Service
public class OidcTransactionStore {

    private final SecurityRepository securityRepository;
    private final Clock clock;

    public OidcTransactionStore(SecurityRepository securityRepository, Clock clock) {
        this.securityRepository = securityRepository;
        this.clock = clock;
    }

    public void save(OidcTransaction transaction) {
        securityRepository.saveOidcTransaction(transaction);
    }

    /**
     * Atomically retrieves and marks the transaction as consumed for one-time use.
     * Replay attacks or expired transactions return empty.
     */
    public Optional<OidcTransaction> take(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        return securityRepository.consumeOidcTransaction(state, clock.instant());
    }
}
