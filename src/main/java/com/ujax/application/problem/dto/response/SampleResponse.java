package com.ujax.application.problem.dto.response;

import com.ujax.domain.problem.Sample;

public record SampleResponse(
	Long id,
	int sampleIndex,
	String input,
	String output
) {

	public static SampleResponse from(Sample sample) {
		return new SampleResponse(
			sample.getId(),
			sample.getSampleIndex(),
			sample.getInput(),
			sample.getOutput()
		);
	}
}
