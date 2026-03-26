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

import java.time.LocalDate;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
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

@Tag("restDocs")
@WebMvcTest(WorkspaceMemberProfileController.class)
@AutoConfigureRestDocs
@Import(TestSecurityConfig.class)
class WorkspaceMemberProfileControllerDocsTest {

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
	@DisplayName("워크스페이스 멤버 프로필 대시보드 조회 API")
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
			WorkspaceMemberProfileSummaryResponse.of(143L, 12, "Java", "DP"),
			WorkspaceMemberProfileAccuracyResponse.of(143L, 183L),
			List.of(
				WorkspaceMemberProfileAlgorithmStatResponse.of("DP", 40L, 143L),
				WorkspaceMemberProfileAlgorithmStatResponse.of("BFS/DFS", 31L, 143L)
			),
			List.of(
				WorkspaceMemberProfileLanguageStatResponse.of("Java", 120L, 183L),
				WorkspaceMemberProfileLanguageStatResponse.of("Python", 40L, 183L)
			)
		);
		given(workspaceMemberProfileService.getMyProfile(anyLong(), anyLong())).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me/profile", 1)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-profile-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("내 워크스페이스 멤버 프로필 대시보드 조회")
					.description("현재 워크스페이스에서 로그인한 사용자의 멤버 프로필 통계를 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMemberProfileResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("워크스페이스 멤버 정보"),
						fieldWithPath("data.member.workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.member.nickname").type(JsonFieldType.STRING).description("워크스페이스 닉네임"),
						fieldWithPath("data.member.email").type(JsonFieldType.STRING).description("사용자 이메일"),
						fieldWithPath("data.member.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
						fieldWithPath("data.member.baekjoonId").type(JsonFieldType.STRING).description("백준 아이디").optional(),
						fieldWithPath("data.member.role").type(JsonFieldType.STRING).description("워크스페이스 권한"),
						fieldWithPath("data.member.joinedAt").type(JsonFieldType.STRING).description("워크스페이스 가입일"),
						fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("요약 정보"),
						fieldWithPath("data.summary.solvedCount").type(JsonFieldType.NUMBER).description("해결한 문제 수"),
						fieldWithPath("data.summary.streakDays").type(JsonFieldType.NUMBER).description("현재 스트릭 일수"),
						fieldWithPath("data.summary.mainLanguage").type(JsonFieldType.STRING).description("주력 언어").optional(),
						fieldWithPath("data.summary.mainAlgorithm").type(JsonFieldType.STRING).description("주력 알고리즘").optional(),
						fieldWithPath("data.accuracy").type(JsonFieldType.OBJECT).description("정답률 정보"),
						fieldWithPath("data.accuracy.rate").type(JsonFieldType.NUMBER).description("정답률(%)"),
						fieldWithPath("data.accuracy.acceptedCount").type(JsonFieldType.NUMBER).description("정답 제출 수"),
						fieldWithPath("data.accuracy.totalCount").type(JsonFieldType.NUMBER).description("전체 제출 수"),
						fieldWithPath("data.algorithmStats").type(JsonFieldType.ARRAY).description("알고리즘 통계"),
						fieldWithPath("data.algorithmStats[].name").type(JsonFieldType.STRING).description("알고리즘 이름"),
						fieldWithPath("data.algorithmStats[].count").type(JsonFieldType.NUMBER).description("알고리즘별 해결 수"),
						fieldWithPath("data.algorithmStats[].ratio").type(JsonFieldType.NUMBER).description("알고리즘 비율"),
						fieldWithPath("data.languageStats").type(JsonFieldType.ARRAY).description("언어 통계"),
						fieldWithPath("data.languageStats[].name").type(JsonFieldType.STRING).description("언어 이름"),
						fieldWithPath("data.languageStats[].count").type(JsonFieldType.NUMBER).description("언어별 제출 수"),
						fieldWithPath("data.languageStats[].ratio").type(JsonFieldType.NUMBER).description("언어 비율"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 멤버 활동 기록 조회 API")
	void getMyWorkspaceMemberProfileActivity() throws Exception {
		WorkspaceMemberProfileActivityResponse response = WorkspaceMemberProfileActivityResponse.of(
			"YEAR",
			2026,
			LocalDate.of(2026, 1, 1),
			LocalDate.of(2026, 12, 31),
			List.of(
				new WorkspaceMemberProfileActivityDayResponse(LocalDate.of(2026, 3, 26), 3L),
				new WorkspaceMemberProfileActivityDayResponse(LocalDate.of(2026, 3, 25), 1L)
			)
		);
		given(workspaceMemberProfileService.getMyProfileActivity(anyLong(), anyLong(), eq(2026))).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members/me/profile/activity", 1)
				.param("year", "2026")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-member-profile-activity-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("내 워크스페이스 멤버 활동 기록 조회")
					.description("현재 워크스페이스에서 로그인한 사용자의 일자별 문제 해결 활동 기록을 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.queryParameters(
						parameterWithName("year").optional().description("조회 연도. 없으면 최근 365일을 반환")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceMemberProfileActivityResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.mode").type(JsonFieldType.STRING).description("조회 모드 (RECENT 또는 YEAR)"),
						fieldWithPath("data.year").type(JsonFieldType.NUMBER).description("조회 연도").optional(),
						fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("조회 시작일"),
						fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("조회 종료일"),
						fieldWithPath("data.days").type(JsonFieldType.ARRAY).description("활동이 있었던 날짜 목록"),
						fieldWithPath("data.days[].date").type(JsonFieldType.STRING).description("활동 날짜"),
						fieldWithPath("data.days[].count").type(JsonFieldType.NUMBER).description("해당 날짜 해결 수"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}
}
