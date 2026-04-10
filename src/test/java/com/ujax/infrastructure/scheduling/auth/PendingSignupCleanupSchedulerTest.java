package com.ujax.infrastructure.scheduling.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ujax.application.auth.PendingSignupCleanupService;
import com.ujax.infrastructure.config.auth.PendingSignupCleanupSchedulerProperties;

class PendingSignupCleanupSchedulerTest {

	@Test
	@DisplayName("scheduler 는 설정된 batch size 로 cleanup service 를 호출한다")
	void cleanupExpiredPendingSignups() {
		PendingSignupCleanupService cleanupService = mock(PendingSignupCleanupService.class);
		PendingSignupCleanupSchedulerProperties properties = new PendingSignupCleanupSchedulerProperties(25);
		PendingSignupCleanupScheduler scheduler = new PendingSignupCleanupScheduler(cleanupService, properties);

		scheduler.cleanupExpiredPendingSignups();

		then(cleanupService).should().cleanupExpired(any(), eq(25));
	}
}
