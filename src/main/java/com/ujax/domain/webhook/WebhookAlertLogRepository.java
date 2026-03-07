package com.ujax.domain.webhook;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookAlertLogRepository extends JpaRepository<WebhookAlertLog, Long> {

	List<WebhookAlertLog> findAllByWorkspaceProblemIdOrderByCreatedAtAsc(Long workspaceProblemId);
}
