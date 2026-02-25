package com.ujax.application.problem.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.problem.ProblemBox;

public record ProblemBoxResponse(
	Long id,
	String title,
	String description,
	LocalDateTime createdAt
) {

	public static ProblemBoxResponse from(ProblemBox problemBox) {
		return new ProblemBoxResponse(
			problemBox.getId(),
			problemBox.getTitle(),
			problemBox.getDescription(),
			problemBox.getCreatedAt()
		);
	}
}
