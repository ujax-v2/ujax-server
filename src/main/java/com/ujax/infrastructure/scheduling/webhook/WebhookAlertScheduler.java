package com.ujax.infrastructure.scheduling.webhook;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ujax.application.webhook.WebhookAlertDeliveryService;
import com.ujax.infrastructure.config.webhook.WebhookAlertSchedulerProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.webhook-alert.scheduler", name = "enabled", havingValue = "true")
public class WebhookAlertScheduler {

	private final WebhookAlertDeliveryService webhookAlertDeliveryService;
	private final WebhookAlertSchedulerProperties properties;

	@Scheduled(fixedDelayString = "${app.webhook-alert.scheduler.fixed-delay-millis:60000}")
	public void runWebhookAlertBatch() {
		LocalDateTime now = LocalDateTime.now();
		webhookAlertDeliveryService.recoverStuckProcessing(now);

		List<Long> reservedAlertIds = webhookAlertDeliveryService.reserveDueAlertIds(now, properties.batchSize());
		for (Long alertId : reservedAlertIds) {
			webhookAlertDeliveryService.deliver(alertId, now);
		}
	}
}
