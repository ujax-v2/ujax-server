package com.ujax.domain.user;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

	@Test
	@DisplayName("이메일로 유저를 조회한다")
	void findByEmail() {
		// given
		userRepository.save(User.createOAuthUser(
			"test@example.com", "테스트유저", "https://example.com/profile.jpg",
			AuthProvider.GOOGLE, "google-123"
		));

		// when
		var foundUser = userRepository.findByEmail("test@example.com");

		// then
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get()).extracting("email", "name")
			.containsExactly("test@example.com", "테스트유저");
	}

	@Test
	@DisplayName("이메일 존재 여부를 확인한다")
	void existsByEmail() {
		// given
		userRepository.save(User.createOAuthUser(
			"exists@example.com", "테스트유저", null,
			AuthProvider.GOOGLE, "google-123"
		));

		// when & then
		assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
	}

	@Test
	@DisplayName("Provider와 ProviderId로 유저를 조회한다")
	void findByProviderAndProviderId() {
		// given
		userRepository.save(User.createOAuthUser(
			"google@example.com", "구글유저", null,
			AuthProvider.GOOGLE, "google-unique-id-123"
		));

		// when
		var foundUser = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-unique-id-123");

		// then
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get()).extracting("email", "provider")
			.containsExactly("google@example.com", AuthProvider.GOOGLE);
	}

	@Test
	@DisplayName("OAuth 유저를 저장한다")
	void save_OAuthUser() {
		// given
		User user = User.createOAuthUser(
			"new@example.com", "새유저", "https://example.com/profile.jpg",
			AuthProvider.KAKAO, "kakao-123"
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
		User user = User.createLocalUser("local@example.com", Password.ofEncoded("password123!"), "로컬유저");

		// when
		User savedUser = userRepository.save(user);

		// then
		assertThat(savedUser.getId()).isNotNull();
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(savedUser.getPassword().getEncodedValue()).isEqualTo("password123!");
	}
}
