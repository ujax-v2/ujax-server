package com.ujax.application.workspace.dto.response.dashboard;

import java.time.LocalDateTime;
import java.util.List;

import com.ujax.domain.problem.AlgorithmTag;
import com.ujax.domain.problem.WorkspaceProblem;

public record DashboardDeadlineProblemResponse(
	Long workspaceProblemId,
	Long problemBoxId,
	String problemBoxTitle,
	int problemNumber,
	String title,
	String tier,
	List<String> algorithmTags,
	LocalDateTime deadline
) {

	private static final int ALGORITHM_TAG_LIMIT = 2;

	public static DashboardDeadlineProblemResponse from(WorkspaceProblem workspaceProblem) {
		return new DashboardDeadlineProblemResponse(
			workspaceProblem.getId(),
			workspaceProblem.getProblemBox().getId(),
			workspaceProblem.getProblemBox().getTitle(),
			workspaceProblem.getProblem().getProblemNumber(),
			workspaceProblem.getProblem().getTitle(),
			workspaceProblem.getProblem().getTier(),
			extractAlgorithmTags(workspaceProblem),
			workspaceProblem.getDeadline()
		);
	}

	private static List<String> extractAlgorithmTags(WorkspaceProblem workspaceProblem) {
		return workspaceProblem.getProblem().getAlgorithmTags().stream()
			.map(AlgorithmTag::getName)
			.limit(ALGORITHM_TAG_LIMIT)
			.toList();
	}
}
