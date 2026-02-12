package com.ujax.application.board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.board.dto.response.CommentListResponse;
import com.ujax.application.board.dto.response.CommentResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardComment;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.dto.PageResponse.PageInfo;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

	private static final int CONTENT_MIN = 1;
	private static final int CONTENT_MAX = 255;

	private final BoardRepository boardRepository;
	private final BoardCommentRepository boardCommentRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public CommentListResponse listComments(Long workspaceId, Long boardId, Long workspaceMemberId, int page, int size) {
		validateMember(workspaceId, workspaceMemberId);
		validatePageable(page, size);
		validateBoardExists(workspaceId, boardId);

		Page<BoardComment> result = boardCommentRepository.findByBoard_Id(boardId, PageRequest.of(page, size));
		List<CommentResponse> items = result.getContent().stream()
			.map(CommentResponse::from)
			.toList();

		return CommentListResponse.of(
			items,
			new PageInfo(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
		);
	}

	@Transactional
	public CommentResponse createComment(Long workspaceId, Long boardId, Long workspaceMemberId, String content) {
		WorkspaceMember author = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);

		validateContent(content);
		BoardComment comment = BoardComment.create(board, author, content);
		BoardComment saved = boardCommentRepository.save(comment);
		return CommentResponse.from(saved);
	}

	@Transactional
	public void deleteComment(Long workspaceId, Long boardId, Long commentId, Long workspaceMemberId) {
		WorkspaceMember actor = validateMember(workspaceId, workspaceMemberId);
		BoardComment comment = findComment(boardId, commentId);
		if (!comment.getAuthor().getId().equals(actor.getId())) {
			throw new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "작성자만 이 작업을 수행할 수 있습니다.");
		}
		boardCommentRepository.delete(comment);
	}

	private WorkspaceMember validateMember(Long workspaceId, Long workspaceMemberId) {
		return workspaceMemberRepository.findByWorkspace_IdAndId(workspaceId, workspaceMemberId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "워크스페이스에 소속된 멤버가 아닙니다."));
	}

	private Board findBoard(Long workspaceId, Long boardId) {
		return boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
	}

	private void validateBoardExists(Long workspaceId, Long boardId) {
		if (!boardRepository.existsByIdAndWorkspace_Id(boardId, workspaceId)) {
			throw new NotFoundException(ErrorCode.BOARD_NOT_FOUND);
		}
	}

	private BoardComment findComment(Long boardId, Long commentId) {
		return boardCommentRepository.findByIdAndBoardId(commentId, boardId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_COMMENT_NOT_FOUND));
	}

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		int length = content.length();
		if (length < CONTENT_MIN || length > CONTENT_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validatePageable(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
	}
}
