package com.ujax.application.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook-alert.scheduler")
public record WebhookAlertSchedulerProperties(
	int batchSize
) {
}
