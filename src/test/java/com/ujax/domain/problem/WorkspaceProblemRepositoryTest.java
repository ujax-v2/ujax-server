package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

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
class WorkspaceProblemRepositoryTest {

	@Autowired
	private WorkspaceProblemRepository workspaceProblemRepository;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("문제집 ID로 문제 목록을 페이징 조회한다")
	void searchByProblemBoxId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));

		Problem problem1 = problemRepository.save(
			Problem.create(1000, "A+B", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/1000"));
		Problem problem2 = problemRepository.save(
			Problem.create(1001, "A-B", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/1001"));

		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, problem1, null, null));
		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, problem2,
			LocalDateTime.of(2026, 3, 1, 0, 0), null));

		// when
		Page<WorkspaceProblem> result = workspaceProblemRepository.searchByProblemBoxId(
			problemBox.getId(),
			null,
			PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
		);

		// then
		assertThat(result.getContent()).hasSize(2)
			.extracting(wp -> wp.getProblem().getProblemNumber())
			.containsExactly(1001, 1000);
	}

	@Test
	@DisplayName("숫자 토큰은 문제 번호와 제목 모두에서 부분 일치 검색한다")
	void searchByNumericToken() {
		User user = userRepository.save(User.createLocalUser("test2@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));

		Problem byNumber = problemRepository.save(
			Problem.create(1000, "A+B", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/1000"));
		Problem byTitle = problemRepository.save(
			Problem.create(2000, "1000", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/2000"));
		Problem other = problemRepository.save(
			Problem.create(3000, "정렬", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/3000"));

		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, byNumber, null, null));
		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, byTitle, null, null));
		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, other, null, null));

		Page<WorkspaceProblem> result = workspaceProblemRepository.searchByProblemBoxId(
			problemBox.getId(),
			"100",
			PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
		);

		assertThat(result.getContent())
			.extracting(wp -> wp.getProblem().getProblemNumber())
			.containsExactlyInAnyOrder(1000, 2000);
	}

	@Test
	@DisplayName("문제집 ID와 문제 ID로 중복 여부를 확인한다")
	void existsByProblemBoxIdAndProblemId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
		Problem problem = problemRepository.save(
			Problem.create(1000, "A+B", "Bronze V", "1초", "256MB", "설명", "입력", "출력", "https://boj.kr/1000"));
		workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, problem, null, null));

		// when
		boolean exists = workspaceProblemRepository.existsByProblemBox_IdAndProblem_Id(
			problemBox.getId(), problem.getId());

		// then
		assertThat(exists).isTrue();
	}
}
