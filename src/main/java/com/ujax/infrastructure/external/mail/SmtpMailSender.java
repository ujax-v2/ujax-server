package com.ujax.infrastructure.external.mail;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ujax.application.mail.outbox.MailSender;
import com.ujax.application.mail.template.RenderedMailContent;
import com.ujax.global.exception.common.ExternalApiException;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SmtpMailSender implements MailSender {

	private static final String SMTP_SERVICE = "SMTP";

	private final JavaMailSender mailSender;
	private final String fromAddress;
	private final String fromName;

	public SmtpMailSender(
		JavaMailSender mailSender,
		@Value("${app.ujax.mail.from}") String fromAddress,
		@Value("${app.ujax.mail.name}") String fromName
	) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	@Override
	public void send(String recipientEmail, String subject, RenderedMailContent content) {
		try {
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(recipientEmail);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject(subject);
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		} catch (MailAuthenticationException exception) {
			log.warn("SMTP 인증 실패: recipientEmail={}", recipientEmail, exception);
			throw new ExternalApiException(
				SMTP_SERVICE,
				"메일 서버 인증에 실패했습니다.",
				exception
			);
		} catch (MailSendException exception) {
			log.warn(
				"SMTP 메일 전송 실패: recipientEmail={}, message={}",
				recipientEmail,
				exception.getMessage(),
				exception
			);
			throw new ExternalApiException(
				SMTP_SERVICE,
				"메일 전송 중 SMTP 오류가 발생했습니다.",
				exception
			);
		} catch (MailException exception) {
			log.warn("SMTP 메일 발송 실패: recipientEmail={}", recipientEmail, exception);
			throw new ExternalApiException(
				SMTP_SERVICE,
				"메일 발송 중 오류가 발생했습니다.",
				exception
			);
		} catch (MessagingException exception) {
			log.warn("메일 메시지 생성 실패: recipientEmail={}", recipientEmail, exception);
			throw new ExternalApiException(
				SMTP_SERVICE,
				"메일 메시지 생성 중 오류가 발생했습니다.",
				exception
			);
		} catch (UnsupportedEncodingException exception) {
			log.warn("메일 발신자 인코딩 실패: recipientEmail={}", recipientEmail, exception);
			throw new ExternalApiException(
				SMTP_SERVICE,
				"메일 발신자 정보 인코딩 중 오류가 발생했습니다.",
				exception
			);
		}
	}
}
