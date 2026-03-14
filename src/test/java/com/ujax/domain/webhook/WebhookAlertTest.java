package com.ujax.domain.webhook;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WebhookAlertTest {

	private static final Long WORKSPACE_PROBLEM_ID = 101L;
	private static final Long WORKSPACE_ID = 11L;
	private static final int MAX_ATTEMPTS = 5;

	@Nested
	@DisplayName("WebhookAlert 생성")
	class CreateWebhookAlert {

		@Test
		@DisplayName("생성 시 PENDING 상태와 기본값을 가진다")
		void createPending() {
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 2, 10, 0);

			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, scheduledAt);

			assertThat(alert).extracting(
				"workspaceProblemId",
				"workspaceId",
				"scheduledAt",
				"nextScheduledAt",
				"status",
				"attemptNo"
			).containsExactly(
				WORKSPACE_PROBLEM_ID,
				WORKSPACE_ID,
				scheduledAt,
				null,
				WebhookAlertStatus.PENDING,
				0
			);
		}

		@Test
		@DisplayName("scheduledAt 없이 생성할 수 없다")
		void createWithoutScheduledAt() {
			assertThatThrownBy(() -> WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, null))
				.isInstanceOf(NullPointerException.class);
		}
	}

	@Nested
	@DisplayName("WebhookAlert 상태 전이")
	class TransitionWebhookAlert {

		@Test
		@DisplayName("PENDING 상태에서 PROCESSING으로 전환할 수 있다")
		void markProcessing() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));

			alert.markProcessing();

			assertThat(alert.getStatus()).isEqualTo(WebhookAlertStatus.PROCESSING);
		}

		@Test
		@DisplayName("PENDING 상태에서는 scheduledAt을 즉시 갱신하고 attemptNo를 초기화한다")
		void applyScheduleUpdateImmediatelyWhenPending() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();
			alert.markRetry(LocalDateTime.of(2026, 3, 2, 10, 1), MAX_ATTEMPTS);

			LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 2, 11, 0);
			alert.applyScheduleUpdate(updatedAt);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(updatedAt, null, WebhookAlertStatus.PENDING, 0);
		}

		@Test
		@DisplayName("PROCESSING 상태에서는 nextScheduledAt에만 보류 저장한다")
		void applyScheduleUpdateDeferredWhenProcessing() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();
			LocalDateTime deferredAt = LocalDateTime.of(2026, 3, 2, 11, 0);

			alert.applyScheduleUpdate(deferredAt);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 2, 10, 0),
					deferredAt,
					WebhookAlertStatus.PROCESSING,
					0
				);
		}

		@Test
		@DisplayName("retry 시 attemptNo를 증가시키고 다음 배치 대상으로 되돌린다")
		void markRetry() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime retryAt = LocalDateTime.of(2026, 3, 2, 10, 1);
			alert.markProcessing();

			alert.markRetry(retryAt, MAX_ATTEMPTS);

			assertThat(alert).extracting("status", "attemptNo", "scheduledAt", "nextScheduledAt")
				.containsExactly(WebhookAlertStatus.PENDING, 1, retryAt, null);
		}

		@Test
		@DisplayName("복구 시 nextScheduledAt이 있으면 그 값을 우선 적용한다")
		void recoverToPendingUsesDeferredScheduleFirst() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 2, 11, 0));
			alert.markRetry(LocalDateTime.of(2026, 3, 2, 10, 1), MAX_ATTEMPTS);
			alert.markProcessing();
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 2, 12, 0));

			alert.recoverToPending(LocalDateTime.of(2026, 3, 2, 12, 30));

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 2, 12, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
		}

		@Test
		@DisplayName("복구 시 nextScheduledAt이 없으면 현재 시각 기준으로 PENDING 복귀한다")
		void recoverToPendingUsesFallbackWhenNoDeferredSchedule() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();
			LocalDateTime recoveredAt = LocalDateTime.of(2026, 3, 2, 10, 10);

			alert.recoverToPending(recoveredAt);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(recoveredAt, null, WebhookAlertStatus.PENDING, 0);
		}

		@Test
		@DisplayName("보류된 schedule이 있으면 전송 전 반영할 수 있다")
		void applyDeferredScheduleIfPresent() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 2, 11, 0));

			boolean applied = alert.applyDeferredScheduleIfPresent();

			assertThat(applied).isTrue();
			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 2, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
		}
	}

	@Nested
	@DisplayName("WebhookAlert 예외 케이스")
	class InvalidTransitionWebhookAlert {

		@Test
		@DisplayName("이미 PROCESSING이면 중복 선점할 수 없다")
		void markProcessingOnlyPending() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.markProcessing();

			assertThatThrownBy(alert::markProcessing)
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DisplayName("재시도 횟수가 최대치이면 추가 재시도 전환을 할 수 없다")
		void retryExhausted() {
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime base = LocalDateTime.of(2026, 3, 2, 10, 0);

			for (int i = 0; i < MAX_ATTEMPTS; i++) {
				alert.markProcessing();
				alert.markRetry(base.plusMinutes(i + 1), MAX_ATTEMPTS);
			}
			alert.markProcessing();

			assertThatThrownBy(() -> alert.markRetry(base.plusMinutes(10), MAX_ATTEMPTS))
				.isInstanceOf(IllegalStateException.class);
			assertThat(alert.isRetryExhausted(MAX_ATTEMPTS)).isTrue();
		}
	}
}
