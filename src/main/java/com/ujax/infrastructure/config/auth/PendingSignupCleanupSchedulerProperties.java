package com.ujax.infrastructure.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.pending-signup-cleanup")
public record PendingSignupCleanupSchedulerProperties(
	int batchSize
) {
}
