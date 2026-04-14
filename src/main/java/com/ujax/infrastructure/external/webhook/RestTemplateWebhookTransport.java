package com.ujax.infrastructure.external.webhook;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateWebhookTransport {

	private static final MediaType JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

	private final RestTemplate restTemplate;

	public RestTemplateWebhookTransport(
		@Qualifier("webhookRestTemplate") RestTemplate restTemplate
	) {
		this.restTemplate = restTemplate;
	}

	public void sendJson(String hookUrl, byte[] body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(JSON_UTF8);
		headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

		HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, headers);
		restTemplate.postForEntity(hookUrl, requestEntity, Void.class);
	}
}
