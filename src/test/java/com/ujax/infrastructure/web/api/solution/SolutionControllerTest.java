package com.ujax.infrastructure.web.api.solution;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.solution.SolutionCommentService;
import com.ujax.application.solution.SolutionLikeService;
import com.ujax.application.solution.SolutionService;
import com.ujax.application.solution.dto.response.SolutionCommentResponse;
import com.ujax.application.solution.dto.response.SolutionLikeStatusResponse;
import com.ujax.application.solution.dto.response.SolutionMemberSummaryResponse;
import com.ujax.application.solution.dto.response.SolutionVersionResponse;
import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.domain.solution.SolutionStatus;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.solution.SolutionController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(SolutionController.class)
@Import(TestSecurityConfig.class)
class SolutionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SolutionService solutionService;

	@MockitoBean
	private SolutionLikeService solutionLikeService;

	@MockitoBean
	private SolutionCommentService solutionCommentService;

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
	@DisplayName("풀이 수집 유효성 검증")
	class IngestValidation {

		@Test
		@DisplayName("workspaceProblemId가 null이면 400 오류가 발생한다")
		void ingestWithNullWorkspaceProblemId() throws Exception {
			// given
			String body = """
				{
					"submissionId": 12345,
					"verdict": "맞았습니다!!"
				}
				""";

			// when & then
			mockMvc.perform(post("/api/v1/submissions/ingest")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("submissionId가 0 이하이면 400 오류가 발생한다")
		void ingestWithInvalidSubmissionId() throws Exception {
			// given
			String body = """
				{
					"workspaceProblemId": 1,
					"submissionId": 0,
					"verdict": "맞았습니다!!"
				}
				""";

			// when & then
			mockMvc.perform(post("/api/v1/submissions/ingest")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("verdict가 비어있으면 400 오류가 발생한다")
		void ingestWithBlankVerdict() throws Exception {
			// given
			String body = """
				{
					"workspaceProblemId": 1,
					"submissionId": 12345,
					"verdict": ""
				}
				""";

			// when & then
			mockMvc.perform(post("/api/v1/submissions/ingest")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Test
	@DisplayName("풀이 멤버 요약 목록을 조회한다")
	void getSolutionMembers() throws Exception {
		// given
		var response = List.of(
			new SolutionMemberSummaryResponse(
				11L,
				"pythonista",
				ProgrammingLanguage.PYTHON,
				SolutionStatus.ACCEPTED,
				2L,
				0L,
				LocalDateTime.of(2026, 3, 10, 10, 15)
			)
		);

		org.mockito.BDDMockito.given(solutionService.getSolutionMembers(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong()
		)).willReturn(response);

		// when & then
		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members",
				1, 1, 1
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].workspaceMemberId").value(11))
			.andExpect(jsonPath("$.data[0].memberName").value("pythonista"))
			.andExpect(jsonPath("$.data[0].programmingLanguage").value("PYTHON"))
			.andExpect(jsonPath("$.data[0].latestStatus").value("ACCEPTED"))
			.andExpect(jsonPath("$.data[0].submissionCount").value(2))
			.andExpect(jsonPath("$.data[0].likes").value(0));
	}

	@Test
	@DisplayName("풀이 버전 목록을 조회한다")
	void getSolutionVersions() throws Exception {
		var item = new SolutionVersionResponse(
			12005L,
			"print(sum(map(int, input().split())))",
			SolutionStatus.ACCEPTED,
			"28 ms",
			"31120 KB",
			ProgrammingLanguage.PYTHON,
			"34 B",
			LocalDateTime.of(2026, 3, 10, 10, 15),
			1L,
			true,
			0L
		);
		PageResponse<SolutionVersionResponse> response = PageResponse.of(List.of(item), 0, 1, 2L, 2);

		org.mockito.BDDMockito.given(solutionService.getSolutionVersions(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.anyInt()
		)).willReturn(response);

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions",
				1, 2, 3, 4
			).param("page", "0").param("size", "1"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].submissionId").value(item.submissionId()))
			.andExpect(jsonPath("$.data.content[0].programmingLanguage").value("PYTHON"))
			.andExpect(jsonPath("$.data.content[0].likes").value(1))
			.andExpect(jsonPath("$.data.content[0].isLiked").value(true))
			.andExpect(jsonPath("$.data.page.page").value(0))
			.andExpect(jsonPath("$.data.page.size").value(1));
	}

	@Test
	@DisplayName("댓글 목록을 조회한다")
	void getSolutionComments() throws Exception {
		var response = List.of(
			new SolutionCommentResponse(1L, "pythonista", "좋은 풀이네요", LocalDateTime.of(2026, 3, 10, 10, 20), true)
		);

		org.mockito.BDDMockito.given(solutionCommentService.getComments(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong()
		)).willReturn(response);

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].authorName").value("pythonista"))
			.andExpect(jsonPath("$.data[0].content").value("좋은 풀이네요"))
			.andExpect(jsonPath("$.data[0].isMyComment").value(true));
	}

	@Test
	@DisplayName("댓글을 생성한다")
	void createSolutionComment() throws Exception {
		org.mockito.BDDMockito.given(solutionCommentService.createComment(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyString()
		)).willReturn(new SolutionCommentResponse(1L, "pythonista", "댓글", LocalDateTime.of(2026, 3, 10, 10, 20), true));

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments",
				1, 2, 3, 4, 5
			).contentType(MediaType.APPLICATION_JSON).content("""
				{"content":"댓글"}
				"""))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.content").value("댓글"))
			.andExpect(jsonPath("$.data.isMyComment").value(true));
	}

	@Test
	@DisplayName("댓글을 삭제한다")
	void deleteSolutionComment() throws Exception {
		mockMvc.perform(delete(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments/{commentId}",
				1, 2, 3, 4, 5, 6
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("좋아요를 등록한다")
	void likeSolution() throws Exception {
		org.mockito.BDDMockito.given(solutionLikeService.like(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong()
		)).willReturn(SolutionLikeStatusResponse.of(1L, true));

		mockMvc.perform(put(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likes").value(1))
			.andExpect(jsonPath("$.data.isLiked").value(true));
	}

	@Test
	@DisplayName("좋아요를 취소한다")
	void unlikeSolution() throws Exception {
		org.mockito.BDDMockito.given(solutionLikeService.unlike(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.anyLong()
		)).willReturn(SolutionLikeStatusResponse.of(0L, false));

		mockMvc.perform(delete(
				"/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes",
				1, 2, 3, 4, 5
			))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likes").value(0))
			.andExpect(jsonPath("$.data.isLiked").value(false));
	}
}
