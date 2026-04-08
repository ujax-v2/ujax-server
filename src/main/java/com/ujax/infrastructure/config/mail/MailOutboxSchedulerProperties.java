package com.ujax.infrastructure.config.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.ujax.mail.outbox.scheduler")
public record MailOutboxSchedulerProperties(
	@DefaultValue("100") @Min(1) int batchSize
) {
}
