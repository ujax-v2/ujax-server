package com.ujax.infrastructure.scheduling.mail;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ujax.application.mail.outbox.MailOutboxService;
import com.ujax.infrastructure.config.mail.MailOutboxSchedulerProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ujax.mail.outbox.scheduler", name = "enabled", havingValue = "true")
public class MailOutboxScheduler {

	private final MailOutboxService mailOutboxService;
	private final MailOutboxSchedulerProperties properties;

	@Scheduled(fixedDelayString = "${app.ujax.mail.outbox.scheduler.fixed-delay-millis:60000}")
	public void runMailOutboxBatch() {
		LocalDateTime now = LocalDateTime.now();
		mailOutboxService.recoverStuckProcessing(now);

		List<Long> reservedOutboxIds = mailOutboxService.reserveDueMailOutboxIds(now, properties.batchSize());
		for (Long outboxId : reservedOutboxIds) {
			mailOutboxService.deliver(outboxId, now);
		}
	}
}
