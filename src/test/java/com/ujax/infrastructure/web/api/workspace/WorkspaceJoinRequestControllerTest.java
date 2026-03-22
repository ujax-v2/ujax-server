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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ujax.application.workspace.WorkspaceJoinRequestService;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatus;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceJoinRequestController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceJoinRequestController.class)
@Import(TestSecurityConfig.class)
class WorkspaceJoinRequestControllerTest {

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
				LocalDateTime.of(2026, 2, 24, 10, 0)
			);
			given(workspaceJoinRequestService.createJoinRequest(anyLong(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.requestId").value(10))
				.andExpect(jsonPath("$.data.workspaceId").value(3));
		}
	}

	@Nested
	@DisplayName("가입 신청 수락")
	class ApproveJoinRequest {

		@Test
		@DisplayName("가입 신청을 수락한다")
		void approveJoinRequest() throws Exception {
			// given
			willDoNothing().given(workspaceJoinRequestService).approveJoinRequest(anyLong(), anyLong(), anyLong());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/approve", 3L, 10L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceJoinRequestService).should().approveJoinRequest(3L, 1L, 10L);
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
			given(workspaceJoinRequestService.getMyJoinRequestStatus(anyLong(), anyLong())).willReturn(response);

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
	@DisplayName("가입 신청 취소")
	class CancelJoinRequest {

		@Test
		@DisplayName("가입 신청을 취소한다")
		void cancelJoinRequest() throws Exception {
			// given
			willDoNothing().given(workspaceJoinRequestService).cancelJoinRequest(anyLong(), anyLong());

			// when & then
			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/join-requests/me", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceJoinRequestService).should().cancelJoinRequest(3L, 1L);
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
				LocalDateTime.of(2026, 2, 24, 10, 0)
			);
			PageResponse<WorkspaceJoinRequestListItemResponse> response = PageResponse.of(
				List.of(item),
				0,
				20,
				1L,
				1
			);
			given(workspaceJoinRequestService.listJoinRequests(anyLong(), anyLong(), anyInt(), anyInt())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/join-requests", 3L)
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].requestId").value(10))
				.andExpect(jsonPath("$.data.content[0].applicantUserId").value(21));

			then(workspaceJoinRequestService).should().listJoinRequests(3L, 1L, 0, 20);
		}
	}

	@Nested
	@DisplayName("가입 신청 거부")
	class RejectJoinRequest {

		@Test
		@DisplayName("가입 신청을 거부한다")
		void rejectJoinRequest() throws Exception {
			// given
			willDoNothing().given(workspaceJoinRequestService).rejectJoinRequest(anyLong(), anyLong(), anyLong());

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests/{requestId}/reject", 3L, 10L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceJoinRequestService).should().rejectJoinRequest(3L, 1L, 10L);
		}
	}
}
