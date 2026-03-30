package com.ujax.application.workspace;

import java.io.UnsupportedEncodingException;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ujax.application.mail.UjaxMailTemplateRenderer;
import com.ujax.global.exception.common.ExternalApiException;

@Service
public class WorkspaceInviteMailer {

	private final JavaMailSender mailSender;
	private final String baseUrl;
	private final String fromAddress;
	private final String fromName;

	public WorkspaceInviteMailer(
		JavaMailSender mailSender,
		@Value("${app.ujax.base-url:https://ujax.site}") String baseUrl,
		@Value("${app.ujax.mail.from:noreply@ujax.site}") String fromAddress,
		@Value("${app.ujax.mail.name:UJAX}") String fromName
	) {
		this.mailSender = mailSender;
		this.baseUrl = baseUrl;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	public void sendInvitation(String email, String workspaceName, Long workspaceId) {
		String link = String.format("%s/workspaces/%d", baseUrl, workspaceId);

		try {
			var content = UjaxMailTemplateRenderer.renderWorkspaceInvitation(workspaceName, link);
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(email);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject("[UJAX] 워크스페이스 초대");
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException ex) {
			throw new ExternalApiException("SMTP", "워크스페이스 초대 메일 발송 중 오류가 발생했습니다.");
		}
	}
}
