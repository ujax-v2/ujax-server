package com.ujax.domain.problem;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.workspace.WorkspaceMember;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "solution")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE solution SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_problem_id", nullable = false)
	private WorkspaceProblem workspaceProblem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SolutionStatus status;

	private Integer timeMs;

	private Integer memoryMb;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProgrammingLanguage language;

	private Solution(WorkspaceProblem workspaceProblem, WorkspaceMember workspaceMember, SolutionStatus status,
		Integer timeMs, Integer memoryMb, String code, ProgrammingLanguage language) {
		this.workspaceProblem = workspaceProblem;
		this.workspaceMember = workspaceMember;
		this.status = status;
		this.timeMs = timeMs;
		this.memoryMb = memoryMb;
		this.code = code;
		this.language = language;
	}

	public static Solution create(WorkspaceProblem workspaceProblem, WorkspaceMember workspaceMember,
		SolutionStatus status, Integer timeMs, Integer memoryMb, String code, ProgrammingLanguage language) {
		return new Solution(workspaceProblem, workspaceMember, status, timeMs, memoryMb, code, language);
	}
}
