package com.ujax.infrastructure.web.api.workspace;

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
import com.ujax.application.workspace.WorkspaceJoinRequestService;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatus;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceJoinRequestController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(WorkspaceJoinRequestController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceJoinRequestControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WorkspaceJoinRequestService workspaceJoinRequestService;

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
	@DisplayName("워크스페이스 가입 신청 API")
	void createJoinRequest() throws Exception {
		// given
		WorkspaceJoinRequestResponse response = new WorkspaceJoinRequestResponse(
			10L,
			1L,
			LocalDateTime.of(2026, 2, 24, 10, 0)
		);
		given(workspaceJoinRequestService.createJoinRequest(anyLong(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청")
					.description("사용자가 워크스페이스 가입을 신청합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceJoinRequestResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.requestId").type(JsonFieldType.NUMBER).description("가입 신청 ID"),
						fieldWithPath("data.workspaceId").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("신청 생성 시각"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("내 워크스페이스 가입 신청 상태 조회 API")
	void getMyJoinRequestStatus() throws Exception {
		// given
		WorkspaceMyJoinRequestStatusResponse response = WorkspaceMyJoinRequestStatusResponse.of(
			false,
			WorkspaceMyJoinRequestStatus.NONE,
			true
		);
		given(workspaceJoinRequestService.getMyJoinRequestStatus(anyLong(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests/me", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-my-status",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("내 워크스페이스 가입 신청 상태 조회")
					.description("사용자의 워크스페이스 가입 신청 상태를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMyJoinRequestStatusResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.isMember").type(JsonFieldType.BOOLEAN).description("현재 멤버 여부"),
						fieldWithPath("data.joinRequestStatus").type(JsonFieldType.STRING).description("가입 신청 상태"),
						fieldWithPath("data.canApply").type(JsonFieldType.BOOLEAN).description("가입 신청 가능 여부"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("내 워크스페이스 가입 신청 취소 API")
	void cancelJoinRequest() throws Exception {
		// given
		willDoNothing().given(workspaceJoinRequestService).cancelJoinRequest(anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/join-requests/me", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-cancel",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("내 워크스페이스 가입 신청 취소")
					.description("사용자가 자신의 워크스페이스 가입 신청을 취소합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
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
	@DisplayName("워크스페이스 가입 신청 목록 조회 API")
	void listJoinRequests() throws Exception {
		// given
		WorkspaceJoinRequestListItemResponse item = new WorkspaceJoinRequestListItemResponse(
			10L,
			1L,
			21L,
			"홍길동",
			LocalDateTime.of(2026, 2, 24, 10, 0)
		);
		PageResponse<WorkspaceJoinRequestListItemResponse> response = PageResponse.of(List.of(item), 0, 20, 1L, 1);
		given(workspaceJoinRequestService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests", 1L)
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 목록 조회")
					.description("소유자가 워크스페이스 가입 신청 목록을 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceJoinRequestPage"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("가입 신청 목록"),
						fieldWithPath("data.content[].requestId").type(JsonFieldType.NUMBER).description("가입 신청 ID"),
						fieldWithPath("data.content[].workspaceId").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.content[].applicantUserId").type(JsonFieldType.NUMBER).description("신청 사용자 ID"),
						fieldWithPath("data.content[].applicantName").type(JsonFieldType.STRING).description("신청 사용자 이름"),
						fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("신청 생성 시각"),
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
	@DisplayName("워크스페이스 가입 신청 목록 조회 API - 권한 없음")
	void listJoinRequestsForbidden() throws Exception {
		// given
		given(workspaceJoinRequestService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests", 1L)
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-join-request-list-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 목록 조회")
					.description("워크스페이스 가입 신청 목록 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 가입 신청 수락 API")
	void approveJoinRequest() throws Exception {
		// given
		willDoNothing().given(workspaceJoinRequestService).approveJoinRequest(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/approve", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-approve",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 수락")
					.description("소유자가 워크스페이스 가입 신청을 수락합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("requestId").description("가입 신청 ID")
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
	@DisplayName("워크스페이스 가입 신청 수락 API - 권한 없음")
	void approveJoinRequestForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED))
			.given(workspaceJoinRequestService).approveJoinRequest(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/approve", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-join-request-approve-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 수락")
					.description("워크스페이스 가입 신청 수락")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("requestId").description("가입 신청 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 가입 신청 거부 API")
	void rejectJoinRequest() throws Exception {
		// given
		willDoNothing().given(workspaceJoinRequestService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/reject", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-join-request-reject",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 거부")
					.description("소유자가 워크스페이스 가입 신청을 거부합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("requestId").description("가입 신청 ID")
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
	@DisplayName("워크스페이스 가입 신청 거부 API - 권한 없음")
	void rejectJoinRequestForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED))
			.given(workspaceJoinRequestService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/reject", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-join-request-reject-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 가입 신청 거부")
					.description("워크스페이스 가입 신청 거부")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("requestId").description("가입 신청 ID")
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
