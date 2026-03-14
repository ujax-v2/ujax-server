package com.ujax.application.solution.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionStatus;

public record SolutionMemberSummaryResponse(
	Long workspaceMemberId,
	String memberName,
	ProgrammingLanguage programmingLanguage,
	SolutionStatus latestStatus,
	long submissionCount,
	long likes,
	LocalDateTime updatedAt
) {

	public static SolutionMemberSummaryResponse from(Solution solution, long submissionCount, long likes) {
		return new SolutionMemberSummaryResponse(
			solution.getWorkspaceMember().getId(),
			solution.getWorkspaceMember().getNickname(),
			solution.getProgrammingLanguage(),
			solution.getStatus(),
			submissionCount,
			likes,
			solution.getCreatedAt()
		);
	}
}
