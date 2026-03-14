package com.ujax.infrastructure.web.api.solution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.ujax.application.solution.SolutionService;
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
}
