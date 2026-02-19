package com.ujax.application.board;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.ujax.application.board.dto.request.BoardCreateRequest;
import com.ujax.application.board.dto.request.BoardListRequest;
import com.ujax.application.board.dto.request.BoardUpdateRequest;
import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.board.BoardType;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BoardServiceUnitTest {

	@Mock
	private BoardRepository boardRepository;
	@Mock
	private BoardCommentRepository boardCommentRepository;
	@Mock
	private BoardLikeRepository boardLikeRepository;
	@Mock
	private WorkspaceRepository workspaceRepository;
	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@InjectMocks
	private BoardService boardService;

	@Test
	@DisplayName("listBoards: 페이지 번호가 음수면 INVALID_PARAMETER 예외가 발생한다")
	void listBoardsThrowsBadRequestWhenPageIsNegative() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		BoardListRequest request = BoardListRequest.builder()
			.page(-1).size(20).pinnedFirst(true)
			.build();

		// when & then
		assertThatThrownBy(() -> boardService.listBoards(1L, 2L, request))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
	}

	@Test
	@DisplayName("listBoards: 정렬 형식이 '필드,방향'이 아니면 INVALID_PARAMETER 예외가 발생한다")
	void listBoardsThrowsBadRequestWhenSortFormatInvalid() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		BoardListRequest request = BoardListRequest.builder()
			.page(0).size(20).sort("createdAt").pinnedFirst(true)
			.build();

		// when & then
		assertThatThrownBy(() -> boardService.listBoards(1L, 2L, request))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
	}

	@Test
	@DisplayName("listBoards: 허용되지 않은 정렬 필드를 요청하면 INVALID_PARAMETER 예외가 발생한다")
	void listBoardsThrowsBadRequestWhenSortPropertyInvalid() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		BoardListRequest request = BoardListRequest.builder()
			.page(0).size(20).sort("title,desc").pinnedFirst(true)
			.build();

		// when & then
		assertThatThrownBy(() -> boardService.listBoards(1L, 2L, request))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
	}

	@Test
	@DisplayName("listBoards: 허용되지 않은 정렬 방향을 요청하면 INVALID_PARAMETER 예외가 발생한다")
	void listBoardsThrowsBadRequestWhenSortDirectionInvalid() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		BoardListRequest request = BoardListRequest.builder()
			.page(0).size(20).sort("createdAt,invalid").pinnedFirst(true)
			.build();

		// when & then
		assertThatThrownBy(() -> boardService.listBoards(1L, 2L, request))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
	}

	@Test
	@DisplayName("listBoards: 키워드가 공백이면 null 검색어로 조회하고 빈 목록을 반환한다")
	void listBoardsReturnsEmptyListWhenNoBoards() {
		// given
		WorkspaceMember member = mock(WorkspaceMember.class);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));
		given(boardRepository.search(eq(1L), isNull(), isNull(), any(PageRequest.class)))
			.willReturn(Page.empty());
		BoardListRequest request = BoardListRequest.builder()
			.page(0).size(20).keyword("   ").pinnedFirst(false)
			.build();

		// when
		BoardListResponse result = boardService.listBoards(1L, 2L, request);

		// then
		assertThat(result.items()).isEmpty();
		then(boardCommentRepository).should(never()).countByBoardIds(anyList());
		then(boardLikeRepository).should(never()).countByBoardIds(anyList());
	}

	@Test
	@DisplayName("listBoards: pinnedFirst=true이면 기본 정렬 앞에 pinned desc 정렬을 추가한다")
	void listBoardsAddsPinnedSortWhenPinnedFirstTrue() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		given(boardRepository.search(eq(1L), isNull(), isNull(), any(PageRequest.class)))
			.willReturn(Page.empty());
		BoardListRequest request = BoardListRequest.builder()
			.page(0).size(20).pinnedFirst(true)
			.build();
		ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);

		// when
		boardService.listBoards(1L, 2L, request);

		// then
		then(boardRepository).should().search(eq(1L), isNull(), isNull(), pageCaptor.capture());
		Sort sort = pageCaptor.getValue().getSort();
		assertThat(sort.getOrderFor("pinned")).isNotNull();
		assertThat(sort.getOrderFor("pinned").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("createBoard: 워크스페이스가 없으면 WORKSPACE_NOT_FOUND 예외가 발생한다")
	void createBoardThrowsNotFoundWhenWorkspaceMissing() {
		// given
		given(workspaceRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> boardService.createBoard(1L, 2L, BoardCreateRequest.builder()
			.type(BoardType.FREE).title("제목").content("내용").build()))
			.isInstanceOf(NotFoundException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_NOT_FOUND);
	}

	@Test
	@DisplayName("createBoard: 제목이 공백이면 INVALID_INPUT 예외가 발생한다")
	void createBoardThrowsBadRequestWhenTitleBlank() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember member = mock(WorkspaceMember.class);
		given(workspaceRepository.findById(1L)).willReturn(Optional.of(workspace));
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> boardService.createBoard(1L, 2L, BoardCreateRequest.builder()
			.type(BoardType.FREE).title(" ").content("내용").build()))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("createBoard: 내용 길이가 2000자를 초과하면 INVALID_INPUT 예외가 발생한다")
	void createBoardThrowsBadRequestWhenContentTooLong() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember member = mock(WorkspaceMember.class);
		given(workspaceRepository.findById(1L)).willReturn(Optional.of(workspace));
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> boardService.createBoard(1L, 2L, BoardCreateRequest.builder()
			.type(BoardType.FREE).title("제목").content("a".repeat(2001)).build()))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("createBoard: OWNER가 NOTICE 타입을 요청하면 공지 게시글을 생성한다")
	void createBoardAllowsNoticeForOwner() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember owner = mock(WorkspaceMember.class);
		given(workspace.getId()).willReturn(1L);
		given(owner.getId()).willReturn(2L);
		given(owner.getNickname()).willReturn("owner");
		given(owner.getRole()).willReturn(WorkspaceMemberRole.OWNER);
		given(workspaceRepository.findById(1L)).willReturn(Optional.of(workspace));
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(owner));

		Board saved = Board.create(workspace, owner, BoardType.NOTICE, true, "공지", "내용");
		given(boardRepository.save(any(Board.class))).willReturn(saved);

		// when
		BoardDetailResponse result = boardService.createBoard(1L, 2L, BoardCreateRequest.builder()
			.type(BoardType.NOTICE).title("공지").content("내용").pinned(true).build());

		// then
		assertThat(result.type()).isEqualTo(BoardType.NOTICE);
		assertThat(result.pinned()).isTrue();
	}

	@Test
	@DisplayName("createBoard: NOTICE가 아닌 타입에서 pinned=true면 INVALID_INPUT 예외가 발생한다")
	void createBoardThrowsBadRequestWhenPinnedTrueForNonNoticeType() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember member = mock(WorkspaceMember.class);
		given(workspaceRepository.findById(1L)).willReturn(Optional.of(workspace));
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> boardService.createBoard(1L, 2L, BoardCreateRequest.builder()
			.type(BoardType.FREE).title("제목").content("내용").pinned(true).build()))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("updateBoard: 수정 필드가 하나도 없으면 INVALID_INPUT 예외가 발생한다")
	void updateBoardThrowsBadRequestWhenNoFieldsToUpdate() {
		// given
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(author.getId()).willReturn(2L);
		given(board.getAuthor()).willReturn(author);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when & then
		assertThatThrownBy(() -> boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder().build()))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("updateBoard: 게시글이 없으면 BOARD_NOT_FOUND 예외가 발생한다")
	void updateBoardThrowsNotFoundWhenBoardMissing() {
		// given
		WorkspaceMember member = mock(WorkspaceMember.class);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder().title("수정").build()))
			.isInstanceOf(NotFoundException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOARD_NOT_FOUND);
	}

	@Test
	@DisplayName("updateBoard: 작성자 요청이면 수정 후 좋아요/댓글 수와 내 좋아요 여부를 반영해 반환한다")
	void updateBoardReturnsReactionCountsAndMyLike() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(workspace.getId()).willReturn(1L);
		given(author.getId()).willReturn(2L);
		given(author.getNickname()).willReturn("author");
		given(author.getRole()).willReturn(WorkspaceMemberRole.MANAGER);
		given(board.getId()).willReturn(3L);
		given(board.getWorkspace()).willReturn(workspace);
		given(board.getAuthor()).willReturn(author);
		given(board.getType()).willReturn(BoardType.NOTICE);
		given(board.isPinned()).willReturn(true);
		given(board.getTitle()).willReturn("수정 제목");
		given(board.getContent()).willReturn("수정 내용");
		given(board.getViewCount()).willReturn(10L);

		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));
		given(boardCommentRepository.countByBoard_Id(3L)).willReturn(4L);
		given(boardLikeRepository.countByBoardIds(List.of(3L))).willReturn(List.<Object[]>of(new Object[] {3L, 7L}));
		given(boardLikeRepository.findMyLikedBoardIds(List.of(3L), 2L)).willReturn(List.of(3L));

		// when
		BoardDetailResponse result = boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder()
			.title("수정 제목").pinned(true).build());

		// then
		assertThat(result.likeCount()).isEqualTo(7L);
		assertThat(result.commentCount()).isEqualTo(4L);
		assertThat(result.myLike()).isTrue();
		then(board).should().update(eq(null), eq("수정 제목"), eq(null), eq(true));
	}

	@Test
	@DisplayName("updateBoard: MEMBER가 NOTICE 타입으로 수정하면 FORBIDDEN_RESOURCE 예외가 발생한다")
	void updateBoardThrowsForbiddenWhenMemberChangesTypeToNotice() {
		// given
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(author.getId()).willReturn(2L);
		given(author.getRole()).willReturn(WorkspaceMemberRole.MEMBER);
		given(board.getAuthor()).willReturn(author);
		given(board.isPinned()).willReturn(false);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when & then
		assertThatThrownBy(() -> boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder()
			.type(BoardType.NOTICE).build()))
			.isInstanceOf(ForbiddenException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
	}

	@Test
	@DisplayName("updateBoard: MANAGER가 NOTICE 타입으로 수정하면 정상 처리한다")
	void updateBoardAllowsManagerChangesTypeToNotice() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(workspace.getId()).willReturn(1L);
		given(author.getId()).willReturn(2L);
		given(author.getNickname()).willReturn("manager");
		given(author.getRole()).willReturn(WorkspaceMemberRole.MANAGER);
		given(board.getId()).willReturn(3L);
		given(board.getWorkspace()).willReturn(workspace);
		given(board.getAuthor()).willReturn(author);
		given(board.getType()).willReturn(BoardType.NOTICE);
		given(board.isPinned()).willReturn(false);
		given(board.getTitle()).willReturn("제목");
		given(board.getContent()).willReturn("내용");
		given(board.getViewCount()).willReturn(5L);

		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));
		given(boardCommentRepository.countByBoard_Id(3L)).willReturn(0L);
		given(boardLikeRepository.countByBoardIds(List.of(3L))).willReturn(List.<Object[]>of());
		given(boardLikeRepository.findMyLikedBoardIds(List.of(3L), 2L)).willReturn(List.of());

		// when
		BoardDetailResponse result = boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder()
			.type(BoardType.NOTICE).build());

		// then
		assertThat(result.type()).isEqualTo(BoardType.NOTICE);
		then(board).should().update(eq(BoardType.NOTICE), isNull(), isNull(), isNull());
	}

	@Test
	@DisplayName("updateBoard: NOTICE가 아닌 상태에서 pinned=true가 되면 INVALID_INPUT 예외가 발생한다")
	void updateBoardThrowsBadRequestWhenPinnedTrueForNonNoticeType() {
		// given
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(author.getId()).willReturn(2L);
		given(board.getAuthor()).willReturn(author);
		given(board.getType()).willReturn(BoardType.FREE);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when & then
		assertThatThrownBy(() -> boardService.updateBoard(1L, 3L, 2L, BoardUpdateRequest.builder()
			.pinned(true).build()))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("pinBoard: 작성자가 아니면 FORBIDDEN_RESOURCE 예외가 발생한다")
	void pinBoardThrowsForbiddenWhenActorIsNotAuthor() {
		// given
		WorkspaceMember actor = mock(WorkspaceMember.class);
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(actor.getId()).willReturn(2L);
		given(author.getId()).willReturn(99L);
		given(board.getAuthor()).willReturn(author);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(actor));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when & then
		assertThatThrownBy(() -> boardService.pinBoard(1L, 3L, 2L, true))
			.isInstanceOf(ForbiddenException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
	}

	@Test
	@DisplayName("pinBoard: NOTICE가 아닌 게시글을 pinned=true로 변경하면 INVALID_INPUT 예외가 발생한다")
	void pinBoardThrowsBadRequestWhenPinnedTrueForNonNoticeType() {
		// given
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(author.getId()).willReturn(2L);
		given(board.getAuthor()).willReturn(author);
		given(board.getType()).willReturn(BoardType.FREE);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when & then
		assertThatThrownBy(() -> boardService.pinBoard(1L, 3L, 2L, true))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("deleteBoard: 작성자가 요청하면 좋아요/댓글을 비활성화하고 게시글을 삭제한다")
	void deleteBoardMarksRelationsDeletedAndDeletesBoard() {
		// given
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);
		given(author.getId()).willReturn(2L);
		given(board.getId()).willReturn(3L);
		given(board.getAuthor()).willReturn(author);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when
		boardService.deleteBoard(1L, 3L, 2L);

		// then
		then(boardLikeRepository).should().markDeletedByBoardId(3L);
		then(boardCommentRepository).should().softDeleteByBoardId(3L);
		then(boardRepository).should().delete(board);
	}

	@Test
	@DisplayName("getBoardDetail: 게시글이 없으면 BOARD_NOT_FOUND 예외가 발생한다")
	void getBoardDetailThrowsNotFoundWhenBoardMissing() {
		// given
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L))
			.willReturn(Optional.of(mock(WorkspaceMember.class)));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> boardService.getBoardDetail(1L, 3L, 2L))
			.isInstanceOf(NotFoundException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOARD_NOT_FOUND);
	}

	@Test
	@DisplayName("getBoardDetail: 정상 조회면 조회수를 증가시키고 상세 정보를 반환한다")
	void getBoardDetailIncrementsViewCountAndReturnsDetail() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember member = mock(WorkspaceMember.class);
		Board board = mock(Board.class);

		given(workspace.getId()).willReturn(1L);
		given(member.getId()).willReturn(2L);
		given(member.getNickname()).willReturn("member");
		given(board.getId()).willReturn(3L);
		given(board.getWorkspace()).willReturn(workspace);
		given(board.getAuthor()).willReturn(member);
		given(board.getType()).willReturn(BoardType.FREE);
		given(board.isPinned()).willReturn(false);
		given(board.getTitle()).willReturn("제목");
		given(board.getContent()).willReturn("내용");
		given(board.getViewCount()).willReturn(11L);

		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(member));
		given(boardRepository.findByIdAndWorkspaceId(3L, 1L)).willReturn(Optional.of(board));

		// when
		BoardDetailResponse result = boardService.getBoardDetail(1L, 3L, 2L);

		// then
		assertThat(result.boardId()).isEqualTo(3L);
		then(boardRepository).should().incrementViewCount(1L, 3L);
	}

	@Test
	@DisplayName("listBoards: 게시글이 있으면 댓글 수/좋아요 수 집계를 응답에 반영한다")
	void listBoardsMapsCommentAndLikeCounts() {
		// given
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember author = mock(WorkspaceMember.class);
		Board board = mock(Board.class);

		given(workspace.getId()).willReturn(1L);
		given(author.getId()).willReturn(2L);
		given(author.getNickname()).willReturn("author");
		given(board.getId()).willReturn(10L);
		given(board.getWorkspace()).willReturn(workspace);
		given(board.getAuthor()).willReturn(author);
		given(board.getType()).willReturn(BoardType.FREE);
		given(board.isPinned()).willReturn(false);
		given(board.getTitle()).willReturn("제목");
		given(board.getContent()).willReturn("c".repeat(120));
		given(board.getViewCount()).willReturn(0L);

		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(1L, 2L)).willReturn(Optional.of(author));
		Page<Board> page = new PageImpl<>(List.of(board), PageRequest.of(0, 20), 1);
		given(boardRepository.search(eq(1L), eq(BoardType.FREE), eq("키워드"), any(PageRequest.class))).willReturn(page);
		given(boardCommentRepository.countByBoardIds(List.of(10L))).willReturn(List.<Object[]>of(new Object[] {10L, 3L}));
		given(boardLikeRepository.countByBoardIds(List.of(10L))).willReturn(List.<Object[]>of(new Object[] {10L, 4L}));

		BoardListRequest request = BoardListRequest.builder()
			.type(BoardType.FREE).keyword("키워드").page(0).size(20).sort("createdAt,desc").pinnedFirst(true)
			.build();

		// when
		BoardListResponse result = boardService.listBoards(1L, 2L, request);

		// then
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).commentCount()).isEqualTo(3L);
		assertThat(result.items().get(0).likeCount()).isEqualTo(4L);
		assertThat(result.items().get(0).preview()).hasSize(100);
	}
}
