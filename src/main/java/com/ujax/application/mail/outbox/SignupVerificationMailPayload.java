package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;

public record SignupVerificationMailPayload(
	String code,
	LocalDateTime expiresAt
) {
}
