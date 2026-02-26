package com.ujax.infrastructure.web.problem.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateWorkspaceProblemRequest(
	@NotNull @Positive Long problemId,
	LocalDateTime deadline,
	LocalDateTime scheduledAt
) {
}
