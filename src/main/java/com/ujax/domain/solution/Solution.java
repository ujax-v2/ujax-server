package com.ujax.domain.solution;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.workspace.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "solution",
	indexes = {
		@Index(
			name = "idx_solution_workspace_problem_created_id",
			columnList = "workspace_problem_id,created_at,id"
		)
	}
)
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

	@Column(nullable = false, unique = true)
	private Long submissionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SolutionStatus status;

	private String time;

	private String memory;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ProgrammingLanguage programmingLanguage;

	private String codeLength;

	@Column(columnDefinition = "MEDIUMTEXT")
	private String code;

	private Solution(WorkspaceProblem workspaceProblem, WorkspaceMember workspaceMember,
		Long submissionId, String verdict, String time,
		String memory, String language, String codeLength, String code) {
		this.workspaceProblem = workspaceProblem;
		this.workspaceMember = workspaceMember;
		this.submissionId = submissionId;
		this.status = SolutionStatus.fromVerdict(verdict);
		this.time = time;
		this.memory = memory;
		this.programmingLanguage = ProgrammingLanguage.fromLanguage(language);
		this.codeLength = codeLength;
		this.code = code;
	}

	public static Solution create(WorkspaceProblem workspaceProblem, WorkspaceMember workspaceMember,
		Long submissionId, String verdict, String time,
		String memory, String language, String codeLength, String code) {
		return new Solution(workspaceProblem, workspaceMember, submissionId,
			verdict, time, memory, language, codeLength, code);
	}
}
