package com.ujax.domain.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

	Optional<PendingSignup> findByRequestToken(String requestToken);

	Optional<PendingSignup> findByEmail(String email);

	void deleteByRequestToken(String requestToken);
}
