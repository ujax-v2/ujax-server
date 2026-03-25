package com.ujax.infrastructure.external.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.ujax.application.webhook.WebhookAlertMessage;

class RestTemplateWebhookSenderTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
	private final RestTemplateWebhookSender webhookSender = new RestTemplateWebhookSender(
		restTemplate,
		new ObjectMapper()
	);

	@Test
	@DisplayName("webhook 요청을 JSON POST로 전송한다")
	void sendWebhookRequest() {
		server.expect(once(), requestTo("https://hook.example.com"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8"))
			.andExpect(content().json("""
				{
				  "attachments": [
				    {
				      "color": "#2563EB",
				      "title": "[알고리즘 스터디] 1000. 백준 1000 A+B",
				      "title_link": "https://ujax.site/ws/11/ide/1000",
				      "text": "### 문제 풀이 마감까지 얼마 남지 않았습니다.\\n\\n**마감일**\\n\\n**03월 08일 11:00**",
				      "footer": "프로젝트명"
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
				"""))
			.andRespond(withSuccess());

		webhookSender.send(
			"https://hook.example.com",
			new WebhookAlertMessage(
				101L,
				11L,
				"알고리즘 스터디",
				"1000. 백준 1000 A+B",
				LocalDateTime.of(2026, 3, 8, 11, 0),
				LocalDateTime.of(2026, 3, 8, 10, 0),
				"https://ujax.site/ws/11/ide/1000"
			)
		);

		server.verify();
	}

	@Test
	@DisplayName("HTTP 5xx 응답은 예외로 그대로 전파한다")
	void propagateServerError() {
		server.expect(once(), requestTo("https://hook.example.com"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		assertThatThrownBy(() -> webhookSender.send(
			"https://hook.example.com",
			new WebhookAlertMessage(
				101L,
				11L,
				"알고리즘 스터디",
				"1000. 백준 1000 A+B",
				LocalDateTime.of(2026, 3, 8, 11, 0),
				LocalDateTime.of(2026, 3, 8, 10, 0),
				"https://ujax.site/ws/11/ide/1000"
			)
		)).isInstanceOf(HttpServerErrorException.class);

		server.verify();
	}
}
