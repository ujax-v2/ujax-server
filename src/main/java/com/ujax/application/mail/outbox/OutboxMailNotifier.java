package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.MailNotifier;
import com.ujax.application.mail.outbox.message.SignupVerificationMailPayload;
import com.ujax.application.mail.outbox.message.WorkspaceInviteMailPayload;
import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxRepository;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxMailNotifier implements MailNotifier {

	private final MailOutboxRepository mailOutboxRepository;
	private final MailOutboxEventLogger mailOutboxEventLogger;
	private final ObjectMapper objectMapper;

	@Override
	public void enqueueSignupVerification(String email, String code, LocalDateTime expiresAt) {
		enqueue(
			MailType.SIGNUP_VERIFICATION,
			email,
			new SignupVerificationMailPayload(code, expiresAt)
		);
	}

	@Override
	public void enqueueWorkspaceInvite(String email, String workspaceName, Long workspaceId) {
		enqueue(
			MailType.WORKSPACE_INVITE,
			email,
			new WorkspaceInviteMailPayload(workspaceId, workspaceName)
		);
	}

	private void enqueue(MailType mailType, String recipientEmail, Object payload) {
		MailOutbox outbox = MailOutbox.create(
			mailType,
			recipientEmail,
			serializePayload(payload),
			LocalDateTime.now()
		);
		MailOutbox savedOutbox = mailOutboxRepository.save(outbox);
		mailOutboxEventLogger.logEnqueued(savedOutbox);
	}

	private String serializePayload(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("failed to serialize mail outbox payload", exception);
		}
	}
}
