package com.ujax.infrastructure.external.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.eq;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.webhook.dto.RenderedWebhookMessage;

@ExtendWith(MockitoExtension.class)
class MattermostWebhookSenderTest {

	@Mock
	private RestTemplateWebhookTransport transport;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("렌더링 결과를 Mattermost payload로 변환해 transport에 전달한다")
	void sendMattermostPayload() throws Exception {
		// given
		MattermostWebhookSender webhookSender = new MattermostWebhookSender(transport, objectMapper);
		RenderedWebhookMessage renderedMessage = new RenderedWebhookMessage(
			101L,
			11L,
			"알고리즘 스터디",
			"1000. 백준 1000 A+B",
			LocalDateTime.of(2026, 3, 8, 11, 0),
			LocalDateTime.of(2026, 3, 8, 10, 0),
			"https://ujax.site/ws/11/ide/1000",
			"[알고리즘 스터디] 1000. 백준 1000 A+B",
			"### 문제 풀이 마감까지 얼마 남지 않았습니다.\n\n**마감일**\n\n**03월 08일 11:00**",
			"우작스(UJAX)"
		);
		ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);

		// when
		webhookSender.send("https://hook.example.com", renderedMessage);

		// then
		then(transport).should().sendJson(eq("https://hook.example.com"), bodyCaptor.capture());
		String actualJson = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);

		assertThat(objectMapper.readTree(actualJson)).isEqualTo(objectMapper.readTree("""
			{
			  "attachments": [
			    {
			      "color": "#2563EB",
			      "title": "[알고리즘 스터디] 1000. 백준 1000 A+B",
			      "title_link": "https://ujax.site/ws/11/ide/1000",
			      "text": "### 문제 풀이 마감까지 얼마 남지 않았습니다.\\n\\n**마감일**\\n\\n**03월 08일 11:00**",
			      "footer": "우작스(UJAX)"
			    }
			  ],
			  "workspaceProblemId": 101,
			  "workspaceId": 11,
			  "workspaceName": "알고리즘 스터디",
			  "problemTitle": "1000. 백준 1000 A+B",
			  "deadline": "2026-03-08T11:00:00",
			  "scheduledAt": "2026-03-08T10:00:00",
			  "problemLink": "https://ujax.site/ws/11/ide/1000"
			}
			"""));
	}
}
