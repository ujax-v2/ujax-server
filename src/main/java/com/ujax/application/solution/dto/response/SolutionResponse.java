package com.ujax.application.solution.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionStatus;

public record SolutionResponse(
	Long id,
	Long submissionId,
	int problemNumber,
	String memberName,
	SolutionStatus status,
	String time,
	String memory,
	ProgrammingLanguage programmingLanguage,
	String codeLength,
	LocalDateTime createdAt
) {

	public static SolutionResponse from(Solution solution) {
		return new SolutionResponse(
			solution.getId(),
			solution.getSubmissionId(),
			solution.getWorkspaceProblem().getProblem().getProblemNumber(),
			solution.getWorkspaceMember().getNickname(),
			solution.getStatus(),
			solution.getTime(),
			solution.getMemory(),
			solution.getProgrammingLanguage(),
			solution.getCodeLength(),
			solution.getCreatedAt()
		);
	}
}
