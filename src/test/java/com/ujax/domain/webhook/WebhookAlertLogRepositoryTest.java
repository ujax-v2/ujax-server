package com.ujax.domain.webhook;

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
class WebhookAlertLogRepositoryTest {

	@Autowired
	private WebhookAlertLogRepository webhookAlertLogRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		webhookAlertLogRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("workspace_problem_id 기준으로 로그를 생성순으로 조회한다")
	void findAllByWorkspaceProblemIdOrderByCreatedAtAsc() {
		// given
		WebhookAlertLog first = webhookAlertLogRepository.saveAndFlush(WebhookAlertLog.create(
			1L, 101L, 11L, WebhookAlertLogEventType.CREATED, null, WebhookAlertStatus.PENDING,
			LocalDateTime.of(2026, 3, 27, 10, 0), null, 0, null, null, null, WebhookAlertLogActorType.USER, 1L
		));
		WebhookAlertLog second = webhookAlertLogRepository.saveAndFlush(WebhookAlertLog.create(
			1L, 101L, 11L, WebhookAlertLogEventType.SCHEDULE_UPDATED, WebhookAlertStatus.PENDING, WebhookAlertStatus.PENDING,
			LocalDateTime.of(2026, 3, 27, 11, 0), null, 0, null, null, null, WebhookAlertLogActorType.USER, 1L
		));

		jdbcTemplate.update(
			"UPDATE webhook_alert_log SET created_at = ? WHERE webhook_alert_log_id = ?",
			Timestamp.valueOf(LocalDateTime.of(2026, 3, 27, 9, 0)),
			first.getId()
		);
		jdbcTemplate.update(
			"UPDATE webhook_alert_log SET created_at = ? WHERE webhook_alert_log_id = ?",
			Timestamp.valueOf(LocalDateTime.of(2026, 3, 27, 9, 5)),
			second.getId()
		);

		// when
		List<WebhookAlertLog> result = webhookAlertLogRepository.findAllByWorkspaceProblemIdOrderByCreatedAtAsc(101L);

		// then
		assertThat(result)
			.extracting(WebhookAlertLog::getId)
			.containsExactly(first.getId(), second.getId());
	}

}
