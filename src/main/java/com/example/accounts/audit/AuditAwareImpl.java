package com.example.accounts.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * DEPRECATED: Use com.example.accounts.config.AuditAwareImpl instead
 * This version is kept for reference only
 */
// @Component("auditAwareImpl")
public class AuditAwareImpl implements AuditorAware<String> {

    /**
     * Returns the current auditor of the application.
     *
     * @return the current auditor.
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("ACCOUNTS_MS");
    }

}
