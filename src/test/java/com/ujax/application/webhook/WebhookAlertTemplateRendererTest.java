package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ujax.application.webhook.dto.RenderedWebhookMessage;

class WebhookAlertTemplateRendererTest {

	private final WebhookAlertTemplateRenderer templateRenderer = new WebhookAlertTemplateRenderer();

	@Test
	@DisplayName("Webhook 알림 메시지를 공용 템플릿으로 렌더링한다")
	void renderMessage() {
		// given
		WebhookAlertMessage message = new WebhookAlertMessage(
			101L,
			11L,
			"알고리즘 스터디",
			"1000. 백준 1000 A+B",
			LocalDateTime.of(2026, 3, 8, 11, 0),
			LocalDateTime.of(2026, 3, 8, 10, 0),
			"https://ujax.site/ws/11/ide/1000"
		);

		// when
		RenderedWebhookMessage renderedMessage = templateRenderer.render(message);

		// then
		assertThat(renderedMessage).extracting(
			"title",
			"body",
			"footer",
			"workspaceProblemId",
			"workspaceId",
			"problemLink"
		).containsExactly(
			"[알고리즘 스터디] 1000. 백준 1000 A+B",
			"### 문제 풀이 마감까지 얼마 남지 않았습니다.\n\n**마감일**\n\n**03월 08일 11:00**",
			"우작스(UJAX)",
			101L,
			11L,
			"https://ujax.site/ws/11/ide/1000"
		);
	}

	@Test
	@DisplayName("마감일이 없으면 템플릿에 미설정 문구를 넣는다")
	void renderMessageWithoutDeadline() {
		// given
		WebhookAlertMessage message = new WebhookAlertMessage(
			101L,
			11L,
			"알고리즘 스터디",
			"1000. 백준 1000 A+B",
			null,
			LocalDateTime.of(2026, 3, 8, 10, 0),
			"https://ujax.site/ws/11/ide/1000"
		);

		// when
		RenderedWebhookMessage renderedMessage = templateRenderer.render(message);

		// then
		assertThat(renderedMessage.body()).contains("미설정");
	}
}
