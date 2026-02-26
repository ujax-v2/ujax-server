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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.workspace.WorkspaceService;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatus;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.domain.workspace.WorkspaceJoinRequestStatus;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceController;
import com.ujax.infrastructure.web.workspace.dto.request.CreateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.InviteWorkspaceMemberRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberNicknameRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(WorkspaceController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WorkspaceService workspaceService;

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
	@DisplayName("워크스페이스 탐색 목록 조회 API")
	void listWorkspaces() throws Exception {
		// given
		WorkspaceResponse workspace = new WorkspaceResponse(1L, "워크스페이스", "소개");
		PageResponse<WorkspaceResponse> response = PageResponse.of(List.of(workspace), 0, 20, 1L, 1);
		given(workspaceService.listWorkspaces(any(), anyInt(), anyInt())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/explore")
				.param("name", "워크")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-explore",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 탐색 목록 조회")
					.description("워크스페이스 탐색 페이지에서 목록을 조회합니다")
					.queryParameters(
						parameterWithName("name").optional().description("검색어"),
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceExplore"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("워크스페이스 목록"),
						fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.content[].name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.content[].description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
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
	@DisplayName("워크스페이스 탐색 목록 조회 API - 잘못된 파라미터")
	void listWorkspacesInvalidParameter() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/workspaces/explore")
				.param("page", "invalid")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-explore-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 탐색 목록 조회")
					.description("워크스페이스 탐색 목록 조회")
					.queryParameters(
						parameterWithName("name").optional().description("검색어"),
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ProblemDetail-InvalidParameter"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("내 워크스페이스 목록 조회 API (/me)")
	void listMyWorkspacesByMe() throws Exception {
		// given
		WorkspaceResponse workspace = new WorkspaceResponse(1L, "워크스페이스", "소개");
		List<WorkspaceResponse> response = List.of(workspace);
		given(workspaceService.listMyWorkspaces(anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-my-list-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("내 워크스페이스 목록 조회")
					.description("유저가 속한 워크스페이스 목록을 조회합니다 (/me)")
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMyList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("응답 데이터"),
						fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data[].name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data[].description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 단건 조회 API")
	void getWorkspace() throws Exception {
		// given
		WorkspaceResponse response = new WorkspaceResponse(1L, "워크스페이스", "소개");
		given(workspaceService.getWorkspace(anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 상세 조회")
					.description("워크스페이스 상세 정보를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 단건 조회 API - 워크스페이스 없음")
	void getWorkspaceNotFound() throws Exception {
		// given
		given(workspaceService.getWorkspace(anyLong()))
			.willThrow(new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 999)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andDo(document("workspace-get-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 상세 조회")
					.description("워크스페이스 상세 조회")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-NotFound"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 설정 조회 API")
	void getWorkspaceSettings() throws Exception {
		// given
		WorkspaceSettingsResponse response = new WorkspaceSettingsResponse(1L, "워크스페이스", "소개", "https://hook.example.com");
		given(workspaceService.getWorkspaceSettings(anyLong(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/settings", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-settings",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 설정 조회")
					.description("워크스페이스 설정 정보를 조회합니다 (소유자 전용)")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceSettings"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("data.mmWebhookUrl").type(JsonFieldType.STRING).description("MM 웹훅 URL").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 설정 조회 API - 권한 없음")
	void getWorkspaceSettingsForbidden() throws Exception {
		// given
		given(workspaceService.getWorkspaceSettings(anyLong(), anyLong()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/settings", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-settings-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 설정 조회")
					.description("워크스페이스 설정 조회")
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
	@DisplayName("워크스페이스 멤버 목록 조회 API")
	void listWorkspaceMembers() throws Exception {
		// given
		WorkspaceMemberResponse member = new WorkspaceMemberResponse(1L, "닉네임", WorkspaceMemberRole.MEMBER);
		PageResponse<WorkspaceMemberResponse> response = PageResponse.of(List.of(member), 0, 20, 1L, 1);
		given(workspaceService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 1)
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-members",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 목록 조회")
					.description("워크스페이스 멤버를 권한(OWNER→MANAGER→MEMBER), 생성일/ID 오름차순으로 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.queryParameters(
						parameterWithName("page").optional().description("페이지 번호"),
						parameterWithName("size").optional().description("페이지 크기")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMemberPage"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("멤버 목록"),
						fieldWithPath("data.content[].workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.content[].nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.content[].role").type(JsonFieldType.STRING).description("권한"),
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
	@DisplayName("워크스페이스 멤버 목록 조회 API - 권한 없음")
	void listWorkspaceMembersForbidden() throws Exception {
		// given
		given(workspaceService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 1)
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-members-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 목록 조회")
					.description("워크스페이스 멤버 목록 조회")
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
	@DisplayName("워크스페이스 멤버 조회 API")
	void getMyWorkspaceMember() throws Exception {
		// given
		WorkspaceMemberResponse response = new WorkspaceMemberResponse(1L, "닉네임", WorkspaceMemberRole.MEMBER);
		given(workspaceService.getMyWorkspaceMember(anyLong(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 조회")
					.description("워크스페이스에서 자신의 멤버 정보를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMemberResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.role").type(JsonFieldType.STRING).description("권한"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 멤버 조회 API - 권한 없음")
	void getMyWorkspaceMemberForbidden() throws Exception {
		// given
		given(workspaceService.getMyWorkspaceMember(anyLong(), anyLong()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-member-me-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 조회")
					.description("워크스페이스 멤버 조회")
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
	@DisplayName("워크스페이스 닉네임 수정 API")
	void updateMyWorkspaceNickname() throws Exception {
		// given
		UpdateWorkspaceMemberNicknameRequest request = new UpdateWorkspaceMemberNicknameRequest("새닉네임");
		WorkspaceMemberResponse response = new WorkspaceMemberResponse(1L, "새닉네임", WorkspaceMemberRole.MEMBER);
		given(workspaceService.updateMyWorkspaceNickname(anyLong(), anyLong(), anyString())).willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/me/nickname", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-nickname-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 닉네임 수정")
					.description("워크스페이스 닉네임 수정")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceMemberNicknameRequest"))
					.requestFields(
						fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMemberResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.role").type(JsonFieldType.STRING).description("권한"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 닉네임 수정 API - 유효성 오류")
	void updateMyWorkspaceNicknameValidationError() throws Exception {
		// given
		UpdateWorkspaceMemberNicknameRequest request = new UpdateWorkspaceMemberNicknameRequest("");

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/me/nickname", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-member-nickname-update-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 닉네임 수정")
					.description("워크스페이스 닉네임 수정")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceMemberNicknameRequest"))
					.requestFields(
						fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임")
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 이미지 Presigned URL 생성 API")
	void createWorkspaceImagePresignedUrl() throws Exception {
		// given
		WorkspaceImageUploadRequest request = new WorkspaceImageUploadRequest("image/png", 1048576L);
		PresignedUrlResponse response = new PresignedUrlResponse(
			"https://ujax-profile-images.s3.ap-northeast-2.amazonaws.com/presigned?X-Amz-Algorithm=...",
			"https://ujax-profile-images.s3.ap-northeast-2.amazonaws.com/workspaces/1/image/uuid.png"
		);
		given(workspaceService.createWorkspaceImagePresignedUrl(anyLong(), anyLong(), any(WorkspaceImageUploadRequest.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/image/presigned-url", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-create-image-presigned-url",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 이미지 업로드 Presigned URL 생성")
					.description("S3에 워크스페이스 이미지를 업로드하기 위한 Presigned URL을 생성합니다. JPEG, PNG, WEBP만 허용되며 최대 5MB입니다.")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("WorkspaceImageUploadRequest"))
					.responseSchema(Schema.schema("ApiResponse-PresignedUrlResponse"))
					.requestFields(
						fieldWithPath("contentType").type(JsonFieldType.STRING).description("이미지 Content-Type (image/jpeg, image/png, image/webp)"),
						fieldWithPath("fileSize").type(JsonFieldType.NUMBER).description("파일 크기 (바이트, 최대 5MB)")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.presignedUrl").type(JsonFieldType.STRING).description("S3 업로드용 Presigned URL"),
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("업로드 완료 후 이미지 접근 URL"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 생성 API")
	void createWorkspace() throws Exception {
		// given
		CreateWorkspaceRequest request = new CreateWorkspaceRequest("워크스페이스", "소개");
		WorkspaceResponse response = new WorkspaceResponse(1L, "워크스페이스", "소개");
		given(workspaceService.createWorkspace(anyString(), any(), anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 생성")
					.description("워크스페이스를 생성하고 소유자를 등록합니다")
					.requestSchema(Schema.schema("CreateWorkspaceRequest"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 생성 API - 유효성 오류")
	void createWorkspaceValidationError() throws Exception {
		// given
		CreateWorkspaceRequest request = new CreateWorkspaceRequest("", "소개");

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-create-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 생성")
					.description("워크스페이스 생성")
					.requestSchema(Schema.schema("CreateWorkspaceRequest"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 수정 API")
	void updateWorkspace() throws Exception {
		// given
		UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
			"새 이름",
			"새 소개",
			"https://hook.example.com",
			"https://new-image.com/workspace.png"
		);
		WorkspaceResponse response = new WorkspaceResponse(1L, "새 이름", "새 소개");
		given(workspaceService.updateWorkspace(anyLong(), anyLong(), anyString(), any(), any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 수정")
					.description("워크스페이스 정보를 수정합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceRequest"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("워크스페이스 이름").optional(),
						fieldWithPath("description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("mmWebhookUrl").type(JsonFieldType.STRING).description("MM 웹훅 URL").optional(),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 수정 API - 유효성 오류")
	void updateWorkspaceValidationError() throws Exception {
		// given
		UpdateWorkspaceRequest request = new UpdateWorkspaceRequest("", "새 소개", null, null);
		willThrow(new com.ujax.global.exception.common.BadRequestException(ErrorCode.INVALID_INPUT))
			.given(workspaceService)
			.updateWorkspace(anyLong(), anyLong(), any(), any(), any(), any());

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-update-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 수정")
					.description("워크스페이스 수정")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceRequest"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("워크스페이스 이름").optional(),
						fieldWithPath("description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("mmWebhookUrl").type(JsonFieldType.STRING).description("MM 웹훅 URL").optional(),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL").optional()
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 삭제 API")
	void deleteWorkspace() throws Exception {
		// given
		willDoNothing().given(workspaceService).deleteWorkspace(anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 삭제")
					.description("워크스페이스를 삭제합니다")
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
	@DisplayName("워크스페이스 삭제 API - 권한 없음")
	void deleteWorkspaceForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED))
			.given(workspaceService).deleteWorkspace(anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-delete-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 삭제")
					.description("워크스페이스 삭제")
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
	@DisplayName("워크스페이스 멤버 권한 변경 API")
	void updateWorkspaceMemberRole() throws Exception {
		// given
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest(WorkspaceMemberRole.MANAGER);
		willDoNothing().given(workspaceService).updateWorkspaceMemberRole(anyLong(), anyLong(), anyLong(), any());

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}/role", 1, 2)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-role-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 권한 변경")
					.description("워크스페이스 멤버 권한을 변경합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID")
					)
					.requestSchema(Schema.schema("UpdateWorkspaceMemberRoleRequest"))
					.requestFields(
						fieldWithPath("role").type(JsonFieldType.STRING).description("변경할 권한")
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
	@DisplayName("워크스페이스 멤버 권한 변경 API - 유효성 오류")
	void updateWorkspaceMemberRoleValidationError() throws Exception {
		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}/role", 1, 2)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-member-role-update-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 권한 변경")
					.description("워크스페이스 멤버 권한 변경")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 멤버 추방 API")
	void removeWorkspaceMember() throws Exception {
		// given
		willDoNothing().given(workspaceService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}", 1, 2)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-remove",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 추방")
					.description("워크스페이스 멤버를 추방합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID")
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
	@DisplayName("워크스페이스 멤버 추방 API - 권한 없음")
	void removeWorkspaceMemberForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(workspaceService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}", 1, 2)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-member-remove-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 추방")
					.description("워크스페이스 멤버 추방")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("workspaceMemberId").description("워크스페이스 멤버 ID")
					)
					.responseSchema(Schema.schema("ProblemDetail-Forbidden"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 탈퇴 API")
	void leaveWorkspace() throws Exception {
		// given
		willDoNothing().given(workspaceService).leaveWorkspace(anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/me", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-leave",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 탈퇴")
					.description("워크스페이스에서 탈퇴합니다")
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
	@DisplayName("워크스페이스 탈퇴 API - 권한 없음")
	void leaveWorkspaceForbidden() throws Exception {
		// given
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(workspaceService).leaveWorkspace(anyLong(), anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/me", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andDo(document("workspace-leave-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 탈퇴")
					.description("워크스페이스 탈퇴")
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
	@DisplayName("워크스페이스 멤버 초대 API")
	void inviteWorkspaceMember() throws Exception {
		// given
		InviteWorkspaceMemberRequest request = new InviteWorkspaceMemberRequest("invite@example.com");
		willDoNothing().given(workspaceService).inviteWorkspaceMember(anyLong(), anyLong(), anyString());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-invite",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 초대")
					.description("워크스페이스 소유자가 이메일로 멤버를 초대합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("InviteWorkspaceMemberRequest"))
					.requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("초대할 이메일")
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
	@DisplayName("워크스페이스 멤버 초대 API - 유효성 오류")
	void inviteWorkspaceMemberValidationError() throws Exception {
		// given
		InviteWorkspaceMemberRequest request = new InviteWorkspaceMemberRequest("invalid-email");

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("workspace-invite-error",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 멤버 초대")
					.description("워크스페이스 멤버 초대")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.requestSchema(Schema.schema("InviteWorkspaceMemberRequest"))
					.requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("초대할 이메일")
					)
					.responseSchema(Schema.schema("ProblemDetail-Validation"))
					.responseFields(problemDetailFieldsWithFieldErrors())
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 가입 신청 API")
	void createJoinRequest() throws Exception {
		// given
		WorkspaceJoinRequestResponse response = new WorkspaceJoinRequestResponse(
			10L,
			1L,
			WorkspaceJoinRequestStatus.PENDING,
			LocalDateTime.of(2026, 2, 24, 10, 0)
		);
		given(workspaceService.createJoinRequest(anyLong(), anyLong())).willReturn(response);

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
						fieldWithPath("data.status").type(JsonFieldType.STRING).description("가입 신청 상태"),
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
		given(workspaceService.getMyJoinRequestStatus(anyLong(), anyLong())).willReturn(response);

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
	@DisplayName("워크스페이스 가입 신청 목록 조회 API")
	void listJoinRequests() throws Exception {
		// given
		WorkspaceJoinRequestListItemResponse item = new WorkspaceJoinRequestListItemResponse(
			10L,
			1L,
			21L,
			"홍길동",
			WorkspaceJoinRequestStatus.PENDING,
			LocalDateTime.of(2026, 2, 24, 10, 0)
		);
		PageResponse<WorkspaceJoinRequestListItemResponse> response = PageResponse.of(List.of(item), 0, 20, 1L, 1);
		given(workspaceService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

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
						fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("가입 신청 상태"),
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
		given(workspaceService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt()))
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
		willDoNothing().given(workspaceService).approveJoinRequest(anyLong(), anyLong(), anyLong());

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
			.given(workspaceService).approveJoinRequest(anyLong(), anyLong(), anyLong());

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
		willDoNothing().given(workspaceService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

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
			.given(workspaceService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

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
