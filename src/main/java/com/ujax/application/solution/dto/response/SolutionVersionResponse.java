package com.ujax.application.solution.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionStatus;

public record SolutionVersionResponse(
	Long submissionId,
	String code,
	SolutionStatus status,
	String time,
	String memory,
	String codeLength,
	LocalDateTime createdAt,
	long likes,
	boolean isLiked,
	long commentCount
) {

	public static SolutionVersionResponse from(Solution solution, long likes, boolean isLiked, long commentCount) {
		return new SolutionVersionResponse(
			solution.getSubmissionId(),
			solution.getCode(),
			solution.getStatus(),
			solution.getTime(),
			solution.getMemory(),
			solution.getCodeLength(),
			solution.getCreatedAt(),
			likes,
			isLiked,
			commentCount
		);
	}
}
