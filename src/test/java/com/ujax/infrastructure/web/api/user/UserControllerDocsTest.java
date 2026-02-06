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

import java.lang.reflect.Field;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.user.UserService;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.infrastructure.web.user.UserController;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

/**
 * UserController RestDocs 문서화 테스트
 */
@Tag("restDocs")
@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("내 정보 조회 API")
	void getMe() throws Exception {
		// given
		User user = createTestUser(1L, "test@example.com", "테스트유저");
		given(userService.getUser(anyLong())).willReturn(user);

		// when & then
		mockMvc.perform(get("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1L))
			.andExpect(jsonPath("$.email").value("test@example.com"))
			.andExpect(jsonPath("$.name").value("테스트유저"))
			.andExpect(jsonPath("$.provider").value("GOOGLE"))
			.andDo(document("user-get-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("내 정보 조회")
					.description("로그인한 사용자의 정보를 조회합니다")
					.responseSchema(Schema.schema("UserResponse"))
					.responseFields(
						fieldWithPath("id").type(JsonFieldType.NUMBER).description("유저 ID"),
						fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
						fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
						fieldWithPath("provider").type(JsonFieldType.STRING).description("인증 제공자 (GOOGLE, KAKAO, LOCAL)")
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("내 정보 수정 API")
	void updateMe() throws Exception {
		// given
		UserUpdateRequest request = new UserUpdateRequest("수정된이름", "https://example.com/new-profile.jpg");
		User updatedUser = createTestUser(1L, "test@example.com", "수정된이름");

		given(userService.updateUser(anyLong(), anyString(), anyString())).willReturn(updatedUser);

		// when & then
		mockMvc.perform(patch("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("수정된이름"))
			.andDo(document("user-update-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("내 정보 수정")
					.description("로그인한 사용자의 정보를 수정합니다")
					.requestSchema(Schema.schema("UserUpdateRequest"))
					.responseSchema(Schema.schema("UserResponse"))
					.requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("수정할 이름 (30자 이내)").optional(),
						fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("수정할 프로필 이미지 URL").optional()
					)
					.responseFields(
						fieldWithPath("id").type(JsonFieldType.NUMBER).description("유저 ID"),
						fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
						fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
						fieldWithPath("provider").type(JsonFieldType.STRING).description("인증 제공자 (GOOGLE, KAKAO, LOCAL)")
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
			.andExpect(status().isNoContent())
			.andDo(document("user-delete-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("User")
					.summary("회원 탈퇴")
					.description("로그인한 사용자의 계정을 삭제합니다")
					.build()
				)
			));
	}

	/**
	 * 테스트용 User 생성
	 */
	private User createTestUser(Long id, String email, String name) {
		User user = User.builder()
			.email(email)
			.name(name)
			.profileImageUrl("https://example.com/profile.jpg")
			.provider(AuthProvider.GOOGLE)
			.providerId("google-123")
			.build();

		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(user, id);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to set id", e);
		}

		return user;
	}
}
