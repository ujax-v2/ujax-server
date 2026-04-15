package com.ujax.application.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.webhook-alert.delivery")
public record WebhookAlertDeliveryProperties(
	@DefaultValue("1") @Min(1) int retryDelayMinutes,
	@DefaultValue("10") @Min(1) int stuckProcessingMinutes,
	@DefaultValue("5") @Min(1) int maxAttempts
) {
}
