package com.ujax.application.board;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ujax.application.board.dto.request.BoardCreateRequest;
import com.ujax.application.board.dto.request.BoardListRequest;
import com.ujax.application.board.dto.request.BoardUpdateRequest;
import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardListItemResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardComment;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLike;
import com.ujax.domain.board.BoardLikeId;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.board.BoardType;
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
import com.ujax.infrastructure.external.s3.S3StorageService;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;
import com.ujax.infrastructure.web.board.dto.request.BoardImageUploadRequest;

@SpringBootTest
@ActiveProfiles("test")
class BoardServiceTest {

	@Autowired
	private BoardService boardService;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private S3StorageService s3StorageService;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAllInBatch();
		boardLikeRepository.deleteAllInBatch();
		boardCommentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("게시글 생성")
	class CreateBoard {

		@Test
		@DisplayName("일반 게시글을 생성한다")
		void createBoardSuccess() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			BoardCreateRequest request = BoardCreateRequest.builder()
				.type(BoardType.FREE)
				.title("제목")
				.content("내용")
				.build();

				// when
				BoardDetailResponse result = boardService.createBoard(workspace.getId(), member.getUser().getId(), request);

			// then
			assertThat(result).extracting("workspaceId", "type", "pinned", "title", "content", "likeCount", "commentCount", "myLike")
				.containsExactly(workspace.getId(), BoardType.FREE, false, "제목", "내용", 0L, 0L, false);
		}

		@Test
		@DisplayName("멤버 권한으로 공지 게시글을 생성하면 오류가 발생한다")
		void createNoticeForbiddenForMember() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			BoardCreateRequest request = BoardCreateRequest.builder()
				.type(BoardType.NOTICE)
				.title("공지")
				.content("공지 내용")
				.build();

