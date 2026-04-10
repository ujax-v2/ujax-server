package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatus;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.problem.AlgorithmTagRepository;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceJoinRequestRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.infrastructure.external.s3.S3StorageService;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceJoinRequestServiceTest {

	@Autowired
	private WorkspaceJoinRequestService workspaceJoinRequestService;

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

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private SolutionRepository solutionRepository;

	@Autowired
	private WorkspaceProblemRepository workspaceProblemRepository;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private AlgorithmTagRepository algorithmTagRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private S3StorageService s3StorageService;

	@BeforeEach
	void setUp() {
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		solutionRepository.deleteAllInBatch();
		workspaceJoinRequestRepository.deleteAllInBatch();
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		jdbcTemplate.update("DELETE FROM problem_algorithm");
		problemRepository.deleteAllInBatch();
		algorithmTagRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("워크스페이스 가입 신청")
	class CreateJoinRequest {

		@Test
		@DisplayName("워크스페이스 가입 신청을 생성할 수 있다")
		void createJoinRequest() {
			// given
			User owner = userRepository.save(User.createLocalUser("owner-join@example.com", Password.ofEncoded("password"), "오너"));
			User applicant = userRepository.save(
				User.createLocalUser("applicant-join@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			var response = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// then
			assertThat(response.workspaceId()).isEqualTo(workspace.getId());
			assertThat(response.requestId()).isNotNull();
			assertThat(response.createdAt()).isNotNull();
		}

		@Test
		@DisplayName("동일 사용자의 대기중 가입 신청은 중복 생성할 수 없다")
		void createJoinRequestDuplicatePending() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-join-dup@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-join-dup@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when & then
			assertThatThrownBy(() -> workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId()))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_JOIN_REQUEST_ALREADY_PENDING);
		}

		@Test
		@DisplayName("이미 멤버면 가입 신청을 생성할 수 없다")
		void createJoinRequestAlreadyMember() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-join-member@example.com", Password.ofEncoded("password"), "오너")
			);
			User member = userRepository.save(
				User.createLocalUser("member-join-member@example.com", Password.ofEncoded("password"), "멤버")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, member, WorkspaceMemberRole.MEMBER));

			// when & then
			assertThatThrownBy(() -> workspaceJoinRequestService.createJoinRequest(workspace.getId(), member.getId()))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WORKSPACE_MEMBER);
		}
	}

	@Nested
	@DisplayName("워크스페이스 가입 신청 수락")
	class ApproveJoinRequest {

		@Test
		@DisplayName("소유자는 가입 신청을 수락할 수 있다")
		void approveJoinRequest() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-approve@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-approve@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			workspaceJoinRequestService.approveJoinRequest(workspace.getId(), owner.getId(), created.requestId());

			// then
			WorkspaceMember member = workspaceMemberRepository
				.findByWorkspace_IdAndUser_Id(workspace.getId(), applicant.getId())
				.orElseThrow();
			assertThat(member.getRole()).isEqualTo(WorkspaceMemberRole.MEMBER);
			assertThat(workspaceJoinRequestRepository.findById(created.requestId())).isEmpty();
		}

		@Test
		@DisplayName("소유자가 아니면 가입 신청을 수락할 수 없다")
		void approveJoinRequestForbidden() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-approve-forbidden@example.com", Password.ofEncoded("password"), "오너")
			);
			User manager = userRepository.save(
				User.createLocalUser("manager-approve-forbidden@example.com", Password.ofEncoded("password"), "매니저")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-approve-forbidden@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, manager, WorkspaceMemberRole.MANAGER));
			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when & then
			assertThatThrownBy(() ->
				workspaceJoinRequestService.approveJoinRequest(workspace.getId(), manager.getId(), created.requestId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}

		@Test
		@DisplayName("삭제된 멤버는 승인 시 복구된다")
		void approveJoinRequestRestoreDeletedMember() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-approve-restore@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-approve-restore@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			WorkspaceMember deletedMember = workspaceMemberRepository.save(
				WorkspaceMember.create(workspace, applicant, WorkspaceMemberRole.MEMBER)
			);
			workspaceMemberRepository.delete(deletedMember);
			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			workspaceJoinRequestService.approveJoinRequest(workspace.getId(), owner.getId(), created.requestId());

			// then
			WorkspaceMember restored = workspaceMemberRepository
				.findByWorkspaceIdAndUserIdIncludingDeleted(workspace.getId(), applicant.getId())
				.orElseThrow();
			assertThat(restored.isDeleted()).isFalse();
			assertThat(restored.getRole()).isEqualTo(WorkspaceMemberRole.MEMBER);
			assertThat(restored.getNickname()).isEqualTo(applicant.getName());
			assertThat(workspaceJoinRequestRepository.findById(created.requestId())).isEmpty();
		}
	}

	@Nested
	@DisplayName("내 가입 신청 상태 조회")
	class GetMyJoinRequestStatus {

		@Test
		@DisplayName("이력이 없으면 NONE으로 조회된다")
		void getMyJoinRequestStatusNone() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-my-join-status@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-my-join-status@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));

			// when
			var response = workspaceJoinRequestService.getMyJoinRequestStatus(workspace.getId(), applicant.getId());

			// then
			assertThat(response).extracting("isMember", "joinRequestStatus", "canApply")
				.containsExactly(false, WorkspaceMyJoinRequestStatus.NONE, true);
		}

		@Test
		@DisplayName("대기중 신청이 있으면 PENDING으로 조회된다")
		void getMyJoinRequestStatusPending() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-my-join-status-pending@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-my-join-status-pending@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			var response = workspaceJoinRequestService.getMyJoinRequestStatus(workspace.getId(), applicant.getId());

			// then
			assertThat(response).extracting("isMember", "joinRequestStatus", "canApply")
				.containsExactly(false, WorkspaceMyJoinRequestStatus.PENDING, false);
		}

		@Test
		@DisplayName("현재 멤버면 MEMBER로 조회된다")
		void getMyJoinRequestStatusMember() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-my-join-status-member@example.com", Password.ofEncoded("password"), "오너")
			);
			User member = userRepository.save(
				User.createLocalUser("member-my-join-status-member@example.com", Password.ofEncoded("password"), "멤버")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, member, WorkspaceMemberRole.MEMBER));

			// when
			var response = workspaceJoinRequestService.getMyJoinRequestStatus(workspace.getId(), member.getId());

			// then
			assertThat(response).extracting("isMember", "joinRequestStatus", "canApply")
				.containsExactly(true, WorkspaceMyJoinRequestStatus.MEMBER, false);
		}

		@Test
		@DisplayName("거부된 신청은 NONE으로 조회된다")
		void getMyJoinRequestStatusRejectedReturnsNone() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-my-join-status-rejected@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-my-join-status-rejected@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());
			workspaceJoinRequestService.rejectJoinRequest(workspace.getId(), owner.getId(), created.requestId());

			// when
			var response = workspaceJoinRequestService.getMyJoinRequestStatus(workspace.getId(), applicant.getId());

			// then
			assertThat(response).extracting("isMember", "joinRequestStatus", "canApply")
				.containsExactly(false, WorkspaceMyJoinRequestStatus.NONE, true);
		}
	}

	@Nested
	@DisplayName("워크스페이스 가입 신청 목록")
	class ListJoinRequests {

		@Test
		@DisplayName("소유자는 가입 신청 목록을 조회할 수 있다")
		void listJoinRequests() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-list-join-request@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-list-join-request@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			var response = workspaceJoinRequestService.listJoinRequests(workspace.getId(), owner.getId(), 0, 20);

			// then
			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().getFirst()).extracting("workspaceId", "applicantUserId")
				.containsExactly(workspace.getId(), applicant.getId());
		}
	}

	@Nested
	@DisplayName("워크스페이스 가입 신청 거부")
	class RejectJoinRequest {

		@Test
		@DisplayName("소유자는 가입 신청을 거부할 수 있다")
		void rejectJoinRequest() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-reject-join-request@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-reject-join-request@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			workspaceJoinRequestService.rejectJoinRequest(workspace.getId(), owner.getId(), created.requestId());

			// then
			assertThat(workspaceJoinRequestRepository.findById(created.requestId())).isEmpty();
		}
	}

	@Nested
	@DisplayName("워크스페이스 가입 신청 취소")
	class CancelJoinRequest {

		@Test
		@DisplayName("사용자는 자신의 가입 신청을 취소할 수 있다")
		void cancelJoinRequest() {
			// given
			User owner = userRepository.save(
				User.createLocalUser("owner-cancel-join-request@example.com", Password.ofEncoded("password"), "오너")
			);
			User applicant = userRepository.save(
				User.createLocalUser("applicant-cancel-join-request@example.com", Password.ofEncoded("password"), "신청자")
			);
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceMemberRole.OWNER));
			var created = workspaceJoinRequestService.createJoinRequest(workspace.getId(), applicant.getId());

			// when
			workspaceJoinRequestService.cancelJoinRequest(workspace.getId(), applicant.getId());

			// then
			assertThat(workspaceJoinRequestRepository.findById(created.requestId())).isEmpty();
			assertThat(workspaceJoinRequestService.getMyJoinRequestStatus(workspace.getId(), applicant.getId()))
				.extracting("isMember", "joinRequestStatus", "canApply")
				.containsExactly(false, WorkspaceMyJoinRequestStatus.NONE, true);
		}
	}
}
