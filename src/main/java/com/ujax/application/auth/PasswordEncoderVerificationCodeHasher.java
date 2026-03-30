package com.ujax.application.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ujax.domain.auth.VerificationCodeHasher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordEncoderVerificationCodeHasher implements VerificationCodeHasher {

	private final PasswordEncoder passwordEncoder;

	@Override
	public String hash(String rawCode) {
		return passwordEncoder.encode(rawCode);
	}

	@Override
	public boolean matches(String rawCode, String encodedCode) {
		return passwordEncoder.matches(rawCode, encodedCode);
	}
}
