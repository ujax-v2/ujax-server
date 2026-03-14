package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class WebhookAlertSchedulerTest {

	@Test
	@DisplayName("배치 실행 시 recover -> reserve -> deliver 순서로 호출한다")
	void runWebhookAlertBatch() {
		WebhookAlertService webhookAlertService = mock(WebhookAlertService.class);
		WebhookAlertSchedulerProperties properties = new WebhookAlertSchedulerProperties(100);
		WebhookAlertScheduler scheduler = new WebhookAlertScheduler(webhookAlertService, properties);
		given(webhookAlertService.reserveDueAlertIds(any(LocalDateTime.class), eq(100)))
			.willReturn(List.of(1L, 2L));

		scheduler.runWebhookAlertBatch();

		ArgumentCaptor<LocalDateTime> recoverNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> reserveNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> deliverNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		InOrder inOrder = inOrder(webhookAlertService);

		inOrder.verify(webhookAlertService).recoverStuckProcessing(recoverNowCaptor.capture());
		inOrder.verify(webhookAlertService).reserveDueAlertIds(reserveNowCaptor.capture(), eq(100));
		inOrder.verify(webhookAlertService).deliver(eq(1L), deliverNowCaptor.capture());
		inOrder.verify(webhookAlertService).deliver(eq(2L), deliverNowCaptor.capture());

		LocalDateTime recoverNow = recoverNowCaptor.getValue();
		assertThat(reserveNowCaptor.getValue()).isEqualTo(recoverNow);
		assertThat(deliverNowCaptor.getAllValues()).allMatch(recoverNow::equals);
	}

	@Test
	@DisplayName("reserve 결과가 비어있으면 deliver는 호출하지 않는다")
	void runWebhookAlertBatchWithoutDeliver() {
		WebhookAlertService webhookAlertService = mock(WebhookAlertService.class);
		WebhookAlertSchedulerProperties properties = new WebhookAlertSchedulerProperties(50);
		WebhookAlertScheduler scheduler = new WebhookAlertScheduler(webhookAlertService, properties);
		given(webhookAlertService.reserveDueAlertIds(any(LocalDateTime.class), eq(50)))
			.willReturn(List.of());

		scheduler.runWebhookAlertBatch();

		then(webhookAlertService).should().recoverStuckProcessing(any(LocalDateTime.class));
		then(webhookAlertService).should().reserveDueAlertIds(any(LocalDateTime.class), eq(50));
		then(webhookAlertService).should(never()).deliver(anyLong(), any(LocalDateTime.class));
	}
}
