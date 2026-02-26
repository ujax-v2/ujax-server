package com.ujax.infrastructure.web.problem.dto.request;

import java.time.LocalDateTime;

public record UpdateWorkspaceProblemRequest(
	LocalDateTime deadline,
	LocalDateTime scheduledAt
) {
}
