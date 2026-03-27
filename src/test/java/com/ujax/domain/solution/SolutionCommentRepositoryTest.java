package com.ujax.domain.solution;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class SolutionCommentRepositoryTest {

	@Autowired
	private SolutionCommentRepository solutionCommentRepository;

	@Autowired
	private SolutionRepository solutionRepository;

	@Autowired
	private WorkspaceProblemRepository workspaceProblemRepository;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		solutionCommentRepository.deleteAllInBatch();
		solutionRepository.deleteAllInBatch();
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		problemRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("풀이 기준으로 댓글 목록을 생성순으로 조회한다")
	void findBySolutionId() {
		// given
		Fixture fixture = createFixture();
		solutionCommentRepository.save(SolutionComment.create(fixture.solution, fixture.member, "첫 댓글"));
		solutionCommentRepository.save(SolutionComment.create(fixture.solution, fixture.member, "둘째 댓글"));

		// when
		List<SolutionComment> result = solutionCommentRepository.findBySolutionId(fixture.solution.getId());

		// then
		assertThat(result)
			.extracting("content")
			.containsExactly("첫 댓글", "둘째 댓글");
	}

	@Test
	@DisplayName("풀이별 댓글 수를 집계할 수 있다")
	void countBySolutionIds() {
		// given
		Fixture fixture = createFixture();
		Solution anotherSolution = solutionRepository.save(Solution.create(
			fixture.workspaceProblem,
			fixture.member,
			101L,
			"틀렸습니다",
			"4 ms",
			"2048 KB",
			"Python 3",
			"50 B",
			null
		));
		solutionCommentRepository.save(SolutionComment.create(fixture.solution, fixture.member, "댓글 1"));
		solutionCommentRepository.save(SolutionComment.create(fixture.solution, fixture.member, "댓글 2"));
		solutionCommentRepository.save(SolutionComment.create(anotherSolution, fixture.member, "댓글 3"));

		// when
		List<Object[]> rows = solutionCommentRepository.countBySolutionIds(
			List.of(fixture.solution.getId(), anotherSolution.getId())
		);
		Map<Long, Long> countMap = rows.stream()
			.collect(Collectors.toMap(
				row -> (Long)row[0],
				row -> ((Number)row[1]).longValue()
			));

		// then
		assertThat(countMap).containsEntry(fixture.solution.getId(), 2L);
		assertThat(countMap).containsEntry(anotherSolution.getId(), 1L);
	}

	private Fixture createFixture() {
		User user = userRepository.save(
			User.createLocalUser(UUID.randomUUID() + "@example.com", Password.ofEncoded("pw"), "유저")
		);
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER)
		);
		Problem problem = problemRepository.save(Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB", null, null, null, null
		));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
		WorkspaceProblem workspaceProblem = workspaceProblemRepository.save(
			WorkspaceProblem.create(problemBox, problem, null, null)
		);
		Solution solution = solutionRepository.save(Solution.create(
			workspaceProblem,
			member,
			100L,
			"맞았습니다!!",
			"0 ms",
			"2020 KB",
			"Java 11",
			"100 B",
			null
		));
		return new Fixture(member, workspaceProblem, solution);
	}

	private record Fixture(
		WorkspaceMember member,
		WorkspaceProblem workspaceProblem,
		Solution solution
	) {
	}
}
