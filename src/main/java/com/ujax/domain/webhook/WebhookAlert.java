package com.ujax.domain.webhook;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "webhook_alert",
	indexes = {
		@Index(name = "idx_webhook_alert_due", columnList = "status,scheduled_at"),
		@Index(name = "idx_webhook_alert_workspace", columnList = "workspace_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_webhook_alert_workspace_problem",
			columnNames = {"workspace_problem_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookAlert {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "webhook_alert_id")
	private Long id;

	@Column(name = "workspace_problem_id", nullable = false)
	private Long workspaceProblemId;

	@Column(name = "workspace_id", nullable = false)
	private Long workspaceId;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "next_scheduled_at")
	private LocalDateTime nextScheduledAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WebhookAlertStatus status;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	private WebhookAlert(Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt) {
		this.workspaceProblemId = Objects.requireNonNull(workspaceProblemId);
		this.workspaceId = Objects.requireNonNull(workspaceId);
		this.scheduledAt = Objects.requireNonNull(scheduledAt);
		this.status = WebhookAlertStatus.PENDING;
		this.attemptNo = 0;
	}

	public static WebhookAlert create(Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt) {
		return new WebhookAlert(workspaceProblemId, workspaceId, scheduledAt);
	}

	public void markProcessing() {
		validateStatus(WebhookAlertStatus.PENDING);
		this.status = WebhookAlertStatus.PROCESSING;
	}

	public void applyScheduleUpdate(LocalDateTime scheduledAt) {
		LocalDateTime newScheduledAt = Objects.requireNonNull(scheduledAt);
		if (this.status == WebhookAlertStatus.PROCESSING) {
			this.nextScheduledAt = newScheduledAt;
			return;
		}
		validateStatus(WebhookAlertStatus.PENDING);
		this.scheduledAt = newScheduledAt;
		this.nextScheduledAt = null;
		this.attemptNo = 0;
	}

	public void markRetry(LocalDateTime retryAt, int maxAttempt) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		if (isRetryExhausted(maxAttempt)) {
			throw new IllegalStateException("retry attempts exhausted");
		}
		this.attemptNo = this.attemptNo + 1;
		this.scheduledAt = Objects.requireNonNull(retryAt);
		this.status = WebhookAlertStatus.PENDING;
	}

	public void recoverToPending(LocalDateTime fallbackScheduledAt) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		if (applyDeferredScheduleIfPresent()) {
			return;
		}
		this.scheduledAt = Objects.requireNonNull(fallbackScheduledAt);
		this.status = WebhookAlertStatus.PENDING;
	}

	public boolean applyDeferredScheduleIfPresent() {
		if (this.nextScheduledAt == null) {
			return false;
		}
		this.scheduledAt = this.nextScheduledAt;
		this.nextScheduledAt = null;
		this.attemptNo = 0;
		this.status = WebhookAlertStatus.PENDING;
		return true;
	}

	public boolean isRetryExhausted(int maxAttempt) {
		if (maxAttempt <= 0) {
			throw new IllegalArgumentException("maxAttempt must be greater than zero");
		}
		return this.attemptNo >= maxAttempt;
	}

	private void validateStatus(WebhookAlertStatus expected) {
		if (this.status != expected) {
			throw new IllegalStateException("invalid webhook alert state");
		}
	}
}
