package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertLog;
import com.ujax.domain.webhook.WebhookAlertLogActorType;
import com.ujax.domain.webhook.WebhookAlertLogEventType;
import com.ujax.domain.webhook.WebhookAlertLogRepository;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;
import com.ujax.domain.workspace.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebhookAlertService {

	private static final int RETRY_DELAY_MINUTES = 1;
	private static final int STUCK_PROCESSING_MINUTES = 10;

	private final WebhookAlertRepository webhookAlertRepository;
	private final WebhookAlertLogRepository webhookAlertLogRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WebhookSender webhookSender;

	@Transactional
	public void reserveOrUpdate(Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt, Long actorId) {
		Optional<WebhookAlert> optionalAlert = webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId);
		if (optionalAlert.isEmpty()) {
			WebhookAlert createdAlert = webhookAlertRepository.save(
				WebhookAlert.create(workspaceProblemId, workspaceId, scheduledAt)
			);
			saveLog(
				createdAlert,
				WebhookAlertLogEventType.CREATED,
				null,
				WebhookAlertStatus.PENDING,
				null,
				null,
				null,
				WebhookAlertLogActorType.USER,
				actorId
			);
			return;
		}

		WebhookAlert alert = optionalAlert.get();
		WebhookAlertStatus fromStatus = alert.getStatus();
		alert.applyScheduleUpdate(scheduledAt);
		webhookAlertRepository.save(alert);
		saveLog(
			alert,
			fromStatus == WebhookAlertStatus.PROCESSING
				? WebhookAlertLogEventType.DEFERRED_SCHEDULED
				: WebhookAlertLogEventType.SCHEDULE_UPDATED,
			fromStatus,
			alert.getStatus(),
			null,
			null,
			null,
			WebhookAlertLogActorType.USER,
			actorId
		);
	}

	@Transactional
	public void deactivate(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> deleteWithLog(
				alert,
				WebhookAlertLogEventType.DEACTIVATED,
				WebhookAlertLogActorType.USER,
				actorId,
				null,
				null,
				null
			));
	}

	@Transactional
	public void cancel(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> deleteWithLog(
				alert,
				WebhookAlertLogEventType.CANCELLED,
				WebhookAlertLogActorType.USER,
				actorId,
				null,
				null,
				null
			));
	}

	private void deleteWithLog(
		WebhookAlert alert,
		WebhookAlertLogEventType eventType,
		WebhookAlertLogActorType actorType,
		Long actorId,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg
	) {
		saveLog(alert, eventType, alert.getStatus(), null, sendAt, lastAttemptAt, errMsg, actorType, actorId);
		webhookAlertRepository.delete(alert);
	}

	private void saveLog(
		WebhookAlert alert,
		WebhookAlertLogEventType eventType,
		WebhookAlertStatus fromStatus,
		WebhookAlertStatus toStatus,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg,
		WebhookAlertLogActorType actorType,
		Long actorId
	) {
		WebhookAlertLog log = WebhookAlertLog.fromAlert(
			alert,
			eventType,
			fromStatus,
			toStatus,
			sendAt,
			lastAttemptAt,
			errMsg,
			actorType,
			actorId
		);
		webhookAlertLogRepository.save(log);
	}

}
