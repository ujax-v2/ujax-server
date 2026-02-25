package com.ujax.infrastructure.web.api.user;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.user.UserService;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.domain.user.AuthProvider;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.user.UserController;
import com.ujax.infrastructure.web.user.dto.request.ProfileImageUploadRequest;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
@Import(TestSecurityConfig.class)
class UserControllerDocsTest {

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

	@Test
	@DisplayName("내 정보 조회 API")
	void getMe() throws Exception {
		// given
		UserResponse response = new UserResponse(1L, "test@example.com", "테스트유저",
			"https://example.com/profile.jpg", AuthProvider.GOOGLE, null);
		given(userService.getUser(anyLong())).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.name").value("테스트유저"))
			.andExpect(jsonPath("$.data.provider").value("GOOGLE"))
			.andDo(document("user-get-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("내 정보 조회")
					.description("로그인한 사용자의 정보를 조회합니다")
					.responseSchema(Schema.schema("ApiResponse-UserResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("유저 ID"),
						fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
						fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
						fieldWithPath("data.provider").type(JsonFieldType.STRING).description("인증 제공자 (GOOGLE, KAKAO, LOCAL)"),
						fieldWithPath("data.baekjoonId").type(JsonFieldType.STRING).description("백준 아이디").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("프로필 이미지 Presigned URL 생성 API")
	void createProfileImagePresignedUrl() throws Exception {
		// given
		ProfileImageUploadRequest request = new ProfileImageUploadRequest("image/png", 1048576L);
		PresignedUrlResponse response = new PresignedUrlResponse(
			"https://ujax-profile-images.s3.ap-northeast-2.amazonaws.com/presigned?X-Amz-Algorithm=...",
			"https://ujax-profile-images.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png"
		);
		given(userService.createProfileImagePresignedUrl(anyLong(), any(ProfileImageUploadRequest.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.presignedUrl").isNotEmpty())
			.andExpect(jsonPath("$.data.imageUrl").isNotEmpty())
			.andDo(document("user-create-profile-image-presigned-url",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("프로필 이미지 업로드 Presigned URL 생성")
					.description("S3에 프로필 이미지를 업로드하기 위한 Presigned URL을 생성합니다. JPEG, PNG, WEBP만 허용되며 최대 5MB입니다.")
					.requestSchema(Schema.schema("ProfileImageUploadRequest"))
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
	@DisplayName("내 정보 수정 API")
	void updateMe() throws Exception {
		// given
		UserUpdateRequest request = new UserUpdateRequest("수정된이름", "https://example.com/new-profile.jpg", null);
		UserResponse response = new UserResponse(1L, "test@example.com", "수정된이름",
			"https://example.com/new-profile.jpg", AuthProvider.GOOGLE, null);
		given(userService.updateUser(anyLong(), any(UserUpdateRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.name").value("수정된이름"))
			.andDo(document("user-update-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("내 정보 수정")
					.description("로그인한 사용자의 정보를 수정합니다")
					.requestSchema(Schema.schema("UserUpdateRequest"))
					.responseSchema(Schema.schema("ApiResponse-UserResponse"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("수정할 이름 (30자 이내)").optional(),
						fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("수정할 프로필 이미지 URL").optional(),
						fieldWithPath("baekjoonId").type(JsonFieldType.STRING).description("백준 아이디").optional()
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("유저 ID"),
						fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
						fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
						fieldWithPath("data.provider").type(JsonFieldType.STRING).description("인증 제공자 (GOOGLE, KAKAO, LOCAL)"),
						fieldWithPath("data.baekjoonId").type(JsonFieldType.STRING).description("백준 아이디").optional(),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("회원 탈퇴 API")
	void deleteMe() throws Exception {
		// given
		willDoNothing().given(userService).deleteUser(anyLong());

		// when & then
		mockMvc.perform(delete("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andDo(document("user-delete-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("회원 탈퇴")
					.description("로그인한 사용자의 계정을 삭제합니다")
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

}
