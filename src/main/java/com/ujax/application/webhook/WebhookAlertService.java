package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertLog;
import com.ujax.domain.webhook.WebhookAlertLogActorType;
import com.ujax.domain.webhook.WebhookAlertLogEventType;
import com.ujax.domain.webhook.WebhookAlertLogRepository;
import com.ujax.domain.webhook.WebhookAlertRepository;
import com.ujax.domain.webhook.WebhookAlertStatus;
import com.ujax.domain.workspace.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebhookAlertService {

	private static final int RETRY_DELAY_MINUTES = 1;
	private static final int STUCK_PROCESSING_MINUTES = 10;

	private final WebhookAlertRepository webhookAlertRepository;
	private final WebhookAlertLogRepository webhookAlertLogRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WebhookSender webhookSender;

	@Transactional
	public void reserveOrUpdate(Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt, Long actorId) {
		Optional<WebhookAlert> optionalAlert = webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId);
		if (optionalAlert.isEmpty()) {
			WebhookAlert createdAlert = webhookAlertRepository.save(
				WebhookAlert.create(workspaceProblemId, workspaceId, scheduledAt)
			);
			saveLog(
				createdAlert,
				WebhookAlertLogEventType.CREATED,
				null,
				WebhookAlertStatus.PENDING,
				null,
				null,
				null,
				WebhookAlertLogActorType.USER,
				actorId
			);
			return;
		}

		WebhookAlert alert = optionalAlert.get();
		WebhookAlertStatus fromStatus = alert.getStatus();
		alert.applyScheduleUpdate(scheduledAt);
		webhookAlertRepository.save(alert);
		saveLog(
			alert,
			fromStatus == WebhookAlertStatus.PROCESSING
				? WebhookAlertLogEventType.DEFERRED_SCHEDULED
				: WebhookAlertLogEventType.SCHEDULE_UPDATED,
			fromStatus,
			alert.getStatus(),
			null,
			null,
			null,
			WebhookAlertLogActorType.USER,
			actorId
		);
	}

	@Transactional
	public void deactivate(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> deleteWithLog(
				alert,
				WebhookAlertLogEventType.DEACTIVATED,
				WebhookAlertLogActorType.USER,
				actorId,
				null,
				null,
				null
			));
	}

	@Transactional
	public void cancel(Long workspaceProblemId, Long actorId) {
		webhookAlertRepository.findByWorkspaceProblemId(workspaceProblemId)
			.ifPresent(alert -> deleteWithLog(
				alert,
				WebhookAlertLogEventType.CANCELLED,
				WebhookAlertLogActorType.USER,
				actorId,
				null,
				null,
				null
			));
	}

	@Transactional
	public void recoverStuckProcessing(LocalDateTime now) {
		// TODO(issue-23):
		// 1) 오래된 PROCESSING alert 목록 조회
		// 2) nextScheduledAt 우선 적용 또는 now 기준 scheduledAt 복구
		// 3) status=PENDING 복귀
		// 4) RECOVERED 로그 적재
		throw new UnsupportedOperationException("TODO(issue-23): recover stuck processing alerts");
	}

	@Transactional
	public List<Long> reserveDueAlertIds(LocalDateTime now, int limit) {
		// TODO(issue-23):
		// 1) due PENDING alert 조회 (scheduledAt <= now, limit 적용)
		// 2) 각 alert를 PROCESSING으로 전환
		// 3) PROCESSING_STARTED 로그 적재
		// 4) reserved alert id 목록 반환
		throw new UnsupportedOperationException("TODO(issue-23): reserve due webhook alerts");
	}

	public void deliver(Long alertId, LocalDateTime now) {
		// TODO(issue-23):
		// 1) alert 존재 및 PROCESSING 상태 검증
		// 2) workspace hookUrl 조회 및 전송 가능 여부 검증
		// 3) nextScheduledAt 존재 시 HTTP 호출 없이 재예약 처리
		// 4) HTTP 호출 수행 (트랜잭션 외부)
		// 5) 결과 반영 직전 alert 재조회
		// 6) nextScheduledAt 재확인 후 재예약 우선 또는 성공/재시도/최종실패 반영
		// 7) 상태 변화에 맞는 로그 적재 및 필요 시 hard delete
		throw new UnsupportedOperationException("TODO(issue-23): deliver webhook alert");
	}

	private void deleteWithLog(
		WebhookAlert alert,
		WebhookAlertLogEventType eventType,
		WebhookAlertLogActorType actorType,
		Long actorId,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg
	) {
		saveLog(alert, eventType, alert.getStatus(), null, sendAt, lastAttemptAt, errMsg, actorType, actorId);
		webhookAlertRepository.delete(alert);
	}

	private void saveLog(
		WebhookAlert alert,
		WebhookAlertLogEventType eventType,
		WebhookAlertStatus fromStatus,
		WebhookAlertStatus toStatus,
		LocalDateTime sendAt,
		LocalDateTime lastAttemptAt,
		String errMsg,
		WebhookAlertLogActorType actorType,
		Long actorId
	) {
		WebhookAlertLog log = WebhookAlertLog.fromAlert(
			alert,
			eventType,
			fromStatus,
			toStatus,
			sendAt,
			lastAttemptAt,
			errMsg,
			actorType,
			actorId
		);
		webhookAlertLogRepository.save(log);
	}

}
