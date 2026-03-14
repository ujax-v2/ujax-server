package com.ujax.application.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.domain.solution.SolutionStatus;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.solution.dto.request.SolutionIngestRequest;

@SpringBootTest
@ActiveProfiles("test")
class SolutionServiceTest {

	@Autowired
	private SolutionService solutionService;

	@Autowired
	private SolutionRepository solutionRepository;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private WorkspaceProblemRepository workspaceProblemRepository;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

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

	private Problem createProblem(int problemNumber) {
		return problemRepository.save(
			Problem.create(problemNumber, "A+B", "Bronze V", "1초", "256MB",
				null, null, null, null));
	}

	private User createUser(String email) {
		return userRepository.save(User.createLocalUser(email, Password.ofEncoded("password"), "유저"));
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace, User user, WorkspaceMemberRole role) {
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, role));
	}

	private ProblemBox createProblemBox(Workspace workspace) {
		return problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
	}

	@Nested
	@DisplayName("풀이 수집")
	class Ingest {

		@Test
		@DisplayName("백준 풀이를 수집한다")
		void ingest_Success() {
			// given
			User user = createUser("user@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000);
			WorkspaceProblem wp = workspaceProblemRepository.save(
				WorkspaceProblem.create(problemBox, problem, null, null));

			SolutionIngestRequest request = new SolutionIngestRequest(
				wp.getId(), 12345L, "맞았습니다!!",
				"0 ms", "2020 KB", "Java 11", "123 B", "code");

			// when
			SolutionResponse response = solutionService.ingest(request, user.getId());

			// then
			assertThat(response).extracting(
				"submissionId", "problemNumber", "memberName", "status"
			).containsExactly(12345L, 1000, member.getNickname(), SolutionStatus.ACCEPTED);
		}

		@Test
		@DisplayName("존재하지 않는 워크스페이스 문제로 수집하면 오류가 발생한다")
		void ingest_WorkspaceProblemNotFound() {
			// given
			User user = createUser("user@example.com");
			SolutionIngestRequest request = new SolutionIngestRequest(
				999L, 12345L, "맞았습니다!!",
				null, null, null, null, null);

			// when & then
			assertThatThrownBy(() -> solutionService.ingest(request, user.getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 오류가 발생한다")
		void ingest_NotMember() {
			// given
			User owner = createUser("owner@example.com");
			User outsider = createUser("outsider@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000);
			WorkspaceProblem wp = workspaceProblemRepository.save(
				WorkspaceProblem.create(problemBox, problem, null, null));

			SolutionIngestRequest request = new SolutionIngestRequest(
				wp.getId(), 12345L, "맞았습니다!!",
				null, null, null, null, null);

			// when & then
			assertThatThrownBy(() -> solutionService.ingest(request, outsider.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}

		@Test
		@DisplayName("중복된 백준 제출 번호로 수집하면 오류가 발생한다")
		void ingest_Duplicate() {
			// given
			User user = createUser("user@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000);
			WorkspaceProblem wp = workspaceProblemRepository.save(
				WorkspaceProblem.create(problemBox, problem, null, null));

			solutionRepository.save(Solution.create(
				wp, member, 12345L, "맞았습니다!!",
				null, null, null, null, null));

			SolutionIngestRequest request = new SolutionIngestRequest(
				wp.getId(), 12345L, "맞았습니다!!",
				null, null, null, null, null);

			// when & then
			assertThatThrownBy(() -> solutionService.ingest(request, user.getId()))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SOLUTION);
		}
	}

	@Nested
	@DisplayName("풀이 목록 조회")
	class GetSolutions {

		@Test
		@DisplayName("워크스페이스 문제의 풀이 목록을 조회한다")
		void getSolutions_Success() {
			// given
			User user = createUser("user@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000);
			WorkspaceProblem wp = workspaceProblemRepository.save(
				WorkspaceProblem.create(problemBox, problem, null, null));

			solutionRepository.save(Solution.create(
				wp, member, 100L, "맞았습니다!!",
				"0 ms", "2020 KB", "Java 11", "100 B", null));
			solutionRepository.save(Solution.create(
				wp, member, 101L, "틀렸습니다",
				"4 ms", "2048 KB", "Python 3", "50 B", null));

			// when
			PageResponse<SolutionResponse> response = solutionService.getSolutions(
				workspace.getId(), problemBox.getId(), wp.getId(),
				user.getId(), 0, 20);

			// then
			assertThat(response.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 오류가 발생한다")
		void getSolutions_NotMember() {
			// given
			User owner = createUser("owner@example.com");
			User outsider = createUser("outsider@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000);
			WorkspaceProblem wp = workspaceProblemRepository.save(
				WorkspaceProblem.create(problemBox, problem, null, null));

			// when & then
			assertThatThrownBy(() -> solutionService.getSolutions(
				workspace.getId(), problemBox.getId(), wp.getId(),
				outsider.getId(), 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}

		@Test
		@DisplayName("존재하지 않는 워크스페이스 문제를 조회하면 오류가 발생한다")
		void getSolutions_WorkspaceProblemNotFound() {
			// given
			User user = createUser("user@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);

			// when & then
			assertThatThrownBy(() -> solutionService.getSolutions(
				workspace.getId(), problemBox.getId(), 999L,
				user.getId(), 0, 20))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 워크스페이스의 문제를 현재 워크스페이스 경로로 조회하면 오류가 발생한다")
		void getSolutions_WorkspaceMismatch() {
			// given
			User user = createUser("user@example.com");
			Workspace myWorkspace = workspaceRepository.save(Workspace.create("내 워크스페이스", "소개"));
			createMember(myWorkspace, user, WorkspaceMemberRole.MEMBER);
			ProblemBox myProblemBox = problemBoxRepository.save(ProblemBox.create(myWorkspace, "내 문제집", "설명"));

			Workspace otherWorkspace = workspaceRepository.save(Workspace.create("다른 워크스페이스", "소개"));
			ProblemBox otherProblemBox = problemBoxRepository.save(ProblemBox.create(otherWorkspace, "다른 문제집", "설명"));
			Problem otherProblem = createProblem(2000);
			WorkspaceProblem otherWorkspaceProblem = workspaceProblemRepository.save(
				WorkspaceProblem.create(otherProblemBox, otherProblem, null, null));

			// when & then
			assertThatThrownBy(() -> solutionService.getSolutions(
				myWorkspace.getId(), otherProblemBox.getId(), otherWorkspaceProblem.getId(),
				user.getId(), 0, 20))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND);

			assertThat(myProblemBox.getId()).isNotEqualTo(otherProblemBox.getId());
		}
	}
}
