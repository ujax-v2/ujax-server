package com.ujax.application.problem;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ujax.application.problem.dto.response.WorkspaceProblemResponse;
import com.ujax.application.webhook.WebhookAlertService;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblemRepository;
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
import com.ujax.global.exception.common.BusinessRuleViolationException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.CreateWorkspaceProblemRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateWorkspaceProblemRequest;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceProblemServiceTest {

	@Autowired
	private WorkspaceProblemService workspaceProblemService;

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

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private WebhookAlertService webhookAlertService;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAllInBatch();
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		problemRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	private User createUser(String email) {
		return userRepository.save(User.createLocalUser(email, Password.ofEncoded("password"), "유저"));
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
	}

	private Workspace createWorkspaceWithHookUrl() {
		Workspace workspace = Workspace.create("워크스페이스", "소개");
		workspace.update(null, null, "https://hook.example.com", null);
		return workspaceRepository.save(workspace);
	}

	private Workspace createWorkspaceWithBlankHookUrl() {
		Workspace workspace = Workspace.create("워크스페이스", "소개");
		workspace.update(null, null, " ", null);
		return workspaceRepository.save(workspace);
	}

	private WorkspaceMember createMember(Workspace workspace, User user, WorkspaceMemberRole role) {
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, role));
	}

	private ProblemBox createProblemBox(Workspace workspace) {
		return problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
	}

	private Problem createProblem(int problemNumber, String title) {
		return problemRepository.save(
			Problem.create(problemNumber, title, "Bronze V", "1초", "256MB", "설명", "입력", "출력",
				"https://boj.kr/" + problemNumber));
	}

	@Nested
	@DisplayName("문제 등록")
	class CreateWorkspaceProblem {

		@Test
		@DisplayName("OWNER가 문제를 등록한다")
		void createByOwner() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");
			LocalDateTime deadline = LocalDateTime.of(2026, 3, 1, 0, 0);

			WorkspaceProblemResponse response = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), deadline, null));

			assertThat(response).extracting("problemNumber", "title", "deadline")
				.containsExactly(1000, "A+B", deadline);
			then(webhookAlertService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("scheduledAt이 있으면 알림 예약을 요청한다")
		void createReservesAlertWhenScheduledAtExists() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspaceWithHookUrl();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 10, 9, 0);

			WorkspaceProblemResponse response = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, scheduledAt));

			then(webhookAlertService).should()
				.reserveOrUpdate(response.id(), workspace.getId(), scheduledAt, user.getId());
		}

		@Test
		@DisplayName("scheduledAt이 있는데 hookUrl이 없으면 오류가 발생한다")
		void createWithScheduledAtWithoutHookUrl() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 10, 9, 0);

			assertThatThrownBy(() -> workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, scheduledAt)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUSINESS_RULE_VIOLATION);
			then(webhookAlertService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("scheduledAt이 있는데 hookUrl이 공백이면 오류가 발생한다")
		void createWithScheduledAtBlankHookUrl() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspaceWithBlankHookUrl();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 10, 9, 0);

			assertThatThrownBy(() -> workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, scheduledAt)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUSINESS_RULE_VIOLATION);
			then(webhookAlertService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("이미 등록된 문제를 중복 등록하면 오류가 발생한다")
		void createDuplicate() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));

			assertThatThrownBy(() -> workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null)))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_WORKSPACE_PROBLEM);
		}

		@Test
		@DisplayName("존재하지 않는 문제 ID로 등록하면 오류가 발생한다")
		void createWithInvalidProblemId() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);

			assertThatThrownBy(() -> workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(999L, null, null)))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROBLEM_NOT_FOUND);
		}

		@Test
		@DisplayName("MEMBER가 등록하면 오류가 발생한다")
		void createByMemberForbidden() {
			User user = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.MEMBER);
			User owner = createUser("owner@example.com");
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			assertThatThrownBy(() -> workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null)))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제 수정")
	class UpdateWorkspaceProblem {

		@Test
		@DisplayName("deadline과 scheduledAt을 수정한다")
		void update() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspaceWithHookUrl();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));

			LocalDateTime newDeadline = LocalDateTime.of(2026, 4, 1, 0, 0);
			LocalDateTime newScheduledAt = LocalDateTime.of(2026, 3, 15, 0, 0);

			WorkspaceProblemResponse response = workspaceProblemService.updateWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), user.getId(),
				new UpdateWorkspaceProblemRequest(newDeadline, newScheduledAt));

			assertThat(response).extracting("deadline", "scheduledAt")
				.containsExactly(newDeadline, newScheduledAt);
			then(webhookAlertService).should()
				.reserveOrUpdate(created.id(), workspace.getId(), newScheduledAt, user.getId());
		}

		@Test
		@DisplayName("scheduledAt을 제거하면 알림 비활성화를 요청한다")
		void updateDeactivatesAlertWhenScheduledAtBecomesNull() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspaceWithHookUrl();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 15, 0, 0);

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, scheduledAt));
			Mockito.reset(webhookAlertService);

			WorkspaceProblemResponse response = workspaceProblemService.updateWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), user.getId(),
				new UpdateWorkspaceProblemRequest(null, null));

			assertThat(response.scheduledAt()).isNull();
			then(webhookAlertService).should().deactivate(created.id(), user.getId());
		}

		@Test
		@DisplayName("scheduledAt으로 수정하는데 hookUrl이 없으면 오류가 발생한다")
		void updateWithScheduledAtWithoutHookUrl() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 20, 9, 0);

			assertThatThrownBy(() -> workspaceProblemService.updateWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), user.getId(),
				new UpdateWorkspaceProblemRequest(null, scheduledAt)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUSINESS_RULE_VIOLATION);
			then(webhookAlertService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("scheduledAt으로 수정하는데 hookUrl이 공백이면 오류가 발생한다")
		void updateWithScheduledAtBlankHookUrl() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspaceWithBlankHookUrl();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 20, 9, 0);

			assertThatThrownBy(() -> workspaceProblemService.updateWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), user.getId(),
				new UpdateWorkspaceProblemRequest(null, scheduledAt)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUSINESS_RULE_VIOLATION);
			then(webhookAlertService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("MEMBER가 수정하면 오류가 발생한다")
		void updateByMemberForbidden() {
			User owner = createUser("owner@example.com");
			User member = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			createMember(workspace, member, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), owner.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));

			assertThatThrownBy(() -> workspaceProblemService.updateWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), member.getId(),
				new UpdateWorkspaceProblemRequest(null, null)))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제 삭제")
	class DeleteWorkspaceProblem {

		@Test
		@DisplayName("OWNER가 문제를 삭제한다")
		void delete() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));

			workspaceProblemService.deleteWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), user.getId());

			PageResponse<?> response = workspaceProblemService.listWorkspaceProblems(
				workspace.getId(), problemBox.getId(), user.getId(), 0, 20);
			assertThat(response.getContent()).isEmpty();
			then(webhookAlertService).should().cancel(created.id(), user.getId());
		}

		@Test
		@DisplayName("MEMBER가 삭제하면 오류가 발생한다")
		void deleteByMemberForbidden() {
			User owner = createUser("owner@example.com");
			User member = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			createMember(workspace, member, WorkspaceMemberRole.MEMBER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem = createProblem(1000, "A+B");

			WorkspaceProblemResponse created = workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), owner.getId(),
				new CreateWorkspaceProblemRequest(problem.getId(), null, null));

			assertThatThrownBy(() -> workspaceProblemService.deleteWorkspaceProblem(
				workspace.getId(), problemBox.getId(), created.id(), member.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제 목록 조회")
	class ListWorkspaceProblems {

		@Test
		@DisplayName("문제집의 문제 목록을 조회한다")
		void list() {
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);
			Problem problem1 = createProblem(1000, "A+B");
			Problem problem2 = createProblem(1001, "A-B");

			workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem1.getId(), null, null));
			workspaceProblemService.createWorkspaceProblem(
				workspace.getId(), problemBox.getId(), user.getId(),
				new CreateWorkspaceProblemRequest(problem2.getId(), null, null));

			PageResponse<?> response = workspaceProblemService.listWorkspaceProblems(
				workspace.getId(), problemBox.getId(), user.getId(), 0, 20);

			assertThat(response.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("멤버가 아니면 오류가 발생한다")
		void listNotMember() {
			User owner = createUser("owner@example.com");
			User outsider = createUser("outsider@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			ProblemBox problemBox = createProblemBox(workspace);

			assertThatThrownBy(() -> workspaceProblemService.listWorkspaceProblems(
				workspace.getId(), problemBox.getId(), outsider.getId(), 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}
}
