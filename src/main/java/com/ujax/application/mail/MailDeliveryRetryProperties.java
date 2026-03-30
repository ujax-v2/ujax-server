package com.ujax.application.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.ujax.mail.retry")
public record MailDeliveryRetryProperties(
	@DefaultValue("3") @Min(1) int maxAttempts,
	@DefaultValue("300") @Min(0) long delayMillis
) {
}
