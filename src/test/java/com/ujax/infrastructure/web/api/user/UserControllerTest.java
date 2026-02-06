package com.ujax.infrastructure.web.api.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.user.UserService;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.infrastructure.web.user.UserController;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

/**
 * UserController 동작 검증 테스트
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@Nested
	@DisplayName("내 정보 수정")
	class UpdateMe {

		@Test
		@DisplayName("일부 필드만 보내도 정상 수정된다")
		void updateMe_PartialUpdate() throws Exception {
			// given
			UserUpdateRequest request = new UserUpdateRequest("새이름", null);
			User user = createTestUser(1L, "test@example.com", "새이름");
			given(userService.updateUser(anyLong(), anyString(), isNull())).willReturn(user);

			// when & then
			mockMvc.perform(patch("/api/v1/users/me")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("새이름"));
		}
	}

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
