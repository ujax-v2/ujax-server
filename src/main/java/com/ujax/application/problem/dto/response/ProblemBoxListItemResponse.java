package com.ujax.application.problem.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.problem.ProblemBox;

public record ProblemBoxListItemResponse(
	Long id,
	String title,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static ProblemBoxListItemResponse from(ProblemBox problemBox) {
		return new ProblemBoxListItemResponse(
			problemBox.getId(),
			problemBox.getTitle(),
			problemBox.getCreatedAt(),
			problemBox.getUpdatedAt()
		);
	}
}
