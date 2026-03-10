package com.ujax.application.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.solution.dto.response.SolutionLikeStatusResponse;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionCommentRepository;
import com.ujax.domain.solution.SolutionLike;
import com.ujax.domain.solution.SolutionLikeId;
import com.ujax.domain.solution.SolutionLikeRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
class SolutionLikeServiceTest {

	@Autowired
	private SolutionLikeService solutionLikeService;

	@Autowired
	private SolutionLikeRepository solutionLikeRepository;

	@Autowired
	private SolutionCommentRepository solutionCommentRepository;

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
		solutionCommentRepository.deleteAllInBatch();
		solutionLikeRepository.deleteAllInBatch();
		solutionRepository.deleteAllInBatch();
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		problemRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("좋아요 등록")
	class Like {

		@Test
		@DisplayName("좋아요가 없으면 새 레코드를 생성하고 상태를 반환한다")
		void likeCreatesSolutionLike() {
			Fixture fixture = createFixture();

			SolutionLikeStatusResponse result = solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			SolutionLike saved = solutionLikeRepository.findById(
				new SolutionLikeId(fixture.solution().getId(), fixture.viewer().getId())
			).orElseThrow();

			assertThat(saved.isDeleted()).isFalse();
			assertThat(result).extracting("likes", "isLiked").containsExactly(1L, true);
		}

		@Test
		@DisplayName("삭제된 좋아요가 있으면 복구한다")
		void likeReactivatesDeletedLike() {
			Fixture fixture = createFixture();
			solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);
			solutionLikeService.unlike(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			SolutionLikeStatusResponse result = solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			SolutionLike saved = solutionLikeRepository.findById(
				new SolutionLikeId(fixture.solution().getId(), fixture.viewer().getId())
			).orElseThrow();

			assertThat(solutionLikeRepository.count()).isEqualTo(1);
			assertThat(saved.isDeleted()).isFalse();
			assertThat(result).extracting("likes", "isLiked").containsExactly(1L, true);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 오류가 발생한다")
		void likeThrowsForbiddenWhenNotMember() {
			Fixture fixture = createFixture();
			User outsider = createUser("outsider@example.com");

			assertThatThrownBy(() -> solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				outsider.getId()
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}

		@Test
		@DisplayName("submission이 path와 맞지 않으면 오류가 발생한다")
		void likeThrowsNotFoundWhenSubmissionDoesNotBelongToMember() {
			Fixture fixture = createFixture();

			assertThatThrownBy(() -> solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.viewer().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOLUTION_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("좋아요 취소")
	class Unlike {

		@Test
		@DisplayName("좋아요가 있으면 삭제 상태로 바꾸고 상태를 반환한다")
		void unlikeMarksDeletedTrue() {
			Fixture fixture = createFixture();
			solutionLikeService.like(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			SolutionLikeStatusResponse result = solutionLikeService.unlike(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			SolutionLike saved = solutionLikeRepository.findById(
				new SolutionLikeId(fixture.solution().getId(), fixture.viewer().getId())
			).orElseThrow();

			assertThat(saved.isDeleted()).isTrue();
			assertThat(result).extracting("likes", "isLiked").containsExactly(0L, false);
		}

		@Test
		@DisplayName("좋아요가 없어도 예외 없이 0, false를 반환한다")
		void unlikeWithoutLikeDoesNothing() {
			Fixture fixture = createFixture();

			SolutionLikeStatusResponse result = solutionLikeService.unlike(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			assertThat(result).extracting("likes", "isLiked").containsExactly(0L, false);
		}
	}

	private Fixture createFixture() {
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		User authorUser = createUser("author@example.com");
		User viewerUser = createUser("viewer@example.com");
		WorkspaceMember author = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, authorUser, WorkspaceMemberRole.MEMBER)
		);
		WorkspaceMember viewer = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, viewerUser, WorkspaceMemberRole.MEMBER)
		);
		ProblemBox problemBox = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));
		Problem problem = problemRepository.save(
			Problem.create(1000, "A+B", "Bronze V", "1초", "256MB", null, null, null, null)
		);
		WorkspaceProblem workspaceProblem = workspaceProblemRepository.save(
			WorkspaceProblem.create(problemBox, problem, null, null)
		);
		Solution solution = solutionRepository.save(Solution.create(
			workspaceProblem,
			author,
			12345L,
			"맞았습니다!!",
			"0 ms",
			"2020 KB",
			"Java 11",
			"100 B",
			"code"
		));

		return new Fixture(workspace, problemBox, workspaceProblem, author, viewer, solution);
	}

	private User createUser(String email) {
		return userRepository.save(User.createLocalUser(email, Password.ofEncoded("password"), "유저"));
	}

	private record Fixture(
		Workspace workspace,
		ProblemBox problemBox,
		WorkspaceProblem workspaceProblem,
		WorkspaceMember author,
		WorkspaceMember viewer,
		Solution solution
	) {
	}
}
