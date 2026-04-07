package com.ujax.infrastructure.web.api.problem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.ujax.application.problem.WorkspaceProblemService;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.WorkspaceProblemController;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceProblemController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceProblemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WorkspaceProblemService workspaceProblemService;

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
	@DisplayName("problemId가 null이면 400 오류가 발생한다")
	void createWithNullProblemId() throws Exception {
		// given
		String body = objectMapper.writeValueAsString(
			new java.util.LinkedHashMap<>() {{
				put("deadline", "2026-03-01T00:00:00");
			}});

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/1/problem-boxes/1/problems")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("page 또는 size가 잘못되면 400 오류가 발생한다")
	void listWithInvalidPageable() throws Exception {
		mockMvc.perform(get("/api/v1/workspaces/1/problem-boxes/1/problems")
				.param("page", "-1")
				.param("size", "0")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}
}
