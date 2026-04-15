package com.ujax.domain.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookAlertRepository extends JpaRepository<WebhookAlert, Long> {

	Optional<WebhookAlert> findByWorkspaceProblemId(Long workspaceProblemId);

	@Query(
		value = """
			SELECT *
			FROM webhook_alert
			WHERE status = 'PENDING'
			  AND scheduled_at <= :now
			ORDER BY scheduled_at ASC, webhook_alert_id ASC
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""",
		nativeQuery = true
	)
	List<WebhookAlert> findDuePendingAlertsForUpdate(@Param("now") LocalDateTime now, @Param("limit") int limit);

	List<WebhookAlert> findAllByStatusAndUpdatedAtBefore(WebhookAlertStatus status, LocalDateTime updatedAt);
}
