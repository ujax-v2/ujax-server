package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
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
import com.ujax.domain.webhook.WebhookAlertLog;
import com.ujax.domain.webhook.WebhookAlertLogActorType;
import com.ujax.domain.webhook.WebhookAlertLogEventType;
import com.ujax.domain.webhook.WebhookAlertLogRepository;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
class WebhookAlertServiceTest {

	private static final Long WORKSPACE_PROBLEM_ID = 101L;
	private static final Long WORKSPACE_ID = 11L;
	private static final Long ACTOR_ID = 1L;

	@Mock
	private WebhookAlertRepository webhookAlertRepository;

	@Mock
	private WebhookAlertLogRepository webhookAlertLogRepository;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Mock
	private WebhookSender webhookSender;

	@InjectMocks
	private WebhookAlertService webhookAlertService;

	private final ArgumentCaptor<WebhookAlert> alertCaptor = ArgumentCaptor.forClass(WebhookAlert.class);
	private final ArgumentCaptor<WebhookAlertLog> logCaptor = ArgumentCaptor.forClass(WebhookAlertLog.class);

	@Nested
	@DisplayName("reserveOrUpdate")
	class ReserveOrUpdate {

		@Test
		@DisplayName("활성 alert가 없으면 CREATED 로그와 함께 생성한다")
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

