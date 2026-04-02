package com.ujax.domain.mail;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MailOutboxTest {

	private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 2, 12, 0);
	private static final int MAX_ATTEMPTS = 3;

	@Nested
	@DisplayName("MailOutbox 생성")
	class CreateMailOutbox {

		@Test
		@DisplayName("생성 시 PENDING 상태와 기본값을 가진다")
		void createPending() {
			MailOutbox outbox = MailOutbox.create(
				MailType.SIGNUP_VERIFICATION,
				"user@example.com",
				"{\"requestToken\":\"token\"}",
				BASE_TIME
			);

			assertThat(outbox).extracting(
				"mailType",
				"recipientEmail",
				"payloadJson",
				"status",
				"attemptNo",
				"nextAttemptAt",
				"sentAt",
				"lastError"
			).containsExactly(
				MailType.SIGNUP_VERIFICATION,
				"user@example.com",
				"{\"requestToken\":\"token\"}",
				MailOutboxStatus.PENDING,
				0,
				BASE_TIME,
				null,
				null
			);
		}
	}

	@Nested
	@DisplayName("MailOutbox 상태 전이")
	class TransitionMailOutbox {

		@Test
		@DisplayName("PENDING 상태에서 PROCESSING으로 전환할 수 있다")
		void markProcessing() {
			MailOutbox outbox = createPendingOutbox();

			outbox.markProcessing();

			assertThat(outbox).extracting("status", "attemptNo")
				.containsExactly(MailOutboxStatus.PROCESSING, 1);
		}

		@Test
		@DisplayName("PROCESSING 상태에서 retry 를 예약할 수 있다")
		void scheduleRetry() {
			MailOutbox outbox = createPendingOutbox();
			outbox.markProcessing();
			LocalDateTime retryAt = BASE_TIME.plusMinutes(1);

			outbox.scheduleRetry(retryAt, "smtp timeout");

			assertThat(outbox).extracting("status", "attemptNo", "nextAttemptAt", "lastError")
				.containsExactly(MailOutboxStatus.PENDING, 1, retryAt, "smtp timeout");
		}

		@Test
		@DisplayName("PROCESSING 상태에서 성공 처리할 수 있다")
		void markSent() {
			MailOutbox outbox = createPendingOutbox();
			outbox.markProcessing();
			LocalDateTime sentAt = BASE_TIME.plusSeconds(30);

			outbox.markSent(sentAt);

			assertThat(outbox).extracting("status", "sentAt", "lastError")
				.containsExactly(MailOutboxStatus.SENT, sentAt, null);
		}

		@Test
		@DisplayName("PROCESSING 상태에서 최종 실패 처리할 수 있다")
		void markFailed() {
			MailOutbox outbox = createPendingOutbox();
			outbox.markProcessing();

			outbox.markFailed("smtp rejected");

			assertThat(outbox).extracting("status", "attemptNo", "lastError")
				.containsExactly(MailOutboxStatus.FAILED, 1, "smtp rejected");
		}

		@Test
		@DisplayName("stuck processing 은 다시 PENDING 으로 복구할 수 있다")
		void recoverToPending() {
			MailOutbox outbox = createPendingOutbox();
			outbox.markProcessing();
			LocalDateTime recoveredAt = BASE_TIME.plusMinutes(2);

			outbox.recoverToPending(recoveredAt);

			assertThat(outbox).extracting("status", "attemptNo", "nextAttemptAt")
				.containsExactly(MailOutboxStatus.PENDING, 1, recoveredAt);
		}

		@Test
		@DisplayName("현재 attemptNo 가 maxAttempts 이상이면 재시도 소진 상태다")
		void isRetryExhausted() {
			MailOutbox outbox = createPendingOutbox();
			for (int i = 0; i < MAX_ATTEMPTS; i++) {
				outbox.markProcessing();
				if (i < MAX_ATTEMPTS - 1) {
					outbox.scheduleRetry(BASE_TIME.plusMinutes(i + 1), "temporary failure");
				}
			}

			assertThat(outbox.isRetryExhausted(MAX_ATTEMPTS)).isTrue();
		}
	}

	@Nested
	@DisplayName("MailOutbox 예외 케이스")
	class InvalidTransitionMailOutbox {

		@Test
		@DisplayName("PROCESSING 이 아닌 상태에서 retry 를 예약할 수 없다")
		void scheduleRetryOnlyProcessing() {
			MailOutbox outbox = createPendingOutbox();

			assertThatThrownBy(() -> outbox.scheduleRetry(BASE_TIME.plusMinutes(1), "smtp timeout"))
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DisplayName("maxAttempts 는 0 이하일 수 없다")
		void maxAttemptsMustBePositive() {
			MailOutbox outbox = createPendingOutbox();

			assertThatThrownBy(() -> outbox.isRetryExhausted(0))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	private MailOutbox createPendingOutbox() {
		return MailOutbox.create(
			MailType.SIGNUP_VERIFICATION,
			"user@example.com",
			"{\"requestToken\":\"token\"}",
			BASE_TIME
		);
	}
}
