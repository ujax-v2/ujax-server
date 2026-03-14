package com.ujax.infrastructure.web.solution.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolutionIngestRequest(
	@NotNull Long workspaceProblemId,
	@Positive long submissionId,
	@NotBlank String verdict,
	String time,
	String memory,
	String language,
	String codeLength,
	String code
) {
}
