package com.ujax.application.auth;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class RandomSignupVerificationCodeGenerator implements SignupVerificationCodeGenerator {

	private final int codeLength;
	private final SecureRandom secureRandom = new SecureRandom();

	public RandomSignupVerificationCodeGenerator(SignupVerificationProperties properties) {
		this.codeLength = properties.codeLength();
	}

	@Override
	public String generate() {
		StringBuilder builder = new StringBuilder(codeLength);
		for (int i = 0; i < codeLength; i++) {
			builder.append(secureRandom.nextInt(10));
		}
		return builder.toString();
	}
}
