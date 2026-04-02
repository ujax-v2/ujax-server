package com.ujax.application.mail.outbox;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ujax.application.mail.RenderedMailContent;
import com.ujax.global.exception.common.ExternalApiException;

import jakarta.mail.MessagingException;

@Component
public class UjaxSmtpMailSender {

	private static final String SMTP_SERVICE = "SMTP";

	private final JavaMailSender mailSender;
	private final String fromAddress;
	private final String fromName;

	public UjaxSmtpMailSender(
		JavaMailSender mailSender,
		@Value("${app.ujax.mail.from}") String fromAddress,
		@Value("${app.ujax.mail.name}") String fromName
	) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	public void send(String recipientEmail, String subject, RenderedMailContent content) {
		try {
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(recipientEmail);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject(subject);
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException exception) {
			throw new ExternalApiException(SMTP_SERVICE, "메일 발송 중 오류가 발생했습니다.", exception);
		}
	}
}
