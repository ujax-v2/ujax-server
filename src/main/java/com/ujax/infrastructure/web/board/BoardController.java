package com.ujax.infrastructure.web.board;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.ujax.application.board.BoardCommentService;
import com.ujax.application.board.BoardLikeService;
import com.ujax.application.board.BoardService;
import com.ujax.application.board.dto.request.BoardCreateRequest;
import com.ujax.application.board.dto.request.BoardListRequest;
import com.ujax.application.board.dto.request.BoardUpdateRequest;
import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardLikeStatusResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.application.board.dto.response.CommentListResponse;
import com.ujax.application.board.dto.response.CommentResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.domain.board.BoardType;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.board.dto.request.BoardImageUploadRequest;
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
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) BoardType type,
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) String sort,
		@RequestParam(defaultValue = "true") boolean pinnedFirst
	) {
		BoardListRequest payload = BoardListRequest.builder()
			.type(type)
			.keyword(keyword)
			.page(page)
			.size(size)
			.sort(sort)
			.pinnedFirst(pinnedFirst)
			.build();
		return ApiResponse.success(
			boardService.listBoards(workspaceId, principal.getUserId(), payload)
		);
	}

	@GetMapping("/{boardId}")
	public ApiResponse<BoardDetailResponse> getBoardDetail(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(boardService.getBoardDetail(workspaceId, boardId, principal.getUserId()));
	}

	@PostMapping
	public ApiResponse<BoardDetailResponse> createBoard(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateBoardRequest request
	) {
		BoardCreateRequest payload = BoardCreateRequest.builder()
			.type(request.type())
			.title(request.title())
			.content(request.content())
			.pinned(request.pinned())
			.build();
		return ApiResponse.success(
			boardService.createBoard(workspaceId, principal.getUserId(), payload)
		);
	}

	@PostMapping("/image/presigned-url")
	public ApiResponse<PresignedUrlResponse> createBoardImagePresignedUrl(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody BoardImageUploadRequest request
	) {
		return ApiResponse.success(
			boardService.createBoardImagePresignedUrl(workspaceId, principal.getUserId(), request)
		);
	}

	@PatchMapping("/{boardId}")
	public ApiResponse<BoardDetailResponse> updateBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateBoardRequest request
	) {
		BoardUpdateRequest payload = BoardUpdateRequest.builder()
			.type(request.type())
			.title(request.title())
			.content(request.content())
			.pinned(request.pinned())
			.build();
		return ApiResponse.success(
			boardService.updateBoard(workspaceId, boardId, principal.getUserId(), payload)
		);
	}

	@PatchMapping("/{boardId}/pin")
	public ApiResponse<Void> pinBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody PinBoardRequest request
	) {
		boardService.pinBoard(workspaceId, boardId, principal.getUserId(), request.pinned());
		return ApiResponse.success();
	}

	@DeleteMapping("/{boardId}")
	public ApiResponse<Void> deleteBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		boardService.deleteBoard(workspaceId, boardId, principal.getUserId());
		return ApiResponse.success();
	}

	@PutMapping("/{boardId}/likes")
	public ApiResponse<Void> likeBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		boardLikeService.like(workspaceId, boardId, principal.getUserId());
		return ApiResponse.success();
	}

	@GetMapping("/{boardId}/likes")
	public ApiResponse<BoardLikeStatusResponse> getLikeStatus(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(boardLikeService.getLikeStatus(workspaceId, boardId, principal.getUserId()));
	}

	@DeleteMapping("/{boardId}/likes")
	public ApiResponse<Void> unlikeBoard(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		boardLikeService.unlike(workspaceId, boardId, principal.getUserId());
		return ApiResponse.success();
	}

	@GetMapping("/{boardId}/comments")
	public ApiResponse<CommentListResponse> listComments(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(
			boardCommentService.listComments(workspaceId, boardId, principal.getUserId(), page, size)
		);
	}

	@PostMapping("/{boardId}/comments")
	public ApiResponse<CommentResponse> createComment(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateCommentRequest request
	) {
		return ApiResponse.success(
			boardCommentService.createComment(workspaceId, boardId, principal.getUserId(), request.content())
		);
	}

	@DeleteMapping("/{boardId}/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
		@PathVariable Long workspaceId,
		@PathVariable Long boardId,
		@PathVariable Long commentId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		boardCommentService.deleteComment(workspaceId, boardId, commentId, principal.getUserId());
		return ApiResponse.success();
	}
}
