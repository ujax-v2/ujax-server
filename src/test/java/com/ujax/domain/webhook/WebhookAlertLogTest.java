package com.ujax.domain.webhook;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebhookAlertLogTest {

	@Test
	@DisplayName("alert snapshot으로 로그를 생성할 수 있다")
	void createFromAlertSnapshot() {
		WebhookAlert alert = WebhookAlert.create(101L, 11L, LocalDateTime.of(2026, 3, 2, 10, 0));
		alert.markProcessing();
		alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 2, 11, 0));
		LocalDateTime attemptedAt = LocalDateTime.of(2026, 3, 2, 10, 5);

		WebhookAlertLog log = WebhookAlertLog.fromAlert(
			alert,
			WebhookAlertLogEventType.PROCESSING_STARTED,
			WebhookAlertStatus.PENDING,
			WebhookAlertStatus.PROCESSING,
			null,
			attemptedAt,
			null,
			WebhookAlertLogActorType.BATCH,
			null
		);

		assertThat(log).extracting(
			"webhookAlertId",
			"workspaceProblemId",
			"workspaceId",
			"eventType",
			"fromStatus",
			"toStatus",
			"scheduledAt",
			"nextScheduledAt",
			"attemptNo",
			"sendAt",
			"lastAttemptAt",
			"errMsg",
			"actorType",
			"actorId"
		).containsExactly(
			null,
			101L,
			11L,
			WebhookAlertLogEventType.PROCESSING_STARTED,
			WebhookAlertStatus.PENDING,
			WebhookAlertStatus.PROCESSING,
			LocalDateTime.of(2026, 3, 2, 10, 0),
			LocalDateTime.of(2026, 3, 2, 11, 0),
			0,
			null,
			attemptedAt,
			null,
			WebhookAlertLogActorType.BATCH,
			null
		);
	}

	@Test
	@DisplayName("errMsg는 255자로 잘라 저장한다")
	void truncateErrMsg() {
		String longErrMsg = "x".repeat(300);

		WebhookAlertLog log = WebhookAlertLog.create(
			null,
			101L,
			11L,
			WebhookAlertLogEventType.FAILED,
			WebhookAlertStatus.PROCESSING,
			null,
			LocalDateTime.of(2026, 3, 2, 10, 0),
			null,
			5,
			null,
			LocalDateTime.of(2026, 3, 2, 10, 5),
			longErrMsg,
			WebhookAlertLogActorType.SYSTEM,
			null
		);

		assertThat(log.getErrMsg()).hasSize(255);
	}
}
