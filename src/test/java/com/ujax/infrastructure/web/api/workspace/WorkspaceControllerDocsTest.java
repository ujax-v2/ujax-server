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
import com.ujax.application.workspace.WorkspaceDashboardService;
import com.ujax.application.workspace.WorkspaceService;
import com.ujax.application.workspace.dto.response.dashboard.DashboardDeadlineProblemResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardDeadlineRateRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardHotProblemResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardNoticeResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardRankingsResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardSolvedRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardStreakRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardSummaryResponse;
import com.ujax.application.workspace.dto.response.dashboard.WorkspaceDashboardResponse;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceController;
import com.ujax.infrastructure.web.workspace.dto.request.CreateWorkspaceRequest;
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

	@MockitoBean
	private WorkspaceDashboardService workspaceDashboardService;

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
		WorkspaceResponse workspace = new WorkspaceResponse(
			1L,
			"워크스페이스",
			"소개",
			"https://image.example.com/workspaces/1.png"
		);
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
						fieldWithPath("data.content[].imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
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
		WorkspaceResponse workspace = new WorkspaceResponse(
			1L,
			"워크스페이스",
			"소개",
			"https://image.example.com/workspaces/1.png"
		);
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
						fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
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
		WorkspaceResponse response = new WorkspaceResponse(
			1L,
			"워크스페이스",
			"소개",
			"https://image.example.com/workspaces/1.png"
		);
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
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
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
	@DisplayName("워크스페이스 대시보드 조회 API")
	void getWorkspaceDashboard() throws Exception {
		WorkspaceDashboardResponse response = new WorkspaceDashboardResponse(
			List.of(new DashboardNoticeResponse(
				10L,
				"운영 공지",
				"Alice",
				LocalDateTime.parse("2026-03-23T10:00:00"),
				true
			)),
			List.of(new DashboardDeadlineProblemResponse(
				20L,
				30L,
				"메인 문제집",
				1697,
				"숨바꼭질",
				"Silver 1",
				List.of("BFS", "Graph"),
				LocalDateTime.parse("2026-03-24T23:59:00")
			)),
			new DashboardSummaryResponse(
				5L,
				new DashboardHotProblemResponse(
					20L,
					30L,
					"메인 문제집",
					1697,
					"숨바꼭질",
					"Silver 1",
					List.of("BFS", "Graph"),
					4L
				)
			),
			new DashboardRankingsResponse(
				List.of(new DashboardSolvedRankingResponse(100L, "Alice", "https://image.example.com/alice.png", 2L)),
				List.of(new DashboardStreakRankingResponse(100L, "Alice", "https://image.example.com/alice.png", 3)),
				List.of(new DashboardDeadlineRateRankingResponse(
					100L,
					"Alice",
					"https://image.example.com/alice.png",
					2L,
					2L,
					100
				))
			)
		);
		given(workspaceDashboardService.getDashboard(anyLong(), anyLong())).willReturn(response);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/dashboard", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("workspace-dashboard",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspace")
					.summary("워크스페이스 대시보드 조회")
					.description("최근 공지, 임박 문제, 요약 통계, 랭킹 정보를 한 번에 조회합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceDashboardResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.recentNotices").type(JsonFieldType.ARRAY).description("최근 공지사항 목록"),
						fieldWithPath("data.recentNotices[].boardId").type(JsonFieldType.NUMBER).description("게시글 ID"),
						fieldWithPath("data.recentNotices[].title").type(JsonFieldType.STRING).description("공지 제목"),
						fieldWithPath("data.recentNotices[].authorNickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
						fieldWithPath("data.recentNotices[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
						fieldWithPath("data.recentNotices[].pinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
						fieldWithPath("data.upcomingDeadlines").type(JsonFieldType.ARRAY).description("기한 임박 문제 목록"),
						fieldWithPath("data.upcomingDeadlines[].workspaceProblemId").type(JsonFieldType.NUMBER).description("워크스페이스 문제 ID"),
						fieldWithPath("data.upcomingDeadlines[].problemBoxId").type(JsonFieldType.NUMBER).description("문제집 ID"),
						fieldWithPath("data.upcomingDeadlines[].problemBoxTitle").type(JsonFieldType.STRING).description("문제집 제목"),
						fieldWithPath("data.upcomingDeadlines[].problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.upcomingDeadlines[].title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("data.upcomingDeadlines[].tier").type(JsonFieldType.STRING).description("문제 티어").optional(),
						fieldWithPath("data.upcomingDeadlines[].algorithmTags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록"),
						fieldWithPath("data.upcomingDeadlines[].deadline").type(JsonFieldType.STRING).description("문제 마감 시각"),
						fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("요약 통계"),
						fieldWithPath("data.summary.weeklySubmissionCount").type(JsonFieldType.NUMBER).description("이번 주 제출 수"),
						fieldWithPath("data.summary.hotProblem").type(JsonFieldType.OBJECT).description("이번 주 가장 많이 제출된 문제").optional(),
						fieldWithPath("data.summary.hotProblem.workspaceProblemId").type(JsonFieldType.NUMBER).description("워크스페이스 문제 ID").optional(),
						fieldWithPath("data.summary.hotProblem.problemBoxId").type(JsonFieldType.NUMBER).description("문제집 ID").optional(),
						fieldWithPath("data.summary.hotProblem.problemBoxTitle").type(JsonFieldType.STRING).description("문제집 제목").optional(),
						fieldWithPath("data.summary.hotProblem.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호").optional(),
						fieldWithPath("data.summary.hotProblem.title").type(JsonFieldType.STRING).description("문제 제목").optional(),
						fieldWithPath("data.summary.hotProblem.tier").type(JsonFieldType.STRING).description("문제 티어").optional(),
						fieldWithPath("data.summary.hotProblem.algorithmTags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록").optional(),
						fieldWithPath("data.summary.hotProblem.weeklySubmissionCount").type(JsonFieldType.NUMBER).description("해당 문제의 이번 주 제출 수").optional(),
						fieldWithPath("data.rankings").type(JsonFieldType.OBJECT).description("랭킹 정보"),
						fieldWithPath("data.rankings.monthlySolved").type(JsonFieldType.ARRAY).description("이번 달 해결 수 랭킹"),
						fieldWithPath("data.rankings.monthlySolved[].workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.rankings.monthlySolved[].nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.rankings.monthlySolved[].userImage").type(JsonFieldType.STRING).description("사용자 프로필 이미지 URL"),
						fieldWithPath("data.rankings.monthlySolved[].solvedCount").type(JsonFieldType.NUMBER).description("이번 달 해결한 문제 수"),
						fieldWithPath("data.rankings.streak").type(JsonFieldType.ARRAY).description("연속 출석 랭킹"),
						fieldWithPath("data.rankings.streak[].workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.rankings.streak[].nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.rankings.streak[].userImage").type(JsonFieldType.STRING).description("사용자 프로필 이미지 URL"),
						fieldWithPath("data.rankings.streak[].streakDays").type(JsonFieldType.NUMBER).description("연속 활동 일수"),
						fieldWithPath("data.rankings.deadlineRate").type(JsonFieldType.ARRAY).description("기한 준수율 랭킹"),
						fieldWithPath("data.rankings.deadlineRate[].workspaceMemberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 ID"),
						fieldWithPath("data.rankings.deadlineRate[].nickname").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data.rankings.deadlineRate[].userImage").type(JsonFieldType.STRING).description("사용자 프로필 이미지 URL"),
						fieldWithPath("data.rankings.deadlineRate[].solvedBeforeDeadlineCount").type(JsonFieldType.NUMBER).description("기한 내 해결한 문제 수"),
						fieldWithPath("data.rankings.deadlineRate[].totalDeadlineProblems").type(JsonFieldType.NUMBER).description("집계 대상 마감 문제 수"),
						fieldWithPath("data.rankings.deadlineRate[].ratePercent").type(JsonFieldType.NUMBER).description("기한 준수율(%)"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("워크스페이스 설정 조회 API")
	void getWorkspaceSettings() throws Exception {
		// given
		WorkspaceSettingsResponse response = new WorkspaceSettingsResponse(
			1L,
			"워크스페이스",
			"소개",
			"https://image.example.com/workspaces/1.png",
			"https://meeting.ssafy.com/hooks/j8ki3j*************e9ak9jh"
		);
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
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
						fieldWithPath("data.hookUrl").type(JsonFieldType.STRING).description("마스킹된 Hook URL").optional(),
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
		WorkspaceResponse response = new WorkspaceResponse(
			1L,
			"워크스페이스",
			"소개",
			"https://image.example.com/workspaces/1.png"
		);
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
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
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
		WorkspaceResponse response = new WorkspaceResponse(
			1L,
			"새 이름",
			"새 소개",
			"https://new-image.com/workspace.png"
		);
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
							fieldWithPath("hookUrl").type(JsonFieldType.STRING).description("Hook URL").optional(),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL").optional()
					)
					.responseSchema(Schema.schema("ApiResponse-WorkspaceResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명").optional(),
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("워크스페이스 이미지 URL"),
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
							fieldWithPath("hookUrl").type(JsonFieldType.STRING).description("Hook URL").optional(),
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
