package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
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

	private final WebhookAlertRepository webhookAlertRepository;
	private final WebhookAlertLogRepository webhookAlertLogRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WebhookSender webhookSender;

	@Value("${app.ujax.base-url:https://ujax.site}")
	private String baseUrl;

	@Value("${app.webhook-alert.delivery.retry-delay-minutes:1}")
	private int retryDelayMinutes;

	@Value("${app.webhook-alert.delivery.stuck-processing-minutes:10}")
	private int stuckProcessingMinutes;

	@Value("${app.webhook-alert.delivery.max-attempts:5}")
	private int maxAttempts;

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

	@Transactional
	public void recoverStuckProcessing(LocalDateTime now) {
		List<WebhookAlert> stuckAlerts = webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
			WebhookAlertStatus.PROCESSING,
			now.minusMinutes(stuckProcessingMinutes)
		);

		for (WebhookAlert alert : stuckAlerts) {
			WebhookAlertStatus fromStatus = alert.getStatus();
			alert.recoverToPending(now);
			webhookAlertRepository.save(alert);
			saveLog(
				alert,
				WebhookAlertLogEventType.RECOVERED,
				fromStatus,
				alert.getStatus(),
				null,
				now,
				null,
				WebhookAlertLogActorType.SYSTEM,
				null
			);
		}
	}

	@Transactional
	public List<Long> reserveDueAlertIds(LocalDateTime now, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		List<WebhookAlert> dueAlerts = webhookAlertRepository
			.findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(WebhookAlertStatus.PENDING, now)
			.stream()
			.limit(limit)
			.toList();

		for (WebhookAlert alert : dueAlerts) {
			WebhookAlertStatus fromStatus = alert.getStatus();
			alert.markProcessing();
			saveLog(
				alert,
				WebhookAlertLogEventType.PROCESSING_STARTED,
				fromStatus,
				alert.getStatus(),
				null,
				now,
				null,
				WebhookAlertLogActorType.BATCH,
				null
			);
		}

		if (!dueAlerts.isEmpty()) {
			webhookAlertRepository.saveAll(dueAlerts);
		}

		return dueAlerts.stream()
			.map(WebhookAlert::getId)
			.toList();
	}

	@Transactional
	public void deliver(Long alertId, LocalDateTime now) {
		Optional<WebhookAlert> optionalAlert = webhookAlertRepository.findById(alertId);
		if (optionalAlert.isEmpty()) {
			return;
		}

		WebhookAlert alert = optionalAlert.get();
		if (alert.getStatus() != WebhookAlertStatus.PROCESSING) {
			return;
		}

		if (applyDeferredScheduleIfPresent(alert, now)) {
			return;
		}

		String hookUrl = findHookUrl(alert.getWorkspaceId());
		if (!StringUtils.hasText(hookUrl)) {
			deleteWithLog(
				alert,
				WebhookAlertLogEventType.FAILED,
				WebhookAlertLogActorType.BATCH,
				null,
				null,
				now,
				"workspace hookUrl is missing"
			);
			return;
		}

		try {
			webhookSender.send(hookUrl, buildWebhookAlertMessage(alert));

			Optional<WebhookAlert> refreshedAlert = webhookAlertRepository.findById(alertId);
			if (refreshedAlert.isEmpty()) {
				return;
			}

			WebhookAlert currentAlert = refreshedAlert.get();
			if (applyDeferredScheduleIfPresent(currentAlert, now)) {
				return;
			}
			if (currentAlert.getStatus() != WebhookAlertStatus.PROCESSING) {
				return;
			}

			deleteWithLog(
				currentAlert,
				WebhookAlertLogEventType.DELIVERED,
				WebhookAlertLogActorType.BATCH,
				null,
				now,
				now,
				null
			);
		} catch (RuntimeException exception) {
			Optional<WebhookAlert> refreshedAlert = webhookAlertRepository.findById(alertId);
			if (refreshedAlert.isEmpty()) {
				return;
			}

			WebhookAlert currentAlert = refreshedAlert.get();
			if (applyDeferredScheduleIfPresent(currentAlert, now)) {
				return;
			}
			if (currentAlert.getStatus() != WebhookAlertStatus.PROCESSING) {
				return;
			}
			if (currentAlert.isRetryExhausted(maxAttempts)) {
				deleteWithLog(
					currentAlert,
					WebhookAlertLogEventType.FAILED,
					WebhookAlertLogActorType.BATCH,
					null,
					null,
					now,
					exception.getMessage()
				);
				return;
			}

			WebhookAlertStatus fromStatus = currentAlert.getStatus();
			currentAlert.markRetry(now.plusMinutes(retryDelayMinutes), maxAttempts);
			webhookAlertRepository.save(currentAlert);
			saveLog(
				currentAlert,
				WebhookAlertLogEventType.RETRY_SCHEDULED,
				fromStatus,
				currentAlert.getStatus(),
				null,
				now,
				exception.getMessage(),
				WebhookAlertLogActorType.BATCH,
				null
			);
		}
	}

	private boolean applyDeferredScheduleIfPresent(WebhookAlert alert, LocalDateTime attemptedAt) {
		if (!alert.applyDeferredScheduleIfPresent()) {
			return false;
		}

		webhookAlertRepository.save(alert);
		saveLog(
			alert,
			WebhookAlertLogEventType.SCHEDULE_UPDATED,
			WebhookAlertStatus.PROCESSING,
			alert.getStatus(),
			null,
			attemptedAt,
			null,
			WebhookAlertLogActorType.BATCH,
			null
		);
		return true;
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

	private String findHookUrl(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.map(workspace -> workspace.getHookUrl())
			.orElse(null);
	}

	private WebhookAlertMessage buildWebhookAlertMessage(WebhookAlert alert) {
		String workspaceName = workspaceRepository.findById(alert.getWorkspaceId())
			.map(workspace -> workspace.getName())
			.orElse("워크스페이스 #" + alert.getWorkspaceId());

		Optional<WorkspaceProblem> optionalWorkspaceProblem = workspaceProblemRepository.findById(
			alert.getWorkspaceProblemId()
		);
		if (optionalWorkspaceProblem.isEmpty()) {
			return new WebhookAlertMessage(
				alert.getWorkspaceProblemId(),
				alert.getWorkspaceId(),
				workspaceName,
				"문제 #" + alert.getWorkspaceProblemId(),
				null,
				alert.getScheduledAt(),
				buildWorkspaceLink(alert.getWorkspaceId())
			);
		}

		WorkspaceProblem workspaceProblem = optionalWorkspaceProblem.get();
		return new WebhookAlertMessage(
			alert.getWorkspaceProblemId(),
			alert.getWorkspaceId(),
			workspaceName,
			"%d. %s".formatted(
				workspaceProblem.getProblem().getProblemNumber(),
				workspaceProblem.getProblem().getTitle()
			),
			workspaceProblem.getDeadline(),
			alert.getScheduledAt(),
			buildWorkspaceProblemLink(
				alert.getWorkspaceId(),
				workspaceProblem.getProblemBox().getId(),
				workspaceProblem.getId()
			)
		);
	}

	private String buildWorkspaceLink(Long workspaceId) {
		return "%s/workspaces/%d".formatted(baseUrl, workspaceId);
	}

	private String buildWorkspaceProblemLink(Long workspaceId, Long problemBoxId, Long workspaceProblemId) {
		return "%s/workspaces/%d/problem-boxes/%d/problems/%d"
			.formatted(baseUrl, workspaceId, problemBoxId, workspaceProblemId);
	}

}
