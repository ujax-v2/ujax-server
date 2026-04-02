package com.ujax.application.mail.outbox;

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.UjaxMailTemplateRenderer;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceInviteMailOutboxHandler implements MailOutboxHandler {

	private final ObjectMapper objectMapper;

	@Value("${app.ujax.base-url}")
	private String baseUrl;

	@Override
	public MailType mailType() {
		return MailType.WORKSPACE_INVITE;
	}

	@Override
	public PreparedMailMessage prepare(String payloadJson) {
		try {
			WorkspaceInviteMailPayload payload = objectMapper.readValue(payloadJson, WorkspaceInviteMailPayload.class);
			Long workspaceId = Objects.requireNonNull(payload.workspaceId(), "workspaceId must not be null");
			String workspaceName = Objects.requireNonNull(payload.workspaceName(), "workspaceName must not be null");
			String link = String.format("%s/workspaces/%d", baseUrl, workspaceId);
			return new PreparedMailMessage(
				String.format("[UJAX] %s에서 당신을 초대했습니다.", workspaceName),
				UjaxMailTemplateRenderer.renderWorkspaceInvitation(workspaceName, link)
			);
		} catch (IOException exception) {
			throw new IllegalArgumentException("invalid workspace invite mail payload", exception);
		}
	}
}
