package com.ujax.infrastructure.external.webhook;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.webhook-alert.http")
public record WebhookHttpProperties(
    @DefaultValue("1000") @Min(1) int connectTimeoutMillis,
    @DefaultValue("2000") @Min(1) int readTimeoutMillis
) {
}
