package com.ujax.application.mail.outbox.handler;

import java.io.IOException;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.outbox.message.PreparedMailMessage;
import com.ujax.application.mail.outbox.message.SignupVerificationMailPayload;
import com.ujax.application.mail.template.SignupVerificationMailTemplateRenderer;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SignupVerificationMailOutboxHandler implements MailOutboxHandler {

	private final ObjectMapper objectMapper;

	@Override
	public MailType mailType() {
		return MailType.SIGNUP_VERIFICATION;
	}

	@Override
	public PreparedMailMessage prepare(String payloadJson) {
		try {
			SignupVerificationMailPayload payload = objectMapper.readValue(payloadJson, SignupVerificationMailPayload.class);
			String code = Objects.requireNonNull(payload.code(), "code must not be null");
			return new PreparedMailMessage(
				"[UJAX] 회원가입 인증 코드 - [ " + code + " ]",
				SignupVerificationMailTemplateRenderer.render(code)
			);
		} catch (IOException exception) {
			throw new IllegalArgumentException("invalid signup verification mail payload", exception);
		}
	}
}
