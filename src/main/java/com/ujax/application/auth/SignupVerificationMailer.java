package com.ujax.application.auth;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ujax.application.mail.MailDeliveryRetryExecutor;
import com.ujax.application.mail.UjaxMailTemplateRenderer;

@Service
public class SignupVerificationMailer {

	private final JavaMailSender mailSender;
	private final MailDeliveryRetryExecutor mailDeliveryRetryExecutor;
	private final String fromAddress;
	private final String fromName;

	public SignupVerificationMailer(
		JavaMailSender mailSender,
		MailDeliveryRetryExecutor mailDeliveryRetryExecutor,
		@Value("${app.ujax.mail.from}") String fromAddress,
		@Value("${app.ujax.mail.name}") String fromName
	) {
		this.mailSender = mailSender;
		this.mailDeliveryRetryExecutor = mailDeliveryRetryExecutor;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
		mailDeliveryRetryExecutor.execute("이메일 인증 메일 발송 중 오류가 발생했습니다.", () -> {
			var content = UjaxMailTemplateRenderer.renderSignupVerification(code);
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(email);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject("[UJAX] 회원가입 인증 코드 - [ " + code + " ]");
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		});
	}
}
