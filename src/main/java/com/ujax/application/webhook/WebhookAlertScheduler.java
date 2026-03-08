package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.webhook-alert.scheduler", name = "enabled", havingValue = "true")
public class WebhookAlertScheduler {

	private final WebhookAlertService webhookAlertService;
	private final WebhookAlertSchedulerProperties properties;

	@Scheduled(fixedDelayString = "${app.webhook-alert.scheduler.fixed-delay-millis:60000}")
	public void runWebhookAlertBatch() {
		LocalDateTime now = LocalDateTime.now();

		// TODO(issue-23): 실제 WebhookSender 구현이 들어가면 enabled=true 전환을 검토한다.
		webhookAlertService.recoverStuckProcessing(now);

		List<Long> reservedAlertIds = webhookAlertService.reserveDueAlertIds(now, properties.batchSize());
		for (Long alertId : reservedAlertIds) {
			webhookAlertService.deliver(alertId, now);
		}
	}
}
