package com.ujax.domain.auth;

public interface VerificationCodeHasher {

	String hash(String rawCode);

	boolean matches(String rawCode, String encodedCode);
}
