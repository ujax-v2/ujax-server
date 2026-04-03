package com.ujax.application.mail.outbox;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.mail.RenderedMailContent;
import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxLog;
import com.ujax.domain.mail.MailOutboxLogEventType;
import com.ujax.domain.mail.MailOutboxLogRepository;
import com.ujax.domain.mail.MailOutboxRepository;
import com.ujax.domain.mail.MailOutboxStatus;
import com.ujax.domain.mail.MailType;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"app.ujax.mail.outbox.delivery.retry-delay-minutes=7",
	"app.ujax.mail.outbox.delivery.stuck-processing-minutes=10",
	"app.ujax.mail.outbox.delivery.max-attempts=2"
})
class MailOutboxServiceTest {

	@Autowired
	private MailOutboxService mailOutboxService;

	@Autowired
	private MailOutboxRepository mailOutboxRepository;

	@Autowired
	private MailOutboxLogRepository mailOutboxLogRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private UjaxSmtpMailSender ujaxSmtpMailSender;

	@BeforeEach
	void setUp() {
		mailOutboxLogRepository.deleteAllInBatch();
		mailOutboxRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("메일 outbox batch reserve")
	class ReserveDueMailOutbox {

		@Test
		@DisplayName("due 순서대로 limit 만큼 PROCESSING 으로 선점한다")
		void reserveDueMailOutboxIds() throws Exception {
			MailOutbox first = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"first@example.com",
				"111111",
				LocalDateTime.of(2026, 4, 2, 10, 0)
			));
			MailOutbox second = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"second@example.com",
				"222222",
				LocalDateTime.of(2026, 4, 2, 10, 5)
			));
			mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"later@example.com",
				"333333",
				LocalDateTime.of(2026, 4, 2, 10, 30)
			));

			List<Long> reservedIds = mailOutboxService.reserveDueMailOutboxIds(
				LocalDateTime.of(2026, 4, 2, 10, 10),
				2
			);
			List<MailOutboxLog> logs = mailOutboxLogRepository.findAll();

			assertThat(reservedIds).containsExactly(first.getId(), second.getId());
			assertThat(mailOutboxRepository.findAllById(reservedIds))
				.extracting(MailOutbox::getStatus)
				.containsOnly(MailOutboxStatus.PROCESSING);
			assertThat(logs)
				.extracting(MailOutboxLog::getEventType)
				.containsOnly(MailOutboxLogEventType.PROCESSING_STARTED);
			assertThat(logs)
				.extracting(MailOutboxLog::getToStatus)
				.containsOnly(MailOutboxStatus.PROCESSING);
		}
	}

	@Nested
	@DisplayName("메일 outbox stuck processing 복구")
	class RecoverStuckProcessing {

		@Test
		@DisplayName("오래된 PROCESSING row 를 다시 PENDING 으로 돌린다")
		void recoverStuckProcessing() throws Exception {
			MailOutbox stuck = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"stuck@example.com",
				"111111",
				LocalDateTime.of(2026, 4, 2, 10, 0)
			));
			stuck.markProcessing();
			mailOutboxRepository.saveAndFlush(stuck);

			LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 4, 2, 9, 0);
			jdbcTemplate.update(
				"UPDATE mail_outbox SET updated_at = ? WHERE mail_outbox_id = ?",
				oldUpdatedAt,
				stuck.getId()
			);

			mailOutboxService.recoverStuckProcessing(LocalDateTime.of(2026, 4, 2, 9, 30));

			MailOutbox recovered = mailOutboxRepository.findById(stuck.getId()).orElseThrow();
			MailOutboxLog log = mailOutboxLogRepository.findAll().get(0);
			assertThat(recovered.getStatus()).isEqualTo(MailOutboxStatus.PENDING);
			assertThat(recovered.getNextAttemptAt()).isEqualTo(LocalDateTime.of(2026, 4, 2, 9, 30));
			assertThat(log.getEventType()).isEqualTo(MailOutboxLogEventType.RECOVERED);
			assertThat(log.getFromStatus()).isEqualTo(MailOutboxStatus.PROCESSING);
			assertThat(log.getToStatus()).isEqualTo(MailOutboxStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("메일 outbox 전송")
	class Deliver {

		@Test
		@DisplayName("전송 성공 시 SENT 로 완료한다")
		void deliver_Success() throws Exception {
			MailOutbox outbox = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"user@example.com",
				"123456",
				LocalDateTime.of(2026, 4, 2, 10, 0)
			));
			outbox.markProcessing();
			mailOutboxRepository.saveAndFlush(outbox);

			LocalDateTime now = LocalDateTime.of(2026, 4, 2, 10, 1);
			mailOutboxService.deliver(outbox.getId(), now);

			MailOutbox delivered = mailOutboxRepository.findById(outbox.getId()).orElseThrow();
			MailOutboxLog log = mailOutboxLogRepository.findAll().get(0);
			assertThat(delivered.getStatus()).isEqualTo(MailOutboxStatus.SENT);
			assertThat(delivered.getSentAt()).isEqualTo(now);
			assertThat(log.getEventType()).isEqualTo(MailOutboxLogEventType.SENT);
			assertThat(log.getFromStatus()).isEqualTo(MailOutboxStatus.PROCESSING);
			assertThat(log.getToStatus()).isEqualTo(MailOutboxStatus.SENT);
			assertThat(log.getSentAt()).isEqualTo(now);
			then(ujaxSmtpMailSender).should().send(
				eq("user@example.com"),
				eq("[UJAX] 회원가입 인증 코드 - [ 123456 ]"),
				any(RenderedMailContent.class)
			);
		}

		@Test
		@DisplayName("전송 실패 시 재시도 가능한 경우 PENDING 으로 되돌린다")
		void deliver_SchedulesRetryOnFailure() throws Exception {
			MailOutbox outbox = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"user@example.com",
				"123456",
				LocalDateTime.of(2026, 4, 2, 10, 0)
			));
			outbox.markProcessing();
			mailOutboxRepository.saveAndFlush(outbox);
			willThrow(new RuntimeException("smtp timeout")).given(ujaxSmtpMailSender)
				.send(anyString(), anyString(), any(RenderedMailContent.class));

			LocalDateTime now = LocalDateTime.of(2026, 4, 2, 10, 1);
			mailOutboxService.deliver(outbox.getId(), now);

			MailOutbox retried = mailOutboxRepository.findById(outbox.getId()).orElseThrow();
			MailOutboxLog log = mailOutboxLogRepository.findAll().get(0);
			assertThat(retried.getStatus()).isEqualTo(MailOutboxStatus.PENDING);
			assertThat(retried.getNextAttemptAt()).isEqualTo(now.plusMinutes(7));
			assertThat(retried.getLastError()).isEqualTo("smtp timeout");
			assertThat(log.getEventType()).isEqualTo(MailOutboxLogEventType.RETRY_SCHEDULED);
			assertThat(log.getFromStatus()).isEqualTo(MailOutboxStatus.PROCESSING);
			assertThat(log.getToStatus()).isEqualTo(MailOutboxStatus.PENDING);
			assertThat(log.getLastError()).isEqualTo("smtp timeout");
		}

		@Test
		@DisplayName("전송 실패 시 최대 시도 횟수를 넘기면 FAILED 로 종료한다")
		void deliver_MarksFailedWhenRetryExhausted() throws Exception {
			MailOutbox outbox = mailOutboxRepository.saveAndFlush(createSignupOutbox(
				"user@example.com",
				"123456",
				LocalDateTime.of(2026, 4, 2, 10, 0)
			));
			outbox.markProcessing();
			outbox.scheduleRetry(LocalDateTime.of(2026, 4, 2, 10, 5), "smtp timeout");
			outbox.markProcessing();
			mailOutboxRepository.saveAndFlush(outbox);
			willThrow(new RuntimeException("smtp rejected")).given(ujaxSmtpMailSender)
				.send(anyString(), anyString(), any(RenderedMailContent.class));

			mailOutboxService.deliver(outbox.getId(), LocalDateTime.of(2026, 4, 2, 10, 6));

			MailOutbox failed = mailOutboxRepository.findById(outbox.getId()).orElseThrow();
			MailOutboxLog log = mailOutboxLogRepository.findAll().get(0);
			assertThat(failed.getStatus()).isEqualTo(MailOutboxStatus.FAILED);
			assertThat(failed.getLastError()).isEqualTo("smtp rejected");
			assertThat(log.getEventType()).isEqualTo(MailOutboxLogEventType.FAILED);
			assertThat(log.getFromStatus()).isEqualTo(MailOutboxStatus.PROCESSING);
			assertThat(log.getToStatus()).isEqualTo(MailOutboxStatus.FAILED);
			assertThat(log.getLastError()).isEqualTo("smtp rejected");
		}
	}

	private MailOutbox createSignupOutbox(String email, String code, LocalDateTime nextAttemptAt)
		throws JsonProcessingException {
		String payloadJson = objectMapper.writeValueAsString(
			new SignupVerificationMailPayload(code, nextAttemptAt.plusMinutes(5))
		);
		return MailOutbox.create(MailType.SIGNUP_VERIFICATION, email, payloadJson, nextAttemptAt);
	}
}
