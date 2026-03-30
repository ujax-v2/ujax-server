package com.ujax.application.auth;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ujax.application.mail.UjaxMailTemplateRenderer;
import com.ujax.global.exception.common.ExternalApiException;

@Service
public class SignupVerificationMailer {

	private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender mailSender;
	private final String fromAddress;
	private final String fromName;

	public SignupVerificationMailer(
		JavaMailSender mailSender,
		@Value("${app.ujax.mail.from:noreply@ujax.site}") String fromAddress,
		@Value("${app.ujax.mail.name:UJAX}") String fromName
	) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
		try {
			var content = UjaxMailTemplateRenderer.renderSignupVerification(code, expiresAt.format(EXPIRY_FORMATTER));
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(email);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject("[UJAX] 회원가입 이메일 인증 코드");
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException ex) {
			throw new ExternalApiException("SMTP", "이메일 인증 메일 발송 중 오류가 발생했습니다.");
		}
	}
}
