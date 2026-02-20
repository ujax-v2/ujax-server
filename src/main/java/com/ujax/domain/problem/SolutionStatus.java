package com.ujax.domain.problem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SolutionStatus {

	PASS("통과"),
	FAIL("실패")

	;

	private final String text;
}
