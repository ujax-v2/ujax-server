package com.ujax.infrastructure.scheduling.auth;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ujax.application.auth.PendingSignupCleanupService;
import com.ujax.infrastructure.config.auth.PendingSignupCleanupSchedulerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth.pending-signup-cleanup", name = "enabled", havingValue = "true")
public class PendingSignupCleanupScheduler {

	private final PendingSignupCleanupService pendingSignupCleanupService;
	private final PendingSignupCleanupSchedulerProperties properties;

	@Scheduled(fixedDelayString = "${app.auth.pending-signup-cleanup.fixed-delay-millis:60000}")
	public void cleanupExpiredPendingSignups() {
		int deletedCount = pendingSignupCleanupService.cleanupExpired(LocalDateTime.now(), properties.batchSize());
		if (deletedCount > 0) {
			log.info("Deleted {} expired pending signups", deletedCount);
		}
	}
}
