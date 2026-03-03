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
		@Index(name = "idx_webhook_alert_due", columnList = "status,scheduled_at")
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

	public static final int MAX_ATTEMPT = 5;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WebhookAlertStatus status;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@Column(name = "send_at")
	private LocalDateTime sendAt;

	@Column(name = "last_attempt_at")
	private LocalDateTime lastAttemptAt;

	@Column(name = "err_msg", length = 255)
	private String errMsg;

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

	public void startProcessing() {
		validateStatus(WebhookAlertStatus.PENDING);
		this.status = WebhookAlertStatus.PROCESSING;
	}

	public void markDone(LocalDateTime attemptedAt) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		LocalDateTime now = Objects.requireNonNull(attemptedAt);
		this.status = WebhookAlertStatus.DONE;
		this.lastAttemptAt = now;
		this.sendAt = now;
		this.errMsg = null;
	}

	public void markRetry(LocalDateTime attemptedAt, String errMsg) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		if (isRetryExhausted()) {
			throw new IllegalStateException("retry attempts exhausted");
		}
		LocalDateTime now = Objects.requireNonNull(attemptedAt);
		this.status = WebhookAlertStatus.PENDING;
		this.attemptNo = this.attemptNo + 1;
		this.lastAttemptAt = now;
		this.scheduledAt = now;
		this.errMsg = normalizeErrMsg(errMsg);
		this.sendAt = null;
	}

	public void markFailed(LocalDateTime attemptedAt, String errMsg) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		LocalDateTime now = Objects.requireNonNull(attemptedAt);
		this.status = WebhookAlertStatus.FAILED;
		this.lastAttemptAt = now;
		this.sendAt = now;
		this.errMsg = normalizeErrMsg(errMsg);
	}

	public void recoverToPending(LocalDateTime scheduledAt) {
		validateStatus(WebhookAlertStatus.PROCESSING);
		this.status = WebhookAlertStatus.PENDING;
		this.scheduledAt = Objects.requireNonNull(scheduledAt);
		this.errMsg = null;
		this.sendAt = null;
	}

	public void cancel() {
		validateStatus(WebhookAlertStatus.PENDING);
		this.status = WebhookAlertStatus.CANCELLED;
	}

	public void reschedule(LocalDateTime scheduledAt) {
		this.scheduledAt = Objects.requireNonNull(scheduledAt);
	}

	public boolean isRetryExhausted() {
		return this.attemptNo >= MAX_ATTEMPT;
	}

	private void validateStatus(WebhookAlertStatus expected) {
		if (this.status != expected) {
			throw new IllegalStateException("invalid webhook alert state");
		}
	}

	private String normalizeErrMsg(String errMsg) {
		if (errMsg == null) {
			return null;
		}
		return errMsg.length() <= 255 ? errMsg : errMsg.substring(0, 255);
	}
}
