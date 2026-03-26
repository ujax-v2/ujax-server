package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.problem.AlgorithmTag;
import com.ujax.domain.problem.AlgorithmTagRepository;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionCommentRepository;
import com.ujax.domain.solution.SolutionLikeRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceJoinRequestRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.common.ForbiddenException;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceMemberProfileServiceTest {

	@Autowired
	private WorkspaceMemberProfileService workspaceMemberProfileService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private SolutionLikeRepository solutionLikeRepository;

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
	private AlgorithmTagRepository algorithmTagRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceJoinRequestRepository workspaceJoinRequestRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void tearDown() {
		solutionLikeRepository.deleteAllInBatch();
		solutionCommentRepository.deleteAllInBatch();
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		solutionRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
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
	@DisplayName("워크스페이스 멤버 프로필 조회")
	class GetMyProfile {

		@Test
		@DisplayName("현재 워크스페이스의 내 멤버 기준으로 프로필 통계를 조회한다")
		void getMyProfile_Success() {
			User user = userRepository.save(User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://example.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			));
			user.updateProfile(null, null, "gwong");
			userRepository.save(user);

			Workspace targetWorkspace = workspaceRepository.save(Workspace.create("목표 워크스페이스", "설명"));
			WorkspaceMember targetMember = workspaceMemberRepository.save(
				WorkspaceMember.create(targetWorkspace, user, WorkspaceMemberRole.MEMBER)
			);
			targetMember.updateNickname("목표닉네임");
			workspaceMemberRepository.save(targetMember);

			Workspace otherWorkspace = workspaceRepository.save(Workspace.create("다른 워크스페이스", "설명"));
			WorkspaceMember otherMember = workspaceMemberRepository.save(
				WorkspaceMember.create(otherWorkspace, user, WorkspaceMemberRole.MEMBER)
			);
			otherMember.updateNickname("다른닉네임");
			workspaceMemberRepository.save(otherMember);

			AlgorithmTag dp = algorithmTagRepository.save(AlgorithmTag.create("DP"));
			AlgorithmTag graph = algorithmTagRepository.save(AlgorithmTag.create("GRAPH"));

			Problem targetProblem = Problem.create(1000, "A+B", "Bronze", "1초", "256MB", null, null, null, null);
			targetProblem.addAlgorithmTag(dp);
			Problem savedTargetProblem = problemRepository.save(targetProblem);

			Problem otherProblem = Problem.create(2000, "BFS", "Silver", "1초", "256MB", null, null, null, null);
			otherProblem.addAlgorithmTag(graph);
			Problem savedOtherProblem = problemRepository.save(otherProblem);

			ProblemBox targetProblemBox = problemBoxRepository.save(ProblemBox.create(targetWorkspace, "기본 문제집", "설명"));
			ProblemBox otherProblemBox = problemBoxRepository.save(ProblemBox.create(otherWorkspace, "기본 문제집", "설명"));

			WorkspaceProblem targetWorkspaceProblem = workspaceProblemRepository.save(
				WorkspaceProblem.create(targetProblemBox, savedTargetProblem, null, null)
			);
			WorkspaceProblem otherWorkspaceProblem = workspaceProblemRepository.save(
				WorkspaceProblem.create(otherProblemBox, savedOtherProblem, null, null)
			);

			solutionRepository.save(Solution.create(
				targetWorkspaceProblem,
				targetMember,
				10001L,
				"맞았습니다!!",
				"100",
				"256",
				"Java",
				"1200",
				"code"
			));
			solutionRepository.save(Solution.create(
				targetWorkspaceProblem,
				targetMember,
				10002L,
				"틀렸습니다",
				"120",
				"256",
				"Java",
				"1300",
				"code"
			));
			solutionRepository.save(Solution.create(
				otherWorkspaceProblem,
				otherMember,
				20001L,
				"맞았습니다!!",
				"140",
				"256",
				"Python",
				"900",
				"code"
			));

			WorkspaceMemberProfileResponse response = workspaceMemberProfileService.getMyProfile(targetWorkspace.getId(), user.getId());

			assertThat(response.member().workspaceMemberId()).isEqualTo(targetMember.getId());
			assertThat(response.member().nickname()).isEqualTo("목표닉네임");
			assertThat(response.member().email()).isEqualTo("test@example.com");
			assertThat(response.member().baekjoonId()).isEqualTo("gwong");
			assertThat(response.summary().solvedCount()).isEqualTo(1L);
			assertThat(response.summary().streakDays()).isEqualTo(1);
			assertThat(response.summary().mainLanguage()).isEqualTo("Java");
			assertThat(response.summary().mainAlgorithm()).isEqualTo("DP");
			assertThat(response.accuracy().acceptedCount()).isEqualTo(1L);
			assertThat(response.accuracy().totalCount()).isEqualTo(2L);
			assertThat(response.accuracy().rate()).isEqualTo(50);
			assertThat(response.languageStats()).singleElement().extracting("name", "count", "ratio")
				.containsExactly("Java", 2L, 100);
			assertThat(response.algorithmStats()).singleElement().extracting("name", "count", "ratio")
				.containsExactly("DP", 1L, 100);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 조회할 수 없다")
		void getMyProfile_Forbidden() {
			User user = userRepository.save(User.createOAuthUser(
				"forbidden@example.com",
				"권한없음",
				null,
				AuthProvider.GOOGLE,
				"google-456"
			));
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "설명"));

			assertThatThrownBy(() -> workspaceMemberProfileService.getMyProfile(workspace.getId(), user.getId()))
				.isInstanceOf(ForbiddenException.class);
		}

	}

	@Nested
	@DisplayName("워크스페이스 멤버 프로필 활동 조회")
	class GetMyProfileActivity {

		@Test
		@DisplayName("현재 워크스페이스 멤버의 활동 기록만 조회한다")
		void getMyProfileActivity_Success() {
			User user = userRepository.save(User.createOAuthUser(
				"activity@example.com",
				"활동유저",
				null,
				AuthProvider.GOOGLE,
				"google-789"
			));

			Workspace targetWorkspace = workspaceRepository.save(Workspace.create("목표 워크스페이스", "설명"));
			WorkspaceMember targetMember = workspaceMemberRepository.save(
				WorkspaceMember.create(targetWorkspace, user, WorkspaceMemberRole.MEMBER)
			);

			Workspace otherWorkspace = workspaceRepository.save(Workspace.create("다른 워크스페이스", "설명"));
			WorkspaceMember otherMember = workspaceMemberRepository.save(
				WorkspaceMember.create(otherWorkspace, user, WorkspaceMemberRole.MEMBER)
			);

			Problem targetProblem = problemRepository.save(
				Problem.create(1001, "A-B", "Bronze", "1초", "256MB", null, null, null, null)
			);
			Problem otherProblem = problemRepository.save(
				Problem.create(1002, "A*C", "Bronze", "1초", "256MB", null, null, null, null)
			);

			ProblemBox targetProblemBox = problemBoxRepository.save(ProblemBox.create(targetWorkspace, "기본 문제집", "설명"));
			ProblemBox otherProblemBox = problemBoxRepository.save(ProblemBox.create(otherWorkspace, "기본 문제집", "설명"));

			WorkspaceProblem targetWorkspaceProblem = workspaceProblemRepository.save(
				WorkspaceProblem.create(targetProblemBox, targetProblem, null, null)
			);
			WorkspaceProblem otherWorkspaceProblem = workspaceProblemRepository.save(
				WorkspaceProblem.create(otherProblemBox, otherProblem, null, null)
			);

			solutionRepository.save(Solution.create(
				targetWorkspaceProblem,
				targetMember,
				30001L,
				"맞았습니다!!",
				"100",
				"256",
				"Python",
				"800",
				"code"
			));
			solutionRepository.save(Solution.create(
				otherWorkspaceProblem,
				otherMember,
				30002L,
				"맞았습니다!!",
				"110",
				"256",
				"Java",
				"850",
				"code"
			));

			WorkspaceMemberProfileActivityResponse response = workspaceMemberProfileService.getMyProfileActivity(
				targetWorkspace.getId(),
				user.getId(),
				LocalDate.now().getYear()
			);

			assertThat(response.mode()).isEqualTo("YEAR");
			assertThat(response.year()).isEqualTo(LocalDate.now().getYear());
			assertThat(response.days()).hasSize(1);
			assertThat(response.days().getFirst().count()).isEqualTo(1L);
		}
	}
}
