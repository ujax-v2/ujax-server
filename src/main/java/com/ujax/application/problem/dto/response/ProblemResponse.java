package com.ujax.application.problem.dto.response;

import java.util.List;

import com.ujax.domain.problem.Problem;

public record ProblemResponse(
	Long id,
	int problemNumber,
	String title,
	String tier,
	String timeLimit,
	String memoryLimit,
	String description,
	String inputDescription,
	String outputDescription,
	String url,
	List<SampleResponse> samples,
	List<AlgorithmTagResponse> algorithmTags
) {

	public static ProblemResponse from(Problem problem) {
		return new ProblemResponse(
			problem.getId(),
			problem.getProblemNumber(),
			problem.getTitle(),
			problem.getTier(),
			problem.getTimeLimit(),
			problem.getMemoryLimit(),
			problem.getDescription(),
			problem.getInputDescription(),
			problem.getOutputDescription(),
			problem.getUrl(),
			problem.getSamples().stream()
				.map(SampleResponse::from)
				.toList(),
			problem.getAlgorithmTags().stream()
				.map(AlgorithmTagResponse::from)
				.toList()
		);
	}
}
