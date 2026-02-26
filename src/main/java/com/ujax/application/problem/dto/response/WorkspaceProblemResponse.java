package com.ujax.application.problem.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.problem.WorkspaceProblem;

public record WorkspaceProblemResponse(
	Long id,
	int problemNumber,
	String title,
	String tier,
	LocalDateTime deadline,
	LocalDateTime scheduledAt,
	LocalDateTime createdAt
) {

	public static WorkspaceProblemResponse from(WorkspaceProblem wp) {
		return new WorkspaceProblemResponse(
			wp.getId(),
			wp.getProblem().getProblemNumber(),
			wp.getProblem().getTitle(),
			wp.getProblem().getTier(),
			wp.getDeadline(),
			wp.getScheduledAt(),
			wp.getCreatedAt()
		);
	}
}
