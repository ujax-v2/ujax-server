package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ProblemRepositoryTest {

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private AlgorithmTagRepository algorithmTagRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("DELETE FROM problem_algorithm");
		jdbcTemplate.execute("DELETE FROM sample");
		problemRepository.deleteAllInBatch();
		algorithmTagRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("문제 번호로 문제를 조회한다")
	void findByProblemNumber() {
		// given
		problemRepository.save(Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			"설명", "입력", "출력", "https://www.acmicpc.net/problem/1000"
		));

		// when
		var found = problemRepository.findByProblemNumber(1000);

		// then
		assertThat(found).isPresent();
		assertThat(found.get()).extracting("problemNumber", "title", "tier")
			.containsExactly(1000, "A+B", "Bronze V");
	}

	@Test
	@DisplayName("문제 번호 존재 여부를 확인한다")
	void existsByProblemNumber() {
		// given
		problemRepository.save(Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null
		));

		// when & then
		assertThat(problemRepository.existsByProblemNumber(1000)).isTrue();
	}

	@Test
	@DisplayName("문제를 입출력 예제와 함께 저장한다")
	void saveWithSamples() {
		// given
		Problem problem = Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null
		);
		problem.addSample(Sample.create(1, "1 2", "3"));
		problem.addSample(Sample.create(2, "3 4", "7"));

		// when
		Problem saved = problemRepository.save(problem);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getSamples()).hasSize(2)
			.extracting("sampleIndex", "input", "output")
			.containsExactlyInAnyOrder(
				tuple(1, "1 2", "3"),
				tuple(2, "3 4", "7")
			);
	}

	@Test
	@DisplayName("문제를 알고리즘 태그와 함께 저장한다")
	void saveWithAlgorithmTags() {
		// given
		AlgorithmTag tag1 = algorithmTagRepository.save(AlgorithmTag.create("수학"));
		AlgorithmTag tag2 = algorithmTagRepository.save(AlgorithmTag.create("구현"));

		Problem problem = Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null
		);
		problem.addAlgorithmTag(tag1);
		problem.addAlgorithmTag(tag2);

		// when
		Problem saved = problemRepository.save(problem);

		// then
		assertThat(saved.getAlgorithmTags()).hasSize(2)
			.extracting("name")
			.containsExactlyInAnyOrder("수학", "구현");
	}
}
