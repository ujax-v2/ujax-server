package com.ujax.application.board;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardLike;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class BoardLikeServiceUnitTest {

	@Mock
	private BoardRepository boardRepository;

	@Mock
	private BoardLikeRepository boardLikeRepository;

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@InjectMocks
	private BoardLikeService boardLikeService;

	@Test
	@DisplayName("like: 저장 시 중복 예외가 발생하면 기존 좋아요를 활성 상태로 복구한다")
	void likeRecoversFromDataIntegrityViolationByReactivatingExistingLike() {
		// given
		Long workspaceId = 1L;
		Long boardId = 2L;
		Long workspaceMemberId = 3L;

		WorkspaceMember member = mock(WorkspaceMember.class);
		Board board = mock(Board.class);

		given(member.getId()).willReturn(workspaceMemberId);
		given(board.getId()).willReturn(boardId);
		given(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, workspaceMemberId))
			.willReturn(Optional.of(member));
		given(boardRepository.findByIdAndWorkspaceId(boardId, workspaceId))
			.willReturn(Optional.of(board));
		given(boardLikeRepository.updateDeleted(boardId, workspaceMemberId, false))
			.willReturn(0);
		willThrow(new DataIntegrityViolationException("duplicate key"))
			.given(boardLikeRepository).saveAndFlush(any(BoardLike.class));

		// when
		boardLikeService.like(workspaceId, boardId, workspaceMemberId);

		// then
		then(boardLikeRepository).should(times(2))
			.updateDeleted(boardId, workspaceMemberId, false);
		then(boardLikeRepository).should().saveAndFlush(any(BoardLike.class));
	}
}
