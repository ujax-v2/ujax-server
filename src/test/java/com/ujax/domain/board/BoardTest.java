package com.ujax.domain.board;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;

class BoardTest {

	@Test
	@DisplayName("update: type/title/content/pinned 값이 주어지면 해당 필드를 모두 갱신한다")
	void updateAllFields() {
		// given
		Board board = createBoard();

		// when
		board.update(BoardType.QNA, "수정 제목", "수정 내용", true);

		// then
		assertThat(board.getType()).isEqualTo(BoardType.QNA);
		assertThat(board.getTitle()).isEqualTo("수정 제목");
		assertThat(board.getContent()).isEqualTo("수정 내용");
		assertThat(board.isPinned()).isTrue();
	}

	@Test
	@DisplayName("update: 일부 필드만 주어지면 null이 아닌 필드만 갱신한다")
	void updateOnlyNonNullFields() {
		// given
		Board board = createBoard();

		// when
		board.update(null, "수정 제목", null, null);

		// then
		assertThat(board.getType()).isEqualTo(BoardType.FREE);
		assertThat(board.getTitle()).isEqualTo("수정 제목");
		assertThat(board.getContent()).isEqualTo("내용");
		assertThat(board.isPinned()).isFalse();
	}

	@Test
	@DisplayName("update: 모든 필드가 null이면 기존 값을 그대로 유지한다")
	void updateWithAllNullKeepsOriginalValues() {
		// given
		Board board = createBoard();

		// when
		board.update(null, null, null, null);

		// then
		assertThat(board.getType()).isEqualTo(BoardType.FREE);
		assertThat(board.getTitle()).isEqualTo("제목");
		assertThat(board.getContent()).isEqualTo("내용");
		assertThat(board.isPinned()).isFalse();
	}

	@Test
	@DisplayName("updatePinned: 고정 여부를 전달한 값으로 변경한다")
	void updatePinnedChangesPinnedState() {
		// given
		Board board = createBoard();

		// when
		board.updatePinned(true);

		// then
		assertThat(board.isPinned()).isTrue();
	}

	private Board createBoard() {
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember author = mock(WorkspaceMember.class);
		return Board.create(workspace, author, BoardType.FREE, false, "제목", "내용");
	}
}
