package com.ujax.infrastructure.external.webhook;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ujax.application.webhook.WebhookAlertMessage;
import com.ujax.application.webhook.WebhookAlertTemplateRenderer;
import com.ujax.application.webhook.dto.RenderedWebhookMessage;

@ExtendWith(MockitoExtension.class)
class TemplatedWebhookSenderTest {

	@Mock
	private WebhookAlertTemplateRenderer templateRenderer;

	@Mock
	private MattermostWebhookSender mattermostWebhookSender;

	@Test
	@DisplayName("공용 템플릿 렌더링 후 Mattermost sender에 위임한다")
	void delegateToMattermostWebhookSender() {
		// given
		TemplatedWebhookSender webhookSender = new TemplatedWebhookSender(templateRenderer, mattermostWebhookSender);
		WebhookAlertMessage message = new WebhookAlertMessage(
			101L,
			11L,
			"알고리즘 스터디",
			"1000. 백준 1000 A+B",
			LocalDateTime.of(2026, 3, 8, 11, 0),
			LocalDateTime.of(2026, 3, 8, 10, 0),
			"https://ujax.site/ws/11/ide/1000"
		);
		RenderedWebhookMessage renderedMessage = new RenderedWebhookMessage(
			101L,
			11L,
			"알고리즘 스터디",
			"1000. 백준 1000 A+B",
			LocalDateTime.of(2026, 3, 8, 11, 0),
			LocalDateTime.of(2026, 3, 8, 10, 0),
			"https://ujax.site/ws/11/ide/1000",
			"[알고리즘 스터디] 1000. 백준 1000 A+B",
			"body",
			"우작스(UJAX)"
		);
		given(templateRenderer.render(message)).willReturn(renderedMessage);

		// when
		webhookSender.send("https://hook.example.com", message);

		// then
		then(templateRenderer).should().render(message);
		then(mattermostWebhookSender).should().send("https://hook.example.com", renderedMessage);
	}
}
