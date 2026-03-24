package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceJoinRequestRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.external.s3.S3StorageService;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceServiceTest {

	@Autowired
	private WorkspaceService workspaceService;

	@Autowired
	private WorkspaceMembershipService workspaceMembershipService;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceJoinRequestRepository workspaceJoinRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private WorkspaceInviteMailer workspaceInviteMailer;

	@MockitoBean
	private S3StorageService s3StorageService;

	@BeforeEach
	void setUp() {
		workspaceJoinRequestRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("워크스페이스 생성")
	class CreateWorkspace {

		@Test
		@DisplayName("워크스페이스 생성 시 소유자 멤버가 생성된다")
		void createWorkspaceCreatesOwner() {
			// given
			User user = userRepository.save(User.createLocalUser("owner@example.com", Password.ofEncoded("password"), "유저"));

			// when
			Long workspaceId = workspaceService.createWorkspace("워크스페이스", "소개", user.getId()).id();
			Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();

			WorkspaceMember member = workspaceMemberRepository
				.findByWorkspace_IdAndUser_Id(workspace.getId(), user.getId())
				.orElseThrow();

			// then
			assertThat(member.getRole()).isEqualTo(WorkspaceMemberRole.OWNER);
			assertThat(workspace.getImageUrl()).isEqualTo(Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}

		@Test
		@DisplayName("이름이 중복되면 오류가 발생한다")
		void createWorkspaceDuplicateName() {
			// given
			User user = userRepository.save(User.createLocalUser("dup@example.com", Password.ofEncoded("password"), "유저"));
			workspaceRepository.save(Workspace.create("중복", "소개"));

			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspace("중복", "소개", user.getId()))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NAME_DUPLICATE);
		}

		@Test
		@DisplayName("이름이 비어 있으면 오류가 발생한다")
		void createWorkspaceInvalidName() {
			// given
			User user = userRepository.save(User.createLocalUser("blank@example.com", Password.ofEncoded("password"), "유저"));

			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspace(" ", "소개", user.getId()))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("이름이 너무 길면 오류가 발생한다")
		void createWorkspaceNameTooLong() {
			// given
			User user = userRepository.save(User.createLocalUser("longname@example.com", Password.ofEncoded("password"), "유저"));
			String longName = "a".repeat(51);

			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspace(longName, "소개", user.getId()))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("설명이 너무 길면 오류가 발생한다")
		void createWorkspaceInvalidDescription() {
			// given
			User user = userRepository.save(User.createLocalUser("desc@example.com", Password.ofEncoded("password"), "유저"));
			String longDescription = "a".repeat(201);

			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspace("워크스페이스", longDescription, user.getId()))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("유저가 없으면 오류가 발생한다")
		void createWorkspaceUserNotFound() {
			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspace("워크스페이스", "소개", 999L))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 이미지 Presigned URL 생성")
	class CreateWorkspaceImagePresignedUrl {

		@Test
		@DisplayName("오너는 이미지 Presigned URL을 생성할 수 있다")
		void createWorkspaceImagePresignedUrl() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-image@example.com", Password.ofEncoded("password"), "오너"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			WorkspaceImageUploadRequest request = new WorkspaceImageUploadRequest("image/png", 1024L);

			given(s3StorageService.generateWorkspaceImagePresignedUrl(workspace.getId(), "image/png", 1024L))
				.willReturn(new PresignedUrlResult("https://presigned-url", "https://image-url"));

			// when
			PresignedUrlResponse response = workspaceService.createWorkspaceImagePresignedUrl(
				workspace.getId(),
				owner.getId(),
				request
			);

			// then
			assertThat(response).extracting("presignedUrl", "imageUrl")
				.containsExactly("https://presigned-url", "https://image-url");
		}

		@Test
		@DisplayName("오너가 아니면 오류가 발생한다")
		void createWorkspaceImagePresignedUrlForbidden() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-image2@example.com", Password.ofEncoded("password"), "오너"));
			User member = userRepository.save(User.createLocalUser("member-image@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, member, WorkspaceMemberRole.MEMBER));
			WorkspaceImageUploadRequest request = new WorkspaceImageUploadRequest("image/png", 1024L);

			// when & then
			assertThatThrownBy(() -> workspaceService.createWorkspaceImagePresignedUrl(
				workspace.getId(),
				member.getId(),
				request
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}
	}

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("소유자가 아니면 수정할 수 없다")
		void updateWorkspaceForbidden() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner2@example.com", Password.ofEncoded("password"), "유저"));
			User memberUser = userRepository.save(User.createLocalUser("member@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(
				workspace.getId(),
				memberUser.getId(),
				"새 이름",
				null,
				null,
				null
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("워크스페이스가 없으면 오류가 발생한다")
		void updateWorkspaceNotFound() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-missing@example.com", Password.ofEncoded("password"), "유저"));

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(
				999L,
				owner.getId(),
				"새 이름",
				null,
				null,
				null
			))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NOT_FOUND);
		}

		@Test
		@DisplayName("이름과 소개를 수정할 수 있다")
		void updateWorkspace() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-update@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			workspaceService.updateWorkspace(workspace.getId(), owner.getId(), "새 이름", "새 소개", null, null);

			// then
			Workspace updated = workspaceRepository.findById(workspace.getId()).orElseThrow();
			assertThat(updated).extracting("name", "description", "hookUrl", "imageUrl")
				.containsExactly("새 이름", "새 소개", null, Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}

		@Test
		@DisplayName("웹훅만 수정할 수 있다")
		void updateWorkspaceOnlyWebhook() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-webhook@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			workspaceService.updateWorkspace(workspace.getId(), owner.getId(), null, null, "https://hook.example.com", null);

			// then
			Workspace updated = workspaceRepository.findById(workspace.getId()).orElseThrow();
			assertThat(updated).extracting("name", "description", "hookUrl", "imageUrl")
				.containsExactly("워크스페이스", "소개", "https://hook.example.com", Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}

		@Test
		@DisplayName("이미지를 수정할 수 있다")
		void updateWorkspaceOnlyImage() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-image-update@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			workspaceService.updateWorkspace(
				workspace.getId(),
				owner.getId(),
				null,
				null,
				null,
				"https://new-image.com/workspace.png"
			);

			// then
			Workspace updated = workspaceRepository.findById(workspace.getId()).orElseThrow();
			assertThat(updated).extracting("name", "description", "hookUrl", "imageUrl")
				.containsExactly("워크스페이스", "소개", null, "https://new-image.com/workspace.png");
		}

		@Test
		@DisplayName("수정 값이 모두 없으면 오류가 발생한다")
		void updateWorkspaceAllNull() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-null@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(workspace.getId(), owner.getId(), null, null, null, null))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("이름이 중복되면 오류가 발생한다")
		void updateWorkspaceDuplicateName() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-dup@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceRepository.save(Workspace.create("중복", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(
				workspace.getId(),
				owner.getId(),
				"중복",
				null,
				null,
				null
			))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NAME_DUPLICATE);
		}

		@Test
		@DisplayName("이름이 비어 있으면 오류가 발생한다")
		void updateWorkspaceInvalidName() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-blank@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(
				workspace.getId(),
				owner.getId(),
				" ",
				null,
				null,
				null
			))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("설명이 너무 길면 오류가 발생한다")
		void updateWorkspaceInvalidDescription() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-desc@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			String longDescription = "a".repeat(201);

			// when & then
			assertThatThrownBy(() -> workspaceService.updateWorkspace(
				workspace.getId(),
				owner.getId(),
				null,
				longDescription,
				null,
				null
			))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}
	}

	@Nested
	@DisplayName("워크스페이스 조회")
	class GetWorkspace {

		@Test
		@DisplayName("워크스페이스 상세를 조회한다")
		void getWorkspace() {
			// given
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when
			var response = workspaceService.getWorkspace(workspace.getId());

			// then
			assertThat(response).extracting("id", "name", "description", "imageUrl")
				.containsExactly(workspace.getId(), "워크스페이스", "소개", Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}

		@Test
		@DisplayName("워크스페이스가 없으면 오류가 발생한다")
		void getWorkspaceNotFound() {
			// when & then
			assertThatThrownBy(() -> workspaceService.getWorkspace(999L))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탐색")
	class ListWorkspaces {

		@Test
		@DisplayName("탐색 목록을 조회한다")
		void listWorkspaces() {
			// given
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when
			PageResponse<?> response = workspaceService.listWorkspaces(null, 0, 20);

			// then
			assertThat(response.getContent())
				.extracting("id", "imageUrl")
				.containsExactly(tuple(workspace.getId(), Workspace.DEFAULT_WORKSPACE_IMAGE_URL));
			assertThat(response.getPage().getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("탐색 목록은 최신순으로 조회한다")
		void listWorkspacesLatestOrder() {
			// given
			Workspace older = workspaceRepository.save(Workspace.create("오래된 공간", "소개"));
			Workspace newer = workspaceRepository.save(Workspace.create("최신 공간", "소개"));

			// when
			PageResponse<?> response = workspaceService.listWorkspaces(null, 0, 20);

			// then
			assertThat(response.getContent())
				.extracting("id")
				.containsExactly(newer.getId(), older.getId());
		}

		@Test
		@DisplayName("검색어가 공백이면 전체 목록을 조회한다")
		void searchWorkspacesInvalid() {
			// given
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when
			var response = workspaceService.listWorkspaces(" ", 0, 20);

			// then
			assertThat(response.getContent()).extracting("id")
				.containsExactly(workspace.getId());
		}

		@Test
		@DisplayName("페이지 값이 잘못되면 오류가 발생한다")
		void listWorkspacesInvalidPageable() {
			// when & then
			assertThatThrownBy(() -> workspaceService.listWorkspaces(null, -1, 0))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
		}

		@Test
		@DisplayName("이름으로 워크스페이스를 검색한다")
		void searchWorkspaces() {
			// given
			Workspace workspace = workspaceRepository.save(Workspace.create("테스트 공간", "소개"));

			// when
			var response = workspaceService.listWorkspaces("테스트", 0, 20);

			// then
			assertThat(response.getContent()).extracting("id")
				.containsExactly(workspace.getId());
		}

		@Test
		@DisplayName("검색 페이지 값이 잘못되면 오류가 발생한다")
		void searchWorkspacesInvalidPageable() {
			// when & then
			assertThatThrownBy(() -> workspaceService.listWorkspaces("테스트", -1, 0))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
		}
	}

	@Nested
	@DisplayName("워크스페이스 내 멤버 조회")
	class ListWorkspaceMembers {

		@Test
		@DisplayName("워크스페이스 멤버 목록을 조회한다")
		void listWorkspaceMembers() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-list@example.com", Password.ofEncoded("password"), "유저"));
			User managerUser = userRepository.save(
				User.createLocalUser("manager-list@example.com", Password.ofEncoded("password"), "매니저")
			);
			User memberUser = userRepository.save(User.createLocalUser("member-list@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember ownerMember = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER)
			);
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER)
			);
			WorkspaceMember manager = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, managerUser, WorkspaceMemberRole.MANAGER)
			);

			// when
			var response = workspaceMembershipService.listWorkspaceMembers(workspace.getId(), owner.getId(), 0, 20);

			// then
			assertThat(response.getContent())
				.extracting("workspaceMemberId", "email", "role")
				.containsExactly(
					tuple(ownerMember.getId(), owner.getEmail(), WorkspaceMemberRole.OWNER),
					tuple(manager.getId(), managerUser.getEmail(), WorkspaceMemberRole.MANAGER),
					tuple(member.getId(), memberUser.getEmail(), WorkspaceMemberRole.MEMBER)
				);
		}

		@Test
		@DisplayName("멤버 목록 조회 페이지 값이 잘못되면 오류가 발생한다")
		void listWorkspaceMembersInvalidPageable() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-list-invalid@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.listWorkspaceMembers(workspace.getId(), owner.getId(), -1, 0))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
		}

		@Test
		@DisplayName("멤버가 아니면 목록을 조회할 수 없다")
		void listWorkspaceMembersForbidden() {
			// given
			User outsider = userRepository.save(User.createLocalUser("outsider@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.listWorkspaceMembers(workspace.getId(), outsider.getId(), 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}

		@Test
		@DisplayName("소프트 삭제된 멤버는 멤버 목록에서 제외된다")
		void listWorkspaceMembersExcludeSoftDeletedMembers() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-soft-member@example.com", Password.ofEncoded("password"), "소유자"));
			User activeUser = userRepository.save(User.createLocalUser("active-soft-member@example.com", Password.ofEncoded("password"), "활성멤버"));
			User deletedUser = userRepository.save(User.createLocalUser("deleted-soft-member@example.com", Password.ofEncoded("password"), "삭제멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember ownerMember = workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			WorkspaceMember activeMember = workspaceMemberRepository.save(WorkspaceMember.create(workspace, activeUser, WorkspaceMemberRole.MEMBER));
			WorkspaceMember deletedMember = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, deletedUser, WorkspaceMemberRole.MEMBER)
			);
			workspaceMemberRepository.delete(deletedMember);

			// when
			var response = workspaceMembershipService.listWorkspaceMembers(workspace.getId(), owner.getId(), 0, 20);

			// then
			assertThat(response.getContent())
				.extracting("workspaceMemberId")
				.containsExactlyInAnyOrder(ownerMember.getId(), activeMember.getId())
				.doesNotContain(deletedMember.getId());
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 조회")
	class GetMyWorkspaceMember {

		@Test
		@DisplayName("멤버는 자신의 정보를 조회할 수 있다")
		void getMyWorkspaceMember() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-self@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER)
			);

			// when
			var response = workspaceMembershipService.getMyWorkspaceMember(workspace.getId(), owner.getId());

			// then
			assertThat(response).extracting("workspaceMemberId", "nickname", "role")
				.containsExactly(member.getId(), owner.getName(), WorkspaceMemberRole.OWNER);
		}

		@Test
		@DisplayName("멤버가 아니면 조회할 수 없다")
		void getMyWorkspaceMemberForbidden() {
			// given
			User outsider = userRepository.save(User.createLocalUser("outsider-self@example.com", Password.ofEncoded("password"), "외부"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.getMyWorkspaceMember(workspace.getId(), outsider.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("워크스페이스 닉네임 수정")
	class UpdateMyWorkspaceNickname {

		@Test
		@DisplayName("멤버는 닉네임을 수정할 수 있다")
		void updateNickname() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-nick@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.MEMBER)
			);

			// when
			var response = workspaceMembershipService.updateMyWorkspaceNickname(workspace.getId(), owner.getId(), "새닉네임");

			// then
			WorkspaceMember updated = workspaceMemberRepository.findById(member.getId()).orElseThrow();
			assertThat(updated.getNickname()).isEqualTo("새닉네임");
			assertThat(response.nickname()).isEqualTo("새닉네임");
		}

		@Test
		@DisplayName("닉네임이 비어 있으면 오류가 발생한다")
		void updateNicknameInvalid() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-nick2@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.updateMyWorkspaceNickname(workspace.getId(), owner.getId(), " "))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("멤버가 아니면 닉네임을 수정할 수 없다")
		void updateNicknameForbidden() {
			// given
			User outsider = userRepository.save(User.createLocalUser("outsider-nick@example.com", Password.ofEncoded("password"), "외부"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.updateMyWorkspaceNickname(workspace.getId(), outsider.getId(), "새닉네임"))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 권한 변경")
	class UpdateWorkspaceMemberRole {

		@Test
		@DisplayName("소유자가 다른 멤버를 소유자로 변경하면 자신은 매니저가 된다")
		void transferOwner() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-role@example.com", Password.ofEncoded("password"), "소유자"));
			User memberUser = userRepository.save(User.createLocalUser("member-role@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember owner = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER)
			);
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER)
			);

			// when
			workspaceMembershipService.updateWorkspaceMemberRole(
				workspace.getId(),
				ownerUser.getId(),
				member.getId(),
				WorkspaceMemberRole.OWNER
			);

			// then
			WorkspaceMember updatedOwner = workspaceMemberRepository.findById(owner.getId()).orElseThrow();
			WorkspaceMember updatedMember = workspaceMemberRepository.findById(member.getId()).orElseThrow();
			assertThat(updatedOwner.getRole()).isEqualTo(WorkspaceMemberRole.MANAGER);
			assertThat(updatedMember.getRole()).isEqualTo(WorkspaceMemberRole.OWNER);
		}

		@Test
		@DisplayName("소유자가 아니면 권한을 변경할 수 없다")
		void updateRoleForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-role2@example.com", Password.ofEncoded("password"), "소유자"));
			User memberUser = userRepository.save(User.createLocalUser("member-role2@example.com", Password.ofEncoded("password"), "멤버"));
			User targetUser = userRepository.save(User.createLocalUser("target-role2@example.com", Password.ofEncoded("password"), "대상"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));
			WorkspaceMember target = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, targetUser, WorkspaceMemberRole.MEMBER)
			);

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.updateWorkspaceMemberRole(
				workspace.getId(),
				memberUser.getId(),
				target.getId(),
				WorkspaceMemberRole.MANAGER
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("소유자 권한은 변경할 수 없다")
		void updateRoleOwnerForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-role3@example.com", Password.ofEncoded("password"), "소유자"));
			User memberUser = userRepository.save(User.createLocalUser("member-role3@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember owner = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER)
			);
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.updateWorkspaceMemberRole(
				workspace.getId(),
				ownerUser.getId(),
				owner.getId(),
				WorkspaceMemberRole.MANAGER
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("대상 멤버가 없으면 오류가 발생한다")
		void updateRoleMemberNotFound() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-role4@example.com", Password.ofEncoded("password"), "소유자"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.updateWorkspaceMemberRole(
				workspace.getId(),
				ownerUser.getId(),
				999L,
				WorkspaceMemberRole.MANAGER
			))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 추방")
	class RemoveWorkspaceMember {

		@Test
		@DisplayName("매니저는 멤버를 추방할 수 있다")
		void removeByManager() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove@example.com", Password.ofEncoded("password"), "소유자"));
			User managerUser = userRepository.save(User.createLocalUser("manager-remove@example.com", Password.ofEncoded("password"), "매니저"));
			User memberUser = userRepository.save(User.createLocalUser("member-remove@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, managerUser, WorkspaceMemberRole.MANAGER));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER)
			);

			// when
			workspaceMembershipService.removeWorkspaceMember(workspace.getId(), managerUser.getId(), member.getId());

			// then
			WorkspaceMember deleted = workspaceMemberRepository.findById(member.getId()).orElseThrow();
			assertThat(deleted.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("매니저는 매니저를 추방할 수 없다")
		void removeManagerForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove2@example.com", Password.ofEncoded("password"), "소유자"));
			User managerUser = userRepository.save(User.createLocalUser("manager-remove2@example.com", Password.ofEncoded("password"), "매니저"));
			User targetUser = userRepository.save(User.createLocalUser("target-remove@example.com", Password.ofEncoded("password"), "매니저2"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, managerUser, WorkspaceMemberRole.MANAGER));
			WorkspaceMember target = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, targetUser, WorkspaceMemberRole.MANAGER)
			);

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), managerUser.getId(), target.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("소유자는 추방할 수 없다")
		void removeOwnerForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove3@example.com", Password.ofEncoded("password"), "소유자"));
			User managerUser = userRepository.save(User.createLocalUser("manager-remove3@example.com", Password.ofEncoded("password"), "매니저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember owner = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER)
			);
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, managerUser, WorkspaceMemberRole.MANAGER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), managerUser.getId(), owner.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("자기 자신은 추방할 수 없다")
		void removeSelfForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove4@example.com", Password.ofEncoded("password"), "소유자"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			WorkspaceMember owner = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER)
			);

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), ownerUser.getId(), owner.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("멤버가 아니면 추방할 수 없다")
		void removeMemberForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove5@example.com", Password.ofEncoded("password"), "소유자"));
			User outsider = userRepository.save(User.createLocalUser("outsider-remove@example.com", Password.ofEncoded("password"), "외부"));
			User memberUser = userRepository.save(User.createLocalUser("member-remove5@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER)
			);

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), outsider.getId(), member.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}

		@Test
		@DisplayName("일반 멤버는 다른 멤버를 추방할 수 없다")
		void removeByMemberForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove6@example.com", Password.ofEncoded("password"), "소유자"));
			User memberUser = userRepository.save(User.createLocalUser("member-remove6@example.com", Password.ofEncoded("password"), "멤버"));
			User targetUser = userRepository.save(User.createLocalUser("target-remove6@example.com", Password.ofEncoded("password"), "대상"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));
			WorkspaceMember target = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, targetUser, WorkspaceMemberRole.MEMBER)
			);

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), memberUser.getId(), target.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("대상 멤버가 없으면 오류가 발생한다")
		void removeMemberNotFound() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-remove6@example.com", Password.ofEncoded("password"), "소유자"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.removeWorkspaceMember(workspace.getId(), ownerUser.getId(), 999L))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탈퇴")
	class LeaveWorkspace {

		@Test
		@DisplayName("멤버는 워크스페이스를 탈퇴할 수 있다")
		void leaveWorkspace() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-leave@example.com", Password.ofEncoded("password"), "소유자"));
			User memberUser = userRepository.save(User.createLocalUser("member-leave@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER)
			);

			// when
			workspaceMembershipService.leaveWorkspace(workspace.getId(), memberUser.getId());

			// then
			WorkspaceMember deleted = workspaceMemberRepository.findById(member.getId()).orElseThrow();
			assertThat(deleted.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("소유자는 워크스페이스를 탈퇴할 수 없다")
		void leaveWorkspaceOwnerForbidden() {
			// given
			User ownerUser = userRepository.save(User.createLocalUser("owner-leave2@example.com", Password.ofEncoded("password"), "소유자"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, ownerUser, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.leaveWorkspace(workspace.getId(), ownerUser.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_FORBIDDEN);
		}

		@Test
		@DisplayName("멤버가 아니면 탈퇴할 수 없다")
		void leaveWorkspaceForbidden() {
			// given
			User outsider = userRepository.save(User.createLocalUser("outsider-leave@example.com", Password.ofEncoded("password"), "외부"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.leaveWorkspace(workspace.getId(), outsider.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("나의 워크스페이스 목록")
	class ListMyWorkspaces {

		@Test
		@DisplayName("유저가 속한 워크스페이스 목록을 조회한다")
		void listMyWorkspaces() {
			// given
			User user = userRepository.save(User.createLocalUser("mine@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));

			// when
			var response = workspaceService.listMyWorkspaces(user.getId());

			// then
			assertThat(response).extracting("id", "imageUrl")
				.containsExactly(tuple(workspace.getId(), Workspace.DEFAULT_WORKSPACE_IMAGE_URL));
		}

		@Test
		@DisplayName("유저 워크스페이스 목록은 최신순으로 정렬된다")
		void listMyWorkspacesOrderedByLatest() {
			// given
			User user = userRepository.save(User.createLocalUser("mine-order@example.com", Password.ofEncoded("password"), "유저"));
			Workspace older = workspaceRepository.save(Workspace.create("오래된 워크스페이스", "소개"));
			Workspace newer = workspaceRepository.save(Workspace.create("새 워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(older, user, WorkspaceMemberRole.MEMBER));
			workspaceMemberRepository.save(WorkspaceMember.create(newer, user, WorkspaceMemberRole.MEMBER));

			// when
			var response = workspaceService.listMyWorkspaces(user.getId());

			// then
			assertThat(response).extracting("id")
				.containsExactly(newer.getId(), older.getId());
		}

		@Test
		@DisplayName("소프트 삭제된 멤버십/워크스페이스는 내 목록에서 제외된다")
		void listMyWorkspacesExcludeSoftDeleted() {
			// given
			User user = userRepository.save(User.createLocalUser("mine-soft@example.com", Password.ofEncoded("password"), "유저"));
			Workspace activeWorkspace = workspaceRepository.save(Workspace.create("활성 워크스페이스", "소개"));
			Workspace deletedMembershipWorkspace = workspaceRepository.save(Workspace.create("탈퇴 워크스페이스", "소개"));
			Workspace deletedWorkspace = workspaceRepository.save(Workspace.create("삭제 워크스페이스", "소개"));

			workspaceMemberRepository.save(WorkspaceMember.create(activeWorkspace, user, WorkspaceMemberRole.MEMBER));
			WorkspaceMember deletedMembership = workspaceMemberRepository.save(
				WorkspaceMember.create(deletedMembershipWorkspace, user, WorkspaceMemberRole.MEMBER)
			);
			workspaceMemberRepository.save(WorkspaceMember.create(deletedWorkspace, user, WorkspaceMemberRole.MEMBER));

			workspaceMemberRepository.delete(deletedMembership);
			workspaceRepository.delete(deletedWorkspace);

			// when
			var response = workspaceService.listMyWorkspaces(user.getId());

			// then
			assertThat(response)
				.extracting("id")
				.containsExactly(activeWorkspace.getId());
		}
	}

	@Nested
	@DisplayName("워크스페이스 설정 조회")
	class GetWorkspaceSettings {

		@Test
		@DisplayName("소유자는 설정 정보를 조회할 수 있다")
		void getWorkspaceSettings() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner3@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceService.updateWorkspace(
				workspace.getId(),
				owner.getId(),
				null,
				null,
				"https://meeting.ssafy.com/hooks/j8ki3jbhg38t7dbgpwse9ak9jh",
				null
			);

			// when
			WorkspaceSettingsResponse response = workspaceService.getWorkspaceSettings(workspace.getId(), owner.getId());

			// then
			assertThat(response).extracting("id", "imageUrl", "hookUrl")
				.containsExactly(
					workspace.getId(),
					Workspace.DEFAULT_WORKSPACE_IMAGE_URL,
					"https://meeting.ssafy.com/hooks/j8ki3j*************e9ak9jh"
				);
		}

		@Test
		@DisplayName("소유자가 아니면 설정을 조회할 수 없다")
		void getWorkspaceSettingsForbidden() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner3-2@example.com", Password.ofEncoded("password"), "유저"));
			User memberUser = userRepository.save(User.createLocalUser("member3-2@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceService.getWorkspaceSettings(workspace.getId(), memberUser.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("워크스페이스가 없으면 오류가 발생한다")
		void getWorkspaceSettingsNotFound() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner3-3@example.com", Password.ofEncoded("password"), "유저"));

			// when & then
			assertThatThrownBy(() -> workspaceService.getWorkspaceSettings(999L, owner.getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 삭제")
	class DeleteWorkspace {

		@Test
		@DisplayName("소유자는 워크스페이스를 삭제할 수 있다")
		void deleteWorkspace() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-delete@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			workspaceService.deleteWorkspace(workspace.getId(), owner.getId());

			// then
			Workspace deleted = workspaceRepository.findById(workspace.getId()).orElseThrow();
			assertThat(deleted.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("소유자가 아니면 삭제할 수 없다")
		void deleteWorkspaceForbidden() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-delete2@example.com", Password.ofEncoded("password"), "유저"));
			User memberUser = userRepository.save(User.createLocalUser("member-delete@example.com", Password.ofEncoded("password"), "멤버"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceService.deleteWorkspace(workspace.getId(), memberUser.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("워크스페이스가 없으면 오류가 발생한다")
		void deleteWorkspaceNotFound() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-delete3@example.com", Password.ofEncoded("password"), "유저"));

			// when & then
			assertThatThrownBy(() -> workspaceService.deleteWorkspace(999L, owner.getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 초대")
	class InviteWorkspaceMember {

		@Test
		@DisplayName("소유자는 이메일로 멤버를 초대할 수 있다")
		void inviteWorkspaceMember() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner4@example.com", Password.ofEncoded("password"), "유저"));
			User target = userRepository.save(User.createLocalUser("invite@example.com", Password.ofEncoded("password"), "초대"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			workspaceMembershipService.inviteWorkspaceMember(workspace.getId(), owner.getId(), target.getEmail());

			// then
			WorkspaceMember member = workspaceMemberRepository
				.findByWorkspace_IdAndUser_Id(workspace.getId(), target.getId())
				.orElseThrow();
			assertThat(member.getRole()).isEqualTo(WorkspaceMemberRole.MEMBER);
		}

		@Test
		@DisplayName("소유자가 아니면 초대할 수 없다")
		void inviteWorkspaceMemberForbidden() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner5@example.com", Password.ofEncoded("password"), "유저"));
			User memberUser = userRepository.save(User.createLocalUser("member2@example.com", Password.ofEncoded("password"), "멤버"));
			User target = userRepository.save(User.createLocalUser("invite2@example.com", Password.ofEncoded("password"), "초대"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, memberUser, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.inviteWorkspaceMember(
				workspace.getId(),
				memberUser.getId(),
				target.getEmail()
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("이미 참여한 멤버면 오류가 발생한다")
		void inviteWorkspaceMemberDuplicate() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner6@example.com", Password.ofEncoded("password"), "유저"));
			User target = userRepository.save(User.createLocalUser("invite3@example.com", Password.ofEncoded("password"), "초대"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, target, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.inviteWorkspaceMember(
				workspace.getId(),
				owner.getId(),
				target.getEmail()
			))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WORKSPACE_MEMBER);
		}

		@Test
		@DisplayName("탈퇴한 멤버는 다시 초대하면 복원된다")
		void inviteWorkspaceMemberRestore() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner8@example.com", Password.ofEncoded("password"), "유저"));
			User target = userRepository.save(User.createLocalUser("invite4@example.com", Password.ofEncoded("password"), "초대"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			WorkspaceMember member = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, target, WorkspaceMemberRole.MEMBER)
			);
			workspaceMemberRepository.delete(member);

			// when
			workspaceMembershipService.inviteWorkspaceMember(workspace.getId(), owner.getId(), target.getEmail());

			// then
			WorkspaceMember restored = workspaceMemberRepository
				.findByWorkspaceIdAndUserIdIncludingDeleted(workspace.getId(), target.getId())
				.orElseThrow();
			assertThat(restored.isDeleted()).isFalse();
			assertThat(restored.getRole()).isEqualTo(WorkspaceMemberRole.MEMBER);
		}

		@Test
		@DisplayName("유저가 없으면 오류가 발생한다")
		void inviteWorkspaceMemberUserNotFound() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner7@example.com", Password.ofEncoded("password"), "유저"));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> workspaceMembershipService.inviteWorkspaceMember(
				workspace.getId(),
				owner.getId(),
				"missing@example.com"
			))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}
	}

}
