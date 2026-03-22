package com.ujax.application.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.solution.dto.response.SolutionCommentResponse;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionComment;
import com.ujax.domain.solution.SolutionCommentRepository;
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
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;

@SpringBootTest
@ActiveProfiles("test")
class SolutionCommentServiceTest {

	@Autowired
	private SolutionCommentService solutionCommentService;

	@Autowired
	private SolutionCommentRepository solutionCommentRepository;

	@Autowired
	private SolutionLikeRepository solutionLikeRepository;

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	@DisplayName("댓글 생성")
	class CreateComment {

		@Test
		@DisplayName("댓글을 생성한다")
		void createCommentSuccess() {
			Fixture fixture = createFixture();

			SolutionCommentResponse result = solutionCommentService.createComment(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId(),
				"좋은 풀이네요"
			);

			assertThat(result.authorName()).isEqualTo(fixture.viewer().getNickname());
			assertThat(result.content()).isEqualTo("좋은 풀이네요");
			assertThat(result.isMyComment()).isTrue();
		}

		@Test
		@DisplayName("공백 댓글은 생성할 수 없다")
		void createCommentBlankContent() {
			Fixture fixture = createFixture();

			assertThatThrownBy(() -> solutionCommentService.createComment(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId(),
				" "
			))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}
	}

	@Nested
	@DisplayName("댓글 목록 조회")
	class GetComments {

		@Test
		@DisplayName("submission 기준으로 댓글 목록을 조회한다")
		void getCommentsSuccess() {
			Fixture fixture = createFixture();
			solutionCommentRepository.save(SolutionComment.create(fixture.solution(), fixture.viewer(), "첫 댓글"));
			solutionCommentRepository.save(SolutionComment.create(fixture.solution(), fixture.author(), "둘째 댓글"));

			var result = solutionCommentService.getComments(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				fixture.viewer().getUser().getId()
			);

			assertThat(result).hasSize(2);
			assertThat(result).extracting(SolutionCommentResponse::content)
				.containsExactly("첫 댓글", "둘째 댓글");
			assertThat(result).extracting(SolutionCommentResponse::isMyComment)
				.containsExactly(true, false);
		}
	}

	@Nested
	@DisplayName("댓글 삭제")
	class DeleteComment {

		@Test
		@DisplayName("작성자는 댓글을 삭제할 수 있다")
		void deleteCommentSuccess() {
			Fixture fixture = createFixture();
			SolutionComment comment = solutionCommentRepository.save(
				SolutionComment.create(fixture.solution(), fixture.viewer(), "삭제할 댓글")
			);

			solutionCommentService.deleteComment(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				comment.getId(),
				fixture.viewer().getUser().getId()
			);

			Long deletedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solution_comments WHERE id = ? AND deleted_at IS NOT NULL",
				Long.class,
				comment.getId()
			);
			assertThat(deletedCount).isEqualTo(1L);
		}

		@Test
		@DisplayName("작성자가 아니면 댓글을 삭제할 수 없다")
		void deleteCommentForbidden() {
			Fixture fixture = createFixture();
			SolutionComment comment = solutionCommentRepository.save(
				SolutionComment.create(fixture.solution(), fixture.author(), "남의 댓글")
			);

			assertThatThrownBy(() -> solutionCommentService.deleteComment(
				fixture.workspace().getId(),
				fixture.problemBox().getId(),
				fixture.workspaceProblem().getId(),
				fixture.author().getId(),
				fixture.solution().getSubmissionId(),
				comment.getId(),
				fixture.viewer().getUser().getId()
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
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
