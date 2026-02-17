package com.ujax.application.board;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.board.dto.response.CommentListResponse;
import com.ujax.application.board.dto.response.CommentResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardComment;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.board.BoardType;
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
class BoardCommentServiceTest {

	@Autowired
	private BoardCommentService boardCommentService;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private BoardRepository boardRepository;

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
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("댓글 생성")
	class CreateComment {

		@Test
		@DisplayName("댓글을 생성한다")
		void createCommentSuccess() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when
			CommentResponse result = boardCommentService.createComment(workspace.getId(), board.getId(), member.getId(), "댓글");

			// then
			assertThat(result).extracting("boardId", "content", "author.workspaceMemberId")
				.containsExactly(board.getId(), "댓글", member.getId());
		}

		@Test
		@DisplayName("공백 댓글은 생성할 수 없다")
		void createCommentBlankContent() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when & then
			assertThatThrownBy(() -> boardCommentService.createComment(workspace.getId(), board.getId(), member.getId(), " "))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}
	}

	@Nested
	@DisplayName("댓글 목록 조회")
	class ListComments {

		@Test
		@DisplayName("페이지 단위로 댓글 목록을 조회한다")
		void listCommentsSuccess() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);
			boardCommentRepository.save(BoardComment.create(board, member, "댓글 1"));
			boardCommentRepository.save(BoardComment.create(board, member, "댓글 2"));

			// when
			CommentListResponse result = boardCommentService.listComments(workspace.getId(), board.getId(), member.getId(), 0, 10);

			// then
			assertThat(result.items()).hasSize(2);
			assertThat(result.page()).extracting("page", "size", "totalElements")
				.containsExactly(0, 10, 2L);
		}
	}

	@Nested
	@DisplayName("댓글 삭제")
	class DeleteComment {

		@Test
		@DisplayName("작성자는 댓글을 삭제할 수 있다")
		void deleteCommentSuccess() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);
			BoardComment comment = boardCommentRepository.save(BoardComment.create(board, member, "댓글"));

			// when
			boardCommentService.deleteComment(workspace.getId(), board.getId(), comment.getId(), member.getId());

			// then
			Long deletedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM board_comments WHERE id = ? AND deleted_at IS NOT NULL",
				Long.class,
				comment.getId()
			);
			assertThat(deletedCount).isEqualTo(1L);
		}

		@Test
		@DisplayName("작성자가 아니면 댓글을 삭제할 수 없다")
		void deleteCommentForbidden() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.MEMBER);
			WorkspaceMember other = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, author);
			BoardComment comment = boardCommentRepository.save(BoardComment.create(board, author, "댓글"));

			// when & then
			assertThatThrownBy(() -> boardCommentService.deleteComment(workspace.getId(), board.getId(), comment.getId(), other.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace, WorkspaceMemberRole role) {
		User user = userRepository.save(User.createLocalUser(uniqueEmail(), "password", "사용자"));
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, role));
	}

	private Board createBoard(Workspace workspace, WorkspaceMember author) {
		return boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.FREE,
			false,
			"제목",
			"내용"
		));
	}

	private String uniqueEmail() {
		return UUID.randomUUID() + "@example.com";
	}
}
