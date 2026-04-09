package com.ujax.infrastructure.external.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;

import com.ujax.application.mail.template.RenderedMailContent;
import com.ujax.global.exception.common.ExternalApiException;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SuppressWarnings("resource")
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SmtpMailSenderTest {

	@Mock
	private JavaMailSender mailSender;

	private SmtpMailSender smtpMailSender;
	private RenderedMailContent content;

	@BeforeEach
	void setUp() {
		smtpMailSender = new SmtpMailSender(mailSender, "no-reply@ujax.com", "UJAX");
		content = new RenderedMailContent("plain text", "<p>html</p>");
	}

	@Nested
	@DisplayName("메일 발송")
	class Send {

		@Test
		@DisplayName("전송 성공 시 MimeMessage를 보낸다")
		void sendSuccess() throws Exception {
			// given
			given(mailSender.createMimeMessage()).willReturn(createMimeMessage());
			ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

			// when
			smtpMailSender.send("user@example.com", "[UJAX] 회원가입 인증 코드", content);

			// then
			then(mailSender).should().send(messageCaptor.capture());
			MimeMessage sentMessage = messageCaptor.getValue();
			assertThat(sentMessage.getAllRecipients())
				.isNotNull()
				.extracting(Object::toString)
				.containsExactly("user@example.com");
			assertThat(sentMessage.getFrom())
				.isNotNull()
				.hasSize(1);
			assertThat(sentMessage.getFrom()[0].toString()).contains("no-reply@ujax.com");
		}

		@Test
		@DisplayName("SMTP 인증 실패 시 warn 로그를 남기고 상세 메시지로 감싼다")
		void sendFailsWithAuthentication(CapturedOutput output) {
			// given
			given(mailSender.createMimeMessage()).willReturn(createMimeMessage());
			willThrow(new MailAuthenticationException("535 5.7.8 Authentication credentials invalid"))
				.given(mailSender)
				.send(any(MimeMessage.class));

			// when & then
			assertThatThrownBy(() -> smtpMailSender.send("user@example.com", "[UJAX] 회원가입 인증 코드", content))
				.isInstanceOf(ExternalApiException.class)
				.hasMessageContaining("메일 서버 인증에 실패했습니다.");
			assertThat(output.getOut())
				.contains("SMTP 인증 실패")
				.contains("recipientEmail=user@example.com");
		}

		@Test
		@DisplayName("MailSendException 발생 시 warn 로그를 남기고 SMTP 오류로 감싼다")
		void sendFailsWithMailSendException(CapturedOutput output) {
			// given
			given(mailSender.createMimeMessage()).willReturn(createMimeMessage());
			willThrow(new MailSendException("550 mailbox unavailable"))
				.given(mailSender)
				.send(any(MimeMessage.class));

			// when & then
			assertThatThrownBy(() -> smtpMailSender.send("user@example.com", "[UJAX] 회원가입 인증 코드", content))
				.isInstanceOf(ExternalApiException.class)
				.hasMessageContaining("메일 전송 중 SMTP 오류가 발생했습니다.");
			assertThat(output.getOut())
				.contains("SMTP 메일 전송 실패")
				.contains("recipientEmail=user@example.com")
				.contains("550 mailbox unavailable");
		}

		@Test
		@DisplayName("일반 MailException 발생 시 warn 로그를 남기고 공통 오류로 감싼다")
		void sendFailsWithMailException(CapturedOutput output) {
			// given
			given(mailSender.createMimeMessage()).willReturn(createMimeMessage());
			willThrow(new MailParseException("mime parse failed"))
				.given(mailSender)
				.send(any(MimeMessage.class));

			// when & then
			assertThatThrownBy(() -> smtpMailSender.send("user@example.com", "[UJAX] 회원가입 인증 코드", content))
				.isInstanceOf(ExternalApiException.class)
				.hasMessageContaining("메일 발송 중 오류가 발생했습니다.");
			assertThat(output.getOut())
				.contains("SMTP 메일 발송 실패")
				.contains("recipientEmail=user@example.com");
		}
	}

	private MimeMessage createMimeMessage() {
		return new MimeMessage(Session.getInstance(new Properties()));
	}
}
