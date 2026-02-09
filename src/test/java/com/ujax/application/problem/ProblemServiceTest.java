package com.ujax.application.problem;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.problem.dto.response.ProblemResponse;
import com.ujax.domain.problem.AlgorithmTagRepository;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.Sample;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.ProblemIngestRequest;

@SpringBootTest
@ActiveProfiles("test")
class ProblemServiceTest {

	@Autowired
	private ProblemService problemService;

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

	@Nested
	@DisplayName("문제 조회")
	class GetProblem {

		@Test
		@DisplayName("ID로 문제를 조회한다")
		void getProblem_Success() {
			// given
			Problem problem = problemRepository.save(Problem.create(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				"설명", "입력", "출력", "https://www.acmicpc.net/problem/1000"
			));

			// when
			ProblemResponse response = problemService.getProblem(problem.getId());

			// then
			assertThat(response).extracting("problemNumber", "title", "tier")
				.containsExactly(1000, "A+B", "Bronze V");
		}

		@Test
		@DisplayName("존재하지 않는 문제를 조회하면 오류가 발생한다")
		void getProblem_NotFound() {
			// when & then
			assertThatThrownBy(() -> problemService.getProblem(999L))
				.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("문제 번호로 조회")
	class GetProblemByNumber {

		@Test
		@DisplayName("문제 번호로 문제를 조회한다")
		void getProblemByNumber_Success() {
			// given
			problemRepository.save(Problem.create(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				"설명", "입력", "출력", null
			));

			// when
			ProblemResponse response = problemService.getProblemByNumber(1000);

			// then
			assertThat(response).extracting("problemNumber", "title")
				.containsExactly(1000, "A+B");
		}

		@Test
		@DisplayName("존재하지 않는 문제 번호로 조회하면 오류가 발생한다")
		void getProblemByNumber_NotFound() {
			// when & then
			assertThatThrownBy(() -> problemService.getProblemByNumber(9999))
				.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("문제 생성")
	class CreateProblem {

		@Test
		@DisplayName("문제를 생성한다")
		void createProblem_Success() {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				"두 정수 A와 B를 입력받은 다음...",
				"첫째 줄에 A와 B가 주어진다.",
				"첫째 줄에 A+B를 출력한다.",
				"https://www.acmicpc.net/problem/1000",
				List.of(
					new ProblemIngestRequest.SampleDto(1, "1 2", "3"),
					new ProblemIngestRequest.SampleDto(2, "3 4", "7")
				),
				List.of(
					new ProblemIngestRequest.TagDto("수학"),
					new ProblemIngestRequest.TagDto("구현")
				)
			);

			// when
			ProblemResponse response = problemService.createProblem(request);

			// then
			assertThat(response).extracting("problemNumber", "title", "tier")
				.containsExactly(1000, "A+B", "Bronze V");
			assertThat(response.samples()).hasSize(2);
			assertThat(response.algorithmTags()).hasSize(2)
				.extracting("name")
				.containsExactlyInAnyOrder("수학", "구현");
		}

		@Test
		@DisplayName("입출력 예제와 태그 없이 문제를 생성한다")
		void createProblem_WithoutSamplesAndTags() {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				"설명", "입력", "출력", null,
				null, null
			);

			// when
			ProblemResponse response = problemService.createProblem(request);

			// then
			assertThat(response.problemNumber()).isEqualTo(1000);
			assertThat(response.samples()).isEmpty();
			assertThat(response.algorithmTags()).isEmpty();
		}

		@Test
		@DisplayName("이미 존재하는 문제 번호로 생성하면 오류가 발생한다")
		void createProblem_Duplicate() {
			// given
			problemRepository.save(Problem.create(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				null, null, null, null
			));

			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				null, null, null, null, null, null
			);

			// when & then
			assertThatThrownBy(() -> problemService.createProblem(request))
				.isInstanceOf(ConflictException.class);
		}

		@Test
		@DisplayName("제목의 탭과 개행이 정리된다")
		void createProblem_CleanTitle() {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "A+B\tBronze V", "Bronze V", "2 초", "128 MB",
				null, null, null, null, null, null
			);

			// when
			ProblemResponse response = problemService.createProblem(request);

			// then
			assertThat(response.title()).isEqualTo("A+B");
		}

		@Test
		@DisplayName("같은 태그 이름이 이미 존재하면 재사용한다")
		void createProblem_ReuseExistingTag() {
			// given
			ProblemIngestRequest request1 = new ProblemIngestRequest(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				null, null, null, null, null,
				List.of(new ProblemIngestRequest.TagDto("수학"))
			);
			problemService.createProblem(request1);

			ProblemIngestRequest request2 = new ProblemIngestRequest(
				1001, "A-B", "Bronze V", "2 초", "128 MB",
				null, null, null, null, null,
				List.of(new ProblemIngestRequest.TagDto("수학"))
			);

			// when
			problemService.createProblem(request2);

			// then
			assertThat(algorithmTagRepository.findAll()).hasSize(1);
		}
	}
}
