package com.ujax.application.mail.outbox;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class MailOutboxSchedulerTest {

	@Test
	@DisplayName("scheduler 는 recover -> reserve -> deliver 순서로 batch 를 실행한다")
	void runMailOutboxBatch() {
		MailOutboxService mailOutboxService = mock(MailOutboxService.class);
		given(mailOutboxService.reserveDueMailOutboxIds(any(), eq(25))).willReturn(List.of(1L, 2L));
		MailOutboxScheduler scheduler = new MailOutboxScheduler(
			mailOutboxService,
			new MailOutboxSchedulerProperties(25)
		);

		scheduler.runMailOutboxBatch();

		InOrder inOrder = inOrder(mailOutboxService);
		inOrder.verify(mailOutboxService).recoverStuckProcessing(any());
		inOrder.verify(mailOutboxService).reserveDueMailOutboxIds(any(), eq(25));
		inOrder.verify(mailOutboxService).deliver(eq(1L), any());
		inOrder.verify(mailOutboxService).deliver(eq(2L), any());
	}
}
