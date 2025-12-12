package com.example.accounts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for enabling scheduled tasks for encryption rotation
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Scheduled tasks are enabled via @Scheduled annotations on service methods
}

