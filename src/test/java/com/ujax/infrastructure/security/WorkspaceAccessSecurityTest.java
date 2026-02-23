package com.ujax.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceAccessSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("비회원은 워크스페이스 목록을 조회할 수 있다")
	void guestCanListWorkspaces() throws Exception {
		mockMvc.perform(get("/api/v1/workspaces/explore")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("탐색 API는 GET만 허용된다")
	void guestCannotCallExploreWithNonGetMethod() throws Exception {
		mockMvc.perform(post("/api/v1/workspaces/explore")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@DisplayName("비회원은 내 워크스페이스 목록을 조회할 수 없다")
	void guestCannotListMyWorkspaces() throws Exception {
		mockMvc.perform(get("/api/v1/workspaces/me")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("비회원은 워크스페이스 상세를 조회할 수 있다")
	void guestCanGetWorkspaceDetail() throws Exception {
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 999999L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("비회원의 워크스페이스 가입 요청은 인증 오류를 반환한다")
	void guestJoinRequestReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join-requests", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}
}
