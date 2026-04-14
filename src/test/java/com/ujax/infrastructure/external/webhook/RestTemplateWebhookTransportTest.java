package com.ujax.infrastructure.external.webhook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class RestTemplateWebhookTransportTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
	private final RestTemplateWebhookTransport transport = new RestTemplateWebhookTransport(restTemplate);

	@Test
	@DisplayName("JSON webhook 요청을 POST로 전송한다")
	void sendJsonRequest() {
		server.expect(once(), requestTo("https://hook.example.com"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8"))
			.andExpect(content().json("""
				{
				  "message": "hello"
				}
				"""))
			.andRespond(withSuccess());

		transport.sendJson("https://hook.example.com", """
			{
			  "message": "hello"
			}
			""".getBytes(StandardCharsets.UTF_8));

		server.verify();
	}

	@Test
	@DisplayName("HTTP 5xx 응답은 예외로 그대로 전파한다")
	void propagateServerError() {
		server.expect(once(), requestTo("https://hook.example.com"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		assertThatThrownBy(() -> transport.sendJson(
			"https://hook.example.com",
			"{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8)
		)).isInstanceOf(HttpServerErrorException.class);

		server.verify();
	}
}
