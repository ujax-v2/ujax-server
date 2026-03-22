package com.ujax.infrastructure.web.api.workspace;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.ujax.application.workspace.WorkspaceMembershipService;
import com.ujax.application.workspace.dto.response.WorkspaceMemberListResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceMembershipController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceMembershipController.class)
@Import(TestSecurityConfig.class)
class WorkspaceMembershipControllerTest {

	@Autowired
	private MockMvc mockMvc;

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

	@Nested
	@DisplayName("워크스페이스 멤버 목록 조회")
	class ListWorkspaceMembers {

		@Test
		@DisplayName("page/size를 반영해 멤버 목록을 조회한다")
		void listWorkspaceMembers() throws Exception {
			WorkspaceMemberListResponse member = new WorkspaceMemberListResponse(
				1L,
				"닉네임",
				"member@example.com",
				WorkspaceMemberRole.MEMBER
			);
			PageResponse<WorkspaceMemberListResponse> response = PageResponse.of(List.of(member), 0, 20, 1L, 1);
			given(workspaceMembershipService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 3L)
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].workspaceMemberId").value(1))
				.andExpect(jsonPath("$.data.content[0].email").value("member@example.com"))
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceMembershipService).should().listWorkspaceMembers(3L, 1L, 0, 20);
		}

		@Test
		@DisplayName("page/size가 없으면 기본값(0,20)으로 조회한다")
		void listWorkspaceMembersDefaultPageable() throws Exception {
			PageResponse<WorkspaceMemberListResponse> response = PageResponse.of(List.of(), 0, 20, 0L, 0);
			given(workspaceMembershipService.listWorkspaceMembers(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceMembershipService).should().listWorkspaceMembers(3L, 1L, 0, 20);
		}
	}

	@Nested
	@DisplayName("내 워크스페이스 멤버 정보 조회")
	class GetMyWorkspaceMember {

		@Test
		@DisplayName("내 워크스페이스 멤버 정보를 조회한다")
		void getMyWorkspaceMember() throws Exception {
			WorkspaceMemberResponse response = new WorkspaceMemberResponse(11L, "테스터", WorkspaceMemberRole.MEMBER);
			given(workspaceMembershipService.getMyWorkspaceMember(anyLong(), anyLong())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.workspaceMemberId").value(11))
				.andExpect(jsonPath("$.data.nickname").value("테스터"))
				.andExpect(jsonPath("$.data.role").value("MEMBER"));

			then(workspaceMembershipService).should().getMyWorkspaceMember(3L, 1L);
		}
	}

	@Nested
	@DisplayName("내 워크스페이스 닉네임 수정")
	class UpdateMyWorkspaceNickname {

		@Test
		@DisplayName("내 워크스페이스 닉네임을 수정한다")
		void updateMyWorkspaceNickname() throws Exception {
			WorkspaceMemberResponse response = new WorkspaceMemberResponse(11L, "변경닉", WorkspaceMemberRole.MEMBER);
			given(workspaceMembershipService.updateMyWorkspaceNickname(anyLong(), anyLong(), anyString())).willReturn(response);

			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/me/nickname", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nickname\":\"변경닉\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.nickname").value("변경닉"));

			then(workspaceMembershipService).should().updateMyWorkspaceNickname(3L, 1L, "변경닉");
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 초대")
	class InviteWorkspaceMember {

		@Test
		@DisplayName("이메일로 워크스페이스 멤버를 초대한다")
		void inviteWorkspaceMember() throws Exception {
			willDoNothing().given(workspaceMembershipService).inviteWorkspaceMember(anyLong(), anyLong(), anyString());

			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"member@example.com\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceMembershipService).should().inviteWorkspaceMember(3L, 1L, "member@example.com");
		}

		@Test
		@DisplayName("이메일 형식이 잘못되면 400을 반환한다")
		void inviteWorkspaceMemberWithInvalidEmail() throws Exception {
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members/invite", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"invalid-email\"}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 권한 수정")
	class UpdateWorkspaceMemberRole {

		@Test
		@DisplayName("멤버 권한을 수정한다")
		void updateWorkspaceMemberRole() throws Exception {
			willDoNothing().given(workspaceMembershipService)
				.updateWorkspaceMemberRole(anyLong(), anyLong(), anyLong(), any(WorkspaceMemberRole.class));

			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}/role", 3L, 11L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"role\":\"MANAGER\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceMembershipService).should().updateWorkspaceMemberRole(3L, 1L, 11L, WorkspaceMemberRole.MANAGER);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 제거")
	class RemoveWorkspaceMember {

		@Test
		@DisplayName("워크스페이스 멤버를 제거한다")
		void removeWorkspaceMember() throws Exception {
			willDoNothing().given(workspaceMembershipService).removeWorkspaceMember(anyLong(), anyLong(), anyLong());

			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/{workspaceMemberId}", 3L, 11L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceMembershipService).should().removeWorkspaceMember(3L, 1L, 11L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탈퇴")
	class LeaveWorkspace {

		@Test
		@DisplayName("현재 워크스페이스에서 탈퇴한다")
		void leaveWorkspace() throws Exception {
			willDoNothing().given(workspaceMembershipService).leaveWorkspace(anyLong(), anyLong());

			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceMembershipService).should().leaveWorkspace(3L, 1L);
		}
	}
}
