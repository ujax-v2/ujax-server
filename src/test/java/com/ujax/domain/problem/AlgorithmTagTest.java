package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlgorithmTagTest {

	@Test
	@DisplayName("알고리즘 태그를 생성한다")
	void createAlgorithmTag() {
		// when
		AlgorithmTag tag = AlgorithmTag.create("다이나믹 프로그래밍");

		// then
		assertThat(tag.getName()).isEqualTo("다이나믹 프로그래밍");
	}
}
