package com.ujax.application.board;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.board.dto.response.BoardLikeStatusResponse;
import com.ujax.domain.board.Board;
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
import com.ujax.global.exception.common.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
class BoardLikeServiceTest {

	@Autowired
	private BoardLikeService boardLikeService;

	@Autowired
	private BoardLikeRepository boardLikeRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private BoardCommentRepository boardCommentRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private UserRepository userRepository;

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
	@DisplayName("like: 좋아요 생성/복구")
	class LikeBoard {

		@Test
		@DisplayName("like: 해당 멤버의 좋아요가 없으면 새 레코드를 생성한다")
		void likeCreatesBoardLike() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());

			// then
			BoardLike saved = boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId())).orElseThrow();
			assertThat(saved.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("like: 삭제 상태 좋아요가 있으면 새로 만들지 않고 활성 상태로 복구한다")
		void likeReactivatesDeletedBoardLike() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());
			boardLikeService.unlike(workspace.getId(), board.getId(), member.getUser().getId());

			// when
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());

			// then
			BoardLike saved = boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId())).orElseThrow();
			assertThat(boardLikeRepository.count()).isEqualTo(1);
			assertThat(saved.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("like: 같은 멤버가 중복 요청해도 좋아요 레코드는 1개만 유지된다")
		void likeIsIdempotent() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());

			// then
			BoardLike saved = boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId())).orElseThrow();
			assertThat(boardLikeRepository.count()).isEqualTo(1);
			assertThat(saved.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("like: 워크스페이스 소속이 아닌 멤버가 요청하면 FORBIDDEN_RESOURCE 예외가 발생한다")
		void likeThrowsForbiddenWhenMemberIsNotInWorkspace() {
			// given
			Workspace workspace = createWorkspace();
			Workspace otherWorkspace = createWorkspace();
			WorkspaceMember boardAuthor = createMember(workspace, WorkspaceMemberRole.MEMBER);
			WorkspaceMember outsider = createMember(otherWorkspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, boardAuthor);

			// when & then
			assertThatThrownBy(() -> boardLikeService.like(workspace.getId(), board.getId(), outsider.getUser().getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}

		@Test
		@DisplayName("like: 게시글이 존재하지 않으면 BOARD_NOT_FOUND 예외가 발생한다")
		void likeThrowsNotFoundWhenBoardDoesNotExist() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);

			// when & then
			assertThatThrownBy(() -> boardLikeService.like(workspace.getId(), 99999L, member.getUser().getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOARD_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("unlike: 좋아요 취소")
	class UnlikeBoard {

		@Test
		@DisplayName("unlike: 기존 좋아요가 있으면 삭제 상태로 변경한다")
		void unlikeMarksDeletedTrue() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);
			boardLikeService.like(workspace.getId(), board.getId(), member.getUser().getId());

			// when
			boardLikeService.unlike(workspace.getId(), board.getId(), member.getUser().getId());

			// then
			BoardLike saved = boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId())).orElseThrow();
			assertThat(saved.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("unlike: 기존 좋아요가 없어도 예외 없이 종료된다")
		void unlikeWithoutLikeDoesNothing() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when
			boardLikeService.unlike(workspace.getId(), board.getId(), member.getUser().getId());

			// then
			assertThat(boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId()))).isEmpty();
		}

		@Test
		@DisplayName("unlike: 워크스페이스 소속이 아닌 멤버가 요청하면 FORBIDDEN_RESOURCE 예외가 발생한다")
		void unlikeThrowsForbiddenWhenMemberIsNotInWorkspace() {
			// given
			Workspace workspace = createWorkspace();
			Workspace otherWorkspace = createWorkspace();
			WorkspaceMember boardAuthor = createMember(workspace, WorkspaceMemberRole.MEMBER);
			WorkspaceMember outsider = createMember(otherWorkspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, boardAuthor);

			// when & then
			assertThatThrownBy(() -> boardLikeService.unlike(workspace.getId(), board.getId(), outsider.getUser().getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}

		@Test
		@DisplayName("unlike: 게시글이 존재하지 않으면 BOARD_NOT_FOUND 예외가 발생한다")
		void unlikeThrowsNotFoundWhenBoardDoesNotExist() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);

			// when & then
			assertThatThrownBy(() -> boardLikeService.unlike(workspace.getId(), 99999L, member.getUser().getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOARD_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("getLikeStatus: 좋아요 상태 조회")
	class GetLikeStatus {

		@Test
		@DisplayName("getLikeStatus: 삭제되지 않은 좋아요 수와 내 좋아요 여부를 함께 반환한다")
		void getLikeStatusReturnsCountAndMyLike() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember me = createMember(workspace, WorkspaceMemberRole.MEMBER);
			WorkspaceMember other = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, me);
			boardLikeService.like(workspace.getId(), board.getId(), me.getUser().getId());
			boardLikeService.like(workspace.getId(), board.getId(), other.getUser().getId());

			// when
			BoardLikeStatusResponse result = boardLikeService.getLikeStatus(workspace.getId(), board.getId(), me.getUser().getId());

			// then
			assertThat(result).extracting("likeCount", "myLike")
				.containsExactly(2L, true);
		}

		@Test
		@DisplayName("getLikeStatus: 좋아요가 없으면 좋아요 수 0과 내 좋아요 false를 반환한다")
		void getLikeStatusReturnsZeroAndFalseWhenNoLikes() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember me = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, me);

			// when
			BoardLikeStatusResponse result = boardLikeService.getLikeStatus(workspace.getId(), board.getId(), me.getUser().getId());

			// then
			assertThat(result).extracting("likeCount", "myLike")
				.containsExactly(0L, false);
		}

		@Test
		@DisplayName("getLikeStatus: 워크스페이스 소속이 아닌 멤버면 FORBIDDEN_RESOURCE 예외가 발생한다")
		void getLikeStatusForbiddenWhenNotMember() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);
			Board board = createBoard(workspace, member);

			// when & then
			assertThatThrownBy(() -> boardLikeService.getLikeStatus(workspace.getId(), board.getId(), 9999L))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_RESOURCE);
		}

		@Test
		@DisplayName("getLikeStatus: 게시글이 존재하지 않으면 BOARD_NOT_FOUND 예외가 발생한다")
		void getLikeStatusThrowsNotFoundWhenBoardDoesNotExist() {
			// given
			Workspace workspace = createWorkspace();
			WorkspaceMember member = createMember(workspace, WorkspaceMemberRole.MEMBER);

			// when & then
			assertThatThrownBy(() -> boardLikeService.getLikeStatus(workspace.getId(), 99999L, member.getUser().getId()))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOARD_NOT_FOUND);
		}
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace, WorkspaceMemberRole role) {
		User user = userRepository.save(User.createLocalUser(uniqueEmail(), Password.ofEncoded("password"), "사용자"));
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
