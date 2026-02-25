package com.ujax.domain.workspace;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ujax.domain.user.User;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BusinessRuleViolationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "workspace_join_requests",
	indexes = {
		@Index(name = "idx_wjr_workspace_status_created_at", columnList = "workspace_id,status,created_at"),
		@Index(name = "idx_wjr_user_created_at", columnList = "user_id,created_at")
	}
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceJoinRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WorkspaceJoinRequestStatus status;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	private WorkspaceJoinRequest(Workspace workspace, User user, WorkspaceJoinRequestStatus status) {
		this.workspace = workspace;
		this.user = user;
		this.status = status;
	}

	public static WorkspaceJoinRequest create(Workspace workspace, User user) {
		return new WorkspaceJoinRequest(workspace, user, WorkspaceJoinRequestStatus.PENDING);
	}

	public void approve() {
		validatePending();
		this.status = WorkspaceJoinRequestStatus.APPROVED;
	}

	public void reject() {
		validatePending();
		this.status = WorkspaceJoinRequestStatus.REJECTED;
	}

	private void validatePending() {
		if (status != WorkspaceJoinRequestStatus.PENDING) {
			throw new BusinessRuleViolationException(ErrorCode.WORKSPACE_JOIN_REQUEST_ALREADY_PROCESSED);
		}
	}
}
