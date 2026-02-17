package com.ujax.application.board;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.board.dto.response.BoardLikeStatusResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardLike;
import com.ujax.domain.board.BoardLikeId;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardLikeService {

	private final BoardRepository boardRepository;
	private final BoardLikeRepository boardLikeRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public BoardLikeStatusResponse getLikeStatus(Long workspaceId, Long boardId, Long workspaceMemberId) {
		validateMember(workspaceId, workspaceMemberId);
		findBoard(workspaceId, boardId);

		long likeCount = extractSingleCount(boardLikeRepository.countByBoardIds(List.of(boardId)));
		boolean myLike = !boardLikeRepository.findMyLikedBoardIds(List.of(boardId), workspaceMemberId).isEmpty();

		return BoardLikeStatusResponse.of(likeCount, myLike);
	}

	@Transactional
	public void like(Long workspaceId, Long boardId, Long workspaceMemberId) {
		WorkspaceMember member = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);

		int updated = boardLikeRepository.updateDeleted(board.getId(), member.getId(), false);
		if (updated > 0) {
			return;
		}

		try {
			boardLikeRepository.saveAndFlush(BoardLike.create(board, member));
		} catch (DataIntegrityViolationException ex) {
			boardLikeRepository.updateDeleted(board.getId(), member.getId(), false);
		}
	}

	@Transactional
	public void unlike(Long workspaceId, Long boardId, Long workspaceMemberId) {
		WorkspaceMember member = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);

		BoardLikeId id = new BoardLikeId(board.getId(), member.getId());
		boardLikeRepository.findById(id)
			.ifPresent(like -> like.updateDeleted(true));
	}

	private WorkspaceMember validateMember(Long workspaceId, Long workspaceMemberId) {
		return workspaceMemberRepository.findByWorkspace_IdAndId(workspaceId, workspaceMemberId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "워크스페이스에 소속된 멤버가 아닙니다."));
	}

	private Board findBoard(Long workspaceId, Long boardId) {
		return boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
	}

	private long extractSingleCount(List<Object[]> results) {
		if (results.isEmpty()) {
			return 0L;
		}
		return (Long)results.get(0)[1];
	}
}
