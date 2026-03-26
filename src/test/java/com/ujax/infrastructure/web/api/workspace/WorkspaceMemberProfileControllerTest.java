package com.ujax.infrastructure.web.api.workspace;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ujax.application.workspace.WorkspaceMemberProfileService;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileAccuracyResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityDayResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileAlgorithmStatResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileLanguageStatResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileMemberResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileSummaryResponse;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceMemberProfileController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceMemberProfileController.class)
@Import(TestSecurityConfig.class)
class WorkspaceMemberProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WorkspaceMemberProfileService workspaceMemberProfileService;

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
	@DisplayName("내 워크스페이스 멤버 프로필 대시보드를 조회한다")
	void getMyWorkspaceMemberProfile() throws Exception {
		WorkspaceMemberProfileResponse response = WorkspaceMemberProfileResponse.of(
			new WorkspaceMemberProfileMemberResponse(
				11L,
				"스터디닉",
				"test@example.com",
				"https://example.com/profile.jpg",
				"gwong",
				WorkspaceMemberRole.MEMBER,
				LocalDate.of(2026, 1, 12)
			),
			WorkspaceMemberProfileSummaryResponse.of(10L, 3, "Java", "DP"),
			WorkspaceMemberProfileAccuracyResponse.of(8L, 10L),
			List.of(WorkspaceMemberProfileAlgorithmStatResponse.of("DP", 4L, 10L)),
			List.of(WorkspaceMemberProfileLanguageStatResponse.of("Java", 7L, 10L))
		);
		given(workspaceMemberProfileService.getMyProfile(anyLong(), anyLong())).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me/profile", 3L))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.member.workspaceMemberId").value(11))
			.andExpect(jsonPath("$.data.member.nickname").value("스터디닉"))
			.andExpect(jsonPath("$.data.summary.mainLanguage").value("Java"))
			.andExpect(jsonPath("$.data.accuracy.rate").value(80));

		then(workspaceMemberProfileService).should().getMyProfile(3L, 1L);
	}

	@Test
	@DisplayName("내 워크스페이스 멤버 활동 기록을 조회한다")
	void getMyWorkspaceMemberProfileActivity() throws Exception {
		WorkspaceMemberProfileActivityResponse response = WorkspaceMemberProfileActivityResponse.of(
			"YEAR",
			2026,
			LocalDate.of(2026, 1, 1),
			LocalDate.of(2026, 12, 31),
			List.of(new WorkspaceMemberProfileActivityDayResponse(LocalDate.of(2026, 3, 26), 2L))
		);
		given(workspaceMemberProfileService.getMyProfileActivity(anyLong(), anyLong(), eq(2026))).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me/profile/activity", 3L)
				.param("year", "2026"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.mode").value("YEAR"))
			.andExpect(jsonPath("$.data.days[0].count").value(2));

		then(workspaceMemberProfileService).should().getMyProfileActivity(3L, 1L, 2026);
	}
}
