package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemTest {

	@Test
	@DisplayName("문제를 생성한다")
	void createProblem() {
		// when
		Problem problem = Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			"두 정수 A와 B를 입력받은 다음...", "첫째 줄에 A와 B가 주어진다.",
			"첫째 줄에 A+B를 출력한다.", "https://www.acmicpc.net/problem/1000"
		);

		// then
		assertThat(problem).extracting(
			"problemNumber", "title", "tier", "timeLimit", "memoryLimit", "url"
		).containsExactly(
			1000, "A+B", "Bronze V", "2 초", "128 MB", "https://www.acmicpc.net/problem/1000"
		);
	}

	@Test
	@DisplayName("문제에 입출력 예제를 추가한다")
	void addSample() {
		// given
		Problem problem = Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null
		);
		Sample sample = Sample.create(1, "1 2", "3");

		// when
		problem.addSample(sample);

		// then
		assertThat(problem.getSamples()).hasSize(1);
		assertThat(problem.getSamples().getFirst()).extracting("sampleIndex", "input", "output")
			.containsExactly(1, "1 2", "3");
		assertThat(sample.getProblem()).isEqualTo(problem);
	}

	@Test
	@DisplayName("문제에 알고리즘 태그를 추가한다")
	void addAlgorithmTag() {
		// given
		Problem problem = Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null
		);
		AlgorithmTag tag = AlgorithmTag.create("수학");

		// when
		problem.addAlgorithmTag(tag);

		// then
		assertThat(problem.getAlgorithmTags()).hasSize(1);
		assertThat(problem.getAlgorithmTags().getFirst().getName()).isEqualTo("수학");
	}
}
