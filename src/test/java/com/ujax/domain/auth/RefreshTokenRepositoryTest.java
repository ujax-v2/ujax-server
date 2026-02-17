package com.ujax.domain.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class RefreshTokenRepositoryTest {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	private User createAndSaveUser() {
		User user = User.createLocalUser("test@example.com", Password.ofEncoded("password"), "테스트");
		return userRepository.save(user);
	}

	@Test
	@DisplayName("해시로 유효한 리프레시 토큰을 조회한다")
	void findByTokenHashAndRevokedAtIsNull() {
		// given
		User user = createAndSaveUser();
		String tokenHash = "validTokenHash";
		RefreshToken refreshToken = RefreshToken.create(user, tokenHash, LocalDateTime.now().plusDays(30));
		refreshTokenRepository.save(refreshToken);

		// when
		Optional<RefreshToken> found = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash);

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getTokenHash()).isEqualTo(tokenHash);
	}

	@Test
	@DisplayName("사용자의 모든 유효한 토큰을 해지한다")
	void revokeAllByUserId() {
		// given
		User user = createAndSaveUser();
		RefreshToken token1 = RefreshToken.create(user, "hash1", LocalDateTime.now().plusDays(30));
		RefreshToken token2 = RefreshToken.create(user, "hash2", LocalDateTime.now().plusDays(30));
		refreshTokenRepository.save(token1);
		refreshTokenRepository.save(token2);

		// when
		refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

		// then
		assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("hash1")).isEmpty();
		assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("hash2")).isEmpty();
	}
}
