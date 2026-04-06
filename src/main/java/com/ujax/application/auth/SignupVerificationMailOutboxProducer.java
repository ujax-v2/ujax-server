package com.ujax.application.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.outbox.SignupVerificationMailPayload;
import com.ujax.application.mail.outbox.MailOutboxLogRecorder;
import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxRepository;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SignupVerificationMailOutboxProducer {

	private final MailOutboxRepository mailOutboxRepository;
	private final MailOutboxLogRecorder mailOutboxLogRecorder;
	private final ObjectMapper objectMapper;

	public void enqueue(String email, String code, LocalDateTime expiresAt) {
		String payloadJson = serializePayload(new SignupVerificationMailPayload(code, expiresAt));
		MailOutbox outbox = MailOutbox.create(
			MailType.SIGNUP_VERIFICATION,
			email,
			payloadJson,
			LocalDateTime.now()
		);
		MailOutbox savedOutbox = mailOutboxRepository.save(outbox);
		mailOutboxLogRecorder.recordEnqueued(savedOutbox);
	}

	private String serializePayload(SignupVerificationMailPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("failed to serialize signup verification mail payload", exception);
		}
	}
}