			webhookAlertService.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, scheduledAt, ACTOR_ID);

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

			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"webhookAlertId",
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"actorType",
				"actorId"
			).containsExactly(
				1L,
				WebhookAlertLogEventType.CREATED,
				null,
				WebhookAlertStatus.PENDING,
				scheduledAt,
				WebhookAlertLogActorType.USER,
				ACTOR_ID
			);
		}

		@Test
		@DisplayName("PENDING alert면 즉시 schedule을 갱신하고 SCHEDULE_UPDATED 로그를 남긴다")
		void reserveOrUpdateUpdatesPendingAlert() {
			WebhookAlert alert = createPendingAlert();
			LocalDateTime rescheduledAt = LocalDateTime.of(2026, 3, 8, 11, 0);
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertService.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, rescheduledAt, ACTOR_ID);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(rescheduledAt, null, WebhookAlertStatus.PENDING, 0);

			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"nextScheduledAt",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.SCHEDULE_UPDATED,
				WebhookAlertStatus.PENDING,
				WebhookAlertStatus.PENDING,
				rescheduledAt,
				null,
				WebhookAlertLogActorType.USER,
				ACTOR_ID
			);
		}

		@Test
		@DisplayName("PROCESSING alert면 nextScheduledAt에 보류 저장하고 DEFERRED_SCHEDULED 로그를 남긴다")
		void reserveOrUpdateDefersWhenProcessing() {
			WebhookAlert alert = createProcessingAlert();
			LocalDateTime deferredAt = LocalDateTime.of(2026, 3, 8, 11, 0);
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertService.reserveOrUpdate(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, deferredAt, ACTOR_ID);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 9, 0),
					deferredAt,
					WebhookAlertStatus.PROCESSING,
					0
				);

			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"nextScheduledAt",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.DEFERRED_SCHEDULED,
				WebhookAlertStatus.PROCESSING,
				WebhookAlertStatus.PROCESSING,
				LocalDateTime.of(2026, 3, 8, 9, 0),
				deferredAt,
				WebhookAlertLogActorType.USER,
				ACTOR_ID
			);
		}
	}

	@Nested
	@DisplayName("deactivate / cancel")
	class DeactivateOrCancel {

		@Test
		@DisplayName("deactivate는 DEACTIVATED 로그 후 alert를 hard delete 한다")
		void deactivateDeletesAlertAfterLogging() {
			WebhookAlert alert = createPendingAlert();
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertService.deactivate(WORKSPACE_PROBLEM_ID, ACTOR_ID);

			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.DEACTIVATED,
				WebhookAlertStatus.PENDING,
				null,
				WebhookAlertLogActorType.USER,
				ACTOR_ID
			);
			then(webhookAlertRepository).should().delete(alert);
		}

		@Test
		@DisplayName("cancel은 CANCELLED 로그 후 alert를 hard delete 한다")
		void cancelDeletesAlertAfterLogging() {
			WebhookAlert alert = createProcessingAlert();
			given(webhookAlertRepository.findByWorkspaceProblemId(WORKSPACE_PROBLEM_ID))
				.willReturn(Optional.of(alert));

			webhookAlertService.cancel(WORKSPACE_PROBLEM_ID, ACTOR_ID);

			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.CANCELLED,
				WebhookAlertStatus.PROCESSING,
				null,
				WebhookAlertLogActorType.USER,
				ACTOR_ID
			);
			then(webhookAlertRepository).should().delete(alert);
		}
	}

	@Nested
	@DisplayName("recover / reserve")
	class RecoverOrReserve {

		@Test
		@DisplayName("stuck PROCESSING은 nextScheduledAt을 우선 적용해 복구한다")
		void recoverStuckProcessingUsesDeferredSchedule() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			WebhookAlert alert = createProcessingAlert();
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 8, 11, 0));
			given(webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
				eq(WebhookAlertStatus.PROCESSING),
				any(LocalDateTime.class)
			)).willReturn(List.of(alert));

			webhookAlertService.recoverStuckProcessing(now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.RECOVERED,
				WebhookAlertStatus.PROCESSING,
				WebhookAlertStatus.PENDING,
				LocalDateTime.of(2026, 3, 8, 11, 0),
				WebhookAlertLogActorType.SYSTEM,
				null
			);
		}

		@Test
		@DisplayName("stuck PROCESSING에 nextScheduledAt이 없으면 now 기준으로 복구한다")
		void recoverStuckProcessingUsesFallbackNowWhenDeferredScheduleMissing() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			WebhookAlert alert = createProcessingAlert();
			given(webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
				eq(WebhookAlertStatus.PROCESSING),
				any(LocalDateTime.class)
			)).willReturn(List.of(alert));

			webhookAlertService.recoverStuckProcessing(now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(now, null, WebhookAlertStatus.PENDING, 0);
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"actorType",
				"actorId"
			).containsExactly(
				WebhookAlertLogEventType.RECOVERED,
				WebhookAlertStatus.PROCESSING,
				WebhookAlertStatus.PENDING,
				now,
				WebhookAlertLogActorType.SYSTEM,
				null
			);
		}

		@Test
		@DisplayName("due alert reserve 시 PROCESSING_STARTED 로그를 남긴다")
		void reserveDueAlertIdsMarksProcessingAndWritesLog() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			WebhookAlert first = createPendingAlert(1L);
			WebhookAlert second = createPendingAlert(2L);
			given(webhookAlertRepository.findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
				WebhookAlertStatus.PENDING,
				now
			)).willReturn(List.of(first, second));

			List<Long> reservedIds = webhookAlertService.reserveDueAlertIds(now, 1);

			assertThat(reservedIds).containsExactly(1L);
			assertThat(first.getStatus()).isEqualTo(WebhookAlertStatus.PROCESSING);
			assertThat(second.getStatus()).isEqualTo(WebhookAlertStatus.PENDING);

				then(webhookAlertRepository).should().saveAll(List.of(first));
				then(webhookAlertLogRepository).should().save(logCaptor.capture());
				assertThat(logCaptor.getValue()).extracting(
					"webhookAlertId",
					"eventType",
					"fromStatus",
					"toStatus",
					"actorType",
					"actorId"
				).containsExactly(
					1L,
					WebhookAlertLogEventType.PROCESSING_STARTED,
					WebhookAlertStatus.PENDING,
					WebhookAlertStatus.PROCESSING,
					WebhookAlertLogActorType.BATCH,
					null
				);
			}

		@Test
		@DisplayName("limit이 0 이하면 빈 목록을 반환하고 조회하지 않는다")
		void reserveDueAlertIdsReturnsEmptyWhenLimitIsZero() {
			List<Long> reservedIds = webhookAlertService.reserveDueAlertIds(
				LocalDateTime.of(2026, 3, 8, 10, 0),
				0
			);

			assertThat(reservedIds).isEmpty();
			then(webhookAlertRepository).shouldHaveNoInteractions();
			then(webhookAlertLogRepository).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("deliver")
	class Deliver {

		@Test
		@DisplayName("성공 시 DELIVERED 로그 후 alert를 hard delete 한다")
		void deliverDeletesAlertOnSuccess() {
			WebhookAlert alert = createProcessingAlert();
			ReflectionTestUtils.setField(alert, "id", 1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L))
				.willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));

			webhookAlertService.deliver(1L, now);

			then(webhookSender).should().send("https://hook.example.com", WORKSPACE_PROBLEM_ID, WORKSPACE_ID,
				LocalDateTime.of(2026, 3, 8, 9, 0));
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"webhookAlertId",
				"eventType",
				"fromStatus",
				"toStatus",
				"sendAt",
				"lastAttemptAt",
				"actorType"
			).containsExactly(
				1L,
				WebhookAlertLogEventType.DELIVERED,
				WebhookAlertStatus.PROCESSING,
				null,
				now,
				now,
				WebhookAlertLogActorType.BATCH
			);
			then(webhookAlertRepository).should().delete(alert);
		}

		@Test
		@DisplayName("재시도 가능 실패 시 RETRY_SCHEDULED 로그와 함께 PENDING으로 복귀한다")
		void deliverRetriesWhenAttemptRemaining() {
			WebhookAlert alert = createProcessingAlert();
			ReflectionTestUtils.setField(alert, "id", 1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L))
				.willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			willThrow(new RuntimeException("network timeout"))
				.given(webhookSender)
				.send(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));

			webhookAlertService.deliver(1L, now);

			assertThat(alert).extracting("status", "attemptNo", "scheduledAt")
				.containsExactly(WebhookAlertStatus.PENDING, 1, now.plusMinutes(1));
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"lastAttemptAt",
				"errMsg",
				"actorType"
			).containsExactly(
				WebhookAlertLogEventType.RETRY_SCHEDULED,
				WebhookAlertStatus.PROCESSING,
				WebhookAlertStatus.PENDING,
				now.plusMinutes(1),
				now,
				"network timeout",
				WebhookAlertLogActorType.BATCH
			);
		}

		@Test
		@DisplayName("전송 전 nextScheduledAt이 있으면 HTTP 호출 없이 재예약한다")
		void deliverReSchedulesBeforeHttpWhenDeferredScheduleExists() {
			WebhookAlert alert = createProcessingAlert();
			ReflectionTestUtils.setField(alert, "id", 1L);
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 8, 11, 0));
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert));

			webhookAlertService.deliver(1L, now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
			then(webhookSender).shouldHaveNoInteractions();
			then(webhookAlertRepository).should().save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"scheduledAt",
				"actorType"
			).containsExactly(
				WebhookAlertLogEventType.SCHEDULE_UPDATED,
				WebhookAlertStatus.PROCESSING,
				WebhookAlertStatus.PENDING,
				LocalDateTime.of(2026, 3, 8, 11, 0),
				WebhookAlertLogActorType.BATCH
			);
		}

		@Test
		@DisplayName("재시도 한도를 소진한 실패는 FAILED 로그 후 alert를 hard delete 한다")
		void deliverDeletesAlertWhenRetryExhausted() {
			WebhookAlert alert = createProcessingAlert(1L);
			exhaustAttempts(alert, LocalDateTime.of(2026, 3, 8, 9, 0));
			Workspace workspace = createWorkspaceWithHookUrl();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L))
				.willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			willThrow(new RuntimeException("final failure"))
				.given(webhookSender)
				.send(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));

			webhookAlertService.deliver(1L, now);

			then(webhookAlertRepository).should(never()).save(alert);
			then(webhookAlertLogRepository).should().save(logCaptor.capture());
			assertThat(logCaptor.getValue()).extracting(
				"eventType",
				"fromStatus",
				"toStatus",
				"lastAttemptAt",
				"errMsg",
				"actorType"
			).containsExactly(
				WebhookAlertLogEventType.FAILED,
				WebhookAlertStatus.PROCESSING,
				null,
				now,
				"final failure",
				WebhookAlertLogActorType.BATCH
			);
			then(webhookAlertRepository).should().delete(alert);
		}
	}

	private WebhookAlert createPendingAlert() {
		return createPendingAlert(null);
	}

	private WebhookAlert createPendingAlert(Long id) {
		WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, LocalDateTime.of(2026, 3, 8, 9, 0));
		if (id != null) {
			ReflectionTestUtils.setField(alert, "id", id);
		}
		return alert;
	}

	private WebhookAlert createProcessingAlert() {
		return createProcessingAlert(null);
	}

	private WebhookAlert createProcessingAlert(Long id) {
		WebhookAlert alert = createPendingAlert(id);
		alert.markProcessing();
		return alert;
	}

	private Workspace createWorkspaceWithHookUrl() {
		Workspace workspace = Workspace.create("워크스페이스", "소개");
		workspace.update(null, null, "https://hook.example.com", null);
		return workspace;
	}

	private void exhaustAttempts(WebhookAlert alert, LocalDateTime base) {
		for (int i = 0; i < WebhookAlert.MAX_ATTEMPT; i++) {
			alert.markRetry(base.plusMinutes(i + 1));
			if (i < WebhookAlert.MAX_ATTEMPT - 1) {
				alert.markProcessing();
			}
		}
		alert.markProcessing();
	}
}
