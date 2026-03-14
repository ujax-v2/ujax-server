package com.ujax.domain.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
class SolutionRepositoryTest {

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
		solutionRepository.deleteAllInBatch();
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		problemRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("워크스페이스 문제 ID로 풀이 목록을 페이징 조회한다")
	void findByWorkspaceProblemId() {
		// given
		User user = userRepository.save(User.createLocalUser("user@test.com", Password.ofEncoded("pw"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));
		Problem problem = problemRepository.save(Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB", null, null, null, null));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
		WorkspaceProblem wp = workspaceProblemRepository.save(
			WorkspaceProblem.create(problemBox, problem, null, null));

		solutionRepository.save(Solution.create(
			wp, member, 100L, "맞았습니다!!",
			"0 ms", "2020 KB", "Java 11", "100 B", null));
		solutionRepository.save(Solution.create(
			wp, member, 101L, "틀렸습니다",
			"4 ms", "2048 KB", "Python 3", "50 B", null));

		// when
		Page<Solution> result = solutionRepository.findByWorkspaceProblemId(
			wp.getId(), PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id"))));

		// then
		assertThat(result.getContent()).hasSize(2)
			.extracting("submissionId", "status")
			.containsExactly(
				tuple(101L, SolutionStatus.WRONG_ANSWER),
				tuple(100L, SolutionStatus.ACCEPTED)
			);
	}

	@Test
	@DisplayName("백준 제출 번호 존재 여부를 확인한다")
	void existsBySubmissionId() {
		// given
		User user = userRepository.save(User.createLocalUser("user@test.com", Password.ofEncoded("pw"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));
		Problem problem = problemRepository.save(Problem.create(
			1000, "A+B", "Bronze V", "2 초", "128 MB", null, null, null, null));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
		WorkspaceProblem wp = workspaceProblemRepository.save(
			WorkspaceProblem.create(problemBox, problem, null, null));

		solutionRepository.save(Solution.create(
			wp, member, 100L, "맞았습니다!!",
			null, null, null, null, null));

		// when & then
		assertThat(solutionRepository.existsBySubmissionId(100L)).isTrue();
	}
}
