package com.ujax.application.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.signup-verification")
public record SignupVerificationProperties(
	int ttlMinutes,
	int codeLength
) {
}
