package com.ujax.application.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ujax.application.mail.MailDeliveryRetryExecutor;
import com.ujax.application.mail.UjaxMailTemplateRenderer;

@Service
public class WorkspaceInviteMailer {

	private final JavaMailSender mailSender;
	private final MailDeliveryRetryExecutor mailDeliveryRetryExecutor;
	private final String baseUrl;
	private final String fromAddress;
	private final String fromName;

	public WorkspaceInviteMailer(
		JavaMailSender mailSender,
		MailDeliveryRetryExecutor mailDeliveryRetryExecutor,
		@Value("${app.ujax.base-url}") String baseUrl,
		@Value("${app.ujax.mail.from}") String fromAddress,
		@Value("${app.ujax.mail.name}") String fromName
	) {
		this.mailSender = mailSender;
		this.mailDeliveryRetryExecutor = mailDeliveryRetryExecutor;
		this.baseUrl = baseUrl;
		this.fromAddress = fromAddress;
		this.fromName = fromName;
	}

	public void sendInvitation(String email, String workspaceName, Long workspaceId) {
		String link = String.format("%s/workspaces/%d", baseUrl, workspaceId);

		mailDeliveryRetryExecutor.execute("워크스페이스 초대 메일 발송 중 오류가 발생했습니다.", () -> {
			var content = UjaxMailTemplateRenderer.renderWorkspaceInvitation(workspaceName, link);
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(email);
			helper.setFrom(fromAddress, fromName);
			helper.setSubject(String.format("[UJAX] %s에서 당신을 초대했습니다.", workspaceName));
			helper.setText(content.plainText(), content.htmlText());
			mailSender.send(message);
		});
	}
}
