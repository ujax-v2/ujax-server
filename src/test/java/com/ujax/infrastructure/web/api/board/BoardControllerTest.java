package com.ujax.infrastructure.web.api.board;

import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.board.BoardCommentService;
import com.ujax.application.board.BoardLikeService;
import com.ujax.application.board.BoardService;
import com.ujax.application.board.dto.response.BoardAuthorResponse;
import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardLikeStatusResponse;
import com.ujax.application.board.dto.response.BoardListItemResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.application.board.dto.response.CommentListResponse;
import com.ujax.application.board.dto.response.CommentResponse;
import com.ujax.domain.board.BoardType;
import com.ujax.global.dto.PageResponse.PageInfo;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.board.BoardController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(BoardController.class)
@Import(TestSecurityConfig.class)
class BoardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private BoardService boardService;

	@MockitoBean
	private BoardCommentService boardCommentService;

	@MockitoBean
	private BoardLikeService boardLikeService;

	@BeforeEach
	void setUpSecurityContext() {
		Claims claims = Jwts.claims()
			.subject("3")
			.add("role", "USER")
			.add("name", "테스트유저")
			.add("email", "test@example.com")
			.build();
		UserPrincipal principal = UserPrincipal.fromClaims(claims);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);
	}

	private BoardDetailResponse boardDetail() {
		return new BoardDetailResponse(
			2L,
			1L,
			BoardType.FREE,
			false,
			"제목",
			"내용",
			3L,
			5L,
			2L,
			true,
			new BoardAuthorResponse(3L, "작성자"),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}

	@Nested
	@DisplayName("GET /boards: 게시글 목록 조회")
	class ListBoards {

		@Test
		@DisplayName("정상 요청이면 게시글 목록을 반환한다")
		void listBoardsSuccess() throws Exception {
			// given
			BoardListResponse response = BoardListResponse.of(
				List.of(new BoardListItemResponse(
					2L,
					1L,
					BoardType.FREE,
					false,
					"제목",
					"미리보기",
					3L,
					5L,
					2L,
					false,
					new BoardAuthorResponse(3L, "작성자"),
					LocalDateTime.now(),
					LocalDateTime.now()
				)),
				new PageInfo(0, 20, 1L, 1)
			);
			given(boardService.listBoards(eq(1L), eq(3L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards", 1L)
					.param("type", "FREE")
					.param("keyword", "검색")
					.param("page", "0")
					.param("size", "20")
					.param("sort", "createdAt,desc")
					.param("pinnedFirst", "true"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].boardId").value(2))
				.andExpect(jsonPath("$.data.page.totalElements").value(1));
		}
	}

	@Nested
	@DisplayName("GET /boards/{boardId}: 게시글 상세 조회")
	class GetBoardDetail {

		@Test
		@DisplayName("정상 요청이면 게시글 상세를 반환한다")
		void getBoardDetailSuccess() throws Exception {
			// given
			given(boardService.getBoardDetail(1L, 2L, 3L)).willReturn(boardDetail());

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.boardId").value(2))
				.andExpect(jsonPath("$.data.title").value("제목"));
		}
	}

	@Nested
	@DisplayName("POST /boards: 게시글 생성")
	class CreateBoard {

		@Test
		@DisplayName("정상 요청이면 게시글을 생성한다")
		void createBoardSuccess() throws Exception {
			// given
			given(boardService.createBoard(eq(1L), eq(3L), any())).willReturn(boardDetail());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new CreateBoardBody("FREE", "제목", "내용", true))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.boardId").value(2));
		}

		@Test
		@DisplayName("제목이 비어 있으면 400 Bad Request를 반환한다")
		void createBoardBadRequestWhenBlankTitle() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new CreateBoardBody("FREE", "", "내용", true))))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("PATCH /boards/{boardId}: 게시글 수정")
	class UpdateBoard {

		@Test
		@DisplayName("정상 요청이면 게시글을 수정한다")
		void updateBoardSuccess() throws Exception {
			// given
			BoardDetailResponse updated = new BoardDetailResponse(
				2L, 1L, BoardType.QNA, true, "수정 제목", "수정 내용", 3L, 5L, 2L, true,
				new BoardAuthorResponse(3L, "작성자"), LocalDateTime.now(), LocalDateTime.now()
			);
			given(boardService.updateBoard(eq(1L), eq(2L), eq(3L), any())).willReturn(updated);

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UpdateBoardBody("QNA", "수정 제목", "수정 내용", true))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.type").value("QNA"))
				.andExpect(jsonPath("$.data.pinned").value(true));
		}
	}

	@Nested
	@DisplayName("PATCH /boards/{boardId}/pin: 게시글 고정 변경")
	class PinBoard {

		@Test
		@DisplayName("정상 요청이면 고정 상태를 변경한다")
		void pinBoardSuccess() throws Exception {
			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/boards/{boardId}/pin", 1L, 2L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new PinBoardBody(true))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(boardService).should().pinBoard(1L, 2L, 3L, true);
		}

		@Test
		@DisplayName("pinned 값이 없으면 400 Bad Request를 반환한다")
		void pinBoardBadRequestWhenPinnedMissing() throws Exception {
			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/boards/{boardId}/pin", 1L, 2L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("DELETE /boards/{boardId}: 게시글 삭제")
	class DeleteBoard {

		@Test
		@DisplayName("정상 요청이면 게시글을 삭제한다")
		void deleteBoardSuccess() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(boardService).should().deleteBoard(1L, 2L, 3L);
		}
	}

	@Nested
	@DisplayName("GET /boards/{boardId}/likes: 좋아요 상태 조회")
	class GetLikeStatus {

		@Test
		@DisplayName("정상 요청이면 좋아요 수와 내 좋아요 여부를 반환한다")
		void getLikeStatusSuccess() throws Exception {
			// given
			given(boardLikeService.getLikeStatus(1L, 2L, 3L))
				.willReturn(BoardLikeStatusResponse.of(5L, true));

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.likeCount").value(5))
				.andExpect(jsonPath("$.data.myLike").value(true));
		}

		@Test
		@DisplayName("게시글이 없으면 404 Not Found를 반환한다")
		void getLikeStatusNotFound() throws Exception {
			// given
			given(boardLikeService.getLikeStatus(1L, 2L, 3L))
				.willThrow(new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("워크스페이스 소속이 아닌 멤버면 403 Forbidden을 반환한다")
		void getLikeStatusForbidden() throws Exception {
			// given
			given(boardLikeService.getLikeStatus(1L, 2L, 3L))
				.willThrow(new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE));

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("PUT/DELETE /boards/{boardId}/likes: 좋아요 변경")
	class ToggleLike {

		@Test
		@DisplayName("PUT 요청이면 좋아요 등록을 처리한다")
		void likeBoardSuccess() throws Exception {
			// when & then
			mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(boardLikeService).should().like(1L, 2L, 3L);
		}

		@Test
		@DisplayName("DELETE 요청이면 좋아요 취소를 처리한다")
		void unlikeBoardSuccess() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(boardLikeService).should().unlike(1L, 2L, 3L);
		}

		@Test
		@DisplayName("PUT 요청에서 게시글이 없으면 404 Not Found를 반환한다")
		void likeBoardNotFound() throws Exception {
			// given
			willThrow(new NotFoundException(ErrorCode.BOARD_NOT_FOUND))
				.given(boardLikeService).like(1L, 2L, 3L);

			// when & then
			mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("DELETE 요청에서 워크스페이스 멤버가 아니면 403 Forbidden을 반환한다")
		void unlikeBoardForbidden() throws Exception {
			// given
			willThrow(new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE))
				.given(boardLikeService).unlike(1L, 2L, 3L);

			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L))
				.andDo(print())
				.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("GET/POST/DELETE /boards/{boardId}/comments: 댓글")
	class Comments {

		@Test
		@DisplayName("댓글 목록 조회 요청이면 목록을 반환한다")
		void listCommentsSuccess() throws Exception {
			// given
			CommentListResponse response = CommentListResponse.of(
				List.of(new CommentResponse(
					10L,
					2L,
					"댓글",
					new BoardAuthorResponse(3L, "작성자"),
					LocalDateTime.now()
				)),
				new PageInfo(0, 20, 1L, 1)
			);
			given(boardCommentService.listComments(1L, 2L, 3L, 0, 20)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments", 1L, 2L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].boardCommentId").value(10));
		}

		@Test
		@DisplayName("댓글 생성 요청이면 댓글을 생성한다")
		void createCommentSuccess() throws Exception {
			// given
			CommentResponse response = new CommentResponse(
				10L, 2L, "댓글", new BoardAuthorResponse(3L, "작성자"), LocalDateTime.now()
			);
			given(boardCommentService.createComment(1L, 2L, 3L, "댓글")).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments", 1L, 2L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new CommentBody("댓글"))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").value("댓글"));
		}

		@Test
		@DisplayName("댓글 내용이 비어 있으면 400 Bad Request를 반환한다")
		void createCommentBadRequestWhenBlankContent() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments", 1L, 2L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new CommentBody(""))))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("댓글 삭제 요청이면 댓글을 삭제한다")
		void deleteCommentSuccess() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments/{commentId}", 1L, 2L, 10L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(boardCommentService).should().deleteComment(1L, 2L, 10L, 3L);
		}
	}

	private record CreateBoardBody(String type, String title, String content, Boolean pinned) {
	}

	private record UpdateBoardBody(String type, String title, String content, Boolean pinned) {
	}

	private record PinBoardBody(Boolean pinned) {
	}

	private record CommentBody(String content) {
	}
}
