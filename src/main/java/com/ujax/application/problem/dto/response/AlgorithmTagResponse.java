package com.ujax.application.problem.dto.response;

import com.ujax.domain.problem.AlgorithmTag;

public record AlgorithmTagResponse(
	Long id,
	String name
) {

	public static AlgorithmTagResponse from(AlgorithmTag tag) {
		return new AlgorithmTagResponse(
			tag.getId(),
			tag.getName()
		);
	}
}
