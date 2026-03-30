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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class WebhookAlertRepositoryTest {

	@Autowired
	private WebhookAlertRepository webhookAlertRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		webhookAlertRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("PROCESSING 상태이면서 cutoff 이전에 갱신된 alert만 조회한다")
	void findAllByStatusAndUpdatedAtBefore() {
		// given
		WebhookAlert stuckAlert = webhookAlertRepository.saveAndFlush(
			WebhookAlert.create(101L, 11L, LocalDateTime.of(2026, 3, 27, 10, 0))
		);
		stuckAlert.markProcessing();
		webhookAlertRepository.saveAndFlush(stuckAlert);

		WebhookAlert freshAlert = webhookAlertRepository.saveAndFlush(
			WebhookAlert.create(102L, 11L, LocalDateTime.of(2026, 3, 27, 11, 0))
		);
		freshAlert.markProcessing();
		webhookAlertRepository.saveAndFlush(freshAlert);

		WebhookAlert pendingAlert = webhookAlertRepository.saveAndFlush(
			WebhookAlert.create(103L, 11L, LocalDateTime.of(2026, 3, 27, 12, 0))
		);

		LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 3, 27, 9, 0);
		LocalDateTime cutoff = LocalDateTime.of(2026, 3, 27, 9, 30);
		jdbcTemplate.update(
			"UPDATE webhook_alert SET updated_at = ? WHERE webhook_alert_id = ?",
			Timestamp.valueOf(oldUpdatedAt),
			stuckAlert.getId()
		);

		// when
		List<WebhookAlert> result = webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
			WebhookAlertStatus.PROCESSING,
			cutoff
		);

		// then
		assertThat(result)
			.extracting(WebhookAlert::getId)
			.containsExactly(stuckAlert.getId());
		assertThat(pendingAlert.getStatus()).isEqualTo(WebhookAlertStatus.PENDING);
	}

	@Test
	@DisplayName("같은 workspaceProblemId의 alert는 하나만 생성된다")
	void uniqueWorkspaceProblemId() {
		// given
		webhookAlertRepository.saveAndFlush(
			WebhookAlert.create(201L, 21L, LocalDateTime.of(2026, 3, 27, 10, 0))
		);

		// when & then
		assertThatThrownBy(() -> webhookAlertRepository.saveAndFlush(
			WebhookAlert.create(201L, 21L, LocalDateTime.of(2026, 3, 27, 11, 0))
		)).isInstanceOf(DataIntegrityViolationException.class);
	}

}
