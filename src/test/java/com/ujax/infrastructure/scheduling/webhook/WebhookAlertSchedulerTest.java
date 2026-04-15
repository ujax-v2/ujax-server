package com.ujax.infrastructure.scheduling.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.ujax.application.webhook.WebhookAlertDeliveryService;
import com.ujax.infrastructure.config.webhook.WebhookAlertSchedulerProperties;

class WebhookAlertSchedulerTest {

	@Test
	@DisplayName("배치 실행 시 recover -> reserve -> deliver 순서로 호출한다")
	void runWebhookAlertBatch() {
		WebhookAlertDeliveryService webhookAlertDeliveryService = mock(WebhookAlertDeliveryService.class);
		WebhookAlertSchedulerProperties properties = new WebhookAlertSchedulerProperties(100);
		WebhookAlertScheduler scheduler = new WebhookAlertScheduler(webhookAlertDeliveryService, properties);
		given(webhookAlertDeliveryService.reserveDueAlertIds(any(LocalDateTime.class), eq(100)))
			.willReturn(List.of(1L, 2L));

		scheduler.runWebhookAlertBatch();

		ArgumentCaptor<LocalDateTime> recoverNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> reserveNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> deliverNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		InOrder inOrder = inOrder(webhookAlertDeliveryService);

		inOrder.verify(webhookAlertDeliveryService).recoverStuckProcessing(recoverNowCaptor.capture());
		inOrder.verify(webhookAlertDeliveryService).reserveDueAlertIds(reserveNowCaptor.capture(), eq(100));
		inOrder.verify(webhookAlertDeliveryService).deliver(eq(1L), deliverNowCaptor.capture());
		inOrder.verify(webhookAlertDeliveryService).deliver(eq(2L), deliverNowCaptor.capture());

		LocalDateTime recoverNow = recoverNowCaptor.getValue();
		assertThat(reserveNowCaptor.getValue()).isEqualTo(recoverNow);
		assertThat(deliverNowCaptor.getAllValues()).allMatch(recoverNow::equals);
	}

	@Test
	@DisplayName("reserve 결과가 비어있으면 deliver는 호출하지 않는다")
	void runWebhookAlertBatchWithoutDeliver() {
		WebhookAlertDeliveryService webhookAlertDeliveryService = mock(WebhookAlertDeliveryService.class);
		WebhookAlertSchedulerProperties properties = new WebhookAlertSchedulerProperties(50);
		WebhookAlertScheduler scheduler = new WebhookAlertScheduler(webhookAlertDeliveryService, properties);
		given(webhookAlertDeliveryService.reserveDueAlertIds(any(LocalDateTime.class), eq(50)))
			.willReturn(List.of());

		scheduler.runWebhookAlertBatch();

		then(webhookAlertDeliveryService).should().recoverStuckProcessing(any(LocalDateTime.class));
		then(webhookAlertDeliveryService).should().reserveDueAlertIds(any(LocalDateTime.class), eq(50));
		then(webhookAlertDeliveryService).should(never()).deliver(anyLong(), any(LocalDateTime.class));
	}
}
