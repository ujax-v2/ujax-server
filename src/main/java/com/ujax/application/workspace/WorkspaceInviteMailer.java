package com.ujax.application.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceInviteMailer {

	private final JavaMailSender mailSender;
	private final String baseUrl;
	private final String fromAddress;

	public WorkspaceInviteMailer(
		JavaMailSender mailSender,
		@Value("${app.ujax.base-url:https://ujax.site}") String baseUrl,
		@Value("${app.ujax.mail.from:noreply@ujax.site}") String fromAddress
	) {
		this.mailSender = mailSender;
		this.baseUrl = baseUrl;
		this.fromAddress = fromAddress;
	}

	public void sendInvitation(String email, String workspaceName, Long workspaceId) {
		String link = String.format("%s/workspaces/%d", baseUrl, workspaceId);

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setFrom(fromAddress);
		message.setSubject("[UJAX] 워크스페이스 초대");
		message.setText(buildBody(workspaceName, link));

		mailSender.send(message);
	}

	private String buildBody(String workspaceName, String link) {
		return String.join("\n",
			"안녕하세요.",
			"",
			"UJAX에서 \"" + workspaceName + "\" 워크스페이스에 당신을 초대했습니다.",
			"아래 링크를 통해 워크스페이스로 이동할 수 있습니다.",
			link,
			"",
			"감사합니다.",
			"UJAX 팀"
		);
	}
}
