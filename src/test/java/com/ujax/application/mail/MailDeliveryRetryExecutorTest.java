package com.ujax.application.mail;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import com.ujax.global.exception.common.ExternalApiException;

class MailDeliveryRetryExecutorTest {

	@Test
	@DisplayName("SMTP 실패가 일시적이면 재시도 후 메일 발송을 완료한다")
	void executeRetriesTemporaryMailFailure() {
		MailDeliveryRetryExecutor executor = new MailDeliveryRetryExecutor(new MailDeliveryRetryProperties(3, 0));
		AtomicInteger attempts = new AtomicInteger();

		executor.execute("메일 발송 실패", () -> {
			if (attempts.getAndIncrement() == 0) {
				throw new MailSendException("temporary failure");
			}
		});

		assertThat(attempts).hasValue(2);
	}

	@Test
	@DisplayName("재시도 횟수를 모두 소진하면 ExternalApiException을 던진다")
	void executeThrowsWhenRetryExhausted() {
		MailDeliveryRetryExecutor executor = new MailDeliveryRetryExecutor(new MailDeliveryRetryProperties(2, 0));

		assertThatThrownBy(() -> executor.execute("메일 발송 실패", () -> {
			throw new MailSendException("permanent failure");
		}))
			.isInstanceOf(ExternalApiException.class)
			.hasMessage("메일 발송 실패")
			.cause()
			.isInstanceOf(MailSendException.class)
			.hasMessageContaining("permanent failure");
	}
}
