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
import com.ujax.application.problem.ProblemBoxService;
import com.ujax.application.problem.dto.response.ProblemBoxListItemResponse;
import com.ujax.application.problem.dto.response.ProblemBoxResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.ProblemBoxController;
import com.ujax.infrastructure.web.problem.dto.request.CreateProblemBoxRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateProblemBoxRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(ProblemBoxController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class ProblemBoxControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProblemBoxService problemBoxService;

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
	@DisplayName("문제집 목록 조회 API")
	void listProblemBoxes() throws Exception {
		// given
		LocalDateTime now = LocalDateTime.now();
		ProblemBoxListItemResponse item = new ProblemBoxListItemResponse(1L, "문제집", now, now);

		PageResponse<ProblemBoxListItemResponse> response = PageResponse.of(List.of(item), 0, 9, 1L, 1);
		given(problemBoxService.listProblemBoxes(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes", 1)
				.param("page", "0")
				.param("size", "9")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("problem-box-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 목록 조회")
					.description("워크스페이스의 문제집 목록을 페이징 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemBoxList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("문제집 목록"),
						fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("문제집 ID"),
						fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성일"),
						fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("최근 수정일"),
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
	@DisplayName("문제집 목록 조회 API - 권한 없음")
	void listProblemBoxesForbidden() throws Exception {
		// given
		given(problemBoxService.listProblemBoxes(anyLong(), anyLong(), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("problem-box-list-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 목록 조회")
					.description("문제집 목록 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 단건 조회 API")
	void getProblemBox() throws Exception {
		// given
		ProblemBoxResponse response = new ProblemBoxResponse(1L, "문제집", "설명", LocalDateTime.now());
		given(problemBoxService.getProblemBox(anyLong(), anyLong(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("problem-box-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 단건 조회")
					.description("문제집 상세 정보를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemBoxResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제집 ID"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제집 설명").optional(),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 단건 조회 API - 문제집 없음")
	void getProblemBoxNotFound() throws Exception {
		// given
		given(problemBoxService.getProblemBox(anyLong(), anyLong(), anyLong()))
			.willThrow(new NotFoundException(ErrorCode.PROBLEM_BOX_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 999)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andDo(document("problem-box-get-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 단건 조회")
					.description("문제집 단건 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-NotFound"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 생성 API")
	void createProblemBox() throws Exception {
		// given
		CreateProblemBoxRequest request = new CreateProblemBoxRequest("문제집", "설명");
		ProblemBoxResponse response = new ProblemBoxResponse(1L, "문제집", "설명", LocalDateTime.now());
		given(problemBoxService.createProblemBox(anyLong(), anyLong(), any())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("problem-box-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 생성")
					.description("문제집을 생성합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("CreateProblemBoxRequest"))
					.requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("문제집 설명").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemBoxResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제집 ID"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제집 설명").optional(),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 생성 API - 유효성 오류")
	void createProblemBoxValidationError() throws Exception {
		// given
		CreateProblemBoxRequest request = new CreateProblemBoxRequest("", "설명");

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("problem-box-create-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 생성")
					.description("문제집 생성")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("CreateProblemBoxRequest"))
					.requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("문제집 설명").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 생성 API - 권한 없음")
	void createProblemBoxForbidden() throws Exception {
		// given
		CreateProblemBoxRequest request = new CreateProblemBoxRequest("문제집", "설명");
		given(problemBoxService.createProblemBox(anyLong(), anyLong(), any()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problem-boxes", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("problem-box-create-forbidden",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 생성")
					.description("문제집 생성")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("CreateProblemBoxRequest"))
					.requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("문제집 설명").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 수정 API")
	void updateProblemBox() throws Exception {
		// given
		UpdateProblemBoxRequest request = new UpdateProblemBoxRequest("새 제목", "새 설명");
		ProblemBoxResponse response = new ProblemBoxResponse(1L, "새 제목", "새 설명", LocalDateTime.now());
		given(problemBoxService.updateProblemBox(anyLong(), anyLong(), anyLong(), any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("problem-box-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 수정")
					.description("문제집 제목/설명을 수정합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("UpdateProblemBoxRequest"))
					.requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("문제집 설명").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemBoxResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제집 ID"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제집 설명").optional(),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 수정 API - 유효성 오류")
	void updateProblemBoxValidationError() throws Exception {
		// given
		UpdateProblemBoxRequest request = new UpdateProblemBoxRequest("", "설명");

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("problem-box-update-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 수정")
					.description("문제집 수정")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
					)
					.requestSchema(Schema.schema("UpdateProblemBoxRequest"))
					.requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("문제집 설명").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제집 삭제 API")
	void deleteProblemBox() throws Exception {
		// given
		willDoNothing().given(problemBoxService).deleteProblemBox(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("problem-box-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 삭제")
					.description("문제집을 삭제합니다 (OWNER/MANAGER 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemBoxId").description("문제집 ID")
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
	@DisplayName("문제집 삭제 API - 권한 없음")
	void deleteProblemBoxForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(problemBoxService).deleteProblemBox(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}", 1, 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("problem-box-delete-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("ProblemBox")
					.summary("문제집 삭제")
					.description("문제집 삭제")
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
