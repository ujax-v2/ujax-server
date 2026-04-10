package com.ujax.application.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.domain.auth.PendingSignup;
import com.ujax.domain.auth.PendingSignupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingSignupCleanupService {

	private final PendingSignupRepository pendingSignupRepository;

	@Transactional
	public int cleanupExpired(LocalDateTime now, int batchSize) {
		if (batchSize <= 0) {
			return 0;
		}

		List<PendingSignup> expiredPendingSignups = pendingSignupRepository
			.findAllByExpiresAtLessThanEqualOrderByExpiresAtAsc(now, PageRequest.of(0, batchSize));

		if (expiredPendingSignups.isEmpty()) {
			return 0;
		}

		pendingSignupRepository.deleteAllInBatch(expiredPendingSignups);
		return expiredPendingSignups.size();
	}
}
