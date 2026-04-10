package com.ujax.application.mail.outbox.message;

import java.time.LocalDateTime;

public record SignupVerificationMailPayload(
	String code,
	LocalDateTime expiresAt
) {
}
