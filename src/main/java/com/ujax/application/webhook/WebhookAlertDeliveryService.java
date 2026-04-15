package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;
import com.ujax.domain.workspace.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebhookAlertDeliveryService {

	private static final int MAX_ERROR_LENGTH = 500;

	private final WebhookAlertRepository webhookAlertRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WebhookAlertMessageResolver webhookAlertMessageResolver;
	private final WebhookSender webhookSender;
	private final WebhookAlertDeliveryProperties properties;
	private final WebhookAlertEventLogger webhookAlertEventLogger;

	@Transactional
	public void recoverStuckProcessing(LocalDateTime now) {
		List<WebhookAlert> stuckAlerts = webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
			WebhookAlertStatus.PROCESSING,
			now.minusMinutes(properties.stuckProcessingMinutes())
		);

		for (WebhookAlert alert : stuckAlerts) {
			alert.recoverToPending(now);
			webhookAlertEventLogger.logRecovered(alert);
		}
	}

	@Transactional
	public List<Long> reserveDueAlertIds(LocalDateTime now, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		List<WebhookAlert> dueAlerts = webhookAlertRepository.findDuePendingAlertsForUpdate(now, limit);
		for (WebhookAlert alert : dueAlerts) {
			WebhookAlertStatus fromStatus = alert.getStatus();
			alert.markProcessing();
			webhookAlertEventLogger.logProcessingStarted(alert, fromStatus);
		}

		return dueAlerts.stream()
			.map(WebhookAlert::getId)
			.toList();
	}

	@Transactional
	public void deliver(Long alertId, LocalDateTime now) {
		WebhookAlert alert = webhookAlertRepository.findById(alertId).orElse(null);
		if (alert == null || alert.getStatus() != WebhookAlertStatus.PROCESSING) {
			return;
		}

		if (applyDeferredScheduleIfPresent(alert, now)) {
			return;
		}

		String hookUrl = workspaceRepository.findById(alert.getWorkspaceId())
			.map(workspace -> workspace.getHookUrl())
			.orElse(null);
		if (!StringUtils.hasText(hookUrl)) {
			recordFailedAndDelete(alert, now, "workspace hookUrl is missing", false);
			return;
		}

		try {
			webhookSender.send(hookUrl, webhookAlertMessageResolver.resolve(alert));

			WebhookAlert currentAlert = webhookAlertRepository.findById(alertId).orElse(null);
			if (currentAlert == null) {
				return;
			}
			if (applyDeferredScheduleIfPresent(currentAlert, now)) {
				return;
			}
			if (currentAlert.getStatus() != WebhookAlertStatus.PROCESSING) {
				return;
			}

			webhookAlertEventLogger.logDelivered(currentAlert, now);
			webhookAlertRepository.delete(currentAlert);
		} catch (RuntimeException exception) {
			handleFailure(alertId, now, exception);
		}
	}

	private void handleFailure(Long alertId, LocalDateTime now, RuntimeException exception) {
		WebhookAlert currentAlert = webhookAlertRepository.findById(alertId).orElse(null);
		if (currentAlert == null) {
			return;
		}
		if (applyDeferredScheduleIfPresent(currentAlert, now)) {
			return;
		}
		if (currentAlert.getStatus() != WebhookAlertStatus.PROCESSING) {
			return;
		}

		String summarizedError = summarizeError(exception);
		boolean retryable = isRetryable(exception);
		if (!retryable || currentAlert.isRetryExhausted(properties.maxAttempts())) {
			recordFailedAndDelete(currentAlert, now, summarizedError, retryable);
			return;
		}

		WebhookAlertStatus fromStatus = currentAlert.getStatus();
		currentAlert.markRetry(
			now.plusMinutes(properties.retryDelayMinutes()),
			properties.maxAttempts(),
			summarizedError
		);
		webhookAlertEventLogger.logRetryScheduled(currentAlert, fromStatus, now);
	}

	private boolean applyDeferredScheduleIfPresent(WebhookAlert alert, LocalDateTime attemptedAt) {
		if (!alert.applyDeferredScheduleIfPresent()) {
			return false;
		}
		webhookAlertEventLogger.logDeferredScheduleApplied(alert, attemptedAt);
		return true;
	}

	private void recordFailedAndDelete(WebhookAlert alert, LocalDateTime lastAttemptAt, String lastError, boolean retryable) {
		webhookAlertEventLogger.logFailed(alert, lastAttemptAt, lastError, retryable);
		webhookAlertRepository.delete(alert);
	}

	private boolean isRetryable(RuntimeException exception) {
		if (exception instanceof ResourceAccessException) {
			return true;
		}
		if (exception instanceof HttpStatusCodeException httpException) {
			HttpStatusCode statusCode = httpException.getStatusCode();
			return statusCode.is5xxServerError() || statusCode.value() == 429;
		}
		return false;
	}

	private String summarizeError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}
		if (message.length() <= MAX_ERROR_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_LENGTH);
	}
}
