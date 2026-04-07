package com.ujax.infrastructure.web.api.problem;

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
import java.util.ArrayList;
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
import com.ujax.application.problem.WorkspaceProblemService;
import com.ujax.application.problem.dto.response.WorkspaceProblemResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.WorkspaceProblemController;
import com.ujax.infrastructure.web.problem.dto.request.CreateWorkspaceProblemRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateWorkspaceProblemRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(WorkspaceProblemController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceProblemControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WorkspaceProblemService workspaceProblemService;

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
	@DisplayName("문제 목록 조회 API")
	void listWorkspaceProblems() throws Exception {
		// given
		LocalDateTime now = LocalDateTime.now();
		WorkspaceProblemResponse item = new WorkspaceProblemResponse(
			1L, 1000, "A+B", "Bronze V", now.plusDays(7), now, now);

		PageResponse<WorkspaceProblemResponse> response = PageResponse.of(List.of(item), 0, 10, 1L, 1);
		given(workspaceProblemService.listWorkspaceProblems(
			anyLong(), anyLong(), anyLong(), nullable(String.class), anyInt(), anyInt()))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.param("keyword", "A+B")
				.param("page", "0")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-problem-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 목록 조회")
					.description("문제집에 등록된 문제 목록을 페이징 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.queryParameters(
						parameterWithName("keyword").optional().description("문제 번호/제목 검색어"),
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceProblemList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("문제 목록"),
						fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("문제집 문제 ID"),
						fieldWithPath("data.content[].problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("data.content[].tier").type(JsonFieldType.STRING).description("문제 난이도").optional(),
						fieldWithPath("data.content[].deadline").type(JsonFieldType.STRING).description("마감일").optional(),
						fieldWithPath("data.content[].scheduledAt").type(JsonFieldType.STRING).description("예정일").optional(),
						fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("등록일"),
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
	@DisplayName("문제 목록 조회 API - 권한 없음")
	void listWorkspaceProblemsForbidden() throws Exception {
		// given
		given(workspaceProblemService.listWorkspaceProblems(
			anyLong(), anyLong(), anyLong(), nullable(String.class), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-problem-list-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 목록 조회")
					.description("문제 목록 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 등록 API")
	void createWorkspaceProblem() throws Exception {
		// given
		LocalDateTime now = LocalDateTime.now();
		CreateWorkspaceProblemRequest request = new CreateWorkspaceProblemRequest(
			1L, now.plusDays(7), now);
		WorkspaceProblemResponse response = new WorkspaceProblemResponse(
			1L, 1000, "A+B", "Bronze V", now.plusDays(7), now, now);
		given(workspaceProblemService.createWorkspaceProblem(anyLong(), anyLong(), anyLong(), any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-problem-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 등록")
					.description("문제집에 문제를 등록합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("CreateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("problemId").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("deadline").type(JsonFieldType.STRING).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.STRING).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceProblemResponse"))
					.responseFields(workspaceProblemResponseFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 등록 API - 중복")
	void createWorkspaceProblemDuplicate() throws Exception {
		// given
		CreateWorkspaceProblemRequest request = new CreateWorkspaceProblemRequest(1L, null, null);
		given(workspaceProblemService.createWorkspaceProblem(anyLong(), anyLong(), anyLong(), any()))
			.willThrow(new ConflictException(ErrorCode.DUPLICATE_WORKSPACE_PROBLEM));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andDo(document("workspace-problem-create-duplicate",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 등록")
					.description("문제 등록")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("CreateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("problemId").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("deadline").type(JsonFieldType.NULL).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.NULL).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Conflict"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 등록 API - 권한 없음")
	void createWorkspaceProblemForbidden() throws Exception {
		// given
		CreateWorkspaceProblemRequest request = new CreateWorkspaceProblemRequest(1L, null, null);
		given(workspaceProblemService.createWorkspaceProblem(anyLong(), anyLong(), anyLong(), any()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-problem-create-forbidden",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 등록")
					.description("문제 등록")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("CreateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("problemId").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("deadline").type(JsonFieldType.NULL).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.NULL).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 등록 API - 문제 없음")
	void createWorkspaceProblemNotFound() throws Exception {
		// given
		CreateWorkspaceProblemRequest request = new CreateWorkspaceProblemRequest(999L, null, null);
		given(workspaceProblemService.createWorkspaceProblem(anyLong(), anyLong(), anyLong(), any()))
			.willThrow(new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andDo(document("workspace-problem-create-not-found",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 등록")
					.description("문제 등록")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("CreateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("problemId").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("deadline").type(JsonFieldType.NULL).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.NULL).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-NotFound"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 수정 API")
	void updateWorkspaceProblem() throws Exception {
		// given
		LocalDateTime now = LocalDateTime.now();
		UpdateWorkspaceProblemRequest request = new UpdateWorkspaceProblemRequest(
			now.plusDays(14), now.plusDays(7));
		WorkspaceProblemResponse response = new WorkspaceProblemResponse(
			1L, 1000, "A+B", "Bronze V", now.plusDays(14), now.plusDays(7), now);
		given(workspaceProblemService.updateWorkspaceProblem(anyLong(), anyLong(), anyLong(), anyLong(), any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}", 1, 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-problem-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 수정")
					.description("문제집 문제의 마감일/예정일을 수정합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("deadline").type(JsonFieldType.STRING).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.STRING).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceProblemResponse"))
					.responseFields(workspaceProblemResponseFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 수정 API - 권한 없음")
	void updateWorkspaceProblemForbidden() throws Exception {
		// given
		UpdateWorkspaceProblemRequest request = new UpdateWorkspaceProblemRequest(null, null);
		given(workspaceProblemService.updateWorkspaceProblem(anyLong(), anyLong(), anyLong(), anyLong(), any()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN));

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}", 1, 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-problem-update-forbidden",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 수정")
					.description("문제 수정")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceProblemRequest"))
					.requestFields(
						fieldWithPath("deadline").type(JsonFieldType.NULL).description("마감일").optional(),
						fieldWithPath("scheduledAt").type(JsonFieldType.NULL).description("예정일").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 삭제 API")
	void deleteWorkspaceProblem() throws Exception {
		// given
		willDoNothing().given(workspaceProblemService)
			.deleteWorkspaceProblem(anyLong(), anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}", 1, 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-problem-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 삭제")
					.description("문제집에서 문제를 삭제합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID"),
						parameterWithName("workspaceProblemId").description("문제집 문제 ID")
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

	@Test
	@DisplayName("문제 삭제 API - 권한 없음")
	void deleteWorkspaceProblemForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(workspaceProblemService).deleteWorkspaceProblem(anyLong(), anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}", 1, 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-problem-delete-forbidden",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("WorkspaceProblem")
					.summary("문제 삭제")
					.description("문제 삭제")
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

	private static FieldDescriptor[] workspaceProblemResponseFields() {
		return new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
			fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제집 문제 ID"),
			fieldWithPath("data.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
			fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제 제목"),
			fieldWithPath("data.tier").type(JsonFieldType.STRING).description("문제 난이도").optional(),
			fieldWithPath("data.deadline").type(JsonFieldType.STRING).description("마감일").optional(),
			fieldWithPath("data.scheduledAt").type(JsonFieldType.STRING).description("예정일").optional(),
			fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("등록일"),
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

	private static FieldDescriptor[] problemDetailFieldsWithFieldErrors() {
		List<FieldDescriptor> descriptors = new ArrayList<>(List.of(problemDetailFields()));
		descriptors.add(fieldWithPath("fieldErrors").type(JsonFieldType.ARRAY).description("필드 오류 목록"));
		descriptors.add(fieldWithPath("fieldErrors[].field").type(JsonFieldType.STRING).description("필드명"));
		descriptors.add(fieldWithPath("fieldErrors[].rejectedValue").type(JsonFieldType.VARIES).description("거절된 값").optional());
		descriptors.add(fieldWithPath("fieldErrors[].message").type(JsonFieldType.STRING).description("오류 메시지"));
		return descriptors.toArray(new FieldDescriptor[0]);
	}
}
