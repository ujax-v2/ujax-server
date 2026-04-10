package com.ujax.domain.mail;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class MailOutboxRepositoryTest {

	@Autowired
	private MailOutboxRepository mailOutboxRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		mailOutboxRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("due 메일을 잠금과 함께 due 순서로 조회한다")
	void findDuePendingOutboxesForUpdate() {
		MailOutbox dueFirst = mailOutboxRepository.saveAndFlush(
			MailOutbox.create(MailType.SIGNUP_VERIFICATION, "first@example.com", "{\"code\":\"111111\"}",
				LocalDateTime.of(2026, 4, 2, 10, 0))
		);
		MailOutbox dueSecond = mailOutboxRepository.saveAndFlush(
			MailOutbox.create(MailType.WORKSPACE_INVITE, "second@example.com", "{\"workspaceId\":1}",
				LocalDateTime.of(2026, 4, 2, 10, 5))
		);
		mailOutboxRepository.saveAndFlush(
			MailOutbox.create(MailType.SIGNUP_VERIFICATION, "later@example.com", "{\"code\":\"222222\"}",
				LocalDateTime.of(2026, 4, 2, 10, 30))
		);

		List<MailOutbox> result = mailOutboxRepository.findDuePendingOutboxesForUpdate(
			LocalDateTime.of(2026, 4, 2, 10, 10),
			10
		);

		assertThat(result)
			.extracting(MailOutbox::getId)
			.containsExactly(dueFirst.getId(), dueSecond.getId());
	}

	@Test
	@DisplayName("PROCESSING 상태이면서 cutoff 이전에 갱신된 row 만 조회한다")
	void findAllByStatusAndUpdatedAtBefore() {
		MailOutbox stuckOutbox = mailOutboxRepository.saveAndFlush(
			MailOutbox.create(MailType.SIGNUP_VERIFICATION, "stuck@example.com", "{\"code\":\"111111\"}",
				LocalDateTime.of(2026, 4, 2, 10, 0))
		);
		stuckOutbox.markProcessing();
		mailOutboxRepository.saveAndFlush(stuckOutbox);

		MailOutbox freshOutbox = mailOutboxRepository.saveAndFlush(
			MailOutbox.create(MailType.WORKSPACE_INVITE, "fresh@example.com", "{\"workspaceId\":1}",
				LocalDateTime.of(2026, 4, 2, 10, 5))
		);
		freshOutbox.markProcessing();
		mailOutboxRepository.saveAndFlush(freshOutbox);

		LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 4, 2, 9, 0);
		LocalDateTime cutoff = LocalDateTime.of(2026, 4, 2, 9, 30);
		jdbcTemplate.update(
			"UPDATE mail_outbox SET updated_at = ? WHERE mail_outbox_id = ?",
			Timestamp.valueOf(oldUpdatedAt),
			stuckOutbox.getId()
		);

		List<MailOutbox> result = mailOutboxRepository.findAllByStatusAndUpdatedAtBefore(
			MailOutboxStatus.PROCESSING,
			cutoff
		);

		assertThat(result)
			.extracting(MailOutbox::getId)
			.containsExactly(stuckOutbox.getId());
	}
}
