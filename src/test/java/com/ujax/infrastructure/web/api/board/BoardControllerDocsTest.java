package com.ujax.infrastructure.web.api.board;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
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
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.infrastructure.web.board.BoardController;

@Tag("restDocs")
@WebMvcTest(BoardController.class)
@AutoConfigureRestDocs
@Import(GlobalExceptionHandler.class)
class BoardControllerDocsTest {

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

	private BoardDetailResponse boardDetail() {
		return new BoardDetailResponse(
			2L, 1L, BoardType.FREE, false, "제목", "내용",
			3L, 5L, 2L, true,
			new BoardAuthorResponse(3L, "작성자"),
			LocalDateTime.now(), LocalDateTime.now()
		);
	}

	@Test
	@DisplayName("게시글 목록 조회 API")
	void listBoards() throws Exception {
		BoardListResponse response = BoardListResponse.of(
			List.of(new BoardListItemResponse(
				2L, 1L, BoardType.FREE, false, "제목", "미리보기",
				3L, 5L, 2L, false,
				new BoardAuthorResponse(3L, "작성자"),
				LocalDateTime.now(), LocalDateTime.now()
			)),
			new PageInfo(0, 20, 1L, 1)
		);
		given(boardService.listBoards(eq(1L), eq(3L), any())).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards", 1L)
				.header("X-WS-MEMBER-ID", 3L)
				.param("type", "FREE")
				.param("keyword", "검색")
				.param("page", "0")
				.param("size", "20")
				.param("sort", "createdAt,desc")
				.param("pinnedFirst", "true"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 목록 조회")
					.description("워크스페이스 게시글 목록을 조회합니다")
					.pathParameters(parameterWithName("workspaceId").description("워크스페이스 ID"))
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.queryParameters(
						parameterWithName("type").optional().description("게시글 타입"),
						parameterWithName("keyword").optional().description("검색어"),
						parameterWithName("page").optional().description("페이지"),
						parameterWithName("size").optional().description("크기"),
						parameterWithName("sort").optional().description("정렬"),
						parameterWithName("pinnedFirst").optional().description("고정글 우선 정렬")
					)
					.responseSchema(Schema.schema("ApiResponse-BoardList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("게시글 상세 조회 API")
	void getBoardDetail() throws Exception {
		given(boardService.getBoardDetail(1L, 2L, 3L)).willReturn(boardDetail());

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 상세 조회")
					.description("게시글 상세 정보를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-BoardDetail"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("게시글 생성 API")
	void createBoard() throws Exception {
		given(boardService.createBoard(eq(1L), eq(3L), any())).willReturn(boardDetail());

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards", 1L)
				.header("X-WS-MEMBER-ID", 3L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CreateBoardBody("FREE", "제목", "내용", true))))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 생성")
					.description("게시글을 생성합니다")
					.pathParameters(parameterWithName("workspaceId").description("워크스페이스 ID"))
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.requestSchema(Schema.schema("CreateBoardRequest"))
					.requestFields(
						fieldWithPath("type").type(JsonFieldType.STRING).description("게시글 타입"),
						fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
						fieldWithPath("content").type(JsonFieldType.STRING).description("내용"),
						fieldWithPath("pinned").type(JsonFieldType.BOOLEAN).description("고정 여부").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-BoardDetail"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("게시글 수정 API")
	void updateBoard() throws Exception {
		given(boardService.updateBoard(eq(1L), eq(2L), eq(3L), any())).willReturn(boardDetail());

		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new UpdateBoardBody("QNA", "수정 제목", "수정 내용", true))))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 수정")
					.description("게시글을 수정합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.requestSchema(Schema.schema("UpdateBoardRequest"))
					.requestFields(
						fieldWithPath("type").type(JsonFieldType.STRING).description("게시글 타입").optional(),
						fieldWithPath("title").type(JsonFieldType.STRING).description("제목").optional(),
						fieldWithPath("content").type(JsonFieldType.STRING).description("내용").optional(),
						fieldWithPath("pinned").type(JsonFieldType.BOOLEAN).description("고정 여부").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-BoardDetail"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("게시글 고정 API")
	void pinBoard() throws Exception {
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/boards/{boardId}/pin", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new PinBoardBody(true))))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-pin",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 고정 상태 변경")
					.description("게시글의 고정 상태를 변경합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.requestSchema(Schema.schema("PinBoardRequest"))
					.requestFields(fieldWithPath("pinned").type(JsonFieldType.BOOLEAN).description("고정 여부"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("게시글 삭제 API")
	void deleteBoard() throws Exception {
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 삭제")
					.description("게시글을 삭제합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("좋아요 상태 조회 API")
	void getLikeStatus() throws Exception {
		given(boardLikeService.getLikeStatus(1L, 2L, 3L)).willReturn(BoardLikeStatusResponse.of(5L, true));

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-like-status",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 좋아요 상태 조회")
					.description("좋아요 수와 내 좋아요 여부를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-BoardLikeStatus"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
						fieldWithPath("data.myLike").type(JsonFieldType.BOOLEAN).description("내 좋아요 여부"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("좋아요 등록 API")
	void likeBoard() throws Exception {
		mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-like",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 좋아요 등록")
					.description("게시글에 좋아요를 등록합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("좋아요 취소 API")
	void unlikeBoard() throws Exception {
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}/likes", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-unlike",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 좋아요 취소")
					.description("게시글 좋아요를 취소합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("댓글 목록 조회 API")
	void listComments() throws Exception {
		CommentListResponse response = CommentListResponse.of(
			List.of(new CommentResponse(10L, 2L, "댓글", new BoardAuthorResponse(3L, "작성자"), LocalDateTime.now())),
			new PageInfo(0, 20, 1L, 1)
		);
		given(boardCommentService.listComments(1L, 2L, 3L, 0, 20)).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-comment-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 댓글 목록 조회")
					.description("게시글 댓글 목록을 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.queryParameters(
						parameterWithName("page").optional().description("페이지"),
						parameterWithName("size").optional().description("크기")
					)
					.responseSchema(Schema.schema("ApiResponse-CommentList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						subsectionWithPath("data").description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("댓글 생성 API")
	void createComment() throws Exception {
		CommentResponse response = new CommentResponse(10L, 2L, "댓글", new BoardAuthorResponse(3L, "작성자"), LocalDateTime.now());
		given(boardCommentService.createComment(1L, 2L, 3L, "댓글")).willReturn(response);

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments", 1L, 2L)
				.header("X-WS-MEMBER-ID", 3L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CommentBody("댓글"))))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-comment-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 댓글 생성")
					.description("게시글 댓글을 생성합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.requestSchema(Schema.schema("CreateCommentRequest"))
					.requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 내용"))
					.responseSchema(Schema.schema("ApiResponse-Comment"))
						.responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
							subsectionWithPath("data").description("응답 데이터"),
							fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
						)
					.build()
				)
			));
	}

	@Test
	@DisplayName("댓글 삭제 API")
	void deleteComment() throws Exception {
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/boards/{boardId}/comments/{commentId}", 1L, 2L, 10L)
				.header("X-WS-MEMBER-ID", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("board-comment-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Board")
					.summary("게시글 댓글 삭제")
					.description("게시글 댓글을 삭제합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("boardId").description("게시글 ID"),
						parameterWithName("commentId").description("댓글 ID")
					)
					.requestHeaders(headerWithName("X-WS-MEMBER-ID").description("워크스페이스 멤버 ID"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
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
