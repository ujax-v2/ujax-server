package com.ujax.domain.user;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

/**
 * UserRepository 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("이메일로 유저 조회")
	class FindByEmail {

		@Test
		@DisplayName("존재하는 이메일로 유저를 조회한다")
		void findByEmail_Success() {
			// given
			User user = User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://example.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			);
			userRepository.save(user);

			// when
			Optional<User> foundUser = userRepository.findByEmail("test@example.com");

			// then
			assertThat(foundUser).isPresent();
			assertThat(foundUser.get()).extracting("email", "name")
				.containsExactly("test@example.com", "테스트유저");
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
		void findByEmail_NotFound() {
			// when
			Optional<User> foundUser = userRepository.findByEmail("notfound@example.com");

			// then
			assertThat(foundUser).isEmpty();
		}
	}

	@Nested
	@DisplayName("이메일 중복 확인")
	class ExistsByEmail {

		@Test
		@DisplayName("존재하는 이메일이면 true를 반환한다")
		void existsByEmail_True() {
			// given
			User user = User.createOAuthUser(
				"exists@example.com",
				"테스트유저",
				null,
				AuthProvider.GOOGLE,
				"google-123"
			);
			userRepository.save(user);

			// when
			boolean exists = userRepository.existsByEmail("exists@example.com");

			// then
			assertThat(exists).isTrue();
		}

		@Test
		@DisplayName("존재하지 않는 이메일이면 false를 반환한다")
		void existsByEmail_False() {
			// when
			boolean exists = userRepository.existsByEmail("notexists@example.com");

			// then
			assertThat(exists).isFalse();
		}
	}

	@Nested
	@DisplayName("OAuth Provider와 ProviderId로 유저 조회")
	class FindByProviderAndProviderId {

		@Test
		@DisplayName("Provider와 ProviderId로 유저를 조회한다")
		void findByProviderAndProviderId_Success() {
			// given
			User user = User.createOAuthUser(
				"google@example.com",
				"구글유저",
				null,
				AuthProvider.GOOGLE,
				"google-unique-id-123"
			);
			userRepository.save(user);

			// when
			Optional<User> foundUser = userRepository.findByProviderAndProviderId(
				AuthProvider.GOOGLE,
				"google-unique-id-123"
			);

			// then
			assertThat(foundUser).isPresent();
			assertThat(foundUser.get()).extracting("email", "provider")
				.containsExactly("google@example.com", AuthProvider.GOOGLE);
		}

		@Test
		@DisplayName("다른 Provider로 조회하면 빈 Optional을 반환한다")
		void findByProviderAndProviderId_DifferentProvider() {
			// given
			User user = User.createOAuthUser(
				"google@example.com",
				"구글유저",
				null,
				AuthProvider.GOOGLE,
				"google-unique-id-123"
			);
			userRepository.save(user);

			// when
			Optional<User> foundUser = userRepository.findByProviderAndProviderId(
				AuthProvider.KAKAO,
				"google-unique-id-123"
			);

			// then
			assertThat(foundUser).isEmpty();
		}
	}

	@Nested
	@DisplayName("유저 저장")
	class SaveUser {

		@Test
		@DisplayName("새로운 유저를 저장한다")
		void save_NewUser() {
			// given
			User user = User.createOAuthUser(
				"new@example.com",
				"새유저",
				"https://example.com/profile.jpg",
				AuthProvider.KAKAO,
				"kakao-123"
			);

			// when
			User savedUser = userRepository.save(user);

			// then
			assertThat(savedUser.getId()).isNotNull();
			assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
		}

		@Test
		@DisplayName("로컬 유저를 저장한다")
		void save_LocalUser() {
			// given
			User user = User.createLocalUser(
				"local@example.com",
				"password123!",
				"로컬유저"
			);

			// when
			User savedUser = userRepository.save(user);

			// then
			assertThat(savedUser.getId()).isNotNull();
			assertThat(savedUser).extracting("provider", "password")
				.containsExactly(AuthProvider.LOCAL, "password123!");
		}
	}
}