				// when & then
				assertThatThrownBy(() -> boardService.createBoard(workspace.getId(), member.getUser().getId(), request))
					.isInstanceOf(ForbiddenException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}
	}

	@Nested
	@DisplayName("게시글 이미지 Presigned URL 생성")
	class CreateBoardImagePresignedUrl {

		@Test
		@DisplayName("워크스페이스 멤버는 게시글 이미지 Presigned URL을 생성할 수 있다")
		void createBoardImagePresignedUrl() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			BoardImageUploadRequest request = new BoardImageUploadRequest("image/png", 1024L);

			given(s3StorageService.generateBoardImagePresignedUrl(workspace.getId(), "image/png", 1024L))
				.willReturn(new PresignedUrlResult("https://presigned-url", "https://image-url"));

			// when
			PresignedUrlResponse response = boardService.createBoardImagePresignedUrl(
				workspace.getId(),
				member.getUser().getId(),
				request
			);

			// then
			assertThat(response).extracting("presignedUrl", "imageUrl")
				.containsExactly("https://presigned-url", "https://image-url");
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 오류가 발생한다")
		void createBoardImagePresignedUrlForbidden() {
			// given
			Workspace workspace = createWorkspace();
			User outsider = userRepository.save(
				User.createLocalUser(uniqueEmail(), Password.ofEncoded("password"), "외부 사용자")
			);
			BoardImageUploadRequest request = new BoardImageUploadRequest("image/png", 1024L);

			// when & then
			assertThatThrownBy(() -> boardService.createBoardImagePresignedUrl(
				workspace.getId(),
				outsider.getId(),
				request
			))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}
	}

	@Nested
	@DisplayName("게시글 목록 조회")
	class ListBoards {

		@Test
		@DisplayName("댓글 수와 좋아요 수를 포함해 목록을 조회한다")
		void listBoardsIncludesCounts() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			WorkspaceMember viewer = createMember(workspace, WorkspaceMemberRole.MEMBER);
			String longContent = "a".repeat(120);

			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"검색 대상",
				longContent
			));
			boardCommentRepository.save(BoardComment.create(board, author, "댓글 1"));
			boardCommentRepository.save(BoardComment.create(board, viewer, "댓글 2"));
			boardLikeRepository.save(BoardLike.create(board, viewer));

			BoardListRequest request = BoardListRequest.builder()
				.type(BoardType.FREE)
				.keyword("검색")
				.page(0)
				.size(20)
				.sort("createdAt,desc")
				.pinnedFirst(true)
				.build();

				// when
				BoardListResponse result = boardService.listBoards(workspace.getId(), viewer.getUser().getId(), request);

			// then
			assertThat(result.items()).hasSize(1);
			BoardListItemResponse item = result.items().get(0);
			assertThat(item).extracting("boardId", "likeCount", "commentCount", "myLike")
				.containsExactly(board.getId(), 1L, 2L, true);
			assertThat(item.preview()).hasSize(100);
			assertThat(item.preview()).isEqualTo(longContent.substring(0, 100));
		}

		@Test
		@DisplayName("작성자가 소프트 삭제되어도 게시글 목록을 조회할 수 있다")
		void listBoardsWithSoftDeletedAuthor() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			WorkspaceMember viewer = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"삭제 멤버 게시글",
				"내용"
			));
			workspaceMemberRepository.delete(author);

			BoardListRequest request = BoardListRequest.builder()
				.type(null)
				.keyword(null)
				.page(0)
				.size(20)
				.sort("createdAt,desc")
				.pinnedFirst(false)
				.build();

			// when
			BoardListResponse result = boardService.listBoards(workspace.getId(), viewer.getUser().getId(), request);

			// then
			assertThat(result.items()).hasSize(1);
			BoardListItemResponse item = result.items().get(0);
			assertThat(item).extracting("boardId", "author.workspaceMemberId", "author.nickname")
				.containsExactly(board.getId(), author.getId(), author.getNickname());
		}
	}

	@Nested
	@DisplayName("게시글 상세 조회")
	class GetBoardDetail {

		@Test
		@DisplayName("상세 조회는 조회수/좋아요/댓글/내 좋아요를 반영해 반환한다")
		void getBoardDetailReturnsReactionInfo() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			WorkspaceMember viewer = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"제목",
				"내용"
			));
			boardCommentRepository.save(BoardComment.create(board, author, "댓글"));
			boardLikeRepository.save(BoardLike.create(board, viewer));

			// when
			BoardDetailResponse result = boardService.getBoardDetail(workspace.getId(), board.getId(), viewer.getUser().getId());

			// then
			assertThat(result).extracting("boardId", "viewCount", "likeCount", "commentCount", "myLike")
				.containsExactly(board.getId(), 1L, 1L, 1L, true);
		}

		@Test
		@DisplayName("작성자가 소프트 삭제되어도 게시글 상세를 조회할 수 있다")
		void getBoardDetailWithSoftDeletedAuthor() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			WorkspaceMember viewer = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"제목",
				"내용"
			));
			workspaceMemberRepository.delete(author);

			// when
			BoardDetailResponse result = boardService.getBoardDetail(workspace.getId(), board.getId(), viewer.getUser().getId());

			// then
			assertThat(result).extracting("boardId", "author.workspaceMemberId", "author.nickname")
				.containsExactly(board.getId(), author.getId(), author.getNickname());
		}
	}

	@Nested
	@DisplayName("게시글 수정")
	class UpdateBoard {

		@Test
		@DisplayName("작성자가 아니면 수정할 수 없다")
		void updateBoardForbidden() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			WorkspaceMember other = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"제목",
				"내용"
			));
			BoardUpdateRequest request = BoardUpdateRequest.builder()
				.title("수정 제목")
				.build();

				// when & then
				assertThatThrownBy(() -> boardService.updateBoard(workspace.getId(), board.getId(), other.getUser().getId(), request))
					.isInstanceOf(ForbiddenException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}
	}

	@Nested
	@DisplayName("게시글 삭제")
	class DeleteBoard {

		@Test
		@DisplayName("게시글 삭제 시 댓글과 좋아요가 비활성화된다")
		void deleteBoardMarksRelationsDeleted() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember author = createMember(workspace, WorkspaceMemberRole.OWNER);
			Board board = boardRepository.save(Board.create(
				workspace,
				author,
				BoardType.FREE,
				false,
				"제목",
				"내용"
			));
			BoardComment comment = boardCommentRepository.save(BoardComment.create(board, author, "댓글"));
			boardLikeRepository.save(BoardLike.create(board, author));

				// when
				boardService.deleteBoard(workspace.getId(), board.getId(), author.getUser().getId());

			// then
			Long deletedBoardCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM boards WHERE id = ? AND deleted_at IS NOT NULL",
				Long.class,
				board.getId()
			);
			Long deletedCommentCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM board_comments WHERE id = ? AND deleted_at IS NOT NULL",
				Long.class,
				comment.getId()
			);
			assertThat(deletedBoardCount).isEqualTo(1L);
			assertThat(deletedCommentCount).isEqualTo(1L);

			BoardLikeId likeId = new BoardLikeId(board.getId(), author.getId());
			BoardLike like = boardLikeRepository.findById(likeId).orElseThrow();
			assertThat(like.isDeleted()).isTrue();
		}
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace, WorkspaceMemberRole role) {
		User user = userRepository.save(User.createLocalUser(uniqueEmail(), Password.ofEncoded("password"), "사용자"));
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, role));
	}

	private String uniqueEmail() {
		return UUID.randomUUID() + "@example.com";
	}
}
