package com.ujax.infrastructure.web.api.problem;

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
import com.ujax.application.problem.ProblemBoxService;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.ProblemBoxController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(ProblemBoxController.class)
@Import(TestSecurityConfig.class)
class ProblemBoxControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProblemBoxService problemBoxService;

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
	@DisplayName("POST /problem-boxes: 문제집 생성")
	class CreateProblemBox {

		@Test
		@DisplayName("제목이 비어있으면 400 오류가 발생한다")
		void createWithBlankTitle() throws Exception {
			// given
			String body = objectMapper.writeValueAsString(
				new java.util.LinkedHashMap<>() {{
					put("title", "");
					put("description", "설명");
				}});

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/1/problem-boxes")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("제목이 null이면 400 오류가 발생한다")
		void createWithNullTitle() throws Exception {
			// given
			String body = objectMapper.writeValueAsString(
				new java.util.LinkedHashMap<>() {{
					put("description", "설명");
				}});

			// when & then
			mockMvc.perform(post("/api/v1/workspaces/1/problem-boxes")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("PATCH /problem-boxes/{id}: 문제집 수정")
	class UpdateProblemBox {

		@Test
		@DisplayName("제목이 비어있으면 400 오류가 발생한다")
		void updateWithBlankTitle() throws Exception {
			// given
			String body = objectMapper.writeValueAsString(
				new java.util.LinkedHashMap<>() {{
					put("title", "");
					put("description", "설명");
				}});

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/1/problem-boxes/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("제목이 null이면 400 오류가 발생한다")
		void updateWithNullTitle() throws Exception {
			// given
			String body = objectMapper.writeValueAsString(
				new java.util.LinkedHashMap<>() {{
					put("description", "설명");
				}});

			// when & then
			mockMvc.perform(patch("/api/v1/workspaces/1/problem-boxes/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}
}
