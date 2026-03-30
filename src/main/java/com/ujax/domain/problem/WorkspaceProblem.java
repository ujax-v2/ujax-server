package com.ujax.domain.problem;

import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "workspace_problem",
	indexes = {
		@Index(name = "idx_workspace_problem_problem_box_id", columnList = "problem_box_id")
	}
)
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE workspace_problem SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceProblem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_box_id", nullable = false)
	private ProblemBox problemBox;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	private LocalDateTime deadline;

	private LocalDateTime scheduledAt;

	private WorkspaceProblem(ProblemBox problemBox, Problem problem, LocalDateTime deadline,
		LocalDateTime scheduledAt) {
		this.problemBox = problemBox;
		this.problem = problem;
		this.deadline = deadline;
		this.scheduledAt = scheduledAt;
	}

	public static WorkspaceProblem create(ProblemBox problemBox, Problem problem, LocalDateTime deadline,
		LocalDateTime scheduledAt) {
		return new WorkspaceProblem(problemBox, problem, deadline, scheduledAt);
	}

	public void update(LocalDateTime deadline, LocalDateTime scheduledAt) {
		this.deadline = deadline;
		this.scheduledAt = scheduledAt;
	}
}
