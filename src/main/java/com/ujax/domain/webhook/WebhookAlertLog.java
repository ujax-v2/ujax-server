package com.ujax.domain.webhook;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "webhook_alert_log",
	indexes = {
		@Index(name = "idx_webhook_alert_log_alert_created", columnList = "webhook_alert_id,created_at"),
		@Index(name = "idx_webhook_alert_log_workspace_problem_created", columnList = "workspace_problem_id,created_at"),
		@Index(name = "idx_webhook_alert_log_event_created", columnList = "event_type,created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookAlertLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "webhook_alert_log_id")
	private Long id;

	@Column(name = "webhook_alert_id")
	private Long webhookAlertId;

	@Column(name = "workspace_problem_id", nullable = false)
	private Long workspaceProblemId;

	@Column(name = "workspace_id", nullable = false)
	private Long workspaceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private WebhookAlertLogEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private WebhookAlertStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 20)
	private WebhookAlertStatus toStatus;

	@Column(name = "scheduled_at")
	private LocalDateTime scheduledAt;

	@Column(name = "next_scheduled_at")
	private LocalDateTime nextScheduledAt;

	@Column(name = "attempt_no")
	private Integer attemptNo;

	@Column(name = "send_at")
	private LocalDateTime sendAt;

	@Column(name = "last_attempt_at")
	private LocalDateTime lastAttemptAt;

	@Column(name = "err_msg", length = 255)
	private String errMsg;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 20)
	private WebhookAlertLogActorType actorType;

	@Column(name = "actor_id")
	private Long actorId;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	private WebhookAlertLog(
		Long webhookAlertId,
		Long workspaceProblemId,
		Long workspaceId,
		WebhookAlertLogEventType eventType,
		WebhookAlertStatus fromStatus,
		WebhookAlertStatus toStatus,
		LocalDateTime scheduledAt,
		LocalDateTime nextScheduledAt,
		Integer attemptNo,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg,
		WebhookAlertLogActorType actorType,
		Long actorId
	) {
		this.webhookAlertId = webhookAlertId;
		this.workspaceProblemId = Objects.requireNonNull(workspaceProblemId);
		this.workspaceId = Objects.requireNonNull(workspaceId);
		this.eventType = Objects.requireNonNull(eventType);
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.scheduledAt = scheduledAt;
		this.nextScheduledAt = nextScheduledAt;
		this.attemptNo = attemptNo;
		this.sendAt = sendAt;
		this.lastAttemptAt = lastAttemptAt;
		this.errMsg = normalizeErrMsg(errMsg);
		this.actorType = Objects.requireNonNull(actorType);
		this.actorId = actorId;
	}

	public static WebhookAlertLog create(
		Long webhookAlertId,
		Long workspaceProblemId,
		Long workspaceId,
		WebhookAlertLogEventType eventType,
		WebhookAlertStatus fromStatus,
		WebhookAlertStatus toStatus,
		LocalDateTime scheduledAt,
		LocalDateTime nextScheduledAt,
		Integer attemptNo,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg,
		WebhookAlertLogActorType actorType,
		Long actorId
	) {
		return new WebhookAlertLog(
			webhookAlertId,
			workspaceProblemId,
			workspaceId,
			eventType,
			fromStatus,
			toStatus,
			scheduledAt,
			nextScheduledAt,
			attemptNo,
			sendAt,
			lastAttemptAt,
			errMsg,
			actorType,
			actorId
		);
	}

	public static WebhookAlertLog fromAlert(
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
		Objects.requireNonNull(alert);
		return create(
			alert.getId(),
			alert.getWorkspaceProblemId(),
			alert.getWorkspaceId(),
			eventType,
			fromStatus,
			toStatus,
			alert.getScheduledAt(),
			alert.getNextScheduledAt(),
			alert.getAttemptNo(),
			sendAt,
			lastAttemptAt,
			errMsg,
			actorType,
			actorId
		);
	}

	private String normalizeErrMsg(String errMsg) {
		if (errMsg == null) {
			return null;
		}
		return errMsg.length() <= 255 ? errMsg : errMsg.substring(0, 255);
	}
}
