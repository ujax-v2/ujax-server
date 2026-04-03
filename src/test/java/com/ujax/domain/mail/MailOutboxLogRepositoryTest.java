package com.ujax.domain.mail;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class MailOutboxLogRepositoryTest {

	@Autowired
	private MailOutboxRepository mailOutboxRepository;

	@Autowired
	private MailOutboxLogRepository mailOutboxLogRepository;

	@BeforeEach
	void setUp() {
		mailOutboxLogRepository.deleteAllInBatch();
		mailOutboxRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("mailOutboxId 기준으로 생성 순서대로 로그를 조회한다")
	void findAllByMailOutboxIdOrderByCreatedAtAsc() {
		MailOutbox outbox = mailOutboxRepository.saveAndFlush(
			MailOutbox.create(
				MailType.SIGNUP_VERIFICATION,
				"user@example.com",
				"{\"code\":\"123456\"}",
				LocalDateTime.of(2026, 4, 3, 10, 0)
			)
		);

		MailOutboxLog enqueued = mailOutboxLogRepository.saveAndFlush(
			MailOutboxLog.fromOutbox(outbox, MailOutboxLogEventType.ENQUEUED, null, MailOutboxStatus.PENDING)
		);
		outbox.markProcessing();
		mailOutboxRepository.saveAndFlush(outbox);
		MailOutboxLog processingStarted = mailOutboxLogRepository.saveAndFlush(
			MailOutboxLog.fromOutbox(
				outbox,
				MailOutboxLogEventType.PROCESSING_STARTED,
				MailOutboxStatus.PENDING,
				MailOutboxStatus.PROCESSING
			)
		);

		List<MailOutboxLog> logs = mailOutboxLogRepository.findAllByMailOutboxIdOrderByCreatedAtAsc(outbox.getId());

		assertThat(logs)
			.extracting(MailOutboxLog::getId)
			.containsExactly(enqueued.getId(), processingStarted.getId());
	}
}
