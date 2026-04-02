package com.ujax.domain.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

	Optional<PendingSignup> findByRequestToken(String requestToken);

	Optional<PendingSignup> findByEmail(String email);

	List<PendingSignup> findAllByExpiresAtLessThanEqualOrderByExpiresAtAsc(LocalDateTime expiresAt, Pageable pageable);

	void deleteByRequestToken(String requestToken);
}
