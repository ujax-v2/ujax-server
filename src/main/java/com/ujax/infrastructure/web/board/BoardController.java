package com.ujax.infrastructure.web.board;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.board.BoardCommentService;
import com.ujax.application.board.BoardLikeService;
import com.ujax.application.board.BoardService;
import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.application.board.dto.response.CommentListResponse;
import com.ujax.application.board.dto.response.CommentResponse;
import com.ujax.domain.board.BoardType;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.board.dto.request.CreateBoardRequest;
import com.ujax.infrastructure.web.board.dto.request.CreateCommentRequest;
import com.ujax.infrastructure.web.board.dto.request.PinBoardRequest;
import com.ujax.infrastructure.web.board.dto.request.UpdateBoardRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/boards")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;
	private final BoardCommentService boardCommentService;
	private final BoardLikeService boardLikeService;

	@GetMapping
	public ApiResponse<BoardListResponse> listBoards(
		@PathVariable Long workspaceId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@RequestParam(required = false) BoardType type,
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) String sort,
		@RequestParam(defaultValue = "true") boolean pinnedFirst
	) {
		return ApiResponse.success(
			boardService.listBoards(workspaceId, workspaceMemberId, type, keyword, page, size, sort, pinnedFirst)
		);
	}

	@GetMapping("/{boardId}")
	public ApiResponse<BoardDetailResponse> getBoardDetail(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId
	) {
		return ApiResponse.success(boardService.getBoardDetail(workspaceId, boardId, workspaceMemberId));
	}

	@PostMapping
	public ApiResponse<BoardDetailResponse> createBoard(
		@PathVariable Long workspaceId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@Valid @RequestBody CreateBoardRequest request
	) {
		return ApiResponse.success(
			boardService.createBoard(
				workspaceId,
				workspaceMemberId,
				request.type(),
				request.title(),
				request.content(),
				request.pinned()
			)
		);
	}

	@PatchMapping("/{boardId}")
	public ApiResponse<BoardDetailResponse> updateBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@Valid @RequestBody UpdateBoardRequest request
	) {
		return ApiResponse.success(
			boardService.updateBoard(
				workspaceId,
				boardId,
				workspaceMemberId,
				request.type(),
				request.title(),
				request.content(),
				request.pinned()
			)
		);
	}

	@PatchMapping("/{boardId}/pin")
	public ApiResponse<Void> pinBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@Valid @RequestBody PinBoardRequest request
	) {
		boardService.pinBoard(workspaceId, boardId, workspaceMemberId, request.pinned());
		return ApiResponse.success();
	}

	@DeleteMapping("/{boardId}")
	public ApiResponse<Void> deleteBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId
	) {
		boardService.deleteBoard(workspaceId, boardId, workspaceMemberId);
		return ApiResponse.success();
	}

	@PutMapping("/{boardId}/likes")
	public ApiResponse<Void> likeBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId
	) {
		boardLikeService.like(workspaceId, boardId, workspaceMemberId);
		return ApiResponse.success();
	}

	@DeleteMapping("/{boardId}/likes")
	public ApiResponse<Void> unlikeBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId
	) {
		boardLikeService.unlike(workspaceId, boardId, workspaceMemberId);
		return ApiResponse.success();
	}

	@GetMapping("/{boardId}/comments")
	public ApiResponse<CommentListResponse> listComments(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(
			boardCommentService.listComments(workspaceId, boardId, workspaceMemberId, page, size)
		);
	}

	@PostMapping("/{boardId}/comments")
	public ApiResponse<CommentResponse> createComment(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId,
		@Valid @RequestBody CreateCommentRequest request
	) {
		return ApiResponse.success(
			boardCommentService.createComment(workspaceId, boardId, workspaceMemberId, request.content())
		);
	}

	@DeleteMapping("/{boardId}/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@PathVariable Long commentId,
		@RequestHeader("X-WS-MEMBER-ID") Long workspaceMemberId
	) {
		boardCommentService.deleteComment(workspaceId, boardId, commentId, workspaceMemberId);
		return ApiResponse.success();
	}
}
