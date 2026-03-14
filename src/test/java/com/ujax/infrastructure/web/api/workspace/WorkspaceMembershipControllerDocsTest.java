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
import com.ujax.application.workspace.WorkspaceMembershipService;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceMembershipController;
import com.ujax.infrastructure.web.workspace.dto.request.InviteWorkspaceMemberRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberNicknameRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(WorkspaceMembershipController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceMembershipControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WorkspaceMembershipService workspaceMembershipService;

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
	@DisplayName("워크스페이스 멤버 목록 조회 API")
	void listWorkspaceMembers() throws Exception {
		WorkspaceMemberResponse member = new WorkspaceMemberResponse(1L, "닉네임", WorkspaceMemberRole.MEMBER);
		PageResponse<WorkspaceMemberResponse> response = PageResponse.of(List.of(member), 0, 20, 1L, 1);
		given(workspaceMembershipService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

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
		given(workspaceMembershipService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

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
		WorkspaceMemberResponse response = new WorkspaceMemberResponse(1L, "닉네임", WorkspaceMemberRole.MEMBER);
		given(workspaceMembershipService.getMyWorkspaceMember(anyLong(), anyLong())).willReturn(response);

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
		given(workspaceMembershipService.getMyWorkspaceMember(anyLong(), anyLong()))
			.willThrow(new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

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
		UpdateWorkspaceMemberNicknameRequest request = new UpdateWorkspaceMemberNicknameRequest("새닉네임");
		WorkspaceMemberResponse response = new WorkspaceMemberResponse(1L, "새닉네임", WorkspaceMemberRole.MEMBER);
		given(workspaceMembershipService.updateMyWorkspaceNickname(anyLong(), anyLong(), anyString())).willReturn(response);

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
		UpdateWorkspaceMemberNicknameRequest request = new UpdateWorkspaceMemberNicknameRequest("");

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
	@DisplayName("워크스페이스 멤버 권한 변경 API")
	void updateWorkspaceMemberRole() throws Exception {
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest(WorkspaceMemberRole.MANAGER);
		willDoNothing().given(workspaceMembershipService).updateWorkspaceMemberRole(anyLong(), anyLong(), anyLong(), any());

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
		willDoNothing().given(workspaceMembershipService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

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
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(workspaceMembershipService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

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
		willDoNothing().given(workspaceMembershipService).leaveWorkspace(anyLong(), anyLong());

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
		willThrow(new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN))
			.given(workspaceMembershipService).leaveWorkspace(anyLong(), anyLong());

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
		InviteWorkspaceMemberRequest request = new InviteWorkspaceMemberRequest("invite@example.com");
		willDoNothing().given(workspaceMembershipService).inviteWorkspaceMember(anyLong(), anyLong(), anyString());

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
		InviteWorkspaceMemberRequest request = new InviteWorkspaceMemberRequest("invalid-email");

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
