package com.ujax.application.webhook;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebhookAlertEventLogger {

	private static final String EVENT_CREATED = "CREATED";
	private static final String EVENT_SCHEDULE_UPDATED = "SCHEDULE_UPDATED";
	private static final String EVENT_DEFERRED_SCHEDULED = "DEFERRED_SCHEDULED";
	private static final String EVENT_DEACTIVATED = "DEACTIVATED";
	private static final String EVENT_CANCELLED = "CANCELLED";
	private static final String EVENT_PROCESSING_STARTED = "PROCESSING_STARTED";
	private static final String EVENT_DELIVERED = "DELIVERED";
	private static final String EVENT_RETRY_SCHEDULED = "RETRY_SCHEDULED";
	private static final String EVENT_FAILED = "FAILED";
	private static final String EVENT_RECOVERED = "RECOVERED";
	private static final String ACTOR_USER = "USER";
	private static final String ACTOR_BATCH = "BATCH";
	private static final String ACTOR_SYSTEM = "SYSTEM";

	private static final String LOG_PATTERN =
		"event=webhook_alert eventType={} alertId={} workspaceProblemId={} workspaceId={} fromStatus={} toStatus={} scheduledAt={} nextScheduledAt={} attemptNo={} sendAt={} lastAttemptAt={} lastError={} actorType={} actorId={} retryable={}";

	public void logCreated(WebhookAlert alert, Long actorId) {
		logInfo(EVENT_CREATED, alert, null, WebhookAlertStatus.PENDING, null, null, null, ACTOR_USER, actorId, null);
	}

	public void logScheduleUpdated(WebhookAlert alert, WebhookAlertStatus fromStatus, Long actorId) {
		logInfo(
			EVENT_SCHEDULE_UPDATED,
			alert,
			fromStatus,
			alert.getStatus(),
			null,
			null,
			alert.getLastError(),
			ACTOR_USER,
			actorId,
			null
		);
	}

	public void logDeferredScheduled(WebhookAlert alert, WebhookAlertStatus fromStatus, Long actorId) {
		logInfo(
			EVENT_DEFERRED_SCHEDULED,
			alert,
			fromStatus,
			alert.getStatus(),
			null,
			null,
			alert.getLastError(),
			ACTOR_USER,
			actorId,
			null
		);
	}

	public void logDeactivated(WebhookAlert alert, Long actorId) {
		logInfo(EVENT_DEACTIVATED, alert, alert.getStatus(), null, null, null, alert.getLastError(), ACTOR_USER, actorId, null);
	}

	public void logCancelled(WebhookAlert alert, Long actorId) {
		logInfo(EVENT_CANCELLED, alert, alert.getStatus(), null, null, null, alert.getLastError(), ACTOR_USER, actorId, null);
	}

	public void logRecovered(WebhookAlert alert) {
		logWarn(
			EVENT_RECOVERED,
			alert,
			WebhookAlertStatus.PROCESSING,
			alert.getStatus(),
			null,
			null,
			alert.getLastError(),
			ACTOR_SYSTEM,
			null,
			null
		);
	}

	public void logProcessingStarted(WebhookAlert alert, WebhookAlertStatus fromStatus) {
		logInfo(
			EVENT_PROCESSING_STARTED,
			alert,
			fromStatus,
			alert.getStatus(),
			null,
			null,
			alert.getLastError(),
			ACTOR_BATCH,
			null,
			null
		);
	}

	public void logDeferredScheduleApplied(WebhookAlert alert, LocalDateTime attemptedAt) {
		logInfo(
			EVENT_SCHEDULE_UPDATED,
			alert,
			WebhookAlertStatus.PROCESSING,
			alert.getStatus(),
			null,
			attemptedAt,
			alert.getLastError(),
			ACTOR_BATCH,
			null,
			null
		);
	}

	public void logDelivered(WebhookAlert alert, LocalDateTime sentAt) {
		logInfo(
			EVENT_DELIVERED,
			alert,
			WebhookAlertStatus.PROCESSING,
			null,
			sentAt,
			sentAt,
			null,
			ACTOR_BATCH,
			null,
			null
		);
	}

	public void logRetryScheduled(WebhookAlert alert, WebhookAlertStatus fromStatus, LocalDateTime lastAttemptAt) {
		logWarn(
			EVENT_RETRY_SCHEDULED,
			alert,
			fromStatus,
			alert.getStatus(),
			null,
			lastAttemptAt,
			alert.getLastError(),
			ACTOR_BATCH,
			null,
			true
		);
	}

	public void logFailed(WebhookAlert alert, LocalDateTime lastAttemptAt, String lastError, boolean retryable) {
		logWarn(
			EVENT_FAILED,
			alert,
			WebhookAlertStatus.PROCESSING,
			null,
			null,
			lastAttemptAt,
			lastError,
			ACTOR_BATCH,
			null,
			retryable
		);
	}

	private void logInfo(
		String eventType,
		WebhookAlert alert,
		Object fromStatus,
		Object toStatus,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String lastError,
		String actorType,
		Long actorId,
		Boolean retryable
	) {
		log.info(
			LOG_PATTERN,
			eventType,
			alert.getId(),
			alert.getWorkspaceProblemId(),
			alert.getWorkspaceId(),
			fromStatus,
			toStatus,
			alert.getScheduledAt(),
			alert.getNextScheduledAt(),
			alert.getAttemptNo(),
			sendAt,
			lastAttemptAt,
			lastError,
			actorType,
			actorId,
			retryable
		);
	}

	private void logWarn(
		String eventType,
		WebhookAlert alert,
		Object fromStatus,
		Object toStatus,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String lastError,
		String actorType,
		Long actorId,
		Boolean retryable
	) {
		log.warn(
			LOG_PATTERN,
			eventType,
			alert.getId(),
			alert.getWorkspaceProblemId(),
			alert.getWorkspaceId(),
			fromStatus,
			toStatus,
			alert.getScheduledAt(),
			alert.getNextScheduledAt(),
			alert.getAttemptNo(),
			sendAt,
			lastAttemptAt,
			lastError,
			actorType,
			actorId,
			retryable
		);
	}
}
