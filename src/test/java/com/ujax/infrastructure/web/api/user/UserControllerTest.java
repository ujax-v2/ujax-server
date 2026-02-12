package com.ujax.infrastructure.web.api.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
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
import com.ujax.application.user.UserService;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.domain.user.AuthProvider;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.user.UserController;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

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
	@DisplayName("내 정보 수정")
	class UpdateMe {

		@Test
		@DisplayName("일부 필드만 보내도 정상 수정된다")
		void updateMe_PartialUpdate() throws Exception {
			// given
			UserUpdateRequest request = new UserUpdateRequest("새이름", null);
			UserResponse response = new UserResponse(1L, "test@example.com", "새이름",
				"https://example.com/profile.jpg", AuthProvider.GOOGLE);
			given(userService.updateUser(anyLong(), anyString(), isNull())).willReturn(response);

			// when & then
			mockMvc.perform(patch("/api/v1/users/me")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.name").value("새이름"));
		}
	}
}
