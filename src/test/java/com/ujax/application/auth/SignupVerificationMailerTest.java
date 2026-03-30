package com.ujax.application.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.ujax.application.mail.MailDeliveryRetryExecutor;
import com.ujax.application.mail.MailDeliveryRetryProperties;

@ExtendWith(MockitoExtension.class)
class SignupVerificationMailerTest {

	@Mock
	private JavaMailSender mailSender;

	private SignupVerificationMailer signupVerificationMailer;

	@BeforeEach
	void setUp() {
		signupVerificationMailer = new SignupVerificationMailer(
			mailSender,
			new MailDeliveryRetryExecutor(new MailDeliveryRetryProperties(3, 0)),
			"no-reply@ujax.kro.kr",
			"UJAX"
		);
	}

	@Test
	@DisplayName("회원가입 인증 메일을 발송한다")
	void sendVerificationCode() throws Exception {
		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		signupVerificationMailer.sendVerificationCode(
			"user@example.com",
			"123456",
			LocalDateTime.parse("2026-03-30T10:30:00")
		);

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());
		MimeMessage message = captor.getValue();
		Address[] fromAddresses = message.getFrom();
		InternetAddress from = (InternetAddress)fromAddresses[0];
		Address[] recipients = message.getRecipients(Message.RecipientType.TO);
		InternetAddress to = (InternetAddress)recipients[0];

		assertThat(from.getAddress()).isEqualTo("no-reply@ujax.kro.kr");
		assertThat(from.getPersonal()).isEqualTo("UJAX");
		assertThat(message.getSubject()).isEqualTo("[UJAX] 회원가입 이메일 인증 코드");
		assertThat(to.getAddress()).isEqualTo("user@example.com");
		String rawMessage = rawMessage(message);
		assertThat(rawMessage)
			.contains("text/plain")
			.contains("text/html")
			.contains("123456")
			.contains("Verification Code");
	}

	@Test
	@DisplayName("회원가입 인증 메일은 SMTP 실패 시 재시도한다")
	void sendVerificationCodeRetriesOnTemporaryFailure() {
		when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
		doThrow(new MailSendException("temporary failure"))
			.doNothing()
			.when(mailSender)
			.send(any(MimeMessage.class));

		signupVerificationMailer.sendVerificationCode(
			"user@example.com",
			"123456",
			LocalDateTime.parse("2026-03-30T10:30:00")
		);

		verify(mailSender, times(2)).send(any(MimeMessage.class));
	}

	private String rawMessage(MimeMessage message) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		message.writeTo(outputStream);
		return outputStream.toString(StandardCharsets.UTF_8);
	}
}
