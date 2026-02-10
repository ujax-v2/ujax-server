package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SampleTest {

	@Test
	@DisplayName("입출력 예제를 생성한다")
	void createSample() {
		// when
		Sample sample = Sample.create(1, "1 2", "3");

		// then
		assertThat(sample).extracting("sampleIndex", "input", "output")
			.containsExactly(1, "1 2", "3");
	}
}
