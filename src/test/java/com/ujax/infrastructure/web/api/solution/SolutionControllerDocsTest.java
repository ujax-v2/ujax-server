package com.ujax.infrastructure.web.api.solution;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.solution.SolutionCommentService;
import com.ujax.application.solution.SolutionLikeService;
import com.ujax.application.solution.SolutionService;
import com.ujax.application.solution.dto.response.SolutionCommentResponse;
import com.ujax.application.solution.dto.response.SolutionLikeStatusResponse;
import com.ujax.application.solution.dto.response.SolutionMemberSummaryResponse;
import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.application.solution.dto.response.SolutionVersionResponse;
import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.domain.solution.SolutionStatus;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.solution.SolutionController;
import com.ujax.infrastructure.web.solution.dto.request.SolutionIngestRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(SolutionController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class SolutionControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SolutionService solutionService;

	@MockitoBean
	private SolutionLikeService solutionLikeService;

	@MockitoBean
	private SolutionCommentService solutionCommentService;

	@BeforeEach
	void setUpSecurityContext() {
		Claims claims = Jwts.claims()
			.subject("1")
			.add("role", "USER")
			.add("name", "테스트유저")
			.add("email", "test@example.com")
			.build();
		UserPrincipal principal = UserPrincipal.fromClaims(claims);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);
	}

	@Test
	@DisplayName("풀이 수집 API")
	void ingest() throws Exception {
		// given
		SolutionIngestRequest request = new SolutionIngestRequest(
			1L, 12345L, "맞았습니다!!",
			"0 ms", "2020 KB", "Java 11", "123 B", "System.out.println(1+2);");

		SolutionResponse response = new SolutionResponse(
			1L, 12345L, 1000, "유저", SolutionStatus.ACCEPTED,
			"0 ms", "2020 KB", ProgrammingLanguage.JAVA, "123 B",
			LocalDateTime.now());

		given(solutionService.ingest(any(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/submissions/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andDo(document("solution-ingest",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 수집")
					.description("Chrome Extension이 백준 채점 결과를 서버로 전송합니다")
					.requestSchema(Schema.schema("SolutionIngestRequest"))
					.requestFields(
						fieldWithPath("workspaceProblemId").type(JsonFieldType.NUMBER).description("워크스페이스 문제 ID"),
						fieldWithPath("submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
						fieldWithPath("verdict").type(JsonFieldType.STRING).description("채점 결과"),
						fieldWithPath("time").type(JsonFieldType.STRING).description("실행 시간").optional(),
						fieldWithPath("memory").type(JsonFieldType.STRING).description("메모리").optional(),
						fieldWithPath("language").type(JsonFieldType.STRING).description("언어").optional(),
						fieldWithPath("codeLength").type(JsonFieldType.STRING).description("코드 길이").optional(),
						fieldWithPath("code").type(JsonFieldType.STRING).description("소스 코드").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionResponse"))
					.responseFields(solutionResponseFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 수집 API - 워크스페이스 문제 없음")
	void ingestWorkspaceProblemNotFound() throws Exception {
		// given
		SolutionIngestRequest request = new SolutionIngestRequest(
			999L, 12345L, "맞았습니다!!",
			null, null, null, null, null);

		given(solutionService.ingest(any(), anyLong()))
			.willThrow(new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/submissions/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andDo(document("solution-ingest-not-found",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 수집")
					.description("풀이 수집")
					.requestSchema(Schema.schema("SolutionIngestRequest"))
					.requestFields(
						fieldWithPath("workspaceProblemId").type(JsonFieldType.NUMBER).description("워크스페이스 문제 ID"),
						fieldWithPath("submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
						fieldWithPath("verdict").type(JsonFieldType.STRING).description("채점 결과"),
						fieldWithPath("time").type(JsonFieldType.NULL).description("실행 시간").optional(),
						fieldWithPath("memory").type(JsonFieldType.NULL).description("메모리").optional(),
						fieldWithPath("language").type(JsonFieldType.NULL).description("언어").optional(),
						fieldWithPath("codeLength").type(JsonFieldType.NULL).description("코드 길이").optional(),
						fieldWithPath("code").type(JsonFieldType.NULL).description("소스 코드").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-NotFound"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 수집 API - 중복")
	void ingestDuplicate() throws Exception {
		// given
		SolutionIngestRequest request = new SolutionIngestRequest(
			1L, 12345L, "맞았습니다!!",
			null, null, null, null, null);

		given(solutionService.ingest(any(), anyLong()))
			.willThrow(new ConflictException(ErrorCode.DUPLICATE_SOLUTION));

		// when & then
		mockMvc.perform(post("/api/v1/submissions/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andDo(document("solution-ingest-duplicate",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 수집")
					.description("풀이 수집")
					.requestSchema(Schema.schema("SolutionIngestRequest"))
					.requestFields(
						fieldWithPath("workspaceProblemId").type(JsonFieldType.NUMBER).description("워크스페이스 문제 ID"),
						fieldWithPath("submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
						fieldWithPath("verdict").type(JsonFieldType.STRING).description("채점 결과"),
						fieldWithPath("time").type(JsonFieldType.NULL).description("실행 시간").optional(),
						fieldWithPath("memory").type(JsonFieldType.NULL).description("메모리").optional(),
						fieldWithPath("language").type(JsonFieldType.NULL).description("언어").optional(),
						fieldWithPath("codeLength").type(JsonFieldType.NULL).description("코드 길이").optional(),
						fieldWithPath("code").type(JsonFieldType.NULL).description("소스 코드").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Conflict"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 목록 조회 API")
	void getSolutions() throws Exception {
		// given
		SolutionResponse item = new SolutionResponse(
			1L, 12345L, 1000, "유저", SolutionStatus.ACCEPTED,
			"0 ms", "2020 KB", ProgrammingLanguage.JAVA, "123 B", LocalDateTime.now());

		PageResponse<SolutionResponse> response = PageResponse.of(List.of(item), 0, 20, 1L, 1);
		given(solutionService.getSolutions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), anyInt()))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solutions",
				1, 1, 1)
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 목록 조회")
					.description("특정 워크스페이스 문제에 대한 풀이 목록을 페이징 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("풀이 목록"),
						fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("풀이 ID"),
						fieldWithPath("data.content[].submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
						fieldWithPath("data.content[].problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.content[].memberName").type(JsonFieldType.STRING).description("멤버 이름"),
						fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("채점 상태"),
						fieldWithPath("data.content[].time").type(JsonFieldType.STRING).description("실행 시간").optional(),
						fieldWithPath("data.content[].memory").type(JsonFieldType.STRING).description("메모리").optional(),
						fieldWithPath("data.content[].programmingLanguage").type(JsonFieldType.STRING).description("프로그래밍 언어"),
						fieldWithPath("data.content[].codeLength").type(JsonFieldType.STRING).description("코드 길이").optional(),
						fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
						fieldWithPath("data.page").type(JsonFieldType.OBJECT).description("페이지 정보"),
						fieldWithPath("data.page.page").type(JsonFieldType.NUMBER).description("페이지 번호"),
						fieldWithPath("data.page.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
						fieldWithPath("data.page.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
						fieldWithPath("data.page.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
						fieldWithPath("data.page.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
						fieldWithPath("data.page.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 목록 조회 API - 권한 없음")
	void getSolutionsForbidden() throws Exception {
		// given
		given(solutionService.getSolutions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solutions",
				1, 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("solution-list-forbidden",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 목록 조회")
					.description("풀이 목록 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 멤버 요약 목록 조회 API")
	void getSolutionMembers() throws Exception {
		List<SolutionMemberSummaryResponse> response = List.of(
			new SolutionMemberSummaryResponse(
				11L,
				"pythonista",
				ProgrammingLanguage.PYTHON,
				SolutionStatus.ACCEPTED,
				2L,
				1L,
				LocalDateTime.now()
			)
		);

		given(solutionService.getSolutionMembers(anyLong(), anyLong(), anyLong(), anyLong()))
			.willReturn(response);

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members",
				1, 2, 3
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-member-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 멤버 요약 목록 조회")
					.description("문제를 푼 멤버별 최신 풀이 요약을 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionMemberSummaryList"))
					.responseFields(solutionMemberSummaryFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 버전 목록 조회 API")
	void getSolutionVersions() throws Exception {
		SolutionVersionResponse item = new SolutionVersionResponse(
			12005L,
			"print(sum(map(int, input().split())))",
			SolutionStatus.ACCEPTED,
			"28 ms",
			"31120 KB",
			ProgrammingLanguage.PYTHON,
			"34 B",
			LocalDateTime.now(),
			1L,
			true,
			0L
		);

		PageResponse<SolutionVersionResponse> response = PageResponse.of(List.of(item), 0, 1, 2L, 2);
		given(solutionService.getSolutionVersions(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), anyInt()))
			.willReturn(response);

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions",
				1, 2, 3, 4
			).param("page", "0").param("size", "1"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-version-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 버전 목록 조회")
					.description("특정 멤버의 제출 버전 목록을 최신순으로 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionVersionList"))
					.responseFields(solutionVersionFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 좋아요 등록 API")
	void likeSolution() throws Exception {
		given(solutionLikeService.like(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
			.willReturn(SolutionLikeStatusResponse.of(1L, true));

		mockMvc.perform(put(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-like",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 좋아요 등록")
					.description("특정 제출 버전에 좋아요를 등록합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID"),
						parameterWithName("submissionId").description("백준 제출 번호")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionLikeStatus"))
					.responseFields(solutionLikeStatusFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 좋아요 취소 API")
	void unlikeSolution() throws Exception {
		given(solutionLikeService.unlike(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
			.willReturn(SolutionLikeStatusResponse.of(0L, false));

		mockMvc.perform(delete(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-unlike",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 좋아요 취소")
					.description("특정 제출 버전의 좋아요를 취소합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID"),
						parameterWithName("submissionId").description("백준 제출 번호")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionLikeStatus"))
					.responseFields(solutionLikeStatusFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 댓글 목록 조회 API")
	void getSolutionComments() throws Exception {
		List<SolutionCommentResponse> response = List.of(
			new SolutionCommentResponse(1L, "pythonista", "좋은 풀이네요", LocalDateTime.now(), true)
		);

		given(solutionCommentService.getComments(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
			.willReturn(response);

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-comment-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 댓글 목록 조회")
					.description("특정 제출 버전의 댓글 목록을 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID"),
						parameterWithName("submissionId").description("백준 제출 번호")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionCommentList"))
					.responseFields(solutionCommentListFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 댓글 생성 API")
	void createSolutionComment() throws Exception {
		given(solutionCommentService.createComment(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyString()))
			.willReturn(new SolutionCommentResponse(1L, "pythonista", "댓글", LocalDateTime.now(), true));

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments",
				1, 2, 3, 4, 5
			).contentType(MediaType.APPLICATION_JSON).content("""
				{"content":"댓글"}
				"""))
			.andDo(print())
			.andExpect(status().isCreated())
			.andDo(document("solution-comment-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 댓글 생성")
					.description("특정 제출 버전에 댓글을 생성합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID"),
						parameterWithName("submissionId").description("백준 제출 번호")
					)
					.requestFields(
						fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 내용")
					)
					.responseSchema(Schema.schema("ApiResponse-SolutionCommentResponse"))
					.responseFields(solutionCommentResponseFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("풀이 댓글 삭제 API")
	void deleteSolutionComment() throws Exception {
		mockMvc.perform(delete(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments/{commentId}",
				1, 2, 3, 4, 5, 6
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("solution-comment-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Solution")
					.summary("풀이 댓글 삭제")
					.description("특정 제출 버전의 댓글을 삭제합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID"),
						parameterWithName("submissionId").description("백준 제출 번호"),
						parameterWithName("commentId").description("댓글 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	private static FieldDescriptor[] solutionResponseFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
			fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("풀이 ID"),
			fieldWithPath("data.submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
			fieldWithPath("data.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
			fieldWithPath("data.memberName").type(JsonFieldType.STRING).description("멤버 이름"),
			fieldWithPath("data.status").type(JsonFieldType.STRING).description("채점 상태"),
			fieldWithPath("data.time").type(JsonFieldType.STRING).description("실행 시간").optional(),
			fieldWithPath("data.memory").type(JsonFieldType.STRING).description("메모리").optional(),
			fieldWithPath("data.programmingLanguage").type(JsonFieldType.STRING).description("프로그래밍 언어"),
			fieldWithPath("data.codeLength").type(JsonFieldType.STRING).description("코드 길이").optional(),
			fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시각"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] solutionMemberSummaryFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.ARRAY).description("응답 데이터"),
			fieldWithPath("data[].workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
			fieldWithPath("data[].memberName").type(JsonFieldType.STRING).description("멤버 이름"),
			fieldWithPath("data[].programmingLanguage").type(JsonFieldType.STRING).description("최신 제출 언어"),
			fieldWithPath("data[].latestStatus").type(JsonFieldType.STRING).description("최신 제출 상태"),
			fieldWithPath("data[].submissionCount").type(JsonFieldType.NUMBER).description("총 제출 수"),
			fieldWithPath("data[].likes").type(JsonFieldType.NUMBER).description("최신 제출 좋아요 수"),
			fieldWithPath("data[].updatedAt").type(JsonFieldType.STRING).description("최신 제출 시각"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] solutionVersionFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
			fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("버전 목록"),
			fieldWithPath("data.content[].submissionId").type(JsonFieldType.NUMBER).description("백준 제출 번호"),
			fieldWithPath("data.content[].code").type(JsonFieldType.STRING).description("소스 코드").optional(),
			fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("채점 상태"),
			fieldWithPath("data.content[].time").type(JsonFieldType.STRING).description("실행 시간").optional(),
			fieldWithPath("data.content[].memory").type(JsonFieldType.STRING).description("메모리").optional(),
			fieldWithPath("data.content[].programmingLanguage").type(JsonFieldType.STRING).description("프로그래밍 언어").optional(),
			fieldWithPath("data.content[].codeLength").type(JsonFieldType.STRING).description("코드 길이").optional(),
			fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
			fieldWithPath("data.content[].likes").type(JsonFieldType.NUMBER).description("좋아요 수"),
			fieldWithPath("data.content[].isLiked").type(JsonFieldType.BOOLEAN).description("내 좋아요 여부"),
			fieldWithPath("data.content[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
			fieldWithPath("data.page").type(JsonFieldType.OBJECT).description("페이지 정보"),
			fieldWithPath("data.page.page").type(JsonFieldType.NUMBER).description("페이지 번호"),
			fieldWithPath("data.page.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
			fieldWithPath("data.page.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
			fieldWithPath("data.page.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
			fieldWithPath("data.page.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
			fieldWithPath("data.page.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] solutionLikeStatusFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
			fieldWithPath("data.likes").type(JsonFieldType.NUMBER).description("좋아요 수"),
			fieldWithPath("data.isLiked").type(JsonFieldType.BOOLEAN).description("내 좋아요 여부"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] solutionCommentResponseFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
			fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
			fieldWithPath("data.authorName").type(JsonFieldType.STRING).description("작성자 이름"),
			fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
			fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시각"),
			fieldWithPath("data.isMyComment").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] solutionCommentListFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.ARRAY).description("응답 데이터"),
			fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("댓글 ID"),
			fieldWithPath("data[].authorName").type(JsonFieldType.STRING).description("작성자 이름"),
			fieldWithPath("data[].content").type(JsonFieldType.STRING).description("댓글 내용"),
			fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
			fieldWithPath("data[].isMyComment").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
			fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
		};
	}

	private static FieldDescriptor[] problemDetailFields() {
		return new FieldDescriptor[] {
			fieldWithPath("type").type(JsonFieldType.STRING).description("오류 문서 URI"),
			fieldWithPath("title").type(JsonFieldType.STRING).description("에러 코드"),
			fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
			fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
			fieldWithPath("instance").type(JsonFieldType.STRING).description("오류 인스턴스").optional(),
			fieldWithPath("exception").type(JsonFieldType.STRING).description("예외 클래스명"),
			fieldWithPath("timestamp").type(JsonFieldType.STRING).description("발생 시각")
		};
	}
}
