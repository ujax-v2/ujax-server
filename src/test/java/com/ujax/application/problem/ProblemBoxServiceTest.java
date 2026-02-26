package com.ujax.application.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.problem.dto.response.ProblemBoxResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
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
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.CreateProblemBoxRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateProblemBoxRequest;

@SpringBootTest
@ActiveProfiles("test")
class ProblemBoxServiceTest {

	@Autowired
	private ProblemBoxService problemBoxService;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

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

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAllInBatch();
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
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

	@Nested
	@DisplayName("문제집 생성")
	class CreateProblemBox {

		@Test
		@DisplayName("OWNER가 문제집을 생성한다")
		void createByOwner() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);

			// when
			ProblemBoxResponse response = problemBoxService.createProblemBox(
				workspace.getId(), user.getId(), new CreateProblemBoxRequest("문제집", "설명"));

			// then
			assertThat(response).extracting("title", "description")
				.containsExactly("문제집", "설명");
		}

		@Test
		@DisplayName("MANAGER가 문제집을 생성한다")
		void createByManager() {
			// given
			User user = createUser("manager@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.MANAGER);

			// when
			ProblemBoxResponse response = problemBoxService.createProblemBox(
				workspace.getId(), user.getId(), new CreateProblemBoxRequest("문제집", "설명"));

			// then
			assertThat(response.title()).isEqualTo("문제집");
		}

		@Test
		@DisplayName("MEMBER가 문제집을 생성하면 오류가 발생한다")
		void createByMemberForbidden() {
			// given
			User user = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.MEMBER);

			// when & then
			assertThatThrownBy(() -> problemBoxService.createProblemBox(
				workspace.getId(), user.getId(), new CreateProblemBoxRequest("문제집", "설명")))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제집 수정")
	class UpdateProblemBox {

		@Test
		@DisplayName("문제집 제목과 설명을 수정한다")
		void update() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "원래 제목", "원래 설명"));

			// when
			ProblemBoxResponse response = problemBoxService.updateProblemBox(
				workspace.getId(), saved.getId(), user.getId(),
				new UpdateProblemBoxRequest("새 제목", "새 설명"));

			// then
			assertThat(response).extracting("title", "description")
				.containsExactly("새 제목", "새 설명");
		}

		@Test
		@DisplayName("MEMBER가 수정하면 오류가 발생한다")
		void updateByMemberForbidden() {
			// given
			User owner = createUser("owner@example.com");
			User member = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember ownerMember = createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			createMember(workspace, member, WorkspaceMemberRole.MEMBER);
			ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "제목", "설명"));

			// when & then
			assertThatThrownBy(() -> problemBoxService.updateProblemBox(
				workspace.getId(), saved.getId(), member.getId(),
				new UpdateProblemBoxRequest("새 제목", "새 설명")))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제집 삭제")
	class DeleteProblemBox {

		@Test
		@DisplayName("OWNER가 문제집을 삭제한다")
		void delete() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "제목", "설명"));

			// when
			problemBoxService.deleteProblemBox(workspace.getId(), saved.getId(), user.getId());

			// then
			assertThatThrownBy(() -> problemBoxService.getProblemBox(
				workspace.getId(), saved.getId(), user.getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROBLEM_BOX_NOT_FOUND);
		}

		@Test
		@DisplayName("MEMBER가 삭제하면 오류가 발생한다")
		void deleteByMemberForbidden() {
			// given
			User owner = createUser("owner@example.com");
			User member = createUser("member@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember ownerMember = createMember(workspace, owner, WorkspaceMemberRole.OWNER);
			createMember(workspace, member, WorkspaceMemberRole.MEMBER);
			ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "제목", "설명"));

			// when & then
			assertThatThrownBy(() -> problemBoxService.deleteProblemBox(
				workspace.getId(), saved.getId(), member.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("문제집 단건 조회")
	class GetProblemBox {

		@Test
		@DisplayName("문제집을 단건 조회한다")
		void get() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.OWNER);
			ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "제목", "설명"));

			// when
			ProblemBoxResponse response = problemBoxService.getProblemBox(
				workspace.getId(), saved.getId(), user.getId());

			// then
			assertThat(response).extracting("title", "description")
				.containsExactly("제목", "설명");
		}

		@Test
		@DisplayName("존재하지 않는 문제집을 조회하면 오류가 발생한다")
		void getNotFound() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			createMember(workspace, user, WorkspaceMemberRole.OWNER);

			// when & then
			assertThatThrownBy(() -> problemBoxService.getProblemBox(
				workspace.getId(), 999L, user.getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROBLEM_BOX_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("문제집 목록 조회")
	class ListProblemBoxes {

		@Test
		@DisplayName("워크스페이스의 문제집 목록을 조회한다")
		void list() {
			// given
			User user = createUser("owner@example.com");
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, user, WorkspaceMemberRole.OWNER);
			problemBoxRepository.save(ProblemBox.create(workspace, "문제집1", "설명1"));
			problemBoxRepository.save(ProblemBox.create(workspace, "문제집2", "설명2"));

			// when
			PageResponse<?> response = problemBoxService.listProblemBoxes(
				workspace.getId(), user.getId(), 0, 20);

			// then
			assertThat(response.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("멤버가 아니면 오류가 발생한다")
		void listNotMember() {
			// given
			User user = createUser("outsider@example.com");
			Workspace workspace = createWorkspace();

			// when & then
			assertThatThrownBy(() -> problemBoxService.listProblemBoxes(
				workspace.getId(), user.getId(), 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}
}
