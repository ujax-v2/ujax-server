package com.ujax.infrastructure.web.api.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.auth.AuthService;
import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.application.auth.dto.response.SignupStartResponse;
import com.ujax.infrastructure.web.auth.AuthController;
import com.ujax.infrastructure.web.auth.dto.request.EmailAvailabilityRequest;
import com.ujax.infrastructure.web.auth.dto.request.LoginRequest;
import com.ujax.infrastructure.web.auth.dto.request.RefreshRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupCompleteRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupStartRequest;
import com.ujax.support.TestSecurityConfig;

@Tag("restDocs")
@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
@Import(TestSecurityConfig.class)
class AuthControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Test
	@DisplayName("이메일 중복 확인 API")
	void emailAvailability() throws Exception {
		EmailAvailabilityRequest request = new EmailAvailabilityRequest("test@example.com");

		mockMvc.perform(post("/api/v1/auth/email-availability")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andDo(document("auth-email-availability",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("이메일 중복 확인")
					.description("회원가입 전에 이메일 형식과 중복 여부를 확인합니다")
					.requestSchema(Schema.schema("EmailAvailabilityRequest"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("확인할 이메일")
					)
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
	@DisplayName("회원가입 인증 요청 API")
	void signupRequest() throws Exception {
		SignupStartRequest request = new SignupStartRequest("test@example.com");
		SignupStartResponse response = new SignupStartResponse("request-token",
			java.time.LocalDateTime.parse("2026-03-30T10:30:00"));
		given(authService.requestSignup(anyString())).willReturn(response);

		mockMvc.perform(post("/api/v1/auth/signup/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andDo(document("auth-signup-request",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("회원가입 인증 요청")
					.description("이메일 인증 세션을 생성하고 이메일 인증 코드를 발송합니다")
					.requestSchema(Schema.schema("SignupStartRequest"))
					.responseSchema(Schema.schema("ApiResponse-SignupStartResponse"))
					.requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("이메일")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.requestToken").type(JsonFieldType.STRING).description("이메일 인증 세션 토큰"),
						fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("인증 코드 만료 시각"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("회원가입 완료 API")
	void signupComplete() throws Exception {
		SignupCompleteRequest request = new SignupCompleteRequest(
			"request-token",
			"123456",
			"test@example.com",
			"password123",
			"테스트유저"
		);
		AuthTokenResponse response = new AuthTokenResponse("access.token.here", "refresh.token.here");
		given(authService.completeSignup(anyString(), anyString(), anyString(), anyString(), anyString())).willReturn(response);

		mockMvc.perform(post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andDo(document("auth-signup-complete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("회원가입 완료")
					.description("이메일 인증 코드가 일치하면 실제 회원가입을 완료합니다")
					.requestSchema(Schema.schema("SignupCompleteRequest"))
					.responseSchema(Schema.schema("ApiResponse-AuthTokenResponse"))
					.requestFields(
						fieldWithPath("requestToken").type(JsonFieldType.STRING).description("회원가입 요청 토큰"),
						fieldWithPath("code").type(JsonFieldType.STRING).description("인증 코드"),
						fieldWithPath("email").type(JsonFieldType.STRING).description("회원가입 이메일"),
						fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
						fieldWithPath("name").type(JsonFieldType.STRING).description("이름")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
						fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("로그인 API")
	void login() throws Exception {
		// given
		LoginRequest request = new LoginRequest("test@example.com", "password123");
		AuthTokenResponse response = new AuthTokenResponse("access.token.here", "refresh.token.here");
		given(authService.login(anyString(), anyString())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andDo(document("auth-login",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("로그인")
					.description("이메일/비밀번호로 로그인합니다")
					.requestSchema(Schema.schema("LoginRequest"))
					.responseSchema(Schema.schema("ApiResponse-AuthTokenResponse"))
					.requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
						fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("토큰 갱신 API")
	void refresh() throws Exception {
		// given
		RefreshRequest request = new RefreshRequest("refresh.token.here");
		AuthTokenResponse response = new AuthTokenResponse("new.access.token", "new.refresh.token");
		given(authService.refresh(anyString())).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andDo(document("auth-refresh",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("토큰 갱신")
					.description("리프레시 토큰으로 새 토큰을 발급받습니다")
					.requestSchema(Schema.schema("RefreshRequest"))
					.responseSchema(Schema.schema("ApiResponse-AuthTokenResponse"))
					.requestFields(
						fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
						fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 리프레시 토큰"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("로그아웃 API")
	void logout() throws Exception {
		// given
		RefreshRequest request = new RefreshRequest("refresh.token.here");
		willDoNothing().given(authService).logout(anyString());

		// when & then
		mockMvc.perform(post("/api/v1/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andDo(document("auth-logout",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Auth")
					.summary("로그아웃")
					.description("리프레시 토큰을 해지합니다")
					.requestSchema(Schema.schema("RefreshRequest"))
					.responseSchema(Schema.schema("ApiResponse-Void"))
					.requestFields(
						fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}
}
