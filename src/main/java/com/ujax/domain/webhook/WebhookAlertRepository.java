package com.ujax.domain.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookAlertRepository extends JpaRepository<WebhookAlert, Long> {

	Optional<WebhookAlert> findByWorkspaceProblemId(Long workspaceProblemId);

	List<WebhookAlert> findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
		WebhookAlertStatus status,
		LocalDateTime scheduledAt
	);

	List<WebhookAlert> findAllByStatusAndUpdatedAtBefore(WebhookAlertStatus status, LocalDateTime updatedAt);
}
