package com.ujax.domain.problem;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.global.exception.common.InvalidParameterException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "problem_box")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE problem_box SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemBox extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@Column(nullable = false, length = 30)
	private String title;

	private String description;

	private ProblemBox(Workspace workspace, WorkspaceMember workspaceMember, String title, String description) {
		this.workspace = workspace;
		this.workspaceMember = workspaceMember;
		this.title = title;
		this.description = description;
	}

	public static ProblemBox create(Workspace workspace, WorkspaceMember workspaceMember, String title,
		String description) {
		validateTitle(title);
		validateDescription(description);
		return new ProblemBox(workspace, workspaceMember, title, description);
	}

	public void update(String title, String description) {
		validateTitle(title);
		validateDescription(description);
		this.title = title;
		this.description = description;
	}

	private static void validateTitle(String title) {
		if (title.length() > 30) {
			throw new InvalidParameterException("문제집 제목은 30자를 초과할 수 없습니다.");
		}
	}

	private static void validateDescription(String description) {
		if (description != null && description.length() > 255) {
			throw new InvalidParameterException("문제집 설명은 255자를 초과할 수 없습니다.");
		}
	}
}
