package com.ujax.application.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.auth.PendingSignup;
import com.ujax.domain.auth.PendingSignupRepository;

@SpringBootTest
@ActiveProfiles("test")
class PendingSignupCleanupServiceTest {

	@Autowired
	private PendingSignupCleanupService pendingSignupCleanupService;

	@Autowired
	private PendingSignupRepository pendingSignupRepository;

	@BeforeEach
	void setUp() {
		pendingSignupRepository.deleteAll();
	}

	@Nested
	@DisplayName("만료된 회원가입 세션 정리")
	class CleanupExpired {

		@Test
		@DisplayName("만료된 세션만 삭제한다")
		void cleanupExpired_DeletesOnlyExpiredPendingSignups() {
			LocalDateTime now = LocalDateTime.now();
			PendingSignup expired = pendingSignupRepository.save(
				PendingSignup.create("expired@example.com", "expired-code", now.minusMinutes(1))
			);
			PendingSignup active = pendingSignupRepository.save(
				PendingSignup.create("active@example.com", "active-code", now.plusMinutes(5))
			);

			int deletedCount = pendingSignupCleanupService.cleanupExpired(now, 100);

			assertThat(deletedCount).isEqualTo(1);
			assertThat(pendingSignupRepository.findByRequestToken(expired.getRequestToken())).isEmpty();
			assertThat(pendingSignupRepository.findByRequestToken(active.getRequestToken())).isPresent();
		}

		@Test
		@DisplayName("batch size 만큼만 삭제한다")
		void cleanupExpired_RespectsBatchSize() {
			LocalDateTime now = LocalDateTime.now();
			PendingSignup oldestExpired = pendingSignupRepository.save(
				PendingSignup.create("expired-old@example.com", "expired-old-code", now.minusMinutes(3))
			);
			PendingSignup newestExpired = pendingSignupRepository.save(
				PendingSignup.create("expired-new@example.com", "expired-new-code", now.minusMinutes(1))
			);

			int deletedCount = pendingSignupCleanupService.cleanupExpired(now, 1);

			assertThat(deletedCount).isEqualTo(1);
			assertThat(pendingSignupRepository.findByRequestToken(oldestExpired.getRequestToken())).isEmpty();
			assertThat(pendingSignupRepository.findByRequestToken(newestExpired.getRequestToken())).isPresent();
		}

		@Test
		@DisplayName("batch size 가 0 이하면 아무것도 삭제하지 않는다")
		void cleanupExpired_IgnoresNonPositiveBatchSize() {
			LocalDateTime now = LocalDateTime.now();
			PendingSignup expired = pendingSignupRepository.save(
				PendingSignup.create("expired@example.com", "expired-code", now.minusMinutes(1))
			);

			int deletedCount = pendingSignupCleanupService.cleanupExpired(now, 0);

			assertThat(deletedCount).isZero();
			assertThat(pendingSignupRepository.findByRequestToken(expired.getRequestToken())).isPresent();
		}
	}
}
