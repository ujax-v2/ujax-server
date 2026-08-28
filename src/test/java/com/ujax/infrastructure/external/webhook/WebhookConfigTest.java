package com.ujax.infrastructure.external.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class WebhookConfigTest {

    private final WebhookConfig webhookConfig = new WebhookConfig();

    @Test
    @DisplayName("Webhook 전용 RestTemplate에 timeout 설정을 반영한다")
    void createWebhookRestTemplate() {
        // given
        WebhookHttpProperties webhookHttpProperties = new WebhookHttpProperties(1500, 2500);

        // when
        RestTemplate restTemplate = webhookConfig.webhookRestTemplate(webhookHttpProperties);

        // then
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(1500);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(2500);
    }
}
