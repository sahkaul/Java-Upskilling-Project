package com.example.accounts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import com.example.accounts.util.SecurityUtil;

import java.util.Optional;

/**
 * Active AuditAwareImpl bean that extracts the current username using SecurityUtil
 * This implementation is used for audit trail tracking
 */
@Component("auditAwareImpl")
public class AuditAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String username = SecurityUtil.getCurrentUsername();
        return Optional.ofNullable(username != null ? username : "SYSTEM");
    }
}

