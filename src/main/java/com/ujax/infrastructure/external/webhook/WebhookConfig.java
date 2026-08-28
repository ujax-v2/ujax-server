package com.ujax.infrastructure.external.webhook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebhookConfig {

    @Bean("webhookRestTemplate")
    public RestTemplate webhookRestTemplate(WebhookHttpProperties webhookHttpProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(webhookHttpProperties.connectTimeoutMillis());
        requestFactory.setReadTimeout(webhookHttpProperties.readTimeoutMillis());
        return new RestTemplate(requestFactory);
    }
}
