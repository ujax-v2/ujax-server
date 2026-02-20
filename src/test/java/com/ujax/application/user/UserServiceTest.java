package com.ujax.application.user;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.common.BusinessRuleViolationException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@BeforeEach
	void tearDown() {
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("유저 조회")
	class GetUser {

		@Test
		@DisplayName("존재하는 유저를 조회한다")
		void getUser_Success() {
			// given
			User user = userRepository.save(User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://example.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			));

			// when
			UserResponse response = userService.getUser(user.getId());

			// then
			assertThat(response).extracting("email", "name")
				.containsExactly("test@example.com", "테스트유저");
		}

		@Test
		@DisplayName("존재하지 않는 유저를 조회하면 오류가 발생한다")
		void getUser_NotFound() {
			// when & then
			assertThatThrownBy(() -> userService.getUser(999L))
				.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("유저 정보 수정")
	class UpdateUser {

		@Test
		@DisplayName("유저 정보를 수정한다")
		void updateUser_Success() {
			// given
			User user = userRepository.save(User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://example.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			));

			// when
			UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest("수정된이름", "https://new-image.com/profile.jpg", null));

			// then
			assertThat(response).extracting("name", "profileImageUrl")
				.containsExactly("수정된이름", "https://new-image.com/profile.jpg");
		}

		@Test
		@DisplayName("이름만 수정한다")
		void updateUser_OnlyName() {
			// given
			User user = userRepository.save(User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://example.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			));

			// when
			UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest("새이름", null, null));

			// then
			assertThat(response).extracting("name", "profileImageUrl")
				.containsExactly("새이름", "https://example.com/profile.jpg");
		}

		@Test
		@DisplayName("존재하지 않는 유저를 수정하면 오류가 발생한다")
		void updateUser_NotFound() {
			// when & then
			assertThatThrownBy(() -> userService.updateUser(999L, new UserUpdateRequest("새이름", null, null)))
				.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("유저 삭제")
	class DeleteUser {

		@Test
		@DisplayName("유저를 삭제한다")
		void deleteUser_Success() {
			// given
			User user = userRepository.save(User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				null,
				AuthProvider.GOOGLE,
				"google-123"
			));

			// when
			userService.deleteUser(user.getId());

			// then
			assertThatThrownBy(() -> userService.getUser(user.getId()))
				.isInstanceOf(NotFoundException.class);
		}

		@Test
		@DisplayName("존재하지 않는 유저를 삭제하면 오류가 발생한다")
		void deleteUser_NotFound() {
			// when & then
			assertThatThrownBy(() -> userService.deleteUser(999L))
				.isInstanceOf(NotFoundException.class);
		}

		@Test
		@DisplayName("워크스페이스 소유자인 유저는 탈퇴할 수 없다")
		void deleteUser_FailWhenWorkspaceOwner() {
			// given
			User user = userRepository.save(User.createOAuthUser(
				"owner@example.com",
				"소유자",
				null,
				AuthProvider.GOOGLE,
				"google-owner"
			));
			Workspace workspace = workspaceRepository.save(Workspace.create("테스트 워크스페이스", "설명"));
			workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));

			// when & then
			assertThatThrownBy(() -> userService.deleteUser(user.getId()))
				.isInstanceOf(BusinessRuleViolationException.class);
		}
	}
}
