package com.ujax.infrastructure.web.api.workspace;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
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
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceController;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceController.class)
@Import(TestSecurityConfig.class)
class WorkspaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

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

	@Nested
	@DisplayName("내 워크스페이스 목록 조회")
	class ListMyWorkspaces {

		@Test
		@DisplayName("페이지 파라미터 없이 내 워크스페이스 목록을 조회한다")
		void listMyWorkspaces() throws Exception {
			// given
			WorkspaceResponse workspace = new WorkspaceResponse(
				1L,
				"워크스페이스",
				"소개",
				"https://image.example.com/workspaces/1.png"
			);
			given(workspaceService.listMyWorkspaces(anyLong())).willReturn(List.of(workspace));

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/me"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].name").value("워크스페이스"))
				.andExpect(jsonPath("$.data[0].imageUrl").value("https://image.example.com/workspaces/1.png"));

			then(workspaceService).should().listMyWorkspaces(1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 목록 조회")
	class ListWorkspaceMembers {

		@Test
		@DisplayName("page/size를 반영해 멤버 목록을 조회한다")
		void listWorkspaceMembers() throws Exception {
			// given
			WorkspaceMemberResponse member = new WorkspaceMemberResponse(1L, "닉네임", WorkspaceMemberRole.MEMBER);
			PageResponse<WorkspaceMemberResponse> response = PageResponse.of(List.of(member), 0, 20, 1L, 1);
			given(workspaceService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 3L)
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].workspaceMemberId").value(1))
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaceMembers(3L, 1L, 0, 20);
		}

		@Test
		@DisplayName("page/size가 없으면 기본값(0,20)으로 조회한다")
		void listWorkspaceMembersDefaultPageable() throws Exception {
			// given
			PageResponse<WorkspaceMemberResponse> response = PageResponse.of(List.of(), 0, 20, 0L, 0);
			given(workspaceService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaceMembers(3L, 1L, 0, 20);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탐색 목록 조회")
	class ListWorkspaces {

		@Test
		@DisplayName("검색어와 페이지 파라미터를 반영해 탐색 목록을 조회한다")
		void listWorkspaces() throws Exception {
			// given
			WorkspaceResponse workspace = new WorkspaceResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png"
			);
			PageResponse<WorkspaceResponse> response = PageResponse.of(List.of(workspace), 0, 20, 1L, 1);
			given(workspaceService.listWorkspaces(any(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/explore")
					.param("name", "알고리즘")
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].id").value(3))
				.andExpect(jsonPath("$.data.content[0].name").value("알고리즘 스터디"))
				.andExpect(jsonPath("$.data.content[0].imageUrl").value("https://image.example.com/workspaces/3.png"))
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaces("알고리즘", 0, 20);
		}

		@Test
		@DisplayName("파라미터가 없으면 기본값(0,20)으로 탐색 목록을 조회한다")
		void listWorkspacesWithDefaultPageable() throws Exception {
			// given
			PageResponse<WorkspaceResponse> response = PageResponse.of(List.of(), 0, 20, 0L, 0);
			given(workspaceService.listWorkspaces(any(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/explore"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaces(isNull(), eq(0), eq(20));
		}
	}

	@Nested
	@DisplayName("워크스페이스 상세 조회")
	class GetWorkspace {

		@Test
		@DisplayName("워크스페이스 상세 정보를 조회한다")
		void getWorkspace() throws Exception {
			// given
			WorkspaceResponse response = new WorkspaceResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png"
			);
			given(workspaceService.getWorkspace(anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.name").value("알고리즘 스터디"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/3.png"));

			then(workspaceService).should().getWorkspace(3L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 설정 조회")
	class GetWorkspaceSettings {

		@Test
		@DisplayName("워크스페이스 설정 정보를 조회한다")
		void getWorkspaceSettings() throws Exception {
			// given
			WorkspaceSettingsResponse response = new WorkspaceSettingsResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png",
				"https://hook.example.com"
			);
			given(workspaceService.getWorkspaceSettings(anyLong(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/settings", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/3.png"))
				.andExpect(jsonPath("$.data.mmWebhookUrl").value("https://hook.example.com"));

			then(workspaceService).should().getWorkspaceSettings(3L, 1L);
		}
	}

	@Nested
	@DisplayName("내 워크스페이스 멤버 정보 조회")
	class GetMyWorkspaceMember {

		@Test
		@DisplayName("내 워크스페이스 멤버 정보를 조회한다")
		void getMyWorkspaceMember() throws Exception {
			// given
			WorkspaceMemberResponse response = new WorkspaceMemberResponse(11L, "테스터", WorkspaceMemberRole.MEMBER);
			given(workspaceService.getMyWorkspaceMember(anyLong(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.workspaceMemberId").value(11))
				.andExpect(jsonPath("$.data.nickname").value("테스터"))
				.andExpect(jsonPath("$.data.role").value("MEMBER"));

			then(workspaceService).should().getMyWorkspaceMember(3L, 1L);
		}
	}

	@Nested
	@DisplayName("내 워크스페이스 닉네임 수정")
	class UpdateMyWorkspaceNickname {

		@Test
		@DisplayName("내 워크스페이스 닉네임을 수정한다")
		void updateMyWorkspaceNickname() throws Exception {
			// given
			WorkspaceMemberResponse response = new WorkspaceMemberResponse(11L, "변경닉", WorkspaceMemberRole.MEMBER);
			given(workspaceService.updateMyWorkspaceNickname(anyLong(), anyLong(), anyString())).willReturn(response);

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/me/nickname", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nickname\":\"변경닉\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.nickname").value("변경닉"));

			then(workspaceService).should().updateMyWorkspaceNickname(3L, 1L, "변경닉");
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 초대")
	class InviteWorkspaceMember {

		@Test
		@DisplayName("이메일로 워크스페이스 멤버를 초대한다")
		void inviteWorkspaceMember() throws Exception {
			// given
			willDoNothing().given(workspaceService).inviteWorkspaceMember(anyLong(), anyLong(), anyString());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"member@example.com\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().inviteWorkspaceMember(3L, 1L, "member@example.com");
		}

		@Test
		@DisplayName("이메일 형식이 잘못되면 400을 반환한다")
		void inviteWorkspaceMemberWithInvalidEmail() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"invalid-email\"}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("워크스페이스 이미지 Presigned URL 생성")
	class CreateWorkspaceImagePresignedUrl {

		@Test
		@DisplayName("오너가 업로드용 Presigned URL을 생성한다")
		void createWorkspaceImagePresignedUrl() throws Exception {
			// given
			PresignedUrlResponse response = new PresignedUrlResponse("https://presigned-url", "https://image-url");
			given(workspaceService.createWorkspaceImagePresignedUrl(anyLong(), anyLong(), any(WorkspaceImageUploadRequest.class)))
				.willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/image/presigned-url", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"contentType\":\"image/png\",\"fileSize\":1024}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.presignedUrl").value("https://presigned-url"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image-url"));

			then(workspaceService).should()
				.createWorkspaceImagePresignedUrl(eq(3L), eq(1L), any(WorkspaceImageUploadRequest.class));
		}

		@Test
		@DisplayName("요청 값이 비어 있으면 400을 반환한다")
		void createWorkspaceImagePresignedUrlValidationError() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/image/presigned-url", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"contentType\":\"\",\"fileSize\":null}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("가입 신청 생성")
	class CreateJoinRequest {

		@Test
		@DisplayName("가입 신청을 생성한다")
		void createJoinRequest() throws Exception {
			// given
			WorkspaceJoinRequestResponse response = new WorkspaceJoinRequestResponse(
				10L,
				3L,
				WorkspaceJoinRequestStatus.PENDING,
				LocalDateTime.of(2026, 2, 24, 10, 0)
			);
			given(workspaceService.createJoinRequest(anyLong(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.requestId").value(10))
				.andExpect(jsonPath("$.data.workspaceId").value(3))
				.andExpect(jsonPath("$.data.status").value("PENDING"));
		}
	}

	@Nested
	@DisplayName("가입 신청 수락")
	class ApproveJoinRequest {

		@Test
		@DisplayName("가입 신청을 수락한다")
		void approveJoinRequest() throws Exception {
			// given
			willDoNothing().given(workspaceService).approveJoinRequest(anyLong(), anyLong(), anyLong());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/approve", 3L, 10L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().approveJoinRequest(3L, 1L, 10L);
		}
	}

	@Nested
	@DisplayName("내 가입 신청 상태 조회")
	class GetMyJoinRequestStatus {

		@Test
		@DisplayName("내 가입 신청 상태를 조회한다")
		void getMyJoinRequestStatus() throws Exception {
			// given
			WorkspaceMyJoinRequestStatusResponse response = WorkspaceMyJoinRequestStatusResponse.of(
				false,
				WorkspaceMyJoinRequestStatus.NONE,
				true
			);
			given(workspaceService.getMyJoinRequestStatus(anyLong(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.isMember").value(false))
				.andExpect(jsonPath("$.data.joinRequestStatus").value("NONE"))
				.andExpect(jsonPath("$.data.canApply").value(true));
		}
	}

	@Nested
	@DisplayName("가입 신청 목록 조회")
	class ListJoinRequests {

		@Test
		@DisplayName("가입 신청 목록을 조회한다")
		void listJoinRequests() throws Exception {
			// given
			WorkspaceJoinRequestListItemResponse item = new WorkspaceJoinRequestListItemResponse(
				10L,
				3L,
				21L,
				"홍길동",
				WorkspaceJoinRequestStatus.PENDING,
				LocalDateTime.of(2026, 2, 24, 10, 0)
			);
			PageResponse<WorkspaceJoinRequestListItemResponse> response = PageResponse.of(
				List.of(item),
				0,
				20,
				1L,
				1
			);
			given(workspaceService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests", 3L)
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].requestId").value(10))
				.andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

			then(workspaceService).should().listJoinRequests(3L, 1L, 0, 20);
		}
	}

	@Nested
	@DisplayName("가입 신청 거부")
	class RejectJoinRequest {

		@Test
		@DisplayName("가입 신청을 거부한다")
		void rejectJoinRequest() throws Exception {
			// given
			willDoNothing().given(workspaceService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/reject", 3L, 10L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().rejectJoinRequest(3L, 1L, 10L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 생성")
	class CreateWorkspace {

		@Test
		@DisplayName("워크스페이스를 생성한다")
		void createWorkspace() throws Exception {
			// given
			WorkspaceResponse response = new WorkspaceResponse(
				20L,
				"신규 워크스페이스",
				"소개",
				"https://image.example.com/workspaces/20.png"
			);
			given(workspaceService.createWorkspace(anyString(), any(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"신규 워크스페이스\",\"description\":\"소개\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(20))
				.andExpect(jsonPath("$.data.name").value("신규 워크스페이스"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/20.png"));

			then(workspaceService).should().createWorkspace("신규 워크스페이스", "소개", 1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("워크스페이스 정보를 수정한다")
		void updateWorkspace() throws Exception {
			// given
			WorkspaceResponse response = new WorkspaceResponse(
				3L,
				"수정된 워크스페이스",
				"수정된 소개",
				"https://new-image.com/workspace.png"
			);
			given(workspaceService.updateWorkspace(anyLong(), anyLong(), any(), any(), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"수정된 워크스페이스\",\"description\":\"수정된 소개\",\"mmWebhookUrl\":\"https://hook.example.com\",\"imageUrl\":\"https://new-image.com/workspace.png\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.name").value("수정된 워크스페이스"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://new-image.com/workspace.png"));

			then(workspaceService).should().updateWorkspace(
				3L,
				1L,
				"수정된 워크스페이스",
				"수정된 소개",
				"https://hook.example.com",
				"https://new-image.com/workspace.png"
			);
		}
	}

	@Nested
	@DisplayName("워크스페이스 삭제")
	class DeleteWorkspace {

		@Test
		@DisplayName("워크스페이스를 삭제한다")
		void deleteWorkspace() throws Exception {
			// given
			willDoNothing().given(workspaceService).deleteWorkspace(anyLong(), anyLong());

			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().deleteWorkspace(3L, 1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 권한 수정")
	class UpdateWorkspaceMemberRole {

		@Test
		@DisplayName("멤버 권한을 수정한다")
		void updateWorkspaceMemberRole() throws Exception {
			// given
			willDoNothing().given(workspaceService)
				.updateWorkspaceMemberRole(anyLong(), anyLong(), anyLong(), any(WorkspaceMemberRole.class));

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}/role", 3L, 11L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"role\":\"MANAGER\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().updateWorkspaceMemberRole(3L, 1L, 11L, WorkspaceMemberRole.MANAGER);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 제거")
	class RemoveWorkspaceMember {

		@Test
		@DisplayName("워크스페이스 멤버를 제거한다")
		void removeWorkspaceMember() throws Exception {
			// given
			willDoNothing().given(workspaceService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}", 3L, 11L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().removeWorkspaceMember(3L, 1L, 11L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탈퇴")
	class LeaveWorkspace {

		@Test
		@DisplayName("현재 워크스페이스에서 탈퇴한다")
		void leaveWorkspace() throws Exception {
			// given
			willDoNothing().given(workspaceService).leaveWorkspace(anyLong(), anyLong());

			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().leaveWorkspace(3L, 1L);
		}
	}
}
