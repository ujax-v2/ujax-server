package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
class WebhookAlertDeliveryServiceTest {

	private static final Long WORKSPACE_PROBLEM_ID = 101L;
	private static final Long WORKSPACE_ID = 11L;

	@Mock
	private WebhookAlertRepository webhookAlertRepository;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Mock
	private WebhookAlertMessageResolver webhookAlertMessageResolver;

	@Mock
	private WebhookSender webhookSender;

	@Mock
	private WebhookAlertEventLogger webhookAlertEventLogger;

	private final WebhookAlertDeliveryProperties properties = new WebhookAlertDeliveryProperties(1, 10, 5);

	private WebhookAlertDeliveryService webhookAlertDeliveryService;

	@BeforeEach
	void setUp() {
		webhookAlertDeliveryService = new WebhookAlertDeliveryService(
			webhookAlertRepository,
			workspaceRepository,
			webhookAlertMessageResolver,
			webhookSender,
			properties,
			webhookAlertEventLogger
		);
	}

	@Nested
	@DisplayName("recover / reserve")
	class RecoverOrReserve {

		@Test
		@DisplayName("stuck PROCESSING은 nextScheduledAt을 우선 적용해 복구한다")
		void recoverStuckProcessingUsesDeferredSchedule() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			WebhookAlert alert = createProcessingAlert(null);
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 8, 11, 0));
			given(webhookAlertRepository.findAllByStatusAndUpdatedAtBefore(
				eq(WebhookAlertStatus.PROCESSING),
				any(LocalDateTime.class)
			)).willReturn(List.of(alert));

			webhookAlertDeliveryService.recoverStuckProcessing(now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
			then(webhookAlertEventLogger).should().logRecovered(alert);
		}

		@Test
		@DisplayName("due alert reserve 시 PROCESSING_STARTED 이벤트를 남긴다")
		void reserveDueAlertIdsMarksProcessing() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			WebhookAlert first = createPendingAlert(1L);
			given(webhookAlertRepository.findDuePendingAlertsForUpdate(now, 1)).willReturn(List.of(first));

			List<Long> reservedIds = webhookAlertDeliveryService.reserveDueAlertIds(now, 1);

			assertThat(reservedIds).containsExactly(1L);
			assertThat(first.getStatus()).isEqualTo(WebhookAlertStatus.PROCESSING);
			then(webhookAlertEventLogger).should().logProcessingStarted(first, WebhookAlertStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("deliver")
	class Deliver {

		@Test
		@DisplayName("성공 시 DELIVERED 이벤트 후 alert를 삭제한다")
		void deliverDeletesAlertOnSuccess() {
			WebhookAlert alert = createProcessingAlert(1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			WebhookAlertMessage message = createMessage();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			given(webhookAlertMessageResolver.resolve(alert)).willReturn(message);

			webhookAlertDeliveryService.deliver(1L, now);

			then(webhookSender).should().send("https://hook.example.com", message);
			then(webhookAlertEventLogger).should().logDelivered(alert, now);
			then(webhookAlertRepository).should().delete(alert);
		}

		@Test
		@DisplayName("재시도 가능 실패 시 RETRY_SCHEDULED 이벤트와 함께 PENDING으로 복귀한다")
		void deliverRetriesWhenAttemptRemaining() {
			WebhookAlert alert = createProcessingAlert(1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			WebhookAlertMessage message = createMessage();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			given(webhookAlertMessageResolver.resolve(alert)).willReturn(message);
			willThrow(new ResourceAccessException("network timeout"))
				.given(webhookSender)
				.send(anyString(), any(WebhookAlertMessage.class));

			webhookAlertDeliveryService.deliver(1L, now);

			assertThat(alert).extracting("status", "attemptNo", "scheduledAt", "lastError")
				.containsExactly(WebhookAlertStatus.PENDING, 1, now.plusMinutes(1), "network timeout");
			then(webhookAlertEventLogger).should().logRetryScheduled(alert, WebhookAlertStatus.PROCESSING, now);
			then(webhookAlertRepository).should(never()).delete(alert);
		}

		@Test
		@DisplayName("전송 전 nextScheduledAt이 있으면 HTTP 호출 없이 재예약한다")
		void deliverReSchedulesBeforeHttpWhenDeferredScheduleExists() {
			WebhookAlert alert = createProcessingAlert(1L);
			alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 8, 11, 0));
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert));

			webhookAlertDeliveryService.deliver(1L, now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
			then(webhookSender).shouldHaveNoInteractions();
			then(webhookAlertEventLogger).should().logDeferredScheduleApplied(alert, now);
		}

		@Test
		@DisplayName("전송 후 nextScheduledAt이 생기면 성공 처리보다 재예약을 우선한다")
		void deliverReSchedulesAfterHttpWhenDeferredScheduleAppears() {
			WebhookAlert alert = createProcessingAlert(1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			WebhookAlertMessage message = createMessage();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			given(webhookAlertMessageResolver.resolve(alert)).willReturn(message);
			willAnswer(invocation -> {
				alert.applyScheduleUpdate(LocalDateTime.of(2026, 3, 8, 11, 0));
				return null;
			}).given(webhookSender).send(anyString(), any(WebhookAlertMessage.class));

			webhookAlertDeliveryService.deliver(1L, now);

			assertThat(alert).extracting("scheduledAt", "nextScheduledAt", "status", "attemptNo")
				.containsExactly(
					LocalDateTime.of(2026, 3, 8, 11, 0),
					null,
					WebhookAlertStatus.PENDING,
					0
				);
			then(webhookAlertEventLogger).should().logDeferredScheduleApplied(alert, now);
			then(webhookAlertEventLogger).should(never()).logDelivered(any(), any());
			then(webhookAlertRepository).should(never()).delete(alert);
		}

		@Test
		@DisplayName("non-retryable 실패는 FAILED 이벤트 후 alert를 삭제한다")
		void deliverDeletesAlertWhenFailureIsNonRetryable() {
			WebhookAlert alert = createProcessingAlert(1L);
			Workspace workspace = createWorkspaceWithHookUrl();
			WebhookAlertMessage message = createMessage();
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 10, 0);
			given(webhookAlertRepository.findById(1L)).willReturn(Optional.of(alert), Optional.of(alert));
			given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
			given(webhookAlertMessageResolver.resolve(alert)).willReturn(message);
			willThrow(HttpClientErrorException.create(
				HttpStatus.BAD_REQUEST,
				"bad request",
				HttpHeaders.EMPTY,
				new byte[0],
				StandardCharsets.UTF_8
			))
				.given(webhookSender)
				.send(anyString(), any(WebhookAlertMessage.class));

			webhookAlertDeliveryService.deliver(1L, now);

			then(webhookAlertEventLogger).should().logFailed(alert, now, "400 bad request", false);
			then(webhookAlertRepository).should().delete(alert);
		}
	}

	private WebhookAlert createPendingAlert(Long id) {
		WebhookAlert alert = WebhookAlert.create(WORKSPACE_PROBLEM_ID, WORKSPACE_ID, LocalDateTime.of(2026, 3, 8, 9, 0));
		if (id != null) {
			ReflectionTestUtils.setField(alert, "id", id);
		}
		return alert;
	}

	private WebhookAlert createProcessingAlert(Long id) {
		WebhookAlert alert = createPendingAlert(id);
		alert.markProcessing();
		return alert;
	}

	private Workspace createWorkspaceWithHookUrl() {
		Workspace workspace = Workspace.create("워크스페이스", "소개");
		workspace.update(null, null, "https://hook.example.com", null);
		ReflectionTestUtils.setField(workspace, "id", WORKSPACE_ID);
		return workspace;
	}

	private WebhookAlertMessage createMessage() {
		return new WebhookAlertMessage(
			WORKSPACE_PROBLEM_ID,
			WORKSPACE_ID,
			"워크스페이스",
			"1000. A+B",
			LocalDateTime.of(2026, 3, 8, 12, 0),
			LocalDateTime.of(2026, 3, 8, 9, 0),
			"https://ujax.site/ws/11/ide/1000"
		);
	}
}
