package com.ujax.domain.webhook;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WebhookAlertTest {

	private static final Long WORKSPACE_PROBLEM_ID = 101L;
	private static final Long WORKSPACE_ID = 11L;

	@Nested
	@DisplayName("WebhookAlert 생성")
	class CreateWebhookAlert {

		@Test
		@DisplayName("생성 시 PENDING 상태와 기본값을 가진다")
		void createPending() {
			// given
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 2, 10, 0);

			// when
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, scheduledAt);

			// then
			assertThat(alert).extracting("workspaceProblemId", "workspaceId", "scheduledAt", "status", "attemptNo",
					"sendAt", "lastAttemptAt", "errMsg")
				.containsExactly(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, scheduledAt, WebhookAlertStatus.PENDING, 0, null, null, null);
		}
	}

	@Nested
	@DisplayName("WebhookAlert 상태 전이")
	class TransitionWebhookAlert {

		@Test
		@DisplayName("PENDING 상태에서 PROCESSING으로 전환할 수 있다")
		void startProcessing() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));

			// when
			alert.startProcessing();

			// then
			assertThat(alert.getStatus()).isEqualTo(WebhookAlertStatus.PROCESSING);
		}

		@Test
		@DisplayName("PROCESSING 상태에서 DONE으로 전환하면 sendAt과 lastAttemptAt을 기록한다")
		void markDone() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime attemptedAt = LocalDateTime.of(2026, 3, 2, 10, 1);
			alert.startProcessing();

			// when
			alert.markDone(attemptedAt);

			// then
			assertThat(alert).extracting("status", "sendAt", "lastAttemptAt", "errMsg")
				.containsExactly(WebhookAlertStatus.DONE, attemptedAt, attemptedAt, null);
		}

		@Test
		@DisplayName("PROCESSING 상태에서 재시도하면 다음 배치 대상(PENDING)으로 돌아가고 attemptNo가 증가한다")
		void markRetry() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime attemptedAt = LocalDateTime.of(2026, 3, 2, 10, 1);
			alert.startProcessing();

			// when
			alert.markRetry(attemptedAt, "timeout");

			// then
			assertThat(alert).extracting("status", "attemptNo", "lastAttemptAt", "scheduledAt", "errMsg")
				.containsExactly(WebhookAlertStatus.PENDING, 1, attemptedAt, attemptedAt, "timeout");
		}

		@Test
		@DisplayName("PROCESSING 상태에서 최종 실패 처리하면 FAILED로 전환되고 sendAt을 기록한다")
		void markFailed() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime attemptedAt = LocalDateTime.of(2026, 3, 2, 10, 5);
			alert.startProcessing();

			// when
			alert.markFailed(attemptedAt, "connection refused");

			// then
			assertThat(alert).extracting("status", "sendAt", "lastAttemptAt", "errMsg")
				.containsExactly(WebhookAlertStatus.FAILED, attemptedAt, attemptedAt, "connection refused");
		}

		@Test
		@DisplayName("PROCESSING 상태가 오래 지속되면 PENDING으로 복구할 수 있다")
		void recoverToPending() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime recoveredAt = LocalDateTime.of(2026, 3, 2, 10, 10);
			alert.startProcessing();

			// when
			alert.recoverToPending(recoveredAt);

			// then
			assertThat(alert).extracting("status", "scheduledAt")
				.containsExactly(WebhookAlertStatus.PENDING, recoveredAt);
		}

		@Test
		@DisplayName("PENDING 상태에서 CANCELLED로 전환할 수 있다")
		void cancelPendingAlert() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));

			// when
			alert.cancel();

			// then
			assertThat(alert.getStatus()).isEqualTo(WebhookAlertStatus.CANCELLED);
		}
	}

	@Nested
	@DisplayName("WebhookAlert 예외 케이스")
	class InvalidTransitionWebhookAlert {

		@Test
		@DisplayName("PENDING이 아닌 상태에서 PROCESSING 전환을 시도하면 예외가 발생한다")
		void startProcessingOnlyPending() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.cancel();

			// when & then
			assertThatThrownBy(alert::startProcessing)
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DisplayName("재시도 횟수가 최대치이면 추가 재시도 전환을 할 수 없다")
		void retryExhausted() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			LocalDateTime base = LocalDateTime.of(2026, 3, 2, 10, 0);

			for (int i = 0; i < WebhookAlert.MAX_ATTEMPT; i++) {
				alert.startProcessing();
				alert.markRetry(base.plusMinutes(i + 1), "err");
			}
			alert.startProcessing();

			// when & then
			assertThatThrownBy(() -> alert.markRetry(base.plusMinutes(10), "err"))
				.isInstanceOf(IllegalStateException.class);
			assertThat(alert.isRetryExhausted()).isTrue();
		}

		@Test
		@DisplayName("PENDING이 아닌 상태에서 CANCELLED 전환을 시도하면 예외가 발생한다")
		void cancelOnlyPending() {
			// given
			WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 2, 10, 0));
			alert.startProcessing();

			// when & then
			assertThatThrownBy(alert::cancel)
				.isInstanceOf(IllegalStateException.class);
		}
	}
}
