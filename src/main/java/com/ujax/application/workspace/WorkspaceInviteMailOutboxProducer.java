package com.ujax.application.workspace;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.outbox.MailOutboxLogRecorder;
import com.ujax.application.mail.outbox.WorkspaceInviteMailPayload;
import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxRepository;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceInviteMailOutboxProducer {

	private final MailOutboxRepository mailOutboxRepository;
	private final MailOutboxLogRecorder mailOutboxLogRecorder;
	private final ObjectMapper objectMapper;

	public void enqueue(String email, String workspaceName, Long workspaceId) {
		String payloadJson = serializePayload(new WorkspaceInviteMailPayload(workspaceId, workspaceName));
		MailOutbox outbox = MailOutbox.create(
			MailType.WORKSPACE_INVITE,
			email,
			payloadJson,
			LocalDateTime.now()
		);
		MailOutbox savedOutbox = mailOutboxRepository.save(outbox);
		mailOutboxLogRecorder.recordEnqueued(savedOutbox);
	}

	private String serializePayload(WorkspaceInviteMailPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("failed to serialize workspace invite mail payload", exception);
		}
	}
}
