package com.ujax.domain.workspace;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.user.User;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "workspace_members",
	uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
)
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE workspace_members SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember extends BaseEntity {

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
	private WorkspaceMemberRole role;

	@Column(nullable = false, length = 30)
	private String nickname;

	private WorkspaceMember(Workspace workspace, User user, WorkspaceMemberRole role, String nickname) {
		this.workspace = workspace;
		this.user = user;
		this.role = role;
		this.nickname = nickname;
	}

	public static WorkspaceMember create(Workspace workspace, User user, WorkspaceMemberRole role) {
		return new WorkspaceMember(workspace, user, role, user.getName());
	}

	public void updateRole(WorkspaceMemberRole role) {
		this.role = role;
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void validateManagerOrOwner() {
		if (!role.isManagerOrOwner()) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}
}
