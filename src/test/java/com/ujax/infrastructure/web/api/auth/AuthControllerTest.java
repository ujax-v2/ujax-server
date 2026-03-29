package com.ujax.infrastructure.web.api.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.auth.AuthService;
import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.infrastructure.web.auth.AuthController;
import com.ujax.infrastructure.web.auth.dto.request.EmailAvailabilityRequest;
import com.ujax.infrastructure.web.auth.dto.request.LoginRequest;
import com.ujax.infrastructure.web.auth.dto.request.RefreshRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupRequest;
import com.ujax.support.TestSecurityConfig;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Nested
	@DisplayName("이메일 중복 확인")
	class EmailAvailability {

		@Test
		@DisplayName("사용 가능 여부를 조회한다")
		void checkEmailAvailability() throws Exception {
			EmailAvailabilityRequest request = new EmailAvailabilityRequest("test@example.com");

			mockMvc.perform(post("/api/v1/auth/email-availability")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").doesNotExist());
		}

		@Test
		@DisplayName("이메일 형식이 올바르지 않으면 오류가 발생한다")
		void checkEmailAvailability_InvalidEmail() throws Exception {
			EmailAvailabilityRequest request = new EmailAvailabilityRequest("invalid-email");

			mockMvc.perform(post("/api/v1/auth/email-availability")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("C009"));
		}

		@Test
		@DisplayName("이미 가입된 이메일이면 중복 오류가 발생한다")
		void checkEmailAvailability_DuplicateEmail() throws Exception {
			willThrow(new ConflictException(ErrorCode.DUPLICATE_EMAIL))
				.given(authService)
				.checkEmailAvailability("existing@example.com");

			mockMvc.perform(post("/api/v1/auth/email-availability")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new EmailAvailabilityRequest("existing@example.com"))))
				.andDo(print())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("D002"));
		}
	}

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("이메일이 비어있으면 오류가 발생한다")
		void signup_BlankEmail() throws Exception {
			// given
			SignupRequest request = new SignupRequest("", "password123", "이름");

			// when & then
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("비밀번호가 비어있으면 오류가 발생한다")
		void signup_BlankPassword() throws Exception {
			// given
			SignupRequest request = new SignupRequest("test@example.com", "", "이름");

			// when & then
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("이메일 형식이 올바르지 않으면 오류가 발생한다")
		void signup_InvalidEmail() throws Exception {
			// given
			SignupRequest request = new SignupRequest("invalid-email", "password123", "이름");

			// when & then
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("C009"));
		}

		@Test
		@DisplayName("이름이 비어있으면 오류가 발생한다")
		void signup_BlankName() throws Exception {
			// given
			SignupRequest request = new SignupRequest("test@example.com", "password123", "");

			// when & then
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("로그인")
	class Login {

		@Test
		@DisplayName("이메일이 비어있으면 오류가 발생한다")
		void login_BlankEmail() throws Exception {
			// given
			LoginRequest request = new LoginRequest("", "password123");

			// when & then
			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("비밀번호가 비어있으면 오류가 발생한다")
		void login_BlankPassword() throws Exception {
			// given
			LoginRequest request = new LoginRequest("test@example.com", "");

			// when & then
			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Test
	@DisplayName("리프레시 토큰이 비어있으면 오류가 발생한다")
	void refresh_BlankToken() throws Exception {
		// given
		RefreshRequest request = new RefreshRequest("");

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}
}
