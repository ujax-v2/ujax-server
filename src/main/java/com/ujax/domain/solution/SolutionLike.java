package com.ujax.domain.solution;

import com.ujax.domain.workspace.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "solution_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolutionLike {

	@EmbeddedId
	private SolutionLikeId id;

	@MapsId("solutionId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solution_id", nullable = false)
	private Solution solution;

	@MapsId("workspaceMemberId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Builder
	private SolutionLike(Solution solution, WorkspaceMember workspaceMember, boolean deleted) {
		this.id = new SolutionLikeId(solution.getId(), workspaceMember.getId());
		this.solution = solution;
		this.workspaceMember = workspaceMember;
		this.deleted = deleted;
	}

	public static SolutionLike create(Solution solution, WorkspaceMember workspaceMember) {
		return SolutionLike.builder()
			.solution(solution)
			.workspaceMember(workspaceMember)
			.deleted(false)
			.build();
	}

	public void updateDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
