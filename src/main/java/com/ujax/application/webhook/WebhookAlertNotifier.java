package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebhookAlertNotifier {

	private final WebhookAlertRepository webhookAlertRepository;
	private final WebhookAlertEventLogger webhookAlertEventLogger;

	@Transactional
	public void reserveOrUpdate(Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt, Long actorId) {
		Optional<WebhookAlert> optionalAlert = webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId);
		if (optionalAlert.isEmpty()) {
			WebhookAlert createdAlert = webhookAlertRepository.save(
				WebhookAlert.create(workspaceProblemId, workspaceId, scheduledAt)
			);
			webhookAlertEventLogger.logCreated(createdAlert, actorId);
			return;
		}

		WebhookAlert alert = optionalAlert.get();
		WebhookAlertStatus fromStatus = alert.getStatus();
		alert.applyScheduleUpdate(scheduledAt);
		webhookAlertRepository.save(alert);

		if (fromStatus == WebhookAlertStatus.PROCESSING) {
			webhookAlertEventLogger.logDeferredScheduled(alert, fromStatus, actorId);
			return;
		}
		webhookAlertEventLogger.logScheduleUpdated(alert, fromStatus, actorId);
	}

	@Transactional
	public void deactivate(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> {
				webhookAlertEventLogger.logDeactivated(alert, actorId);
				webhookAlertRepository.delete(alert);
			});
	}

	@Transactional
	public void cancel(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> {
				webhookAlertEventLogger.logCancelled(alert, actorId);
				webhookAlertRepository.delete(alert);
			});
	}
}
