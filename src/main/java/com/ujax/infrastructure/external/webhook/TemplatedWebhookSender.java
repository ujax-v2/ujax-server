package com.ujax.infrastructure.external.webhook;

import org.springframework.stereotype.Component;

import com.ujax.application.webhook.WebhookAlertMessage;
import com.ujax.application.webhook.WebhookAlertTemplateRenderer;
import com.ujax.application.webhook.WebhookSender;
import com.ujax.application.webhook.dto.RenderedWebhookMessage;

@Component
public class TemplatedWebhookSender implements WebhookSender {

	private final WebhookAlertTemplateRenderer templateRenderer;
	private final MattermostWebhookSender mattermostWebhookSender;

	public TemplatedWebhookSender(
		WebhookAlertTemplateRenderer templateRenderer,
		MattermostWebhookSender mattermostWebhookSender
	) {
		this.templateRenderer = templateRenderer;
		this.mattermostWebhookSender = mattermostWebhookSender;
	}

	@Override
	public void send(String hookUrl, WebhookAlertMessage message) {
		RenderedWebhookMessage renderedMessage = templateRenderer.render(message);
		mattermostWebhookSender.send(hookUrl, renderedMessage);
	}
}
