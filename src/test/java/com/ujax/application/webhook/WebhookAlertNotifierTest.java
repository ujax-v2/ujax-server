package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;

@ExtendWith(MockitoExtension.class)
class WebhookAlertNotifierTest {

	private static final Long WORKSPACE_PROBLEM_ID = 101L;
	private static final Long WORKSPACE_ID = 11L;
	private static final Long ACTOR_ID = 1L;

	@Mock
	private WebhookAlertRepository webhookAlertRepository;

	@Mock
	private WebhookAlertEventLogger webhookAlertEventLogger;

	@InjectMocks
	private WebhookAlertNotifier webhookAlertNotifier;

	private final ArgumentCaptor<WebhookAlert> alertCaptor = ArgumentCaptor.forClass(WebhookAlert.class);

	@Nested
	@DisplayName("reserveOrUpdate")
	class ReserveOrUpdate {

		@Test
		@DisplayName("활성 alert가 없으면 생성 후 created event를 남긴다")
		void reserveOrUpdateCreatesAlertWhenMissing() {
			LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.empty());
			given(webhookAlertRepository.save(any(WebhookAlert.class)))
				.willAnswer(invocation -> {
					WebhookAlert alert = invocation.getArgument(0, WebhookAlert.class);
					ReflectionTestUtils.setField(alert, "id", 1L);
					return alert;
				});

			webhookAlertNotifier.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, scheduledAt, ACTOR_ID);

			then(webhookAlertRepository).should().save(alertCaptor.capture());
			assertThat(alertCaptor.getValue()).extracting(
				"workspaceProblemId",
				"workspaceId",
				"scheduledAt",
				"status",
				"attemptNo"
			).containsExactly(
				WORKSPACE_PROBLEM_ID,
				WORKSPACE_ID,
				scheduledAt,
				WebhookAlertStatus.PENDING,
				0
			);
			then(webhookAlertEventLogger).should().logCreated(any(WebhookAlert.class), eq(ACTOR_ID));
		}

		@Test
		@DisplayName("PENDING alert면 즉시 schedule을 갱신한다")
		void reserveOrUpdateUpdatesPendingAlert() {
			WebhookAlert alert = createPendingAlert();
			LocalDateTime rescheduledAt = LocalDateTime.of(2026, 3, 8, 11, 0);
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertNotifier.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, rescheduledAt, ACTOR_ID);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(rescheduledAt, null, WebhookAlertStatus.PENDING, 0);
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertEventLogger).should().logScheduleUpdated(alert, WebhookAlertStatus.PENDING, ACTOR_ID);
		}

		@Test
		@DisplayName("PROCESSING alert면 nextScheduledAt에 보류 저장한다")
		void reserveOrUpdateDefersWhenProcessing() {
			WebhookAlert alert = createProcessingAlert();
			LocalDateTime deferredAt = LocalDateTime.of(2026, 3, 8, 11, 0);
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertNotifier.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, deferredAt, ACTOR_ID);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 9, 0),
					deferredAt,
					WebhookAlertStatus.PROCESSING,
					0
				);
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertEventLogger).should().logDeferredScheduled(alert, WebhookAlertStatus.PROCESSING, ACTOR_ID);
		}
	}

	@Nested
	@DisplayName("deactivate / cancel")
	class DeactivateOrCancel {

		@Test
		@DisplayName("deactivate는 로그 후 alert를 삭제한다")
		void deactivateDeletesAlert() {
			WebhookAlert alert = createPendingAlert();
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertNotifier.deactivate(WORKSPACE_PROBLEM_ID, ACTOR_ID);

			then(webhookAlertEventLogger).should().logDeactivated(alert, ACTOR_ID);
			then(webhookAlertRepository).should().delete(alert);
		}

		@Test
		@DisplayName("cancel은 로그 후 alert를 삭제한다")
		void cancelDeletesAlert() {
			WebhookAlert alert = createProcessingAlert();
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertNotifier.cancel(WORKSPACE_PROBLEM_ID, ACTOR_ID);

			then(webhookAlertEventLogger).should().logCancelled(alert, ACTOR_ID);
			then(webhookAlertRepository).should().delete(alert);
		}
	}

	private WebhookAlert createPendingAlert() {
		return WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, LocalDateTime.of(2026, 3, 8, 9, 0));
	}

	private WebhookAlert createProcessingAlert() {
		WebhookAlert alert = createPendingAlert();
		alert.markProcessing();
		return alert;
	}
}
